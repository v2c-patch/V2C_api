package org.monazilla.v2c;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.jar.JarOutputStream;
import java.util.zip.GZIPInputStream;
import javax.swing.JOptionPane;

public class V2CLauncher
  implements V2CUpdaterConstants
{
  private static final String sPropertyFileName = "v2cprops.txt";
  private static final String sSysLauncherPathKey = V2CApp.createPropertyName("launcher.path");
  private static final String sSysLauncherLocalPathKey = V2CApp.createPropertyName("launcher.localpath");
  private static final String sSysLauncherVerKey = V2CApp.createPropertyName("launcher.version");
  private static final String sSysLauncherDLLPathKey = V2CApp.createPropertyName("launcher.dll");
  private static final String sSysLauncherJavaCmdKey = V2CApp.createPropertyName("launcher.javacmd");
  private static final String sSysLauncherArchKey = V2CApp.createPropertyName("launcher.arch");
  private static final String sSysDirectoryKey = V2CApp.createPropertyName("directory");
  private static final String sSysPJREKey = V2CApp.createPropertyName("launcher.pjre");
  private static final String sMaxMemoryKey = "MaxHeapSize";
  private static final String sDirectoryKey = "Directory";
  private static final String sDLRelSeqNoKey = "DLRelSeqNo";
  private static final String sDLTestRelSeqNoKey = "DLTestRelSeqNo";
  private static final String sDLReleaseNameKey = "DLReleaseName";
  private static final String sDLTestReleaseNameKey = "DLTestReleaseName";
  private static final String sAuxVMOptionsKey = "AuxVMOptions";
  private static final int nMajorVersion;
  private static final int nMinorVersion;
  private static final String sV2CArgsFileName = "v2cargs.txt";
  private static final String sV2COldArgsFileName = "v2coldargs.txt";
  private static final int nUnixMajorVersion = 0;
  private static final int nUnixMinorVersion = 1;
  private static final String sV2CJarFileName = "v2cjar.txt";
  private static final String sV2CNewExecFileName = "v2cnewexec.bat";
  private static final String sInfoPlistFileName = "Info.plist";
  private static final String sPkgInfoFileName = "PkgInfo";
  private static final String sNewInfoPlistFileName = "NewInfo.plist";
  private static final String sOldInfoPlistFileName = "OldInfo.plist";
  private static final boolean bStartedByLauncher;
  private static final int MAX_MEMORY = 16384;
  private static File fApplicationDir;
  private static File fMacAppDir;
  private static File fLauncherDir;
  private static File fLocalLauncherDir;
  private static boolean bLocalLauncherDirDefined;
  private static File fPropertyDir;
  private static File fMacContentsDir;
  private static File fMacResourcesDir;
  private static File fJarDir;
  private static File fJar;
  private static V2CProperty vProperty;
  private static boolean bUseDLVerNext;
  private static boolean bLauncherDirPermissionChecked;
  private static boolean bLauncherDirPermissionOK;
  private static boolean bV2CJarFileChecked;

  static
  {
    String s = System.getProperty(sSysLauncherVerKey);
    int i = 0;
    int j = 0;
    boolean flag = s != null;
    if (flag) {
      int[] ai = V2CMiscUtil.parseVersionString(s);
      if (ai != null) {
        i = ai[0];
        j = ai[1];
      }
      if ((i == 0) && (j == 0))
        flag = false;
    }
    bStartedByLauncher = flag;
    nMajorVersion = i;
    nMinorVersion = j;
    findJarPath();
  }

  static V2CLauncherSP createLauncherSP(boolean flag)
  {
    if (!canWriteInLauncherDir()) {
      if (flag) {
        String s = fLocalLauncherDir + " に書き込めません。\n";
        if (V2CApp.isWindowsVista())
          s = s + "一旦V2Cを終了し、管理者として再度V2Cを起動してください。";
        else
          s = s + "書き込みパーミッションがあることを確認してください。";
        V2CSwingUtil.showErrorMessage(s);
      }
      return null;
    }
    File file = getProperyFile();
    if (file == null)
      return null;
    if ((file.exists()) && ((file.isDirectory()) || (!file.canWrite())) && (flag))
      V2CSwingUtil.showErrorMessage(file + " に書き込めません。\n設定が正常に保存されない可能性があります。");
    V2CProperty v2cproperty = getProperty();
    if (v2cproperty == null)
      return null;
    int i = v2cproperty.getInt("MaxHeapSize");
    if ((!flag) && (i <= 0))
      i = 256;
    boolean flag1 = V2CApp.isMacintosh();
    String s1 = flag1 ? v2cproperty.get("AuxVMOptions") : null;
    return new V2CLauncherSP(i, s1);
  }

  static void setInitialMaxMem()
  {
    if (V2CApp.isWinType())
      return;
    File file = getProperyFile();
    if ((file == null) || (file.exists())) {
      return;
    }

    V2CLauncherSP v2claunchersp = createLauncherSP(false);
    v2claunchersp.postConfig();
  }

  static void setLauncher()
  {
    V2CLauncherSP v2claunchersp = createLauncherSP(true);
    if (v2claunchersp != null)
      v2claunchersp.showDialog();
  }

  private static File getProperyFile()
  {
    File file = fPropertyDir;
    return file == null ? null : new File(file, "v2cprops.txt");
  }

  private static V2CProperty getProperty()
  {
    if (!bStartedByLauncher)
      return null;
    if (vProperty == null) {
      File file = getProperyFile();
      if (file == null)
        return null;
      vProperty = new V2CProperty(file, null, null);
    }
    return vProperty;
  }

  static boolean startedByLauncher()
  {
    return bStartedByLauncher;
  }

  static boolean shouldDownloadNewVersion()
  {
    return (bStartedByLauncher) && (!bLocalLauncherDirDefined);
  }

  static String getLauncherVersion()
  {
    return System.getProperty(sSysLauncherVerKey);
  }

  static String getLauncherJavaCmd()
  {
    return System.getProperty(sSysLauncherJavaCmdKey);
  }

  static boolean getStartedByInvocationAPI()
  {
    return (V2CApp.isWinType()) && (System.getProperty(sSysLauncherDLLPathKey) != null);
  }

  static String getLauncherArch()
  {
    return System.getProperty(sSysLauncherArchKey);
  }

  static boolean isPrivateJRE()
  {
    String s = System.getProperty(sSysPJREKey);
    return (s != null) && (s.length() > 0);
  }

  static File getApplicationDir()
  {
    return fApplicationDir;
  }

  static File getMacAppDir()
  {
    return fMacAppDir;
  }

  private static boolean canWriteInLauncherDir()
  {
    File file = fLocalLauncherDir;
    String s;
    if ((V2CApp.isWinType()) && (
      (V2CApp.isWindowsVista()) || (((s = System.getProperty("os.arch")) != null) && (s.indexOf("64") >= 0)))) {
      if (bLauncherDirPermissionChecked)
        return bLauncherDirPermissionOK;
      bLauncherDirPermissionChecked = true;
      File file1 = new File(file, "testfile");
      OutputStreamWriter outputstreamwriter = V2CLocalFileHandler.getOutputStreamWriter(file1, false);
      boolean flag = outputstreamwriter != null;
      if (flag) {
        bLauncherDirPermissionOK = true;
        V2CLocalFileHandler.closeWriter(outputstreamwriter);
        file1.delete();
      }
      return flag;
    }

    return (file != null) && (file.canWrite());
  }

  static void setUseDLVerNext(boolean flag)
  {
    bUseDLVerNext = flag;
    String s;
    if ((flag) && (!bV2CJarFileChecked) && (V2CApp.isWinType()) && (
      (V2CApp.isWindowsVista()) || (((s = System.getProperty("os.arch")) != null) && (s.indexOf("64") >= 0)))) {
      bV2CJarFileChecked = true;
      File file = new File(fLauncherDir, "v2cjar.txt");
      OutputStreamWriter outputstreamwriter = V2CLocalFileHandler.getOutputStreamWriter(file, true, "UTF-8");
      if (outputstreamwriter != null)
        V2CLocalFileHandler.closeWriter(outputstreamwriter);
      else
        V2CSwingUtil.showErrorMessage(
          "V2Cの更新ファイルを保存できませんでした。\n一旦V2Cを終了し、管理者としてV2Cを起動してから\n再度「次回起動から新バージョンを使用する」を\nチェックしてください。", "V2C更新エラー");
    }
  }

  static boolean getUseDLVerNext()
  {
    return bUseDLVerNext;
  }

  static void getMacIcnsFile(String s)
  {
    if ((!V2CApp.isMacintosh()) || (fMacResourcesDir == null))
      return;
    File file = new File(fMacResourcesDir, "V2C.icns");
    File file1 = new File(fMacResourcesDir, "V2C_new.icns");
    if (V2CMiscUtil2.getContentWithDialog(s, file, file1, "V2C.icns"))
      V2CSwingUtil.showInformationMessage("アプリケーションアイコンが設定されました。\n一旦ログアウト・ログインすると変更が反映されます。");
    else
      V2CSwingUtil.showErrorMessage("V2C.icnsのダウンロードに失敗しました。");
  }

  static String getAbsolutePathInJarDir(String s, boolean flag)
  {
    if (fJarDir == null)
      return null;
    File file = new File(fJarDir, s);
    if ((flag) || (file.exists())) {
      return file.getAbsolutePath();
    }
    return null;
  }

  static File getJarDir()
  {
    return fJarDir;
  }

  static URL getJarFileURL()
  {
    File file = fJar;
    if (file == null)
      return null;
    try {
      return file.toURI().toURL();
    } catch (Exception exception) {
    }
    return null;
  }

  private static void findJarPath()
  {
    URL url = V2CLauncher.class.getClassLoader().getResource("V2C.class");
    if (url == null)
      return;
    String s = url.getPath();
    try {
      s = URLDecoder.decode(s, "UTF-8");
    }
    catch (UnsupportedEncodingException unsupportedencodingexception) {
      return;
    }
    if ((s == null) || (!s.startsWith("file:")))
      return;
    int i = s.lastIndexOf(".jar!");
    if (i < 0)
      return;
    String s1 = s.substring(5, i + 4);
    File file = new File(s1);
    if (!file.exists())
      return;
    if (!file.isAbsolute()) {
      file = file.getAbsoluteFile();
      if ((file == null) || (!file.exists()))
        return;
    }
    fJar = file;
    fJarDir = file.getParentFile();
    if (bStartedByLauncher)
      if (V2CApp.isMacintosh()) {
        fMacResourcesDir = fJarDir.getParentFile();
        if (fMacResourcesDir != null) {
          fMacContentsDir = fMacResourcesDir.getParentFile();
          if (fMacContentsDir != null) {
            fMacAppDir = fMacContentsDir.getParentFile();
            if (fMacAppDir != null)
              fApplicationDir = fMacAppDir.getParentFile();
          }
        }
        fLocalLauncherDir = V2CLauncher.fLauncherDir = fMacResourcesDir;
        fPropertyDir = fMacResourcesDir;
      }
      else {
        fApplicationDir = fJarDir.getParentFile();
        fLauncherDir = fJarDir;
        File file1 = fJarDir;
        if (V2CApp.isUNIX()) {
          String s2 = System.getProperty(sSysLauncherLocalPathKey);
          if ((s2 != null) && (s2.length() > 0)) {
            file1 = new File(s2);
            if (!file1.isDirectory())
              file1.mkdirs();
            bLocalLauncherDirDefined = true;
          }
        }
        fLocalLauncherDir = file1;
        fPropertyDir = file1;
      }
  }

  static String getNextV2CDirectory()
  {
    V2CProperty v2cproperty = getProperty();
    if (v2cproperty == null) {
      return null;
    }
    return v2cproperty.get("Directory");
  }

  private static boolean setNextV2CDirectory(String s)
  {
    V2CProperty v2cproperty = getProperty();
    if (v2cproperty == null)
      return false;
    String s1 = v2cproperty.get("Directory");
    boolean flag = (s1 == null) || (!s1.equals(s));
    if (flag)
      v2cproperty.put("Directory", s);
    return flag;
  }

  private static void setV2CDirectoryFirst(String s)
  {
    if (s == null)
      return;
    String s1 = System.getProperty(sSysDirectoryKey);
    if ((s1 != null) && (s1.equals(s)))
      return;
    System.setProperty(sSysDirectoryKey, s);
    V2CProperty v2cproperty = getProperty();
    if (v2cproperty != null) {
      v2cproperty.put("Directory", s);
      updateV2CArgs();
    }
  }

  static String getV2CDirectory()
  {
    String s;
    while ((s = System.getProperty(sSysDirectoryKey)) == null)
    {
      String s1 = null;
      if ((V2CApp.isWinType()) && (isPrivateJRE())) {
        if (!canWriteInLauncherDir()) {
          String s2 = "<html><body><font size=+1>" + fLocalLauncherDir + " に書き込めません。<br>";
          if (V2CApp.isWindowsVista())
            s2 = s2 + "一旦V2Cを終了し、管理者として再度V2Cを起動してください。</body></html>";
          else
            s2 = s2 + "書き込みパーミッションがあることを確認してください。";
          V2CHTMLPane v2chtmlpane = new V2CHTMLPane(s2);
          Object[] aobj = { "終了" };
          int i = JOptionPane.showOptionDialog(null, v2chtmlpane, "V2Cエラー", -1, 0, null, aobj, aobj[0]);
          V2CApp.immediateExit(0);
        }
        String s3 = System.getProperty("user.home");
        if ((s3 != null) && (s3.length() >= 2) && (s3.charAt(1) == ':')) {
          String s4 = fLocalLauncherDir.getAbsolutePath();
          if (s4.startsWith(s3.charAt(0) + ":\\Program Files"))
            s1 = "";
        }
        if (s1 == null)
          s1 = ".";
      }
      else {
        V2CApp.setInitialLookAndFeel();
        s1 = V2CDirectorySP.showPanel();
      }
      if (s1 != null)
        setV2CDirectoryFirst(s1);
    }
    if (s.length() == 0)
      return null;
    if (s.equals(".")) {
      File file = fApplicationDir;
      return file == null ? null : file.getAbsolutePath();
    }

    return s;
  }

  private static String getNextReleaseName()
  {
    String s = null;
    if (bUseDLVerNext) {
      V2CProperty v2cproperty = getProperty();
      if (v2cproperty != null) {
        if (V2CUpdater.getCheckTestVersion())
          s = v2cproperty.get("DLTestReleaseName");
        if (s == null)
          s = v2cproperty.get("DLReleaseName");
      }
    }
    return s == null ? V2CReleaseInfo.getReleaseName() : s;
  }

  private static File getJarFileFromName(String s)
  {
    return getJarFileFromName(s, false);
  }

  private static File getJarFileFromName(String s, boolean flag)
  {
    return fJarDir == null ? null : new File(fJarDir, "V2C_" + s + (flag ? ".pack.gz" : ".jar"));
  }

  private static int[] getDLRelSeqNoArray()
  {
    if ((!bStartedByLauncher) || (fLauncherDir == null))
      return null;
    V2CProperty v2cproperty = getProperty();
    if (v2cproperty == null)
      return null;
    String s = null;
    String s1 = null;
    if (V2CUpdater.getCheckTestVersion()) {
      s = v2cproperty.get("DLTestReleaseName");
      s1 = v2cproperty.get("DLTestRelSeqNo");
    }
    if ((s == null) || (s1 == null)) {
      s = v2cproperty.get("DLReleaseName");
      s1 = v2cproperty.get("DLRelSeqNo");
    }
    if ((s == null) || (s1 == null) || (!getJarFileFromName(s).exists())) {
      return null;
    }

    int[] ai = V2CMiscUtil.parseDotSeparatedNumbers(s1, 3);
    return (ai == null) || (ai.length != 3) ? null : ai;
  }

  static boolean isNewVersionAvailable()
  {
    int[] ai = getDLRelSeqNoArray();
    return ai == null ? false : V2CReleaseInfo.isNewer(ai[0], ai[1], ai[2]);
  }

  static void downloadNewVersion(V2CProperty v2cproperty, boolean flag)
  {
    if ((!shouldDownloadNewVersion()) || (fLauncherDir == null))
      return;
    if (flag)
      System.out.println("バージョン情報のチェック中…");
    int i = v2cproperty.getInt("SequenceNumber");
    int j = v2cproperty.getInt("SubSequenceNumber");
    int k = v2cproperty.getInt("TestSequenceNumber");
    if (!V2CReleaseInfo.isNewer(i, j, k)) {
      if (flag)
        V2CUpdater.showErrorMessage("利用可能な更新はありません。", false);
      return;
    }
    int[] ai = getDLRelSeqNoArray();
    if (ai != null) {
      String s = null;
      if (V2CReleaseInfo.isEqual(i, j, k, ai[0], ai[1], ai[2]))
        s = "と同じ";
      else if (!V2CReleaseInfo.isNewer(i, j, k, ai[0], ai[1], ai[2]))
        s = "より新しい";
      if (s != null) {
        if (flag)
          V2CUpdater.showErrorMessage("ダウンロード予定" + s + "バージョンが既にダウンロードされています。", false);
        return;
      }
    }
    if (flag)
      V2CUpdater.showErrorMessage("V2Cの新しいバージョンをダウンロード中…", false);
    boolean flag1 = V2CApp.javaVersionEqualOrGreaterThan(1, 5);
    String s1 = v2cproperty.get("Name");
    String s2 = v2cproperty.get(flag1 ? "PackPath" : "Path");
    if (flag) {
      System.out.println("Name: " + s1);
      System.out.println("Path: " + s2);
    }
    if ((s1 == null) || ((s1.length() < 9) && (s1.length() > 10)) || (s2 == null) || (s2.length() == 0)) {
      if (flag)
        V2CUpdater.showErrorMessage("ファイル名またはパスが不正です。", true);
      return;
    }
    File file = getJarFileFromName(s1);
    if (file.equals(fJar)) {
      if (flag)
        V2CUpdater.showErrorMessage("ダウンロードするファイル名と同じ名前のファイルを使用中です（" + file + "）。", true);
      return;
    }
    File file1 = flag1 ? getJarFileFromName(s1, true) : file;
    BufferedOutputStream bufferedoutputstream = V2CLocalFileHandler.getBufferedOutputStream(file1);
    if (bufferedoutputstream == null) {
      if (flag)
        V2CUpdater.showErrorMessage(file1 + " の読み込みに失敗しました。", false);
      StringBuffer stringbuffer = new StringBuffer("V2Cの更新ファイルを保存できませんでした。\n");
      if (V2CApp.isWindowsVista()) {
        stringbuffer.append("一旦V2Cを終了し、管理者としてV2Cを起動してから");
      }
      else {
        stringbuffer.append(fLauncherDir);
        stringbuffer.append("\nに書き込みパーミッションがあることを確認してから");
      }
      stringbuffer.append("\n再度「ファイル」メニューの「V2Cの更新チェック…」を\n試してみてください。");
      V2CSwingUtil.showMessageLater(stringbuffer.toString(), "V2C更新エラー", 0);
      return;
    }
    String s3 = "http://v2.boxhost.me/jars/" + s2;
    if (!V2CHttpUtil.saveContentsToFile(s3, null, bufferedoutputstream)) {
      if (file1.exists())
        file1.delete();
      if (flag)
        V2CUpdater.showErrorMessage(s1 + " のダウンロードに失敗しました。ネットワークのログを確認してください。", true);
      return;
    }
    int l = v2cproperty.getInt(flag1 ? "PackLength" : "Length");
    if (file1.length() != l) {
      if (flag)
        V2CUpdater.showErrorMessage("ファイルサイズ不一致: " + file1.length() + "!=" + l, true);
      if (file1.exists())
        file1.delete();
      return;
    }
    String s4 = v2cproperty.get("DigestAlgorithm");
    if ((s4 == null) || (s4.length() == 0)) {
      if (flag)
        V2CUpdater.showErrorMessage("チェックサムのアルゴリズムが不明です。", true);
      file1.delete();
      return;
    }
    String s5 = V2CReleaseInfo.calcMessageDigest(file1, s4);
    if (s5 != null) { if (s5.equals(v2cproperty.get(flag1 ? "PackHashValue" : "HashValue"))); } else {
      if (flag)
        V2CUpdater.showErrorMessage("チェックサムが一致しませんでした。", true);
      file1.delete();
      return;
    }
    if (flag1) {
      JarOutputStream jaroutputstream = null;
      boolean flag3 = false;
      try {
        GZIPInputStream gzipinputstream = new GZIPInputStream(new FileInputStream(file1));
        jaroutputstream = new JarOutputStream(new FileOutputStream(file));
        V2CJ2SE5Util.unpackJarFile(gzipinputstream, jaroutputstream);
        flag3 = true;
      }
      catch (IOException ioexception) {
        if (flag)
          V2CUpdater.showErrorMessage("Unpack に失敗しました。", true);
        return;
      }
      finally {
        if (jaroutputstream != null)
          try {
            jaroutputstream.close();
          }
          catch (IOException localIOException2) {
          }
        file1.delete();
        if (!flag3)
          file.delete();
      }
    }
    boolean flag2 = V2CUpdater.getCheckTestVersion();
    int i1 = v2cproperty.getInt("TestSequenceNumber");
    V2CProperty v2cproperty1 = getProperty();
    String s6 = v2cproperty.getInt("SequenceNumber") + "." + v2cproperty.getInt("SubSequenceNumber") + "." + i1;
    if (flag2) {
      v2cproperty1.put("DLTestRelSeqNo", s6);
      v2cproperty1.put("DLTestReleaseName", s1);
      if (i1 == 0) {
        v2cproperty1.put("DLRelSeqNo", s6);
        v2cproperty1.put("DLReleaseName", s1);
      }
    }
    else {
      v2cproperty1.put("DLRelSeqNo", s6);
      v2cproperty1.put("DLReleaseName", s1);
    }
    if (flag)
      if (isNewVersionAvailable()) {
        V2CUpdater.showErrorMessage(s1 + "： ダウンロード完了。", false);
      }
      else {
        int[] ai1 = getDLRelSeqNoArray();
        String s7;
        if (ai1 != null) {
          System.out.println("getDLRelSeqNoArray(): " + ai1[0] + "," + ai1[1] + "," + ai1[2]);
          s7 = "のバージョン情報に問題があります。";
        }
        else {
          System.out.println("getDLRelSeqNoArray()==null");
          s7 = "からバージョン情報を取得できませんでした。";
        }
        V2CUpdater.showErrorMessage(s1 + "： ダウンロードファイル" + s7, true);
      }
  }

  static void separatorAdded(boolean flag)
  {
    if (!flag) {
      return;
    }

    V2CTwitterBBS.sConsumerSecret = "Q9LL4wX1MKSYnf4hzBRPDhlNelz90BHXBpKtf13ZteLL8sXQKj";
  }

  static void lastCheck()
  {
    if (!bStartedByLauncher)
      return;
    boolean flag = (!bLocalLauncherDirDefined) && (bUseDLVerNext) && (isNewVersionAvailable());
    if (flag)
      if (V2CApp.isMacintosh()) {
        updateV2CArgs();
      }
      else {
        V2CProperty v2cproperty = getProperty();
        if ((v2cproperty != null) && 
          (fLauncherDir != null))
        {
          if (!V2CLocalFileHandler.saveToFile(new File(fLauncherDir, "v2cjar.txt"), "V2C_" + getNextReleaseName() + 
            ".jar"))
            System.out.println("set new version failed !!"); 
        }
      }
    if (V2CApp.isMacintosh())
      maybeSetNewInfoPlist();
    if (((flag) && (V2CApp.isWinType())) || (V2CApp.getLastRelSeqNum() < V2CReleaseInfo.getReleaseSequenceNumber()))
      try {
        deleteOldJars();
      }
      catch (Exception localException)
      {
      }
  }

  static void deleteOldJars() {
    if ((bLocalLauncherDirDefined) || (fJarDir == null))
      return;
    File[] afile = fJarDir.listFiles(new FilenameFilter()
    {
      public boolean accept(File file1, String s)
      {
        return (s.startsWith("V2C_")) && (s.endsWith(".jar"));
      }
    });
    if ((afile == null) || (afile.length == 0))
      return;
    int i = V2CReleaseInfo.getUniqueReleaseSequenceNumber();
    ArrayList arraylist = new ArrayList();
    boolean flag = false;
    for (int j = 0; j < afile.length; j++) {
      File file = afile[j];
      int i1 = V2CLocalFileHandler.getV2CJarID(file);
      if (i1 > 0)
      {
        if (i1 == i) {
          flag = true;
        }
        else if (i1 < i)
          arraylist.add(new URSQNHolder(file, i1));
      }
    }
    if ((!flag) || (arraylist.size() < 2))
      return;
    Collections.sort(arraylist);
    int k = 2;
    int l = (i & 0xFF) != 0 ? 2 : 1;
    for (int j1 = arraylist.size() - 1; j1 >= 0; j1--) {
      URSQNHolder ursqnholder = (URSQNHolder)arraylist.get(j1);
      int k1 = ursqnholder.nURSQN;
      boolean flag1 = (k1 & 0xFF) == 0;
      if (flag1) {
        if (l > 0) {
          l--;
          continue;
        }
      }
      else if ((l > 0) && (k > 0)) {
        k--;
        continue;
      }
      ursqnholder.fJarFile.delete();
    }
  }

  private static int checkMaxMemory(int i)
  {
    if (i <= 0)
      return 0;
    if (i <= 32)
      return 32;
    if (i >= 16384) {
      return 16384;
    }
    return i;
  }

  static void setV2CArgs(String s, int i, String s1)
  {
    V2CProperty v2cproperty = getProperty();
    if (v2cproperty == null)
      return;
    boolean flag = false;
    if (setNextV2CDirectory(s))
      flag = true;
    i = checkMaxMemory(i);
    if (i != v2cproperty.getInt("MaxHeapSize")) {
      flag = true;
      if (i > 0)
        v2cproperty.putInt("MaxHeapSize", i);
      else
        v2cproperty.remove("MaxHeapSize");
    }
    String s2 = v2cproperty.get("AuxVMOptions");
    if ((s1 != null) && (s1.length() > 0)) {
      if ((s2 == null) || (!s1.equals(s2))) {
        flag = true;
        v2cproperty.put("AuxVMOptions", s1);
      }
    }
    else if ((s2 != null) && (s2.length() > 0)) {
      flag = true;
      v2cproperty.remove("AuxVMOptions");
    }
    if (flag)
      updateV2CArgs();
  }

  private static void updateV2CArgs()
  {
    if (V2CApp.isMacintosh())
      updateMacInfoPlist();
    else
      updateCommonV2CArgs();
  }

  private static void updateCommonV2CArgs()
  {
    File file = fLocalLauncherDir;
    if (file == null)
      return;
    V2CProperty v2cproperty = getProperty();
    if (v2cproperty == null)
      return;
    StringBuffer stringbuffer = new StringBuffer(1000);
    String s = v2cproperty.get("Directory");
    if (s != null) {
      boolean flag = (s.length() == 0) || (s.equals("."));
      boolean flag1 = V2CApp.isWinType();
      if ((flag1) && (!flag))
        stringbuffer.append('"');
      stringbuffer.append("-D");
      stringbuffer.append(sSysDirectoryKey);
      stringbuffer.append('=');
      appendEscapingSpace(stringbuffer, s);
      if ((flag1) && (!flag))
        stringbuffer.append('"');
      stringbuffer.append(' ');
    }
    int i = checkMaxMemory(v2cproperty.getInt("MaxHeapSize"));
    if (i > 0) {
      stringbuffer.append("-Xmx");
      stringbuffer.append(String.valueOf(i));
      stringbuffer.append("m ");
    }
    File file1 = new File(file, "v2coldargs.txt");
    File file2 = new File(file, "v2cargs.txt");
    if ((file1.exists()) && (!file1.delete())) {
      V2CSwingUtil.showMessageLater("v2coldargs.txt を削除できませんでした。", "V2C書き込みエラー", 0);
      return;
    }
    if ((file2.exists()) && (!file2.renameTo(file1))) {
      V2CSwingUtil.showMessageLater("v2cargs.txt を v2coldargs.txt に移動できませんでした。", "V2C書き込みエラー", 0);
      return;
    }
    if (!V2CLocalFileHandler.saveToFile(file2, stringbuffer.toString(), V2CApp.isWinType() ? "MS932" : "UTF-8"))
      V2CSwingUtil.showMessageLater("v2cargs.txt に書き込めませんでした。", "V2C書き込みエラー", 0);
  }

  private static void appendEscapingSpace(StringBuffer stringbuffer, String s)
  {
    if (s == null)
      return;
    if (V2CApp.isUNIX()) {
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if ((c == ' ') || (c == '"') || (c == '`') || (c == '$'))
          stringbuffer.append('\\');
        stringbuffer.append(c);
      }
    }
    else
    {
      stringbuffer.append(s);
    }
  }

  private static void appendEscapingXMLSpecialChars(StringBuffer stringbuffer, String s)
  {
    if (s == null)
      return;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '<') {
        stringbuffer.append("&lt;");
      }
      else if (c == '>') {
        stringbuffer.append("&gt;");
      }
      else if (c == '&') {
        stringbuffer.append("&amp;");
      }
      else if (c == '\'') {
        stringbuffer.append("&apos;");
      }
      else if (c == '"')
        stringbuffer.append("&quot;");
      else
        stringbuffer.append(c);
    }
  }

  private static void appendKeyStringPair(StringBuffer stringbuffer, int i, String s, String s1)
  {
    for (int j = 0; j < i; j++) {
      stringbuffer.append('\t');
    }
    stringbuffer.append("<key>");
    appendEscapingXMLSpecialChars(stringbuffer, s);
    stringbuffer.append("</key>\n");
    for (int k = 0; k < i; k++) {
      stringbuffer.append('\t');
    }
    stringbuffer.append("<string>");
    appendEscapingXMLSpecialChars(stringbuffer, s1);
    stringbuffer.append("</string>\n");
  }

  private static void updateMacInfoPlist()
  {
    if (fMacResourcesDir == null)
      return;
    V2CProperty v2cproperty = getProperty();
    if (v2cproperty == null)
      return;
    String s = getNextReleaseName();
    StringBuffer stringbuffer = new StringBuffer(3000);
    stringbuffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    stringbuffer
      .append("<!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
    stringbuffer.append("<plist version=\"1.0\">\n");
    stringbuffer.append("<dict>\n");
    appendKeyStringPair(stringbuffer, 1, "CFBundleAllowMixedLocalizations", "true");
    appendKeyStringPair(stringbuffer, 1, "CFBundleDevelopmentRegion", "Japanese");
    appendKeyStringPair(stringbuffer, 1, "CFBundleExecutable", "JavaApplicationStub");
    appendKeyStringPair(stringbuffer, 1, "CFBundleGetInfoString", "V2C " + s);
    appendKeyStringPair(stringbuffer, 1, "CFBundleIconFile", "V2C.icns");
    appendKeyStringPair(stringbuffer, 1, "CFBundleIdentifier", "org.monazilla.v2c");
    appendKeyStringPair(stringbuffer, 1, "CFBundleInfoDictionaryVersion", "6.0");
    appendKeyStringPair(stringbuffer, 1, "CFBundleName", "V2C");
    appendKeyStringPair(stringbuffer, 1, "CFBundlePackageType", "APPL");
    appendKeyStringPair(stringbuffer, 1, "CFBundleShortVersionString", s);
    appendKeyStringPair(stringbuffer, 1, "CFBundleSignature", "V2CJ");
    appendKeyStringPair(stringbuffer, 1, "CFBundleVersion", s);
    if (nMajorVersion == 0 && nMinorVersion == 5){
    	appendKeyStringPair(stringbuffer, 1, "NSHighResolutionCapable", "true");
    }
    stringbuffer.append("\t<key>Java</key>\n");
    stringbuffer.append("\t<dict>\n");
    if (nMajorVersion == 0 && nMinorVersion == 5){
    	appendKeyStringPair(stringbuffer, 2, "ClassPath", "$JAVAROOT/v2c_api_patch_mac.jar");
        appendKeyStringPair(stringbuffer, 2, "JVMVersion", "1.5+");
    } else {
    	appendKeyStringPair(stringbuffer, 2, "ClassPath", "$JAVAROOT/V2C_" + s + ".jar");
        appendKeyStringPair(stringbuffer, 2, "JVMVersion", "1.4+");
    }
    appendKeyStringPair(stringbuffer, 2, "MainClass", "V2C");
    stringbuffer.append("\t\t<key>Properties</key>\n");
    stringbuffer.append("\t\t<dict>\n");
    appendKeyStringPair(stringbuffer, 3, "apple.laf.useScreenMenuBar", "true");
    if (nMajorVersion == 0 && nMinorVersion == 5){
        appendKeyStringPair(stringbuffer, 3, sSysLauncherVerKey, "0.5");
    } else {
    	appendKeyStringPair(stringbuffer, 3, sSysLauncherVerKey, "0.3");
    }
    String s1 = v2cproperty.get("Directory");
    if (s1 != null)
      appendKeyStringPair(stringbuffer, 3, sSysDirectoryKey, s1);
    stringbuffer.append("\t\t</dict>\n");
    StringBuffer stringbuffer1 = new StringBuffer();
    int i = checkMaxMemory(v2cproperty.getInt("MaxHeapSize"));
    if (i > 0) {
      stringbuffer1.append("-Xmx");
      stringbuffer1.append(i);
      stringbuffer1.append('m');
    }
    String s2 = v2cproperty.get("AuxVMOptions");
    if ((s2 != null) && (s2.length() > 0)) {
      if (stringbuffer1.length() > 0)
        stringbuffer1.append(' ');
      stringbuffer1.append(s2);
    }
    if (stringbuffer1.length() > 0)
      appendKeyStringPair(stringbuffer, 2, "VMOptions", stringbuffer1.toString());
    stringbuffer.append("\t</dict>\n");
    stringbuffer.append("</dict>\n");
    stringbuffer.append("</plist>\n");
    File file = new File(fMacResourcesDir, "NewInfo.plist");
    V2CLocalFileHandler.saveToFile(file, stringbuffer.toString(), "UTF-8");
  }

  private static void maybeSetNewInfoPlist()
  {
    if ((!bStartedByLauncher) || (!V2CApp.isMacintosh()))
      return;
    if ((fMacResourcesDir == null) || (fMacContentsDir == null))
      return;
    File file = new File(fMacResourcesDir, "NewInfo.plist");
    if ((nMajorVersion == 0) && (nMinorVersion == 1)) {
      if ((!file.exists()) || (file.length() == 0L))
        updateMacInfoPlist();
      File file1 = new File(fMacContentsDir, "PkgInfo");
      String s = V2CLocalFileHandler.restoreFromFile(file1);
      if ((s == null) || (!s.equals("APPLV2CJ")))
        V2CLocalFileHandler.saveToFile(file1, "APPLV2CJ", "UTF-8");
    }
    if ((!file.exists()) || (file.length() == 0L)) {
      return;
    }

    File file2 = new File(fMacContentsDir, "Info.plist");
    File file3 = new File(fMacResourcesDir, "OldInfo.plist");
    file2.renameTo(file3);
    file.renameTo(file2);
  }

  private static class URSQNHolder
    implements Comparable
  {
    final File fJarFile;
    final int nURSQN;

    public int compareTo(Object obj)
    {
      return this.nURSQN - ((URSQNHolder)obj).nURSQN;
    }

    URSQNHolder(File file, int i)
    {
      this.fJarFile = file;
      this.nURSQN = i;
    }
  }
}