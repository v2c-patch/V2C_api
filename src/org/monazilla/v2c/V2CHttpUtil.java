package org.monazilla.v2c;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

@SuppressWarnings({"rawtypes", "unchecked"})
public class V2CHttpUtil {
	static final String sPreferIPv6AddressesKey = "PreferIPv6Addresses";
	private static final HashMap hmHosts = new HashMap();
	private static final HashMap hmKeySpecs = new HashMap();
	static final String sOtherUAName = "Mozilla/4.0 (compatible)";
	public static int nBufLen = 2048;
	private static int nInitialContentLength = 0x20000;
	private static int nMaxContentLength = 0x1000000;
	private static int iConnectTimeout;
	private static int iReadTimeout;
	public static boolean bPolipo;
	public static boolean bPolipoWarned;
	private static final byte blToken[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
			0, 1, 1, 0, 1, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 0};
	private static String sRequestTokenURL = "https://api.twitter.com/oauth/request_token";
	private static String sAuthorizeURL = "https://api.twitter.com/oauth/authorize";
	private static String sAccessTokenURL = "https://api.twitter.com/oauth/access_token";
	private static Mac vMac;
	static long nOAuthTimeOffset;
	static Random vRandom;

	private static final Pattern is2ch = Pattern
			.compile("http://([^\\.]*)(\\.2ch\\.net|\\.bbspink\\.com)/.*");
	private static final Pattern pattern = Pattern
			.compile("http://([^\\.]*)(\\.2ch\\.net|\\.bbspink\\.com)/([^/]*)/dat/([0-9]*)\\.dat");
	static Pattern html2dat = Pattern
			.compile("<dt>([0-9]*).+(<a href=\"(.+)\">(.+)<\\/a>|<font color=green>(.+)<\\/font>)：(.+)<dd>(.+)<br><br>");
	private static String sid;
	public static final V2CProperty apiProperty;

	public static boolean useSC;
	public static boolean useAPI;
	public static boolean useHTML;
	public static String HMKey = null;
	public static String AppKey = null;
	public static String UAName = null;
	public static String X2CHUA = null;
	public static String UAPost = null;
	public static String UAAuth = null;
        public static String DEFAULT_UA = "Monazilla/1.00 (V2C/" 
                + V2CReleaseInfo.getVersionOrName() + ")";
        public static String IE_UA = 
                "Mozilla/5.0 (Windows NT 6.3; WOW64; Trident/7.0; rv:11.0) like Gecko";

	static Thread SID_updater = null;
	
	static {
		V2CProperty v2cproperty = V2CMain.getUserProperty();
		HttpURLConnection.setFollowRedirects(false);
		iConnectTimeout = v2cproperty.getInt("HttpUtil.ConnectTimeout", 60) * 1000;
		iReadTimeout = v2cproperty.getInt("HttpUtil.ReadTimeout", 120) * 1000;
		Authenticator.setDefault(new DefAuth());
		File apiFile = new File(v2cproperty.fParentFolder.getAbsolutePath()
				+ "/2ch_api.txt");
		if (!apiFile.exists()) {
			try {
				apiFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		apiProperty = new V2CProperty(apiFile);
		useAPI = apiProperty.getBoolean("2chAPI.UseAPI");
		useSC = apiProperty.getBoolean("2chAPI.UseSC")
				&& !apiProperty.getBoolean("I.hate.Tarako");
		useHTML = apiProperty.getBoolean("2chAPI.UseHTML");
		HMKey = apiProperty.get("2chAPI.HMKey");
		AppKey = apiProperty.get("2chAPI.AppKey");
		UAName = apiProperty.get("2chAPI.UAName");
                if (null == UAName) {
                    UAName = DEFAULT_UA;
                }
		UAPost = apiProperty.get("2chAPI.UAPost");
                if (null == UAPost) {
                    UAName = DEFAULT_UA;
                }
		UAAuth = apiProperty.get("2chAPI.UAAuth");
                if (null == UAAuth){
                    UAAuth = UAName;
                }
		X2CHUA = apiProperty.get("2chAPI.X2chUA");
		SID_updater = new Thread() {
			public void run() {
				while (true) {
					if (V2CHttpUtil.useAPI) {
						System.out.println("now update SID.. ");
						V2CHttpUtil.login2ch(prev_userId, prev_passwd, null,
								false);
					}
					try {
						Thread.sleep(55 * 60 * 1000);
					} catch (InterruptedException ie) {

					}
				}
			}
		};
		SID_updater.setDaemon(true);
		SID_updater.start();
	}

	public static String getHMKey() {
		if (null != HMKey) {
			return HMKey;
		}
		if (apiProperty.get("2chAPI.HMKey") != null) {
			HMKey = apiProperty.get("2chAPI.HMKey");
		} else {
			HMKey = "INVALID_HM_KEY";
		}
		return HMKey;
	}
	
	public static String getUAName(boolean isAuth, boolean isPost, boolean is2ch) {
		if (!is2ch){
			return DEFAULT_UA;
		}
		if (useAPI) {
			if (isPost) {
				return UAPost;
			} else if (isAuth) {
                            return UAAuth;
                        }
			return UAName;
		} else if (useHTML){
			return IE_UA;
		} else {
			if (isAuth){
				return "DOLIB/1.00";
			}
			return DEFAULT_UA;
		}
	}

	public static String getX2chUA() {
		if (useAPI) {
			return X2CHUA;
		} else {
			return "V2C/" + V2CReleaseInfo.getVersionOrName();
		}
	}

	public static String getAppKey() {
		if (null != AppKey) {
			return AppKey;
		}
		if (apiProperty.get("2chAPI.AppKey") != null) {
			AppKey = apiProperty.get("2chAPI.AppKey");
		} else {
			AppKey = "INVALID_APP_KEY";
		}
		return AppKey;
	}

	public static class CAndC {

		HttpURLConnection getConnection() {
			return hURLConnection;
		}

		int getResponseCode() {
			if (hURLConnection == null)
				return 0;
			int i = 0;
			try {
				i = hURLConnection.getResponseCode();
			} catch (IOException ioexception) {
			}
			return i;
		}

		private boolean checkContentType(String s) {
			return V2CHttpUtil.checkContentType(getContentType(), s);
		}

		String contentType;
		String getContentType() {
			if (null != contentType) {
				return contentType;
			}
			return hURLConnection == null ? null : hURLConnection
					.getContentType();
		}

		public void setContents(String s) {
			sContents = s;
		}

		String getContents() {
			return sContents;
		}

		byte[] getRawContents() {
			return blContents;
		}

		String getErrorMessage() {
			return sErrorMessage;
		}

		public void setErrorMessage(String msg) {
			this.sErrorMessage = msg;
		}

		byte[] getErrorContents() {
			return blErrorContents;
		}

		Object getErrorJSONObject() {
			byte abyte0[] = blErrorContents;
			if (abyte0 == null || !checkContentType("application/json"))
				return null;
			try {
				return V2CJSONUtil.parse(abyte0);
			} catch (IllegalArgumentException illegalargumentexception) {
				illegalargumentexception.printStackTrace();
				return null;
			}
		}

		public boolean isError() {
			return bError;
		}

		public void setError(boolean isError) {
			this.bError = isError;
		}

		HttpURLConnection hURLConnection;
		String sContents;
		byte blContents[];
		String sErrorMessage;
		boolean bError;
		byte blErrorContents[];

		public CAndC(HttpURLConnection httpurlconnection) {
			hURLConnection = httpurlconnection;
		}

		CAndC(HttpURLConnection httpurlconnection, String s) {
			hURLConnection = httpurlconnection;
			sContents = s;
		}

		CAndC(HttpURLConnection httpurlconnection, byte abyte0[]) {
			hURLConnection = httpurlconnection;
			blContents = abyte0;
		}

		CAndC(String s, HttpURLConnection httpurlconnection) {
			this(s);
			hURLConnection = httpurlconnection;
		}

		CAndC(String s, HttpURLConnection httpurlconnection, byte abyte0[]) {
			this(s, httpurlconnection);
			blErrorContents = abyte0;
		}

		public CAndC(String s) {
			sErrorMessage = s;
			bError = true;
		}
	}
	static class DefAuth extends V2CJ2SE5Util.Auth2 {

		protected PasswordAuthentication getPasswordAuthentication() {
			InetAddress inetaddress = getRequestingSite();
			if (!bJ15 || !isServerType()) {
				return V2CProxySetting.getProxyPasswordAuth(inetaddress,
						getRequestingPort());
			} else {
				URL url = getRequestingURL();
				PasswordAuthentication passwordauthentication = V2CBoardItem
						.getPWAuth(url);
				return passwordauthentication;
			}
		}

		DefAuth() {
			super(V2CApp.javaVersionEqualOrGreaterThan(1, 5));
		}
	}

	public static class GZIPFilterInputStream extends FilterInputStream {

		public boolean markSupported() {
			return false;
		}

		public int read() throws IOException {
			int i = super.read();
			vThreadRes.addProgressValue(1);
			return i;
		}

		public int read(byte abyte0[]) throws IOException {
			int i = super.read(abyte0);
			if (i > 0)
				vThreadRes.addProgressValue(i);
			return i;
		}

		public int read(byte abyte0[], int i, int j) throws IOException {
			int k = super.read(abyte0, i, j);
			if (k > 0)
				vThreadRes.addProgressValue(k);
			return k;
		}

		public long skip(long l) throws IOException {
			return super.skip(l);
		}

		V2CThreadRes vThreadRes;

		public GZIPFilterInputStream(InputStream inputstream,
				V2CThreadRes v2cthreadres) {
			super(inputstream);
			vThreadRes = v2cthreadres;
		}
	}

	static class GetHTTPFileBG implements Runnable {

		boolean start() {
			Thread thread = new Thread(this);
			boolean flag = vProgressPanel.showPanel(thread);
			if (!flag)
				thread.interrupt();
			return flag;
		}

		public void run() {
			try {
				ccResult = V2CHttpUtil.getHTTPFile(sURL, null, sCharsets,
						sContentType, ltLastModified, nRepeat);
			} finally {
				vProgressPanel.hidePanel();
			}
		}

		final String sURL;
		final String sCharsets;
		final String sContentType;
		final long ltLastModified;
		final int nRepeat;
		final V2CProgressPanel vProgressPanel;
		CAndC ccResult;

		GetHTTPFileBG(String s, String s1, String s2, String s3, long l) {
			this(s, null, s1, s2, s3, l, 0);
		}

		GetHTTPFileBG(String s, String s1, String s2, String s3, String s4,
				long l) {
			this(s, s1, s2, s3, s4, l, 0);
		}

		GetHTTPFileBG(String s, String s1, String s2, String s3, String s4,
				long l, int i) {
			sURL = s2;
			sCharsets = s3;
			sContentType = s4;
			ltLastModified = l;
			nRepeat = i;
			vProgressPanel = new V2CProgressPanel(s, s1);
		}
	}

	private static class MyHostnameVerifier implements HostnameVerifier {

		public boolean verify(String s, SSLSession sslsession) {
			String s1 = sslsession.getPeerHost();
			if (s.equals(s1))
				return true;
			InetAddress inetaddress = null;
			InetAddress inetaddress1 = null;
			try {
				inetaddress = InetAddress.getByName(s);
				inetaddress1 = InetAddress.getByName(s1);
			} catch (UnknownHostException unknownhostexception) {
				System.out.println(unknownhostexception.getMessage());
				return false;
			}
			return inetaddress.equals(inetaddress1);
		}

		private MyHostnameVerifier() {
		}

	}

	static class PostFormBG implements Runnable {

		boolean start() {
			Thread thread = new Thread(this);
			thread.start();
			boolean flag = vProgressPanel.showPanel();
			if (!flag)
				thread.interrupt();
			return flag;
		}

		public void run() {
			try {
				ccResult = V2CHttpUtil.postForm(sURL, sRef, sData, sCharsets);
			} finally {
				vProgressPanel.hidePanel();
			}
		}

		final String sURL;
		final String sRef;
		final String sData;
		final String sCharsets;
		final V2CProgressPanel vProgressPanel;
		CAndC ccResult;

		PostFormBG(String s, String s1, String s2, String s3, String s4,
				String s5) {
			sURL = s2;
			sRef = s3;
			sData = s4;
			sCharsets = s5;
			vProgressPanel = new V2CProgressPanel(s, s1);
		}
	}
	public static class RemoteHost {

		public void start() {
			ArrayList arraylist = alWaitingThread;
			Thread thread = Thread.currentThread();
			try {
				synchronized (arraylist) {
					arraylist.add(thread);
					if (thread != arraylist.get(0))
						for (; arraylist.size() > 0
								&& arraylist.get(0) != thread; arraylist.wait());
				}
			} catch (InterruptedException interruptedexception) {
				synchronized (arraylist) {
					arraylist.remove(thread);
				}
				thread.interrupt();
			}
		}

		void finished() {
			ArrayList arraylist = alWaitingThread;
			synchronized (arraylist) {
				if (arraylist.size() > 0) {
					arraylist.remove(Thread.currentThread());
					for (int i = arraylist.size() - 1; i >= 0; i--)
						if (!((Thread) arraylist.get(i)).isAlive())
							arraylist.remove(i);

				}
				arraylist.notifyAll();
			}
		}

		void interrupted(Thread thread) {
			ArrayList arraylist = alWaitingThread;
			synchronized (arraylist) {
				if (arraylist.size() > 0 && thread == arraylist.get(0)) {
					arraylist.remove(0);
					arraylist.notifyAll();
				}
			}
		}

		synchronized void check() {
		}

		final String sName;
		final ArrayList alWaitingThread = new ArrayList();

		RemoteHost(String s) {
			sName = s;
		}
	}

	static boolean getPreferIPv6Addresses() {
		return V2CApp.getUserProperty().getBoolean("PreferIPv6Addresses");
	}

	static URL checkPreferIPv6(URL url) {
		if (!getPreferIPv6Addresses())
			return url;
		String s = System.getProperty("java.net.preferIPv6Addresses");
		if (s != null && s.equals("true"))
			return url;
		InetAddress ainetaddress[] = null;
		try {
			ainetaddress = InetAddress.getAllByName(url.getHost());
		} catch (UnknownHostException unknownhostexception) {
		}
		if (ainetaddress == null || ainetaddress.length == 0)
			return url;
		Inet6Address inet6address = null;
		int i = 0;
		do {
			if (i >= ainetaddress.length)
				break;
			InetAddress inetaddress = ainetaddress[i];
			if (inetaddress instanceof Inet6Address) {
				inet6address = (Inet6Address) inetaddress;
				break;
			}
			i++;
		} while (true);
		if (inet6address == null)
			return url;
		try {
			url = new URL(url.getProtocol(), inet6address.getHostAddress(),
					url.getFile());
		} catch (MalformedURLException malformedurlexception) {
			V2CMiscUtil.printMalformedURLException(malformedurlexception);
		}
		return url;
	}

	static boolean isPolipoUsed() {
		boolean flag = bPolipo;
		if (bPolipo) {
			bPolipo = false;
			bPolipoWarned = true;
		}
		return flag;
	}

	private static URL str2URL(String s) {
		try {
			return new URL(s);
		} catch (MalformedURLException malformedurlexception) {
			V2CMiscUtil.printMalformedURLException(malformedurlexception);
		}
		return null;
	}

	private static boolean isRedirect(int i) {
		return i == 301 || i == 302 || i == 303 || i == 307;
	}

	static HashMap putOtherUAName(HashMap hashmap) {
		if (hashmap == null)
			hashmap = new HashMap();
		hashmap.put("User-Agent", "Mozilla/4.0 (compatible)");
		return hashmap;
	}

	private static void setUA(HttpURLConnection httpurlconnection, URL url) {
		String s = url.getHost();
		String s1;
		if (s.endsWith(".2ch.net") && !s.equals("find.2ch.net")
				|| s.endsWith(".bbspink.com"))
			s1 = getUAName(false, true, true);
		else
			s1 = "Mozilla/4.0 (compatible)";
		httpurlconnection.setRequestProperty("User-Agent", s1);
	}

	static boolean isLocalURL(String s) {
		return s != null && s.startsWith("http://localboard/");
	}

	static boolean isLocalURL(URL url) {
		return url != null && "localboard".equals(url.getHost());
	}

	static URL checkShitarabaURL(URL url) {
		String s = url.getHost();
		if (s == null || !s.startsWith("jbbs."))
			return url;
		try {
			url = new URL(V2CJBBSShitarabaBBS.unnormalizeURL(url
					.toExternalForm()));
		} catch (MalformedURLException malformedurlexception) {
			V2CMiscUtil.printMalformedURLException(malformedurlexception);
		}
		return url;
	}

	static String getCharsetFromCAndC(CAndC candc) {
		if (candc == null)
			return null;
		String s = getCharsetFromBytes(candc.getRawContents());
		if (s != null) {
			return s;
		} else {
			HttpURLConnection httpurlconnection = candc.getConnection();
			return httpurlconnection == null
					? null
					: getCharsetFromString(httpurlconnection.getContentType());
		}
	}

	static String getCharsetFromBytes(byte abyte0[]) {
		if (abyte0 == null)
			return null;
		String s = null;
		try {
			s = new String(abyte0, "ISO-8859-1");
		} catch (UnsupportedEncodingException unsupportedencodingexception) {
		}
		if (s == null) {
			return null;
		} else {
			Pattern pattern = Pattern.compile("<meta ([^>]+)>", 2);
			Matcher matcher = pattern.matcher(s);
			return matcher.find()
					? getCharsetFromString(matcher.group(1))
					: null;
		}
	}

	static String getCharsetFromString(String s) {
		if (s == null)
			return null;
		s = s.toLowerCase();
		if (s.indexOf("utf-8") >= 0)
			return "UTF-8";
		if (s.indexOf("shift_jis") >= 0 || s.indexOf("sjis") >= 0)
			return "MS932";
		if (s.indexOf("euc-jp") >= 0)
			return "EUC-JP";
		if (s.indexOf("iso-2022-jp") >= 0)
			return "ISO-2022-JP";
		else
			return null;
	}

	static void getTimeoutValues(
			V2CHttpTimeoutSettingPanel v2chttptimeoutsettingpanel) {
		V2CProperty v2cproperty = V2CMain.getUserProperty();
		int i = iConnectTimeout / 1000;
		boolean flag = v2cproperty.containsKey("HttpUtil.ConnectTimeout");
		int j = iReadTimeout / 1000;
		boolean flag1 = v2cproperty.containsKey("HttpUtil.ReadTimeout");
		v2chttptimeoutsettingpanel.setTimeoutValues(i, flag, j, flag1);
	}

	static void setTimeoutValues(int i, int j) {
		V2CProperty v2cproperty = V2CMain.getUserProperty();
		if (i > 0) {
			iConnectTimeout = i * 1000;
			v2cproperty.putInt("HttpUtil.ConnectTimeout", i);
		} else {
			iConnectTimeout = 60000;
			v2cproperty.remove("HttpUtil.ConnectTimeout");
		}
		if (j > 0) {
			iReadTimeout = j * 1000;
			v2cproperty.putInt("HttpUtil.ReadTimeout", j);
		} else {
			iReadTimeout = 0x1d4c0;
			v2cproperty.remove("HttpUtil.ReadTimeout");
		}
	}

	static void setTimeout(URLConnection urlconnection) {
		setTimeout(urlconnection, iConnectTimeout, iReadTimeout);
	}

	static void setTimeout(URLConnection urlconnection, int i, int j) {
		if (V2CApp.javaVersionLessThan(1, 5)) {
			return;
		} else {
			V2CJ2SE5Util.setURLConnectionTimeouts(urlconnection, i, j);
			return;
		}
	}
	static RemoteHost getRemoteHost(URL url) {
		String s = url.getHost();
		if (s == null)
			s = "";
		RemoteHost remotehost = null;
		synchronized (hmHosts) {
			remotehost = (RemoteHost) hmHosts.get(s);
			if (remotehost == null) {
				remotehost = new RemoteHost(s);
				hmHosts.put(s, remotehost);
			}
		}
		return remotehost;
	}

	static void threadInterrupted(Thread thread) {
		synchronized (hmHosts) {
			for (Iterator iterator = hmHosts.values().iterator(); iterator
					.hasNext(); ((RemoteHost) iterator.next())
					.interrupted(thread));
		}
	}

	static int getMaxConnectionNumber() {
		return V2CMain.getUserProperty().getInt("HttpUtil.MaxConnectionNumber",
				5);
	}

	private V2CHttpUtil() {
	}

	private static String getHash(String message) {
		try {
			Key key = new SecretKeySpec(getHMKey().getBytes(), "HmacSHA256");
			Mac mac = Mac.getInstance(key.getAlgorithm());
			mac.init(key);
			byte[] data = mac.doFinal(message.getBytes());
			StringBuffer sb = new StringBuffer();
			for (byte b : data) {
				String s = Integer.toHexString(0xff & b);
				if (s.length() == 1) {
					sb.append("0");
				}
				sb.append(s);
			}
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	static String prev_userId;
	static String prev_passwd;
	static boolean login2ch(String userId, String passwd,
			V2CProgressPanel v2cprogresspanel, boolean flag) {
		prev_userId = userId;
		prev_passwd = passwd;
		URL url = null;
		if (useAPI) {
			url = str2URL("https://api.2ch.net/v1/auth/");
		} else {
			url = V2C2chBBS.getLoginServerURL();
		}
		Thread thread = Thread.currentThread();
		PrintWriter printwriter = null;
		BufferedReader bufferedreader = null;
		char[] ac = new char[nBufLen];
		StringBuffer stringbuffer = new StringBuffer();
		if (!V2CMain.isOnlineMode()) {
			V2CHttpUtil.logNotOnline(userId);
			return false;
		}
		if (null == url) {
			V2CLogger.logError(url, "Error in bbs2ch.getLoginServerURL().");
			V2C2chBBS.maruLoginFailed(flag);
			return false;
		}
		HttpsURLConnection httpsurlconnection;
		try {
			if (v2cprogresspanel != null) {
				v2cprogresspanel.setMessage("opening connection ...");
			}
			httpsurlconnection = (HttpsURLConnection) V2CProxySetting
					.openReadConnection(url);
			if (thread.isInterrupted()) {
				logInterrupt(url);
				V2C2chBBS.maruLoginFailed(flag);
				return false;
			}
			if (V2CApp.javaVersionLessThan(1, 5)) {
				httpsurlconnection
						.setHostnameVerifier(new MyHostnameVerifier());
			}
			httpsurlconnection.setRequestMethod("POST");
			httpsurlconnection.setDoOutput(true);
			httpsurlconnection.setUseCaches(false);
			httpsurlconnection.setAllowUserInteraction(false);
			httpsurlconnection.setRequestProperty("Host", url.getHost());
			httpsurlconnection.setRequestProperty("Accept", "*/*");
			httpsurlconnection.setRequestProperty("User-Agent", getUAName(true, false, true));
			httpsurlconnection.setRequestProperty("X-2ch-UA", getX2chUA());
			V2CLogger.logHTTPHeaderFields(url, "2ch Login Request:",
					httpsurlconnection.getRequestProperties());
			printwriter = new PrintWriter(httpsurlconnection.getOutputStream());
			if (thread.isInterrupted()) {
				logInterrupt(url);
				V2C2chBBS.maruLoginFailed(flag);
				return false;
			}

			int j;
			if (v2cprogresspanel != null) {
				v2cprogresspanel.setMessage("sending id&pw ...");
			}

			printwriter.print("ID=" + V2CJPConverter.urlEncode(userId, "MS932")
					+ "&PW=" + V2CJPConverter.urlEncode(passwd, "MS932"));
			if (useAPI) {
				String CT = String.valueOf(System.currentTimeMillis());
				CT = CT.substring(0,10);
				String message = getAppKey() + CT;
				String HB = getHash(message);
				printwriter.print("&KY="
						+ V2CJPConverter.urlEncode(getAppKey(), "MS932")
						+ "&CT=" + V2CJPConverter.urlEncode(CT, "MS932")
						+ "&HB=" + V2CJPConverter.urlEncode(HB, "MS932"));
			}
			printwriter.close();
			bufferedreader = new BufferedReader(new InputStreamReader(
					httpsurlconnection.getInputStream()));
			if (thread.isInterrupted()) {
				logInterrupt(url);
				V2C2chBBS.maruLoginFailed(flag);
				return false;
			}
			if (v2cprogresspanel != null) {
				v2cprogresspanel.setMessage("receiving session ID ...");
			}
			while ((j = bufferedreader.read(ac)) >= 0) {
				if (thread.isInterrupted()) {
					logInterrupt(url);
					V2C2chBBS.maruLoginFailed(flag);
					return false;
				}
				if (j > 0) {
					stringbuffer.append(ac, 0, j);
				}
			}
			Map map = httpsurlconnection.getHeaderFields();
			if (map.containsKey("Set-Cookie")) {
				List list = (List) map.get("Set-Cookie");
				for (int l = 0; l < list.size(); l++)
					V2CCookie.addCookie((String) list.get(l), url);

			}
			logHTTPResponse(httpsurlconnection, "2ch Login Response: ");
		} catch (IOException ioexception) {
			V2CLogger.logError(url, V2CMiscUtil.getMessage(ioexception),
					ioexception);
			V2C2chBBS.maruLoginFailed(flag);
			return false;
		} finally {
			V2CProxySetting.resetReadProxy();
			V2CLocalFileHandler.closeWriter(printwriter);
			V2CLocalFileHandler.closeReader(bufferedreader);
		}
		for (int i = stringbuffer.length() - 1; i >= 0; i--) {
			char k = stringbuffer.charAt(i);
			if ((k != '\n') && (k != '\r'))
				break;
			stringbuffer.setLength(i);
		}
		String str = stringbuffer.toString();
		if ((!str.startsWith("SESSION-ID="))
				|| (str.regionMatches(11, "ERROR", 0, 5))) {
			V2C2chBBS.maruLoginFailed(flag);
			return false;
		}
		str = str.substring(11);
		int k = str.indexOf(':');
		if (k < 0) {
			V2C2chBBS.maruLoginFailed(flag);
			return false;
		}
		if (useAPI) {
			sid = str.substring(k + 1);
		}
		V2C2chBBS.setMaruSessionID(str, flag);
		return true;
	}

	private static boolean checkContentType(String s, String s1) {
		return s1 == null || s != null
				&& (V2CMiscUtil.isEqual(s, s1) || s.startsWith(s1 + ';'));
	}

	private static void logHTTPResponse(HttpURLConnection httpurlconnection,
			String s) throws IOException {
		V2CLogger.logHTTPHeaderFields(httpurlconnection.getURL(), s,
				httpurlconnection.getHeaderFields());
	}

	private static void logRemoteHostError(URL url) {
		V2CLogger.logError(url, "    Error in getRemoteHost(u).");
	}

	private static void logInterrupt(URL url) {
		V2CLogger.logInfo(url, "    Interrupt detected.");
	}

	private static void logNotOnline(URL url) {
		V2CLogger.logInfo(url, "    Not online.");
	}

	private static void logNotOnline(String s) {
		V2CLogger.logInfo(s, "    Not online.");
	}

	static CAndC getDatOchiFile(URL url, V2CBBSThreadRes v2cbbsthreadres,
			V2CBBS v2cbbs) {
		if (useAPI && useSC) {
			Matcher matcher = pattern.matcher(url.toString());
			if (matcher.matches()) {
				url = V2CHttpUtil.str2URL(url.toString().replaceFirst(".net",
						".sc"));
				return updateDatFile(url, 0, 0L, null, v2cbbsthreadres, v2cbbs,
						true);
			}
		}
		return updateDatFile(url, 0, 0L, null, v2cbbsthreadres, v2cbbs, false);
	}

	static CAndC getDatGzFile(URL url, V2CBBSThreadRes v2cbbsthreadres,
			V2CBBS v2cbbs, boolean flag) {
		return updateDatFile(url, 0, 0L, null, v2cbbsthreadres, v2cbbs, flag);
	}

	static CAndC updateDatFile(URL url, int i, long l, String s,
			V2CBBSThreadRes v2cbbsthreadres, V2CBBS v2cbbs) {
		return updateDatFile(url, i, l, s, v2cbbsthreadres, v2cbbs, false);
	}

	static String html2Dat(String orig, int resNum) {
		StringBuffer buff = new StringBuffer();
		Matcher datMatcher = html2dat.matcher(orig);
		while (datMatcher.find()) {
			int currRes = Integer.parseInt(datMatcher.group(1).trim());
			if (currRes <= resNum) {
				continue;
			}
			String name = datMatcher.group(4) == null
					? datMatcher.group(5)
					: datMatcher.group(4);
			name = name.replaceAll("<b>(.*)<\\/b>", "$1");
			String mail = datMatcher.group(3) == null ? "" : datMatcher
					.group(3);
			mail = mail.replaceFirst("mailto:", "");
			String date = datMatcher.group(6) == null ? "" : datMatcher
					.group(6);
			date = date.replaceFirst("<a href=\"javascript:be\\(([0-9]*)\\);\">.([^<]*)</a>", "BE:$1-$2");
			String message = datMatcher.group(7) == null ? "" : datMatcher
					.group(7);
			message = message.replaceAll(
					"<a href=\"([a-z0-9]+:\\/\\/[^\\\"]+)\"[^>]+>([^<]+)<\\/a>", "$2");
			message = message.replaceAll("<br> (?=<br>)", "$0 ");
			String title = "";
			if (currRes == 1) {
				Pattern titlePat = Pattern.compile("<title>([^<]+)<\\/title>");
				Matcher m = titlePat.matcher(orig);
				if (m.find()) {
					title = m.group(1);
				} else {
					title = "";
				}
			}
			buff.append(name + "<>" + mail + "<>" + date + "<>" + message
					+ "<>" + title + "\n");
		}
		return buff.toString();
	}

	static CAndC updateDatFile(URL url, int startPos, long lastModified,
			String eTag, V2CBBSThreadRes threadRes, V2CBBS v2cbbs,
			boolean acceptGZ) {
		return updateDatFile(url, startPos, lastModified, eTag,
				threadRes, v2cbbs, acceptGZ, false);
	}

	static CAndC updateDatFile(URL url, int startPos, long lastModified,
			String eTag, V2CBBSThreadRes v2cbbsthreadres, V2CBBS v2cbbs,
			boolean acceptGZ, boolean isAPIFail) {
		String uri = null;
		BufferedReader bufferedreader = null;
		if (url == null || isLocalURL(url)) {
			return null;
		}
		if (!V2CMain.isOnlineMode()) {
			logNotOnline(url);
			return null;
		}
		url = checkShitarabaURL(url);
		String original = url.toString();
		Thread thread = Thread.currentThread();
		RemoteHost remotehost = getRemoteHost(url);
		remotehost.start();
		CAndC candc;
		InputStream istream = null;
		PrintStream out = null;
		boolean usingAPI = false;
		boolean usingHTML = false;
		try {
			if (useAPI && !isAPIFail) {
				Matcher matcher = pattern.matcher(original);
				if (matcher.matches()) {
					usingAPI = true;
					if (null == sid) {
						V2CHttpUtil.login2ch("", "", null, false);
					}
					String server = matcher.group(1);
					String board = matcher.group(3);
					String threadId = matcher.group(4);
					uri = "/v1/" + server + "/" + board + "/" + threadId;
					url = V2CHttpUtil.str2URL("https://api.2ch.net" + uri);
				}
			} else if (useHTML) {
				Matcher matcher = pattern.matcher(original);
				if (matcher.matches()) {
					usingHTML = true;
					String server = matcher.group(1);
					String s2ch = matcher.group(2);
					String board = matcher.group(3);
					String threadId = matcher.group(4);
					uri = "http://" + server + s2ch + "/test/read.cgi/"
							+ board + "/" + threadId + "/";
					if (v2cbbsthreadres.nRes > 0) {
						uri = uri + v2cbbsthreadres.nRes + "-";
					}
					url = V2CHttpUtil.str2URL(uri);
				}
			} else if (useSC) {
				Matcher matcher = pattern.matcher(original);
				if (matcher.matches()) {
					url = V2CHttpUtil.str2URL(url.toString().replace(
							".2ch.net", ".2ch.sc"));
				}
			}
			if (thread.isInterrupted()) {
				logInterrupt(url);
				return null;
			}
			int responseCode;
			HttpURLConnection conn = V2CProxySetting.openReadConnection(url);
			conn.setInstanceFollowRedirects(true);
			setTimeout(conn);
			conn.setRequestProperty("Host", url.getHost());
			conn.setRequestProperty("Accept", "*/*");
			if (usingHTML) {
				conn.setRequestProperty("User-Agent",IE_UA);				
			} else {
				conn.setRequestProperty("User-Agent",
					v2cbbs != null && !v2cbbs.is2chEq()
							? getUAName(false,false,usingAPI)
							: "Mozilla/4.0 (compatible)");
			}
			if (startPos > 0 && lastModified > 0L) {
				conn.setIfModifiedSince(lastModified);
				if (eTag != null && eTag.length() > 0) {
					conn.setRequestProperty("If-None-Match", eTag);
				}
			}
			boolean useRange = startPos > 0 && v2cbbs.useRangeRequestHeader()
					&& (!usingHTML);
			if (useRange) {
				conn.setRequestProperty("Range", "bytes=" + startPos + "-");
				conn.setRequestProperty("Accept-Encoding", "identity");
			} else if (!acceptGZ) {
				conn.setRequestProperty("Accept-Encoding", "gzip");
			}
			V2CLogger.logHTTPHeaderFields(url, "Dat Request:",
					conn.getRequestProperties());
			if (usingAPI) {
				conn.setDoOutput(true);
				conn.setRequestMethod("POST");
				String message = uri + sid + getAppKey();
				String hobo = getHash(message);
				String param = "sid=" + sid + "&hobo=" + hobo + "&appkey="
						+ getAppKey();
				out = new PrintStream(conn.getOutputStream());
				out.print(param);
				out.flush();
			}
			remotehost.check();
			try {
				responseCode = conn.getResponseCode();
			} catch (Exception e){
				e.printStackTrace();
				responseCode = 999;
			}
			if (thread.isInterrupted()) {
				logInterrupt(url);
				return null;
			}
			logHTTPResponse(conn, "Dat Response: ");
			candc = new CAndC(conn);
			String contentType = conn.getContentType();
			if (responseCode < 300) {
				if (usingHTML) {
					contentType = "text/plain";
				}
				if (usingAPI) {
					int len = conn.getContentLength();
					if (len == 3 && responseCode == 200) { // APIのバグ対応
						return candc;
					}
				}				
			} else if (responseCode == 302) {
				if (original.contains("bbspink.com")){
					url = str2URL(original);
					return updateDatFile(url, 0, 0L, null, v2cbbsthreadres,
							v2cbbs, false, true);					
				}
				// do nothing
			} else if (responseCode == 304) {
				return candc;
			} else if (responseCode >= 300 && responseCode < 400) {
				logRemoteHostError(url);
				return candc;
			} else if (responseCode == 416) {
				return candc;
			} else if (responseCode >= 400 && responseCode < 500) {
				String etag = conn.getHeaderField("ETag");
				if (usingAPI && etag == null) {// 過去ログ取得を失敗したらHTMLで取りに行く
					url = str2URL(original);
					return updateDatFile(url, 0, 0L, null, v2cbbsthreadres,
							v2cbbs, false, true);
				} else if (usingAPI) {
					return new CAndC("新着レスなし");// APIのよくあるバグなので無視
				} else if (usingHTML && useSC) {//APIもHTMLも失敗ならSCに取りに行く
					url = V2CHttpUtil.str2URL(original.replaceFirst(".2ch.net",
							".2ch.sc"));
					return updateDatFile(url, 0, 0L, null, v2cbbsthreadres,
							v2cbbs, false);
				} else {
					logRemoteHostError(url);
					return candc;
				}
			} else if (responseCode >= 500) {
				if (usingAPI) {
					url = V2CHttpUtil.str2URL(original);
					return updateDatFile(url, 0, 0L, null, v2cbbsthreadres,
							v2cbbs, false, true);
				} else if (usingHTML && useSC){
					url = V2CHttpUtil.str2URL(original.replaceFirst(".2ch.net",
							".2ch.sc"));
					return updateDatFile(url, 0, 0L, null, v2cbbsthreadres,
							v2cbbs, false);
				} else {
					logRemoteHostError(url);
					return candc;
				}
			}

			boolean polipoDetection;
			boolean isGZip;
			polipoDetection = useRange && responseCode == 200;
			if (polipoDetection) {
				int k = conn.getContentLength();
				String s3 = conn.getHeaderField("Connection");
				if (k <= startPos || s3 == null || !s3.equals("keep-alive")) {
					polipoDetection = false;
				}
			}
			if (polipoDetection && !bPolipoWarned && !useAPI) {
				bPolipo = true;
			}
			String contentEncoding = conn.getContentEncoding();
			isGZip = acceptGZ || contentEncoding != null
					&& contentEncoding.equals("gzip");
			if ((contentType == null || !V2CMiscUtil.contentTypeStartsWith(
					contentType, v2cbbs.getDatContentType(), true))) {
				boolean contentTypeError = false;
				if (contentType != null) {
					String s4 = url.toString();
					if (isGZip
							&& (contentType
									.equalsIgnoreCase("application/x-gzip") || contentType
									.equalsIgnoreCase("text/html"))
							&& v2cbbs.is2ch()
							&& url.toString().endsWith(".dat.gz")) {
						contentTypeError = true;
					} else if (contentType
							.equalsIgnoreCase("application/octet-stream")) {
						contentTypeError = s4
								.startsWith("http://ookamitokari2.run.buttobi.net/ookamikakolog/")
								|| s4.startsWith("http://www.2ich.net/");
					} else if (contentType
							.equalsIgnoreCase("chemical/x-mopac-input")) {
						contentTypeError = s4
								.startsWith("http://tmhkym.net/maka/bbs/maka/");
					}
				} else if (v2cbbs.is2ch()) {
					contentTypeError = true;
				}
				if (!contentTypeError) {
					return new CAndC("Content-Typeが"
							+ v2cbbs.getDatContentType() + "で始まっていません。（"
							+ contentType + "）");
				}
			}
			istream = conn.getInputStream();
			int offset;
			byte abyte0[];
			int contentLength = conn.getContentLength();
			if (usingHTML) {
				if (isGZip) {
					istream = new GZIPInputStream(new GZIPFilterInputStream(
							istream, v2cbbsthreadres));
				}

				byte[] buff = new byte[istream.available() < 2048
						? 2048
						: istream.available()];
				
				int read = 0;
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
								
				while ((read = istream.read(buff)) > 0) {
					bos.write(buff, 0, read);
				}
				istream.close();
				int lastRes = v2cbbsthreadres.nRes;
				String dat = html2Dat(bos.toString("Windows-31J"), lastRes);
				if (dat.length() == 0) {
					lastRes = 0;
					dat = html2Dat(bos.toString("Windows-31J"), lastRes);
				}
				byte[] data = dat.getBytes("Windows-31J");
				if (lastRes == 0 && startPos > 0) {
					istream = new ByteArrayInputStream(data, startPos, data.length - startPos);
				} else if (lastRes > 0) {
					bos.reset();
					bos.write('\n');
					bos.write(data);
					istream = new ByteArrayInputStream(bos.toByteArray());
					contentLength = bos.size();
					bos.close();
				} else {
					istream = new ByteArrayInputStream(data);
					contentLength = data.length;
				}
			} else if (isGZip) {
				try {
					istream = new GZIPInputStream(new GZIPFilterInputStream(
							istream, v2cbbsthreadres));
				} catch (IOException ioexception1) {
					if (responseCode == 302 && V2CProxySetting.isO2onUsed()) {
						return candc;
					}
					throw ioexception1;
				}
			}
			v2cbbsthreadres.setContentLength(polipoDetection ? Math.max(
					contentLength - startPos, 1) : contentLength);
			v2cbbsthreadres.setHeaderFields(conn);
			offset = polipoDetection ? startPos : 0;
			abyte0 = new byte[nBufLen];
			while (true) {
				remotehost.check();
				int k1 = istream.read(abyte0);
				if (thread.isInterrupted()) {
					logInterrupt(url);
					return null;
				}
				if (k1 < 0) {
					if (v2cbbsthreadres.bCheckFirstLine) {
						v2cbbsthreadres.appendToDatFile(abyte0, 0);
						break;
					}
					break;
				}
				if (k1 > 0) {
					if (offset > 0) {
						if (offset >= k1) {
							offset -= k1;
						} else {
							k1 -= offset;
							System.arraycopy(abyte0, offset, abyte0, 0, k1);
							offset = 0;
						}
					} else {
						if (!v2cbbsthreadres.appendToDatFile(abyte0, k1)) {
							break;
						}
						if (!isGZip) {
							v2cbbsthreadres.addProgressValue(k1);
						}
					}
				}
			}
			StringBuffer stringbuffer;
			bufferedreader = new BufferedReader(new InputStreamReader(istream,
					v2cbbsthreadres.getThreadItem().getBoardItem()
							.getCharsetString()));
			stringbuffer = new StringBuffer();
			String line;
			while ((line = bufferedreader.readLine()) != null) {
				if (thread.isInterrupted()) {
					logInterrupt(url);
					return null;
				}
				stringbuffer.append(line);
			}
			candc.setContents(stringbuffer.toString());
		} catch (IOException ioexception) {
			ioexception.printStackTrace();
			bPolipo = false;
			String s1 = V2CMiscUtil.getMessage(ioexception);
			V2CLogger.logError(url, s1, ioexception);
			return new CAndC(ioexception.getClass().getName() + ": " + s1);
		} finally {
			V2CProxySetting.resetReadProxy();
			remotehost.finished();
			V2CLocalFileHandler.closeInputStream(((InputStream) (istream)));
			V2CLocalFileHandler.closeReader(bufferedreader);
			try {
				istream.close();
			} catch (Exception ignore) {
			}
		}
		return candc;
	}
	static int connectTwitterStream(
			V2CTwitterUserStream paramV2CTwitterUserStream, HashMap paramHashMap) {
		boolean bool = V2CReleaseInfo.isEqual(102, 91, 0);
		int i = paramHashMap == null ? 1 : 0;
		if (i != 0) {
			paramHashMap = new HashMap();
			paramHashMap.put("with", "followings");
		}
		paramHashMap.put("delimited", "length");
		String str1 = i != 0
				? "https://userstream.twitter.com/1.1/user.json"
				: "https://stream.twitter.com/1.1/statuses/filter.json";
		URL localURL = str2URL(str1);
		int j = 0;
		PrintWriter localPrintWriter = null;
		Object localObject1 = null;
		Thread localThread = Thread.currentThread();
		int k = 0;
		int m = 0;
		int n = 0;
		int i1 = 0;
		int i2 = 0;
		while (true) {
			if (bool)
				System.out.println("ncto,nrto: " + m + "," + n);
			int i3 = 0;
			try {
				int i4;
				if (localThread.isInterrupted()) {
					logInterrupt(localURL);
					i4 = 2;
					return i4;
				}
				if ((m >= 10) || (k >= 10)) {
					i4 = 5;
					return i4;
				}
				if (m > 1) {
					if (i1 <= 0) {
						i4 = (int) System.currentTimeMillis() & 0x7FFF;
						i1 = 20 + i4 % 20;
						i2 = 240 + (i4 >>> 5) % 60;
					} else {
						i1 = Math.min(i1 * 2, i2);
					}
					if (bool)
						System.out.println("sleeping: " + i1);
					try {
						Thread.sleep(i1 * 1000);
					} catch (InterruptedException localInterruptedException) {
						int i5 = 2;
						return i5;
					}
				}
				HttpURLConnection conn = V2CProxySetting
						.openReadConnection(localURL);
				setTimeout(conn, 5000, 90000);
				conn.setRequestMethod("POST");
				conn.setDoOutput(true);
				conn.setUseCaches(false);
				conn.setAllowUserInteraction(false);
				conn.setRequestProperty("Host", localURL.getHost());
				conn.setRequestProperty("Accept", "*/*");
				conn.setRequestProperty("Accept-Encoding", "deflate,gzip");
				conn.setRequestProperty("User-Agent",
						"V2C/" + V2CReleaseInfo.getVersionOrName());
				String str2 = createOAuthHeader(
						paramV2CTwitterUserStream.getUser(), true, str1,
						paramHashMap);
				conn.setRequestProperty("Authorization", str2);
				int i6;
				if (paramV2CTwitterUserStream.disconnectReceived()) {
					i6 = 4;
					return i6;
				}
				V2CLogger.logHTTPHeaderFields(localURL,
						"TwitterUserStream Request:",
						conn.getRequestProperties());
				localPrintWriter = new PrintWriter(conn.getOutputStream());
				if (localThread.isInterrupted()) {
					logInterrupt(localURL);
					i6 = 2;
					return i6;
				}
				localPrintWriter.print(constructQuery(paramHashMap));
				localPrintWriter.close();
				localPrintWriter = null;
				j = conn.getResponseCode();
				if (localThread.isInterrupted()) {
					logInterrupt(localURL);
					i6 = 2;
					return i6;
				}
				i3 = 1;
				logHTTPResponse(conn, "TwitterUserStream Response: ");
				paramV2CTwitterUserStream.addToConsole("接続 (ResponseCode: " + j
						+ ")", false);
				if (j != 200) {
					if (bool)
						System.out.println("Response Code: " + j + " "
								+ conn.getResponseMessage());
					i6 = j;
					return i6;
				}
				k = m = n = 0;
				localObject1 = conn.getInputStream();
				String str3 = conn.getContentEncoding();
				if ((str3 != null) && (str3.matches("gzip|deflate")))
					localObject1 = new GZIPInputStream(
							(InputStream) localObject1);
				byte[] arrayOfByte = new byte[4096];
				int i8;
				while ((i8 = ((InputStream) localObject1).read(arrayOfByte)) >= 0) {
					int i10;
					if (localThread.isInterrupted()) {
						logInterrupt(localURL);
						i10 = 2;
						return i10;
					}
					if (((i != 0) && (!paramV2CTwitterUserStream.getUser()
							.getUseUserStream()))
							|| (!paramV2CTwitterUserStream.newStreamData(
									arrayOfByte, i8))) {
						i10 = 6;
						return i10;
					}
				}
			} catch (IOException localIOException) {
				k++;
				String str2 = V2CMiscUtil.getMessage(localIOException);
				if ((str2 != null)
						&& ((localIOException instanceof SocketTimeoutException))) {
					paramV2CTwitterUserStream.notifySocketTimeout();
					if (i3 != 0) {
						m++;
						n = 0;
					} else {
						n++;
						m = 0;
						i1 = 0;
					}
				}
				if (bool)
					System.out.println(str2 + ", " + new Date());
				V2CLogger.logError(localURL, str2, localIOException);
				paramV2CTwitterUserStream.addToConsole(str2, false);
				if ((localIOException instanceof SSLHandshakeException)) {
					int i7 = 3;
					return i7;
				}
			} finally {
				V2CProxySetting.resetReadProxy();
				V2CLocalFileHandler.closeWriter(localPrintWriter);
				V2CLocalFileHandler
						.closeInputStream((InputStream) localObject1);
			}
		}
	}

	static boolean getImageFile(URL paramURL, String paramString1,
			String paramString2, OutputStream paramOutputStream,
			V2CLink paramV2CLink, V2CSHA1Value paramV2CSHA1Value,
			HashMap paramHashMap, boolean paramBoolean1, boolean paramBoolean2,
			int paramInt) {
		if (paramURL == null)
			return false;
		if (!V2CMain.isOnlineMode()) {
			logNotOnline(paramURL);
			return false;
		}
		InputStream localInputStream = null;
		Thread localThread = Thread.currentThread();
		boolean bool1 = true;
		try {
			HttpURLConnection localHttpURLConnection = V2CProxySetting
					.openReadConnection(paramURL);
			setTimeout(localHttpURLConnection);
			String str1 = paramURL.getHost();
			localHttpURLConnection.setRequestProperty("Host", str1);
			localHttpURLConnection.setRequestProperty("Accept", "*/*");
			localHttpURLConnection.setRequestProperty("Accept-Language",
					"ja,en;q=0.5");
			localHttpURLConnection.setRequestProperty("User-Agent",
					paramString2 != null
							? paramString2
							: "Mozilla/4.0 (compatible)");
			localHttpURLConnection.setUseCaches(true);
			if ((paramString1 == null) && (str1.equals("up.80.kg")))
				paramString1 = "http://up.80.kg/";
			if (paramString1 != null)
				localHttpURLConnection.setRequestProperty("Referer",
						paramString1);
			String str2 = V2CCookie.getCookie(paramURL);
			if (str2 != null)
				localHttpURLConnection.setRequestProperty("Cookie", str2);
			if (paramHashMap != null) {
				Iterator localIterator = paramHashMap.keySet().iterator();
				while (localIterator.hasNext()) {
					String str3 = (String) localIterator.next();
					localHttpURLConnection.setRequestProperty(str3,
							(String) paramHashMap.get(str3));
				}
			}
			V2CLogger.logHTTPHeaderFields(paramURL, "Image Request:",
					localHttpURLConnection.getRequestProperties());
			int i = localHttpURLConnection.getResponseCode();
			if (localThread.isInterrupted()) {
				logInterrupt(paramURL);
				return false;
			}
			logHTTPResponse(localHttpURLConnection, "Image Response: ");
			paramV2CLink.setFields(localHttpURLConnection);
			if ((isRedirect(i)) && (paramInt > 0)) {
				paramInt--;
				String str4 = localHttpURLConnection.getHeaderField("Location");
				if (str4 != null) {
					URL localURL = null;
					try {
						localURL = new URL(paramURL, str4);
					} catch (MalformedURLException localMalformedURLException) {
						paramV2CLink.setResponseCode(4,
								localMalformedURLException.getMessage());
					}
					if (localURL != null) {
						String str5 = localURL.toExternalForm();
						if (!str5.equals(str4))
							paramV2CLink.setLocationString(str5);
						boolean k = getImageFile(localURL,
								paramURL.toExternalForm(), paramString2,
								paramOutputStream, paramV2CLink,
								paramV2CSHA1Value, paramHashMap, paramBoolean1,
								paramBoolean2, paramInt);
						return k;
					}
				}
			}
			if ((!paramBoolean1)
					&& (paramV2CLink
							.exceedsFileSizeLimit(localHttpURLConnection
									.getContentLength()))) {
				return false;
			}
			boolean bool4 = !paramV2CLink.isDisplayableImage();
			if ((!paramBoolean2) && (bool4)) {
				boolean bool5 = false;
				return bool5;
			}
			localInputStream = localHttpURLConnection.getInputStream();
			byte[] arrayOfByte = new byte[nBufLen];
			int k = 0;
			int j;
			while ((j = localInputStream.read(arrayOfByte)) >= 0) {
				if (localThread.isInterrupted()) {
					logInterrupt(paramURL);
					boolean bool6 = false;
					return bool6;
				}
				if (j != 0) {
					if (bool4) {
						if (V2CLink.checkImageExt(arrayOfByte, j) == 1) {
							String str6 = localHttpURLConnection
									.getContentType();
							if ((str6 == null)
									|| (!str6.startsWith("text/html"))) {
								return false;
							}
							bool1 = false;
						}
						bool4 = false;
					}
					paramOutputStream.write(arrayOfByte, 0, j);
					paramV2CSHA1Value.update(arrayOfByte, 0, j);
					k += j;
					if (!paramV2CLink.setDownloadedLength(k, paramBoolean1)) {
						return false;
					}
				}
			}
		} catch (IOException localIOException) {
			paramV2CLink.setError(localIOException);
			V2CLogger.logError(paramURL,
					V2CMiscUtil.getMessage(localIOException), localIOException);
			boolean bool2 = false;
			return bool2;
		} finally {
			V2CProxySetting.resetReadProxy();
			V2CLocalFileHandler.closeInputStream(localInputStream);
			try {
				localInputStream.close();
			} catch (Exception ignore) {
			}
		}
		return bool1;
	}

	static boolean saveContentsToFile(String s, String s1, File file) {
		if (!V2CMain.isOnlineMode()) {
			return false;
		}
		BufferedOutputStream bufferedoutputstream = V2CLocalFileHandler
				.getBufferedOutputStream(file);
		if (bufferedoutputstream == null) {
			return false;
		} else {
			return saveContentsToFile(s, s1, bufferedoutputstream);
		}
	}

	static boolean saveContentsToFile(String paramString1, String paramString2,
			BufferedOutputStream paramBufferedOutputStream) {
		URL localURL = null;
		InputStream localInputStream = null;
		try {
			if (!V2CMain.isOnlineMode()) {
				boolean bool1 = false;
				return bool1;
			}
			try {
				localURL = new URL(paramString1);
			} catch (MalformedURLException localMalformedURLException) {
				V2CMiscUtil
						.printMalformedURLException(localMalformedURLException);
				boolean bool2 = false;
				return bool2;
			}
			Thread localThread = Thread.currentThread();
			HttpURLConnection localHttpURLConnection = V2CProxySetting
					.openReadConnection(localURL);
			setTimeout(localHttpURLConnection);
			String str1 = localURL.getHost();
			localHttpURLConnection.setRequestProperty("Host", str1);
			localHttpURLConnection.setRequestProperty("Accept", "*/*");
			if (paramString2 != null)
				localHttpURLConnection.setRequestProperty("Referer",
						paramString2);
			setUA(localHttpURLConnection, localURL);
			localHttpURLConnection.setRequestProperty("Accept-Language",
					"ja,en;q=0.5");
			String str2 = V2CCookie.getCookie(localURL);
			if (str2 != null)
				localHttpURLConnection.setRequestProperty("Cookie", str2);
			V2CLogger.logHTTPHeaderFields(localURL, "Contents Request:",
					localHttpURLConnection.getRequestProperties());
			int i = localHttpURLConnection.getResponseCode();
			if (localThread.isInterrupted()) {
				logInterrupt(localURL);
				boolean bool4 = false;
				return bool4;
			}
			Map localMap = localHttpURLConnection.getHeaderFields();
			int j;
			if (localMap.containsKey("Set-Cookie")) {
				List localList = (List) localMap.get("Set-Cookie");
				for (j = 0; j < localList.size(); j++)
					V2CCookie.addCookie((String) localList.get(j), localURL);
			}
			logHTTPResponse(localHttpURLConnection, "Contents Response: ");
			if (i != 200) {
				boolean bool5 = false;
				return bool5;
			}
			localInputStream = localHttpURLConnection.getInputStream();
			byte[] arrayOfByte = new byte[nBufLen];
			try {
				while ((j = localInputStream.read(arrayOfByte)) >= 0) {
					if (localThread.isInterrupted()) {
						return false;
					}
					if (j > 0)
						paramBufferedOutputStream.write(arrayOfByte, 0, j);
				}
			} finally {
				localInputStream.close();
			}
		} catch (IOException localIOException) {
			V2CLogger.logError(localURL,
					V2CMiscUtil.getMessage(localIOException), localIOException);
			boolean bool3 = false;
			return bool3;
		} finally {
			V2CProxySetting.resetReadProxy();
			V2CLocalFileHandler.closeOutputStream(paramBufferedOutputStream);
			V2CLocalFileHandler.closeInputStream(localInputStream);
		}
		return true;
	}

	static URL getRedirectURL(URL paramURL) {
		if (paramURL == null)
			return null;
		if (!V2CMain.isOnlineMode()) {
			logNotOnline(paramURL);
			return null;
		}
		InputStream localInputStream = null;
		Thread localThread = Thread.currentThread();
		try {
			HttpURLConnection localHttpURLConnection = V2CProxySetting
					.openReadConnection(paramURL);
			setTimeout(localHttpURLConnection);
			String str = paramURL.getHost();
			localHttpURLConnection.setRequestProperty("Host", str);
			localHttpURLConnection.setRequestProperty("Accept", "*/*");
			localHttpURLConnection.setRequestProperty("Accept-Language",
					"ja,en;q=0.5");
			localHttpURLConnection.setRequestProperty("User-Agent",
					"Mozilla/4.0 (compatible)");
			V2CLogger.logHTTPHeaderFields(paramURL, "RedirectCheck Request:",
					localHttpURLConnection.getRequestProperties());
			int i = localHttpURLConnection.getResponseCode();
			Object localObject1;
			if (localThread.isInterrupted()) {
				logInterrupt(paramURL);
				return null;
			}
			logHTTPResponse(localHttpURLConnection, "RedirectCheck Response: ");
			if (isRedirect(i)) {
				localObject1 = localHttpURLConnection
						.getHeaderField("Location");
				if (localObject1 != null)
					try {
						URL localURL1 = new URL(paramURL, (String) localObject1);
						return localURL1;
					} catch (MalformedURLException localMalformedURLException) {
						System.out.println("u,loc: " + paramURL + ","
								+ (String) localObject1);
						URL localURL2 = null;
						return localURL2;
					}
			}
			if (i == 200) {
				int j = Math.min(localHttpURLConnection.getContentLength(),
						10000);
				if (j == 0) {
					return null;
				}
				if (j < 0)
					j = 10000;
				Object localObject2 = new byte[j];
				localInputStream = localHttpURLConnection.getInputStream();
				int k = 0;
				int m = 0;
				try {
					while ((k = localInputStream.read((byte[]) localObject2, m,
							j - m)) >= 0) {
						if (localThread.isInterrupted()) {
							logInterrupt(paramURL);
							return null;
						}
						if (k != 0) {
							m += k;
							if (m >= j)
								break;
						}
					}
				} finally {
					localInputStream.close();
				}
				if (m == 0) {
					return null;
				}
				if (m < j) {
					byte[] localObject3 = new byte[m];
					System.arraycopy(localObject2, 0, localObject3, 0, m);
					localObject2 = localObject3;
				}
				Object localObject3 = V2CMiscUtil.byteArrayToString(
						(byte[]) localObject2, null,
						localHttpURLConnection.getContentType());
				if (localObject3 != null) {
					Matcher localMatcher;
					Object localObject4;
					URL localURL3;
					if ((str.equals("url4.eu")) || (str.equals("om.ly"))
							|| (str.equals("viigo.im"))
							|| (str.equals("nxy.in"))) {
						localMatcher = Pattern
								.compile(
										"<iframe\\s[^>]*src=\"(https?://[!-~&&[^\">]]+)\"",
										2).matcher((CharSequence) localObject3);
						if (localMatcher.find()) {
							return new URL(localMatcher.group(1));
						}
						if (str.equals("nxy.in")) {
							localObject4 = Pattern
									.compile(
											"Redirect\\('(https?://[!-~&&[^\">]]+)'",
											2).matcher(
											(CharSequence) localObject3);
							if (((Matcher) localObject4).find()) {
								localURL3 = new URL(localMatcher.group(1));
								return localURL3;
							}
						}
					} else {
						if (str.equals("i.s-a.cc")) {
							localMatcher = Pattern.compile(
									"<iframe\\s[^>]*src=\""
											+ paramURL.getPath()
											+ "\\?([!-~&&[^\">]]+)\"", 2)
									.matcher((CharSequence) localObject3);
							if (!localMatcher.find()) {
								return null;
							}
							localObject4 = Pattern.compile(
									"&utm_content=(http[^&]+)(?:&|$)").matcher(
									V2CResItem.replaceEscapes(localMatcher
											.group(1)));
							if (!((Matcher) localObject4).find()) {
								localURL3 = null;
								return localURL3;
							}
							localURL3 = new URL(URLDecoder.decode(
									((Matcher) localObject4).group(1), "UTF-8"));
							return localURL3;
						}
						if (str.equals("shar.es")) {
							localMatcher = Pattern
									.compile(
											"window\\.location='(https?://[!-~&&[^\"'>]]+)'",
											2).matcher(
											(CharSequence) localObject3);
							if (localMatcher.find()) {
								return new URL(localMatcher.group(1));
							}
						} else if (str.equals("p.tl")) {
							localMatcher = Pattern
									.compile(
											"\\bwindow\\.short_url *= *\"(https?://[!-~&&[^\"'>]]+)\"",
											2).matcher(
											(CharSequence) localObject3);
							if ((localMatcher.find())
									&& (localMatcher.group(1).equals(paramURL
											.toString()))) {
								localObject4 = Pattern
										.compile(
												"\\bwindow\\.long_url *= *\"(https?://[!-~&&[^\"'>]]+)\"",
												2).matcher(
												(CharSequence) localObject3);
								if (((Matcher) localObject4).find()) {
									localURL3 = new URL(
											((Matcher) localObject4).group(1));
									return localURL3;
								}
							}
						} else if (str.equals("ll.ly")) {
							localMatcher = Pattern
									.compile(
											"<a\\s[^>]*href=\"([^\"]+\\.axfc\\.net/uploader/[^\"]+)\"",
											2).matcher(
											(CharSequence) localObject3);
							if (localMatcher.find()) {
								return new URL(localMatcher.group(1));
							}
							localObject4 = Pattern
									.compile(
											"<h\\d>リンク先</h\\d>\\s*(?:<p>)?<a href=\"([^\"]+)\">")
									.matcher((CharSequence) localObject3);
							if (((Matcher) localObject4).find()) {
								return new URL(
										((Matcher) localObject4).group(1));
							}
						}
					}
				}
			}
			localHttpURLConnection.disconnect();
		} catch (Exception localException) {
			localException.printStackTrace();
			V2CLogger.logError(paramURL,
					V2CMiscUtil.getMessage(localException), localException);
			return null;
		} finally {
			V2CProxySetting.resetReadProxy();
			V2CLocalFileHandler.closeInputStream(localInputStream);
		}
		return null;
	}

	static CAndC postFormBG(String s, String s1, String s2, String s3,
			String s4, String s5) {
		PostFormBG postformbg = new PostFormBG(s, s1, s2, s3, s4, s5);
		return postformbg.start() ? postformbg.ccResult : null;
	}

	static CAndC postForm(String s, String s1) {
		return postForm(s, null, s1, null, null, false, null);
	}

	static CAndC postForm(String s, String s1, String s2, String s3) {
		return postForm(s, s1, s2, s3, null, false, null);
	}

	static CAndC postForm(String s, String s1, HashMap hashmap) {
		return postForm(s, null, s1, null, hashmap, false, null);
	}

	static CAndC postForm(String s, String s1, String s2, String s3,
			HashMap hashmap) {
		return postForm(s, s1, s2, s3, hashmap, false, null);
	}

	static CAndC postForm(String paramString1, String paramString2,
			String paramString3, String paramString4, HashMap paramHashMap,
			boolean paramBoolean, V2CProxyItem paramV2CProxyItem) {
		if (isLocalURL(paramString1))
			return null;
		if (!V2CMain.isOnlineMode()) {
			logNotOnline(paramString1);
			return new CAndC("Not Online");
		}
		URL localURL = null;
		try {
			localURL = new URL(paramString1);
		} catch (MalformedURLException localMalformedURLException) {
			V2CMiscUtil.printMalformedURLException(localMalformedURLException);
		}
		if (localURL == null) {
			V2CLogger.logError(localURL, "URL for posting form is null");
			return new CAndC("URL for posting form is null");
		}
		int i = paramString4 == null ? 1 : 0;
		InputStream localInputStream = null;
		PrintWriter localPrintWriter = null;
		BufferedReader localBufferedReader = null;
		String str1 = null;
		Object localObject1 = null;
		HttpURLConnection localHttpURLConnection = null;
		Thread localThread = Thread.currentThread();
		try {
			if (paramBoolean)
				localHttpURLConnection = V2CProxySetting.openWriteConnection(
						localURL, paramV2CProxyItem);
			else
				localHttpURLConnection = V2CProxySetting
						.openReadConnection(localURL);
			setTimeout(localHttpURLConnection);
			localHttpURLConnection.setRequestMethod("POST");
			localHttpURLConnection.setDoOutput(true);
			localHttpURLConnection.setUseCaches(false);
			localHttpURLConnection.setAllowUserInteraction(false);
			localHttpURLConnection.setRequestProperty("Host",
					localURL.getHost());
			localHttpURLConnection.setRequestProperty("Accept", "*/*");
			setUA(localHttpURLConnection, localURL);
			if (paramString1
					.startsWith("https://www.googleapis.com/urlshortener/v1/url"))
				localHttpURLConnection.setRequestProperty("Content-Type",
						"application/json");
			else
				localHttpURLConnection.setRequestProperty("Content-Type",
						"application/x-www-form-urlencoded");
			if (paramString2 != null)
				localHttpURLConnection.setRequestProperty("Referer",
						paramString2);
			if (i != 0)
				localHttpURLConnection.setRequestProperty("Accept-Encoding",
						"gzip");
			String str2 = V2CCookie.getCookie(localURL);
			if (str2 != null)
				localHttpURLConnection.setRequestProperty("Cookie", str2);
			Iterator localIterator;
			if (paramHashMap != null) {
				localIterator = paramHashMap.keySet().iterator();
				while (localIterator.hasNext()) {
					String localObject2 = (String) localIterator.next();
					localHttpURLConnection.setRequestProperty(
							(String) localObject2,
							(String) paramHashMap.get(localObject2));
				}
			}
			V2CLogger.logHTTPHeaderFields(localURL, "PostForm Request:",
					localHttpURLConnection.getRequestProperties());
			localPrintWriter = new PrintWriter(
					localHttpURLConnection.getOutputStream());
			if (localThread.isInterrupted()) {
				logInterrupt(localURL);
				return null;
			}
			localPrintWriter.print(paramString3);
			localPrintWriter.close();
			localPrintWriter = null;
			if (localThread.isInterrupted()) {
				logInterrupt(localURL);
				return null;
			}
			Object localObject2 = localHttpURLConnection.getHeaderFields();
			Object localObject3;
			int k;
			if (((Map) localObject2).containsKey("Set-Cookie")) {
				localObject3 = (List) ((Map) localObject2).get("Set-Cookie");
				for (k = 0; k < ((List) localObject3).size(); k++)
					V2CCookie.addCookie((String) ((List) localObject3).get(k),
							localURL);
			}
			logHTTPResponse(localHttpURLConnection, "PostForm Response: ");
			localInputStream = localHttpURLConnection.getInputStream();
			int m;
			if (i != 0) {
				localObject3 = localHttpURLConnection.getContentEncoding();
				k = (localObject3 != null)
						&& (((String) localObject3).equals("gzip")) ? 1 : 0;
				m = localHttpURLConnection.getContentLength();
				if (m == 0) {
					localObject1 = new byte[0];
				} else {
					int n = m > 0 ? 1 : 0;
					if (n == 0)
						m = nInitialContentLength;
					localObject1 = new byte[m];
					int i1 = 0;
					while (true) {
						int i2 = localInputStream.read((byte[]) localObject1,
								i1, m - i1);
						if (localThread.isInterrupted()) {
							logInterrupt(localURL);
							return null;
						}
						if (i2 < 0)
							break;
						i1 += i2;
						if (i1 >= m) {
							if ((n != 0) || (m >= nMaxContentLength))
								break;
							byte[] localObject5 = new byte[m * 2];
							System.arraycopy(localObject1, 0, localObject5, 0,
									m);
							m = localObject5.length;
							localObject1 = localObject5;
						}
					}
					if (n == 0) {
						if (i1 == m) {
							return null;
						}
						Object localObject4 = new byte[i1];
						System.arraycopy(localObject1, 0, localObject4, 0, i1);
						localObject1 = localObject4;
					}
					if (k != 0)
						localObject1 = V2CMiscUtil
								.unGZIP((byte[]) localObject1);
				}
			} else {
				localObject3 = new char[nBufLen];
				StringBuffer localStringBuffer = new StringBuffer();
				localBufferedReader = new BufferedReader(new InputStreamReader(
						localInputStream, paramString4));
				while ((m = localBufferedReader.read((char[]) localObject3)) >= 0)
					if (m > 0)
						localStringBuffer.append((char[]) localObject3, 0, m);
				str1 = localStringBuffer.toString();
			}
		} catch (IOException localIOException) {
			String str3 = V2CMiscUtil.getMessage(localIOException);
			V2CLogger.logError(localURL, str3, localIOException);
			CAndC localObject2 = new CAndC(localIOException.getClass()
					.getName() + ": " + str3, localHttpURLConnection,
					getErrorContents(localHttpURLConnection, localURL));
			return localObject2;
		} finally {
			if (paramBoolean) {
				V2CProxySetting.resetWriteProxy();
			} else {
				V2CProxySetting.resetReadProxy();
			}
			V2CLocalFileHandler.closeWriter(localPrintWriter);
			V2CLocalFileHandler.closeReader(localBufferedReader);
			V2CLocalFileHandler.closeInputStream(localInputStream);
			try {
				localInputStream.close();
			} catch (Exception ignore) {
			}
		}
		return i != 0
				? new CAndC(localHttpURLConnection, (byte[]) localObject1)
				: new CAndC(localHttpURLConnection, str1);
	}

	private static byte[] getErrorContents(
			HttpURLConnection paramHttpURLConnection, URL paramURL) {
		byte[] localObject1 = null;
		InputStream localInputStream = null;
		try {
			localInputStream = paramHttpURLConnection.getErrorStream();
			if (localInputStream == null) {
				return null;
			}
			Object localObject2 = paramHttpURLConnection.getContentEncoding();
			int i = (localObject2 != null)
					&& (((String) localObject2).equals("gzip")) ? 1 : 0;
			int j = paramHttpURLConnection.getContentLength();
			if (j == 0) {
				localObject1 = new byte[0];
			} else {
				int k = j > 0 ? 1 : 0;
				if (k == 0)
					j = 65536;
				localObject1 = new byte[j];
				int m = 0;
				while (true) {
					int n = localInputStream.read((byte[]) localObject1, m, j
							- m);
					if (Thread.currentThread().isInterrupted()) {
						logInterrupt(paramURL);
						return null;
					}
					if ((n < 0) || (m + n >= j))
						break;
					m += n;
				}
				if (k == 0) {
					if (m == j) {
						return null;
					}
					byte[] arrayOfByte2 = new byte[m];
					System.arraycopy(localObject1, 0, arrayOfByte2, 0, m);
					localObject1 = arrayOfByte2;
				}
				if (i != 0)
					localObject1 = V2CMiscUtil.unGZIP((byte[]) localObject1);
			}
		} catch (IOException localIOException) {
			String str = V2CMiscUtil.getMessage(localIOException);
			V2CLogger.logError(paramURL, str, localIOException);
			return null;
		} finally {
			V2CLocalFileHandler.closeInputStream(localInputStream);
			try {
				localInputStream.close();
			} catch (Exception ignore) {
			}
		}
		return localObject1;
	}

	static CAndC postMessage(URL url, String s, String s1, String s2) {
		return postMessage(url, s, s1, null, null, s2, null, true);
	}

	static CAndC postMessage(URL url, String s, V2CProxyItem v2cproxyitem,
			String s1, HashMap hashmap) {
		return postMessage(url, null, s, null, v2cproxyitem, s1, hashmap, false);
	}

	static CAndC postMessage(URL url, String s, String s1,
			V2CProxyItem v2cproxyitem, String s2, boolean flag) {
		return postMessage(url, s, s1, null, v2cproxyitem, s2, null, flag);
	}

	static CAndC postMessage(URL url, String s, String s1,
			V2CBeIDListItem v2cbeidlistitem, V2CProxyItem v2cproxyitem,
			String s2) {
		return postMessage(url, s, s1, v2cbeidlistitem, v2cproxyitem, s2, null,
				true);
	}

	
	private static CAndC postMessage(URL paramURL, String paramString1,
			String paramString2, V2CBeIDListItem paramV2CBeIDListItem,
			V2CProxyItem paramV2CProxyItem, String paramString3,
			HashMap paramHashMap, boolean paramBoolean) {
		if (paramURL == null) {
			V2CLogger.logError(paramURL, "URL for posting is null");
			return new CAndC("URL for posting is null");
		}
		if (isLocalURL(paramURL))
			return null;
		if (!V2CMain.isOnlineMode()) {
			logNotOnline(paramURL);
			return new CAndC("Not Online");
		}
		paramURL = checkShitarabaURL(paramURL);
		String str1 = paramURL.getHost();
		paramURL = checkPreferIPv6(paramURL);
		char[] arrayOfChar = new char[nBufLen];
		StringBuffer localStringBuffer = new StringBuffer();
		PrintWriter localPrintWriter = null;
		BufferedReader localBufferedReader = null;
		HttpURLConnection localHttpURLConnection = null;
		Thread localThread = Thread.currentThread();
		try {
			localHttpURLConnection = V2CProxySetting.openWriteConnection(
					paramURL, paramV2CProxyItem);
			setTimeout(localHttpURLConnection);
			localHttpURLConnection.setRequestMethod("POST");
			localHttpURLConnection.setDoOutput(true);
			localHttpURLConnection.setUseCaches(false);
			localHttpURLConnection.setAllowUserInteraction(false);
			localHttpURLConnection.setRequestProperty("Host", str1);
			localHttpURLConnection.setRequestProperty("Accept", "*/*");			
			localHttpURLConnection.setRequestProperty("User-Agent",
					paramBoolean ? getUAName(false, true, is2ch.matcher(paramURL.toString()).matches()) : "Mozilla/4.0 (compatible)");
			localHttpURLConnection.setRequestProperty("Accept-Language", "ja-JP,en-US;q=0.5");
			localHttpURLConnection.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			if (paramString1 != null)
				localHttpURLConnection.setRequestProperty("Referer",
						paramString1);
			String str2 = V2CCookie.getCookie(paramURL, paramV2CBeIDListItem);
			if (str2 != null)
				localHttpURLConnection.setRequestProperty("Cookie", str2);
			Iterator localIterator;
			if (paramHashMap != null) {
				localIterator = paramHashMap.keySet().iterator();
				while (localIterator.hasNext()) {
					Object localObject1 = localIterator.next();
					if ((localObject1 instanceof String)) {
						Object localObject2 = paramHashMap.get(localObject1);
						if ((localObject2 instanceof String))
							localHttpURLConnection.setRequestProperty(
									(String) localObject1,
									(String) localObject2);
					}
				}
			}
			V2CLogger.logHTTPHeaderFields(paramURL, "Post Request:",
					localHttpURLConnection.getRequestProperties());
			localPrintWriter = new PrintWriter(
					localHttpURLConnection.getOutputStream());
			if (localThread.isInterrupted()) {
				logInterrupt(paramURL);
				return null;
			}
			if (paramString2 != null)
				localPrintWriter.print(paramString2);
			localPrintWriter.close();
			localPrintWriter = null;
			if (localThread.isInterrupted()) {
				logInterrupt(paramURL);
				return null;
			}
			Object localObject1 = localHttpURLConnection.getHeaderFields();
			if (((Map) localObject1).containsKey("Set-Cookie")) {
				Object localObject2 = (List) ((Map) localObject1)
						.get("Set-Cookie");
				for (int j = 0; j < ((List) localObject2).size(); j++)
					V2CCookie.addCookie((String) ((List) localObject2).get(j),
							paramURL);
			}
			logHTTPResponse(localHttpURLConnection, "Post Response: ");
			Object localObject2 = localHttpURLConnection.getInputStream();
			String str5 = localHttpURLConnection.getContentEncoding();
			if ((str5 != null) && (str5.matches("gzip|deflate")))
				localObject2 = new GZIPInputStream((InputStream) localObject2);
			localBufferedReader = new BufferedReader(new InputStreamReader(
					(InputStream) localObject2, paramString3));
			int k;
			while ((k = localBufferedReader.read(arrayOfChar)) >= 0)
				if (k > 0)
					localStringBuffer.append(arrayOfChar, 0, k);
		} catch (IOException localIOException) {
			String str4 = V2CMiscUtil.getMessage(localIOException);
			V2CLogger.logError(paramURL, str4, localIOException);
			return new CAndC(localIOException.getClass().getName() + ": "
					+ str4, localHttpURLConnection, getErrorContents(
					localHttpURLConnection, paramURL));
		} finally {
			V2CProxySetting.resetWriteProxy();
			V2CLocalFileHandler.closeWriter(localPrintWriter);
			V2CLocalFileHandler.closeReader(localBufferedReader);
		}
		String str3 = localStringBuffer.toString();
		return new CAndC(localHttpURLConnection, str3);
	}

	static String encodeMIMEParameterValue(String s) {
		if (s == null)
			return null;
		byte abyte0[] = V2CJPConverter.getBytes(s, "UTF-8");
		byte abyte1[] = blToken;
		StringBuffer stringbuffer = new StringBuffer();
		for (int i = 0; i < abyte0.length; i++) {
			byte byte0 = abyte0[i];
			if (byte0 > 0 && abyte1[byte0] != 0) {
				stringbuffer.append((char) byte0);
			} else {
				stringbuffer.append('%');
				String s1 = "00"
						+ Integer.toHexString(byte0 & 0xff).toUpperCase();
				stringbuffer.append(s1.substring(s1.length() - 2));
			}
		}

		return stringbuffer.toString();
	}

	private static byte[] generateMultiPartBoundary() {
		Random random = new Random(System.currentTimeMillis());
		byte abyte0[] = new byte[24];
		for (int i = 0; i < abyte0.length; i++) {
			int j = Math.abs(random.nextInt()) % 62;
			if (j < 26)
				j += 65;
			else if (j < 52)
				j += 71;
			else
				j -= 4;
			abyte0[i] = (byte) j;
		}

		return abyte0;
	}

	static String getMultiPartBoundary() {
		return "---------------------------"
				+ new String(generateMultiPartBoundary());
	}

	static String getMultiPartBoundary(String[] paramArrayOfString,
			byte[] paramArrayOfByte) {
		byte[][] arrayOfByte = new byte[paramArrayOfString.length][];
		try {
			for (int i = 0; i < paramArrayOfString.length; i++)
				if (paramArrayOfString[i] != null)
					arrayOfByte[i] = paramArrayOfString[i].getBytes("MS932");
		} catch (UnsupportedEncodingException localUnsupportedEncodingException) {
			return null;
		}
		byte[] arrayOfByte1 = null;
		label139 : for (int j = 0; j < 100; j++) {
			arrayOfByte1 = generateMultiPartBoundary();
			for (int k = 0; k < arrayOfByte.length; k++)
				if ((arrayOfByte[k] != null)
						&& (containsByteArray(arrayOfByte[k], arrayOfByte1)))
					break label139;
			if ((paramArrayOfByte == null)
					|| (!containsByteArray(paramArrayOfByte, arrayOfByte1)))
				return "---------------------------" + new String(arrayOfByte1);
		}
		return null;
	}

	private static boolean containsByteArray(byte[] paramArrayOfByte1,
			byte[] paramArrayOfByte2) {
		int i = paramArrayOfByte2[0];
		label59 : for (int j = paramArrayOfByte1.length
				- paramArrayOfByte2.length; j >= 0; j--)
			if (paramArrayOfByte1[j] == i) {
				for (int k = paramArrayOfByte2.length - 1; k > 0; k--)
					if (paramArrayOfByte2[k] != paramArrayOfByte1[(j + k)])
						break label59;
				return true;
			}
		return false;
	}

	static CAndC postMultiPartMessage(URL paramURL, String paramString1,
			String paramString2, byte[] paramArrayOfByte,
			V2CProxyItem paramV2CProxyItem, String paramString3) {
		return postMultiPartMessage(paramURL, paramString1, paramString2,
				paramArrayOfByte, paramV2CProxyItem, paramString3, null);
	}

	static CAndC postMultiPartMessage(URL paramURL, String paramString1,
			String paramString2, byte[] paramArrayOfByte,
			V2CProxyItem paramV2CProxyItem, String paramString3,
			HashMap paramHashMap) {
		if (paramURL == null) {
			V2CLogger.logError(paramURL, "URL for posting is null");
			return new CAndC("URL for posting is null");
		}
		if (isLocalURL(paramURL))
			return null;
		if (!V2CMain.isOnlineMode()) {
			logNotOnline(paramURL);
			return new CAndC("Not Online");
		}
		String str1 = paramURL.getHost();
		paramURL = checkShitarabaURL(paramURL);
		char[] arrayOfChar = new char[nBufLen];
		StringBuffer localStringBuffer = new StringBuffer();
		BufferedReader localBufferedReader = null;
		HttpURLConnection localHttpURLConnection = null;
		Thread localThread = Thread.currentThread();
		try {
			localHttpURLConnection = V2CProxySetting.openWriteConnection(
					paramURL, paramV2CProxyItem);
			setTimeout(localHttpURLConnection);
			localHttpURLConnection.setRequestMethod("POST");
			localHttpURLConnection.setDoOutput(true);
			localHttpURLConnection.setUseCaches(false);
			localHttpURLConnection.setAllowUserInteraction(false);
			localHttpURLConnection.setRequestProperty("Host", str1);
			localHttpURLConnection.setRequestProperty("Accept", "*/*");
			if (str1.endsWith("ula.cc"))
				localHttpURLConnection.setRequestProperty("User-Agent",
						getUAName(false, true, true));
			else
				localHttpURLConnection.setRequestProperty("User-Agent",
						"Mozilla/4.0 (compatible)");
			if (paramString1 != null)
				localHttpURLConnection.setRequestProperty("Referer",
						paramString1);
			String str2 = V2CCookie.getCookie(paramURL);
			if (str2 != null)
				localHttpURLConnection.setRequestProperty("Cookie", str2);
			localHttpURLConnection.setRequestProperty("Content-Type",
					"multipart/form-data; boundary=" + paramString2);
			Object localObject2;
			if (paramHashMap != null) {
				Iterator localObject1 = paramHashMap.keySet().iterator();
				while (((Iterator) localObject1).hasNext()) {
					localObject2 = ((Iterator) localObject1).next();
					if ((localObject2 instanceof String)) {
						Object localObject3 = paramHashMap.get(localObject2);
						if ((localObject3 instanceof String))
							localHttpURLConnection.setRequestProperty(
									(String) localObject2,
									(String) localObject3);
					}
				}
			}
			V2CLogger.logHTTPHeaderFields(paramURL, "MPPost Request:",
					localHttpURLConnection.getRequestProperties());
			OutputStream localObject1 = localHttpURLConnection
					.getOutputStream();
			if (localThread.isInterrupted()) {
				logInterrupt(paramURL);
				return null;
			}
			((OutputStream) localObject1).write(paramArrayOfByte);
			((OutputStream) localObject1).close();
			if (localThread.isInterrupted()) {
				logInterrupt(paramURL);
				return null;
			}
			Object localObject3 = localHttpURLConnection.getHeaderFields();
			if (((Map) localObject3).containsKey("Set-Cookie")) {
				List localList = (List) ((Map) localObject3).get("Set-Cookie");
				for (int k = 0; k < localList.size(); k++)
					V2CCookie.addCookie((String) localList.get(k), paramURL);
			}
			logHTTPResponse(localHttpURLConnection, "MPPost Response: ");
			localBufferedReader = new BufferedReader(new InputStreamReader(
					localHttpURLConnection.getInputStream(), paramString3));
			int j;
			while ((j = localBufferedReader.read(arrayOfChar)) >= 0) {
				if (j > 0) {
					localStringBuffer.append(arrayOfChar, 0, j);
				}
			}
		} catch (IOException localIOException) {
			Object localObject1 = V2CMiscUtil.getMessage(localIOException);
			V2CLogger.logError(paramURL, (String) localObject1,
					localIOException);
			CAndC localCAndC = new CAndC(localIOException.getClass().getName()
					+ ": " + (String) localObject1, localHttpURLConnection,
					getErrorContents(localHttpURLConnection, paramURL));
			return localCAndC;
		} finally {
			V2CProxySetting.resetWriteProxy();
			V2CLocalFileHandler.closeReader(localBufferedReader);
		}
		String str3 = localStringBuffer.toString();
		return new CAndC(localHttpURLConnection, str3);
	}

	static CAndC getHTTPFileBG(String paramString1, String paramString2,
			String paramString3, String paramString4, long paramLong) {
		return getHTTPFileBG(paramString1, null, paramString2, paramString3,
				paramString4, paramLong);
	}

	static CAndC getHTTPFileBG(String paramString1, String paramString2,
			String paramString3, String paramString4, String paramString5,
			long paramLong) {
		GetHTTPFileBG localGetHTTPFileBG = new GetHTTPFileBG(paramString1,
				paramString2, paramString3, paramString4, paramString5,
				paramLong);
		return localGetHTTPFileBG.start() ? localGetHTTPFileBG.ccResult : null;
	}

	static CAndC getHTTPFileBG(String paramString1, String paramString2,
			String paramString3, String paramString4, String paramString5,
			long paramLong, int paramInt) {
		GetHTTPFileBG localGetHTTPFileBG = new GetHTTPFileBG(paramString1,
				paramString2, paramString3, paramString4, paramString5,
				paramLong, paramInt);
		return localGetHTTPFileBG.start() ? localGetHTTPFileBG.ccResult : null;
	}

	static String getHTMLFile(String paramString) {
		return getHTMLFile(paramString, "MS932");
	}

	static String getHTMLFile(String paramString1, String paramString2) {
		return getHTMLFile(paramString1, paramString2, 0);
	}

	static String getHTMLFile(String paramString1, String paramString2,
			int paramInt) {
		CAndC localCAndC = getHTTPFile(paramString1, null, paramString2,
				"text/html", 0L, paramInt);
		if (localCAndC != null)
			return localCAndC.getContents();
		return null;
	}

	static CAndC getTextFile(String paramString) {
		return getHTTPFile(paramString, null, "MS932", "text/plain", 0L);
	}

	static CAndC getTextFile(String paramString1, String paramString2) {
		return getHTTPFile(paramString1, null, paramString2, "text/plain", 0L);
	}

	static CAndC getGenTextFile(String paramString1, String paramString2) {
		return getHTTPFile(paramString1, null, paramString2, "text/", 0L);
	}

	static CAndC getHTTPFile(String url, String paramString2,
			String paramString3, String paramString4, long paramLong) {
		return getHTTPFile(url, paramString2, paramString3, paramString4,
				paramLong, 0);
	}

	static CAndC getHTTPFile(String url, String paramString2,
			String paramString3, String paramString4, long paramLong,
			int paramInt) {
		return getHTTPFile(url, paramString2, paramString3, paramString4,
				paramLong, paramInt, false);
	}

	static CAndC getHTTPFile(String url, String paramString2,
			String paramString3, String paramString4, long paramLong,
			int paramInt, boolean paramBoolean) {
		if (isLocalURL(url))
			return null;
		if (!V2CMain.isOnlineMode()) {
			logNotOnline(url);
			return null;
		}
		URL localURL = null;
		try {
			localURL = new URL(url);
		} catch (MalformedURLException localMalformedURLException) {
			V2CLogger.logError(url,
					V2CMiscUtil.getMessage(localMalformedURLException),
					localMalformedURLException);
			V2CMiscUtil.printMalformedURLException(localMalformedURLException);
			return null;
		}
		localURL = checkShitarabaURL(localURL);
		char[] arrayOfChar = new char[nBufLen];
		StringBuffer localStringBuffer = new StringBuffer();
		InputStreamReader localInputStreamReader = null;
		HttpURLConnection localHttpURLConnection = null;
		Thread localThread = Thread.currentThread();
		RemoteHost localRemoteHost = getRemoteHost(localURL);
		localRemoteHost.start();
		try {
			if (localThread.isInterrupted()) {
				logInterrupt(localURL);
				return null;
			}
			if (paramBoolean)
				localHttpURLConnection = V2CProxySetting
						.openWriteConnection(localURL);
			else
				localHttpURLConnection = V2CProxySetting
						.openReadConnection(localURL);
			setTimeout(localHttpURLConnection);
			localHttpURLConnection.setRequestProperty("Host",
					localURL.getHost());
			localHttpURLConnection.setRequestProperty("Accept", "*/*");
			setUA(localHttpURLConnection, localURL);
			localHttpURLConnection.setRequestProperty("Accept-Language",
					"ja,en;q=0.5");
			localHttpURLConnection
					.setRequestProperty("Accept-Encoding", "gzip");
			if (paramLong > 0L)
				localHttpURLConnection.setIfModifiedSince(paramLong);
			Object localObject1 = V2CCookie.getCookie(localURL);
			if (localObject1 != null)
				localHttpURLConnection.setRequestProperty("Cookie",
						(String) localObject1);
			V2CLogger.logHTTPHeaderFields(localURL, "File Request:",
					localHttpURLConnection.getRequestProperties());
			localRemoteHost.check();
			int i = localHttpURLConnection.getResponseCode();
			Object localObject2 = localHttpURLConnection.getHeaderFields();
			if (((Map) localObject2).containsKey("Set-Cookie")) {
				List localObject3 = (List) ((Map) localObject2)
						.get("Set-Cookie");
				for (int j = 0; j < ((List) localObject3).size(); j++)
					V2CCookie.addCookie((String) ((List) localObject3).get(j),
							localURL);
			}
			if (localThread.isInterrupted()) {
				logInterrupt(localURL);
				return null;
			}
			logHTTPResponse(localHttpURLConnection, "File Response: ");
			if ((i == 304) || (i == 404)) {
				return new CAndC(localHttpURLConnection, (String) null);
			}
			CAndC localCAndC;
			if ((isRedirect(i)) && (paramInt > 0)) {
				paramInt--;
				Object localObject3 = localHttpURLConnection
						.getHeaderField("Location");
				if (localObject3 == null) {
					localCAndC = new CAndC("Response Code: " + i
							+ " Locationが不明です。");
					return localCAndC;
				}
				localCAndC = getHTTPFile((String) localObject3, paramString2,
						paramString3, paramString4, paramLong, paramInt,
						paramBoolean);
				return localCAndC;
			}
			if (i != 200) {
				return new CAndC("Response Code: " + i + " "
						+ localHttpURLConnection.getResponseMessage());
			}
			Object localObject3 = localHttpURLConnection.getContentType();
			if ((paramString4 != null)
					&& ((localObject3 == null) || (!V2CMiscUtil
							.contentTypeStartsWith((String) localObject3,
									paramString4, false)))) {
				localCAndC = null;
				return localCAndC;
			}
			int k = localHttpURLConnection.getContentLength();
			if (k > 0)
				localStringBuffer.ensureCapacity(k);
			Object localObject4 = localHttpURLConnection.getInputStream();
			String str3 = localHttpURLConnection.getContentEncoding();
			if ((str3 != null) && (str3.equals("gzip")))
				localObject4 = new GZIPInputStream((InputStream) localObject4);
			String str4;
			if (paramString3 == null) {
				if (localObject3 != null) {
					Matcher localMatcher = Pattern.compile(
							"text/(?:plain|html);charset=([\\w-]+)").matcher(
							(CharSequence) localObject3);
					if (localMatcher.matches()) {
						str4 = localMatcher.group(1);
						if (str4.equals("utf-8"))
							paramString3 = "UTF-8";
						else if (str4.equals("euc-jp"))
							paramString3 = "EUC-JP";
					}
				}
				if (paramString3 == null)
					paramString3 = "MS932";
			}
			localInputStreamReader = new InputStreamReader(
					(InputStream) localObject4, paramString3);
			while (true) {
				localRemoteHost.check();
				int m = localInputStreamReader.read(arrayOfChar);
				if (localThread.isInterrupted()) {
					logInterrupt(localURL);
					return null;
				}
				if (m < 0)
					break;
				if (m > 0)
					localStringBuffer.append(arrayOfChar, 0, m);
			}
		} catch (IOException localIOException) {
			String str2 = V2CMiscUtil.getMessage(localIOException);
			V2CLogger.logError(localURL, str2, localIOException);
			return new CAndC(localIOException.getClass().getName() + ": "
					+ str2);
		} finally {
			if (paramBoolean)
				V2CProxySetting.resetWriteProxy();
			else
				V2CProxySetting.resetReadProxy();
			localRemoteHost.finished();
			V2CLocalFileHandler.closeReader(localInputStreamReader);
		}
		String str1 = localStringBuffer.toString();
		return new CAndC(localHttpURLConnection, str1);
	}

	static CAndC getRawTextFile(String paramString) {
		return getRawHTTPFile(paramString, null, "text/plain", 0L, null, 0,
				null);
	}

	static CAndC getRawTextFile(String paramString, HashMap paramHashMap) {
		return getRawHTTPFile(paramString, null, "text/plain", 0L, null, 0,
				paramHashMap);
	}

	static CAndC getRawTextFile(String paramString, long paramLong) {
		return getRawHTTPFile(paramString, null, "text/plain", paramLong, null,
				0, null);
	}

	static CAndC getRawTextFile(String paramString1, long paramLong,
			String paramString2) {
		return getRawHTTPFile(paramString1, null, "text/plain", paramLong,
				paramString2, 0, null);
	}

	static CAndC getRawHTTPFile(String paramString) {
		return getRawHTTPFile(paramString, null, null, 0L, null, 0, null);
	}

	static CAndC getRawHTTPFile(String paramString, int paramInt,
			HashMap paramHashMap) {
		return getRawHTTPFile(paramString, null, null, 0L, null, paramInt,
				paramHashMap);
	}

	static CAndC getRawHTTPFile(String paramString1, String paramString2,
			long paramLong) {
		return getRawHTTPFile(paramString1, null, paramString2, paramLong,
				null, 0, null);
	}

	static CAndC getRawHTTPFile(String paramString1, String paramString2,
			long paramLong, int paramInt) {
		return getRawHTTPFile(paramString1, null, paramString2, paramLong,
				null, paramInt, null);
	}

	static CAndC getRawHTTPFile(String paramString1, String paramString2,
			long paramLong, String paramString3, int paramInt) {
		return getRawHTTPFile(paramString1, null, paramString2, paramLong,
				paramString3, paramInt, null);
	}

	static CAndC getRawHTTPFile(String paramString1, String paramString2,
			String paramString3, long paramLong, String paramString4,
			int paramInt) {
		return getRawHTTPFile(paramString1, paramString2, paramString3,
				paramLong, paramString4, paramInt, null);
	}

	static CAndC getRawHTTPFile(String paramString1, String paramString2,
			String paramString3, long paramLong, String paramString4,
			int paramInt, HashMap paramHashMap) {
		if (isLocalURL(paramString1))
			return null;
		if (!V2CMain.isOnlineMode()) {
			logNotOnline(paramString1);
			return null;
		}
		URL localURL = null;
		try {
			localURL = new URL(paramString1);
		} catch (MalformedURLException localMalformedURLException1) {
			V2CLogger.logError(paramString1,
					V2CMiscUtil.getMessage(localMalformedURLException1),
					localMalformedURLException1);
			V2CMiscUtil.printMalformedURLException(localMalformedURLException1);
			return null;
		}
		localURL = checkShitarabaURL(localURL);
		Object localObject1 = null;
		InputStream localInputStream = null;
		HttpURLConnection localHttpURLConnection = null;
		Thread localThread = Thread.currentThread();
		RemoteHost localRemoteHost = getRemoteHost(localURL);
		localRemoteHost.start();
		try {
			if (localThread.isInterrupted()) {
				logInterrupt(localURL);
				return null;
			}
			localHttpURLConnection = V2CProxySetting
					.openReadConnection(localURL);
			setTimeout(localHttpURLConnection);
			localHttpURLConnection.setRequestProperty("Host",
					localURL.getHost());
			localHttpURLConnection.setRequestProperty("Accept", "*/*");
			setUA(localHttpURLConnection, localURL);
			if (paramString2 != null)
				localHttpURLConnection.setRequestProperty("Referer",
						paramString2);
			localHttpURLConnection.setRequestProperty("Accept-Language",
					"ja,en;q=0.5");
			localHttpURLConnection
					.setRequestProperty("Accept-Encoding", "gzip");
			if (paramLong > 0L)
				localHttpURLConnection.setIfModifiedSince(paramLong);
			if ((paramString4 != null) && (paramString4.length() > 0))
				localHttpURLConnection.setRequestProperty("If-None-Match",
						paramString4);
			Object localObject2 = V2CCookie.getCookie(localURL);
			if (localObject2 != null)
				localHttpURLConnection.setRequestProperty("Cookie",
						(String) localObject2);
			int i = 0;
			int k;
			if (paramHashMap != null) {
				Iterator localIterator = paramHashMap.keySet().iterator();
				while (localIterator.hasNext()) {
					Object localObject3 = localIterator.next();
					Object localObject4 = paramHashMap.get(localObject3);
					if ((localObject3 instanceof String)) {
						if ((localObject4 instanceof String))
							localHttpURLConnection.setRequestProperty(
									(String) localObject3,
									(String) localObject4);
					} else if ((localObject3 instanceof Character)) {
						k = ((Character) localObject3).charValue();
						if ((k == 77)
								&& (V2CMiscUtil.isEqual(localObject4, "HEAD"))) {
							localHttpURLConnection.setRequestMethod("HEAD");
							i = 1;
						}
					}
				}
			}
			V2CLogger.logHTTPHeaderFields(localURL, "RawFile Request:",
					localHttpURLConnection.getRequestProperties());
			localRemoteHost.check();
			int j = localHttpURLConnection.getResponseCode();
			if (localThread.isInterrupted()) {
				logInterrupt(localURL);
				return null;
			}
			if (i != 0) {
				logHTTPResponse(localHttpURLConnection, "HEAD Response: ");
				return new CAndC(localHttpURLConnection, new byte[0]);
			}
			Object localObject3 = localHttpURLConnection.getHeaderFields();
			if (((Map) localObject3).containsKey("Set-Cookie")) {
				List localObject4 = (List) ((Map) localObject3)
						.get("Set-Cookie");
				for (k = 0; k < ((List) localObject4).size(); k++)
					V2CCookie.addCookie((String) ((List) localObject4).get(k),
							localURL);
			}
			if (localThread.isInterrupted()) {
				logInterrupt(localURL);
				return null;
			}
			logHTTPResponse(localHttpURLConnection, "RawFile Response: ");
			if ((isRedirect(j)) && (paramInt > 0)) {
				paramInt--;
				Object localObject4 = localHttpURLConnection
						.getHeaderField("Location");
				if (localObject4 == null) {
					return null;
				}
				URL localObject5 = null;
				try {
					localObject5 = new URL(localURL, (String) localObject4);
				} catch (MalformedURLException localMalformedURLException2) {
					CAndC localCAndC3 = null;
					return localCAndC3;
				}
				CAndC localCAndC2 = getRawHTTPFile(
						((URL) localObject5).toExternalForm(), paramString2,
						paramString3, paramLong, paramString4, paramInt,
						paramHashMap);
				return localCAndC2;
			}
			if ((j == 302) || (j == 304) || (j == 404)) {
				return new CAndC(localHttpURLConnection);
			}
			if ((j == 401) && (paramString1.indexOf("twitter.com/") > 0)) {
				return new CAndC("Response Code: "
						+ localHttpURLConnection.getResponseCode() + " "
						+ localHttpURLConnection.getResponseMessage(),
						localHttpURLConnection);
			}
			if ((j != 200)
					&& (j != 203)
					&& ((paramString1.indexOf("twitter.com/") <= 0) || (j < 400))) {
				return new CAndC("Response Code: "
						+ localHttpURLConnection.getResponseCode() + " "
						+ localHttpURLConnection.getResponseMessage());
			}
			Object localObject4 = localHttpURLConnection.getContentType();
			if ((localObject4 == null)
					|| ((paramString3 != null) && (!V2CMiscUtil
							.contentTypeStartsWith((String) localObject4,
									paramString3, false)))) {
				return null;
			}
			localInputStream = localHttpURLConnection.getInputStream();
			Object localObject5 = localHttpURLConnection.getContentEncoding();
			int m = (localObject5 != null)
					&& (((String) localObject5).equals("gzip")) ? 1 : 0;
			int n = localHttpURLConnection.getContentLength();
			if (n == 0) {
				localObject1 = new byte[0];
			} else {
				int i1 = n > 0 ? 1 : 0;
				if (i1 == 0)
					n = nInitialContentLength;
				localObject1 = new byte[n];
				int i2 = 0;
				while (true) {
					localRemoteHost.check();
					int i3 = localInputStream.read((byte[]) localObject1, i2, n
							- i2);
					if (localThread.isInterrupted()) {
						logInterrupt(localURL);
						return null;
					}
					if (i3 < 0)
						break;
					i2 += i3;
					if (i2 >= n) {
						if ((i1 != 0) || (n >= nMaxContentLength))
							break;
						byte[] localObject7 = new byte[n * 2];
						System.arraycopy(localObject1, 0, localObject7, 0, n);
						n = localObject7.length;
						localObject1 = localObject7;
					}
				}
				if (i1 == 0) {
					if (i2 == n) {
						return null;
					}
					Object localObject6 = new byte[i2];
					System.arraycopy(localObject1, 0, localObject6, 0, i2);
					localObject1 = localObject6;
				}
				if (m != 0)
					localObject1 = V2CMiscUtil.unGZIP((byte[]) localObject1);
			}
		} catch (IOException localIOException) {
			String str = V2CMiscUtil.getMessage(localIOException);
			V2CLogger.logError(localURL, str, localIOException);
			CAndC localCAndC1 = new CAndC(localIOException.getClass().getName()
					+ ": " + str, localHttpURLConnection, getErrorContents(
					localHttpURLConnection, localURL));
			return localCAndC1;
		} finally {
			V2CProxySetting.resetReadProxy();
			localRemoteHost.finished();
			V2CLocalFileHandler.closeInputStream(localInputStream);
			try {
				localInputStream.close();
			} catch (Exception ignore) {
			}
		}
		return new CAndC(localHttpURLConnection, (byte[]) localObject1);
	}

	static HTMLDocument getHTMLDocument(String paramString) {
		return getHTMLDocument(paramString, "MS932");
	}

	static HTMLDocument getHTMLDocument(String paramString1, String paramString2) {
		String str = getHTMLFile(paramString1, paramString2);
		if (str == null)
			return null;
		return parseHTMLDocument(str);
	}

	static HTMLDocument parseHTMLDocument(String paramString) {
		StringReader localStringReader = new StringReader(paramString);
		HTMLEditorKit localHTMLEditorKit = new HTMLEditorKit();
		HTMLDocument localHTMLDocument = (HTMLDocument) localHTMLEditorKit
				.createDefaultDocument();
		localHTMLDocument.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
		try {
			localHTMLEditorKit.read(localStringReader, localHTMLDocument, 0);
		} catch (IOException localIOException) {
			System.out.println(localIOException);
			localIOException.printStackTrace();
			return null;
		} catch (BadLocationException localBadLocationException) {
			System.out.println(localBadLocationException);
			localBadLocationException.printStackTrace();
			return null;
		}
		return localHTMLDocument;
	}

	static String downloadFile(String paramString, int paramInt,
			V2CDownloader paramV2CDownloader) {
		if (!V2CMain.isOnlineMode()) {
			logNotOnline(paramString);
			return "オンラインではありません。";
		}
		URL localURL = null;
		try {
			localURL = new URL(paramString);
		} catch (MalformedURLException localMalformedURLException1) {
			V2CLogger.logError(paramString,
					V2CMiscUtil.getMessage(localMalformedURLException1),
					localMalformedURLException1);
			V2CMiscUtil.printMalformedURLException(localMalformedURLException1);
			return "不正なURLです。";
		}
		InputStream localInputStream = null;
		HttpURLConnection localHttpURLConnection = null;
		Thread localThread = Thread.currentThread();
		if (localThread.isInterrupted()) {
			logInterrupt(localURL);
			return null;
		}
		try {
			localHttpURLConnection = V2CProxySetting
					.openReadConnection(localURL);
			setTimeout(localHttpURLConnection);
			localHttpURLConnection.setRequestProperty("Host",
					localURL.getHost());
			localHttpURLConnection.setRequestProperty("Accept", "*/*");
			localHttpURLConnection
					.setRequestProperty("Accept-Encoding", "gzip");
			setUA(localHttpURLConnection, localURL);
			String str1 = V2CCookie.getCookie(localURL);
			if (str1 != null)
				localHttpURLConnection.setRequestProperty("Cookie", str1);
			V2CLogger.logHTTPHeaderFields(localURL, "Download Request:",
					localHttpURLConnection.getRequestProperties());
			localInputStream = localHttpURLConnection.getInputStream();
			int i = localHttpURLConnection.getResponseCode();
			String str3;
			if (localThread.isInterrupted()) {
				logInterrupt(localURL);
				str3 = null;
				return str3;
			}
			logHTTPResponse(localHttpURLConnection, "Download Response: ");
			if ((isRedirect(i)) && (paramInt > 0)) {
				paramInt--;
				str3 = localHttpURLConnection.getHeaderField("Location");
				if (str3 == null) {
					return "Locationが不明です。";
				}
				try {
					localURL = new URL(localURL, str3);
				} catch (MalformedURLException localMalformedURLException2) {
					return "不正なURLです。";
				}
				return downloadFile(str3, paramInt, paramV2CDownloader);
			}
			if (!paramV2CDownloader.setHeaderFields(localHttpURLConnection)) {
				return null;
			}
			int j = localHttpURLConnection.getContentLength();
			if (j == 0) {
				return null;
			}
			int k = (j > 0) && (j < 2048) ? j : 2048;
			Object localObject2 = new byte[k];
			int m = 0;
			while (true) {
				int n = localInputStream.read((byte[]) localObject2);
				if (localThread.isInterrupted()) {
					logInterrupt(localURL);
					return null;
				}
				if (n < 0)
					break;
				if (n > 0) {
					m += n;
					if (!paramV2CDownloader.appendBytes((byte[]) localObject2,
							n, m)) {
						return null;
					}
				}
				if ((j > 0) && (m >= j))
					break;
			}
		} catch (IOException localIOException) {
			String str2 = V2CMiscUtil.getMessage(localIOException);
			V2CLogger.logError(localURL, str2, localIOException);
			String str4 = str2;
			return str4;
		} finally {
			V2CProxySetting.resetReadProxy();
			V2CLocalFileHandler.closeInputStream(localInputStream);
			try {
				localInputStream.close();
			} catch (Exception ignore) {
			}
		}
		return null;
	}
	static boolean isOAuthTimeOffsetChanged(long paramLong) {
		return Math.abs(nOAuthTimeOffset - paramLong) > 120000L;
	}

	static String constructQuery(HashMap paramHashMap) {
		if ((paramHashMap == null) || (paramHashMap.size() == 0))
			return null;
		StringBuffer localStringBuffer = new StringBuffer();
		Iterator localIterator = paramHashMap.keySet().iterator();
		while (localIterator.hasNext()) {
			String str = (String) localIterator.next();
			localStringBuffer.append(str);
			localStringBuffer.append('=');
			localStringBuffer.append((String) paramHashMap.get(str));
			localStringBuffer.append('&');
		}
		int i = localStringBuffer.length();
		return i > 0 ? localStringBuffer.substring(0, i - 1) : null;
	}

	static HashMap splitQuery(String paramString) {
		HashMap localHashMap = new HashMap();
		if ((paramString != null) && (paramString.length() > 0)) {
			if (paramString.charAt(0) == '?')
				paramString = paramString.substring(1);
			String[] arrayOfString = V2CMiscUtil.split(paramString, '&', true);
			if (arrayOfString != null)
				for (int i = 0; i < arrayOfString.length; i++) {
					String str = arrayOfString[i];
					int j = str.indexOf('=');
					if (j > 0)
						localHashMap.put(str.substring(0, j),
								str.substring(j + 1));
				}
		}
		return localHashMap;
	}

	static void checkOAuthTimeOffset(CAndC paramCAndC, long paramLong1,
			long paramLong2) {
		if ((paramCAndC == null) || (paramLong1 <= 0L)
				|| (paramLong2 <= paramLong1)
				|| (paramLong1 + 30000L <= paramLong2))
			return;
		HttpURLConnection localHttpURLConnection = paramCAndC.getConnection();
		if (localHttpURLConnection == null)
			return;
		long l = localHttpURLConnection.getDate();
		if (l > 0L)
			nOAuthTimeOffset = l - (2L * paramLong1 + paramLong2) / 3L;
	}

	static boolean getOAuthRequestToken(V2CTwitterUser paramV2CTwitterUser) {
		String str1 = V2CTwitterBBS.getOAuthConsumerKey();
		String str2 = V2CTwitterBBS.getOAuthConsumerSecret();
		if ((str1 == null) || (str2 == null))
			return false;
		String str3 = "oauth_consumer_key=" + str1 + "&oauth_nonce="
				+ createNonce()
				+ "&oauth_signature_method=HMAC-SHA1&oauth_timestamp="
				+ (System.currentTimeMillis() + nOAuthTimeOffset) / 1000L
				+ "&oauth_version=1.0";
		String str4 = "GET&" + oauthURLEncode(sRequestTokenURL) + '&'
				+ oauthURLEncode(str3);
		String str5 = calcAndEncodeSignature(str4, "");
		if (str5 == null)
			return false;
		String str6 = sRequestTokenURL + '?' + str3 + "&oauth_signature="
				+ str5;
		long l1 = System.currentTimeMillis();
		CAndC localCAndC1 = getRawHTTPFile(str6, 0, null);
		long l2 = System.currentTimeMillis();
		checkOAuthTimeOffset(localCAndC1, l1, l2);
		if ((localCAndC1 == null) || (localCAndC1.getResponseCode() != 200))
			return false;
		byte[] arrayOfByte1 = localCAndC1.getRawContents();
		if (arrayOfByte1 == null)
			return false;
		String str7 = new String(arrayOfByte1);
		HashMap localHashMap1 = splitQuery(str7);
		String str8 = (String) localHashMap1.get("oauth_token");
		if (str8 == null)
			return false;
		String str9 = (String) localHashMap1.get("oauth_token_secret");
		if (str9 == null)
			return false;
		addSecretKeySpec(str9);
		String str10 = paramV2CTwitterUser.getUserName();
		String str11 = sAuthorizeURL + "?oauth_token=" + str8
				+ "&oauth_callback=oob&force_login=true&screen_name=" + str10;
		V2CMiscUtil.openLinkOutside(str11);
		V2CPINInputPanel localV2CPINInputPanel = new V2CPINInputPanel(str10,
				str11);
		String str12 = localV2CPINInputPanel.showDialog();
		if ((str12 == null) || (str12.length() == 0))
			return false;
		int i = V2CMiscUtil.parseInt(str12, -1);
		if (i < 0)
			return false;
		String str13 = "oauth_consumer_key=" + str1 + "&oauth_nonce="
				+ createNonce()
				+ "&oauth_signature_method=HMAC-SHA1&oauth_timestamp="
				+ (System.currentTimeMillis() + nOAuthTimeOffset) / 1000L
				+ "&oauth_token=" + str8 + "&oauth_verifier=" + str12
				+ "&oauth_version=1.0";
		String str14 = "GET&" + oauthURLEncode(sAccessTokenURL) + '&'
				+ oauthURLEncode(str13);
		String str15 = calcAndEncodeSignature(str14, str9);
		if (str15 == null)
			return false;
		String str16 = sAccessTokenURL + '?' + str13 + "&oauth_signature="
				+ str15;
		CAndC localCAndC2 = getRawHTTPFile(str16, 0, null);
		if ((localCAndC2 == null) || (localCAndC2.getResponseCode() != 200))
			return false;
		byte[] arrayOfByte2 = localCAndC2.getRawContents();
		if (arrayOfByte2 == null)
			return false;
		String str17 = new String(arrayOfByte2);
		HashMap localHashMap2 = splitQuery(str17);
		long l3 = V2CMiscUtil.parseLong((String) localHashMap2.get("user_id"));
		if (l3 <= 0L)
			return false;
		return paramV2CTwitterUser.setOAuthAccessTokens(l3,
				(String) localHashMap2.get("screen_name"),
				(String) localHashMap2.get("oauth_token"),
				(String) localHashMap2.get("oauth_token_secret"), null);
	}

	static String createOAuthHeader(V2CTwitterUser paramV2CTwitterUser,
			boolean usePost, String paramString, HashMap paramHashMap) {
		String consumerKey = V2CTwitterBBS.getOAuthConsumerKey();
		String consumerSecret = V2CTwitterBBS.getOAuthConsumerSecret();
		if ((consumerKey == null) || (consumerSecret == null)) {
			return null;
		}
		String accessToken = paramV2CTwitterUser.getOAuthAccessToken();
		String tokenSecret = paramV2CTwitterUser.getOAuthAccessTokenSecret();
		if ((accessToken == null) || (tokenSecret == null)) {
			return null;
		}
		HashMap parameterMap = new HashMap();
		if (paramHashMap != null) {
			parameterMap.putAll(paramHashMap);
		}
		parameterMap.put("oauth_consumer_key", consumerKey);
		parameterMap.put("oauth_token", accessToken);
		parameterMap.put("oauth_signature_method", "HMAC-SHA1");
		parameterMap.put("oauth_timestamp", String.valueOf((System
				.currentTimeMillis() + nOAuthTimeOffset) / 1000L));
		parameterMap.put("oauth_nonce", createNonce());
		parameterMap.put("oauth_version", "1.0");
		String[] parameterNames = (String[]) parameterMap.keySet().toArray(
				new String[parameterMap.size()]);
		Arrays.sort(parameterNames);
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < parameterNames.length; i++) {
			String parameter = parameterNames[i];
			buff.append(parameter);
			buff.append('=');
			buff.append(parameterMap.get(parameter));
			buff.append('&');
		}
		String str7 = buff.substring(0, buff.length() - 1);
		String str8 = (usePost ? "POST" : "GET") + '&'
				+ oauthURLEncode(paramString) + '&' + oauthURLEncode(str7);
		String str9 = calcAndEncodeSignature(str8, tokenSecret);
		if (str9 == null) {
			return null;
		}
		buff.setLength(0);
		buff.append("OAuth ");
		for (int j = 0; j < parameterNames.length; j++) {
			String str10 = parameterNames[j];
			if (str10.startsWith("oauth_")) {
				buff.append(str10);
				buff.append("=\"");
				buff.append(parameterMap.get(str10));
				buff.append("\",");
			}
		}
		buff.append("oauth_signature=\"");
		buff.append(str9);
		buff.append('"');
		return buff.toString();
	}

	static void addSecretKeySpec(String userId) {
		addSecretKeySpec(userId, V2CTwitterBBS.getOAuthConsumerSecret());
	}

	static void addSecretKeySpec(String userId, String secret) {
		if ((userId == null) || (secret == null)) {
			return;
		}
		HashMap localHashMap = hmKeySpecs;
		synchronized (localHashMap) {
			localHashMap.put(userId,
					new SecretKeySpec((secret + '&' + userId).getBytes(),
							"HmacSHA1"));
		}
	}

	static String calcAndEncodeSignature(String message, String userId) {
		byte[] arrayOfByte = null;
		try {
			SecretKeySpec keySpec;
			synchronized (hmKeySpecs) {
				keySpec = (SecretKeySpec) hmKeySpecs.get(userId);
			}
			if (keySpec == null) {
				return null;
			}
			if (vMac == null) {
				vMac = Mac.getInstance("HmacSHA1");
			}
			synchronized (vMac) {
				((Mac) vMac).init(keySpec);
				arrayOfByte = ((Mac) vMac).doFinal(message.getBytes());
			}
		} catch (Exception localException) {
			localException.printStackTrace();
		}
		return arrayOfByte != null ? oauthURLEncode(new String(
				V2CMiscUtil.encodeBase64(arrayOfByte))) : null;
	}

	static String oauthURLEncode(String paramString) {
		if ((paramString == null) || (paramString.length() == 0)) {
			return paramString;
		}
		byte[] arrayOfByte = V2CJPConverter.getBytes(paramString, "UTF-8");
		if (arrayOfByte == null) {
			return null;
		}
		StringBuffer localStringBuffer = new StringBuffer();
		for (int i = 0; i < arrayOfByte.length; i++) {
			int j = arrayOfByte[i] & 0xFF;
			if ((j == 45) || (j == 46) || (j == 95) || (j == 126)
					|| ((j >= 48) && (j <= 57)) || ((j >= 65) && (j <= 90))
					|| ((j >= 97) && (j <= 122))) {
				localStringBuffer.append((char) j);
			} else {
				localStringBuffer.append('%');
				localStringBuffer.append(toHexChar(j >> 4 & 0xF));
				localStringBuffer.append(toHexChar(j & 0xF));
			}
		}
		return localStringBuffer.toString();
	}

	private static char toHexChar(int paramInt) {
		return (char) (paramInt + (paramInt < 10 ? 48 : 55));
	}

	static String createNonce() {
		if (vRandom == null) {
			vRandom = new Random(System.currentTimeMillis());
		}
		char[] arrayOfChar = new char[32];
		for (int i = 0; i < arrayOfChar.length; i++) {
			arrayOfChar[i] = ((char) ((vRandom.nextInt() & 0x7FFF) % 10 + 48));
		}
		return new String(arrayOfChar);
	}
}
