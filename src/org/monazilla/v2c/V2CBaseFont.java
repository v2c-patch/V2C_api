package org.monazilla.v2c;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

public class V2CBaseFont {
	static final String sFileName = V2CApp.createConfigFileName("basefont");
	static final String sOldFileName = "basefont.txt";
	static final String sUIFontScaleKey = "Main.UIFontScale";
	static final String sSkipJPFontCheckKey = "Main.SkipJPFontCheck";
	static final int TYPE_NORMAL = 1;
	static final int TYPE_TTFFILE = 2;
	static final String[] slDefFontComp = {"Button", "CheckBox",
			"CheckBoxMenuItem", "ColorChooser", "ComboBox", "DesktopIcon",
			"EditorPane", "FormattedTextField", "Label", "List", "MenuBar",
			"Menu", "MenuItem", "OptionPane", "Panel", "PasswordField",
			"PopupMenu", "ProgressBar", "RadioButton", "RadioButtonMenuItem",
			"ScrollPane", "Spinner", "TabbedPane", "Table", "TableHeader",
			"TextArea", "TextField", "TextPane", "TitledBorder",
			"ToggleButton", "ToolBar", "ToolTip", "Tree", "Viewport"};

	static final Font fOriginalMenuFont = UIManager.getFont("MenuItem.font");
	static final Vector<String> vTTFontName = new Vector();
	static final Hashtable<String, Font> htTTFFont = new Hashtable();
	static final Hashtable<String, Object> htBaseFont = new Hashtable();
	static final HashMap<String, Integer> hmDefaultScale = new HashMap();
	static final String DEFAULTNAME = "Default";
	static final String THREADDEFAULTNAME = "ThreadDefault";
	static final String THREADALTNAME = "ThreadAlt";
	static V2CBaseFont bfDefault;
	static final String sMSPGothicName = "MS PGothic";
	static final ClassStateSaver classStateSaver = new ClassStateSaver();
	int nType;
	Font fFont;
	String sName;
	String sTTFileName;
	int nTTFontNumber;
	boolean[] blDisplayableChars;
	static Font fSynthDefault;

	static boolean isTTFontName(String s) {
		return vTTFontName.contains(s);
	}

	static Font createFontFromFile(String s, int i) {
		String s1 = s + ":" + i;
		Font font = (Font) htTTFFont.get(s1);
		if (font != null)
			return font;
		InputStream is = null;
		try {
			if (i == 0)
				is = new FileInputStream(s);
			else
				is = new ExchangeTTFInputStream(s, i);
			font = Font.createFont(0, is);
		} catch (FontFormatException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException localIOException3) {
				}
		}
		if (is != null) {
			try {
				is.close();
			} catch (IOException localIOException4) {
			}
		}
		htTTFFont.put(s1, font);
		String s2 = font.getFontName();
		if ((s2 != null) && (s2.length() > 0) && (!vTTFontName.contains(s2)))
			vTTFontName.add(s2);
		return font;
	}
	static void restoreBaseFonts() {
		BufferedReader localBufferedReader = V2CLocalFileHandler
				.getUserSaveFileReader(sFileName);
		Object localObject1;
		if (localBufferedReader == null) {
			localObject1 = V2CLocalFileHandler.getUserConfigFile(sFileName);
			if (!((File) localObject1).exists()) {
				File localFile = V2CLocalFileHandler
						.getUserConfigFile("basefont.txt");
				if ((localFile.exists())
						&& (localFile.renameTo((File) localObject1)))
					localBufferedReader = V2CLocalFileHandler
							.getUserSaveFileReader(sFileName);
			}
		}
		if (localBufferedReader != null)
			try {
				while ((localObject1 = localBufferedReader.readLine()) != null)
					if ((((String) localObject1).length() != 0)
							&& (((String) localObject1).charAt(0) != '#')) {
						int i = ((String) localObject1).length();
						int j = ((String) localObject1).indexOf(',');
						if ((j >= 0)
								&& (j + 3 < i)
								&& (((String) localObject1).charAt(j + 2) == ',')) {
							String str = ((String) localObject1)
									.substring(0, j);
							int k = ((String) localObject1).charAt(j + 1);
							j += 3;
							if (k == 78) {
								new V2CBaseFont(str,
										((String) localObject1).substring(j));
							} else if (k == 80) {
								int m = ((String) localObject1)
										.lastIndexOf(',');
								if ((m > j) && (m + 1 < i)) {
									int n = 0;
									try {
										n = Integer
												.parseInt(((String) localObject1)
														.substring(m + 1));
									} catch (NumberFormatException localNumberFormatException) {
									}
									new V2CBaseFont(str,
											((String) localObject1).substring(
													j, m), n);
								}
							} else {
								System.out.println(sFileName
										+ " format error !");
								V2CApp.errorExit(sFileName + "のフォーマットが不正です。");
							}
						}
					}
			} catch (IOException localIOException) {
			} finally {
				V2CLocalFileHandler.closeReader(localBufferedReader);
			}
		if ((!htBaseFont.containsKey("Default"))
				|| (!htBaseFont.containsKey("ThreadDefault")))
			createNewBaseFonts();
		if (bfDefault != null)
			setUIFonts(bfDefault.fFont);

	}

	static void saveBaseFonts() {
		V2CSaveFile v2csavefile = new V2CSaveFile(sFileName);
		BufferedWriter bufferedwriter = v2csavefile.getWriter();
		if (bufferedwriter == null)
			return;
		Vector vector = null;
		StringBuffer stringbuffer = new StringBuffer();
		Enumeration enumeration = htBaseFont.keys();
		try {
			Object obj = htBaseFont.get("Default");
			if ((obj instanceof V2CBaseFont)) {
				V2CBaseFont v2cbasefont = (V2CBaseFont) obj;
				v2cbasefont.getSaveString(stringbuffer);
				if (stringbuffer.length() > 0) {
					bufferedwriter.write(stringbuffer.toString());
					bufferedwriter.newLine();
				}
			}

			while (enumeration.hasMoreElements()) {
				String s = (String) enumeration.nextElement();
				if (!s.equals("Default")) {
					Object obj1 = htBaseFont.get(s);
					if ((obj1 instanceof V2CBaseFont)) {
						V2CBaseFont v2cbasefont1 = (V2CBaseFont) obj1;
						v2cbasefont1.getSaveString(stringbuffer);
						if (stringbuffer.length() != 0) {
							bufferedwriter.write(stringbuffer.toString());
							bufferedwriter.newLine();
						}
					} else if ((obj1 instanceof String)) {
						if (vector == null)
							vector = new Vector();
						vector.add(s);
					}
				}
			}
			v2csavefile.setOK();
		} catch (IOException localIOException) {
		} finally {
			V2CLocalFileHandler.closeWriter(bufferedwriter);
		}
		v2csavefile.replaceTmpFile();
	}

	static void setUIFontScale(int i) {
		V2CProperty v2cproperty = V2CApp.getUserProperty();
		if (v2cproperty != null)
			if (i != 0)
				v2cproperty.putInt("Main.UIFontScale", i);
			else
				v2cproperty.remove("Main.UIFontScale");
	}

	static int getUIFontScale() {
		V2CProperty v2cproperty = V2CApp.getUserProperty();
		return v2cproperty == null ? 0 : v2cproperty.getInt("Main.UIFontScale");
	}

	static boolean shouldAvoidFontUIResource() {
		return (V2CApp.isUNIX()) && (V2CApp.javaVersionEqualTo(1, 5));
	}

	static void setUIFonts(Font font) {
		boolean shouldSetUIFont = getSetUIFonts();
		boolean isMacOS = V2CApp.isMacintosh();
		boolean isSynthLaF = V2CSwingUtil.isSynthLaF();
		boolean noFontUIResource = shouldAvoidFontUIResource();
		int i = getUIFontScale();
		Object obj = null;
		for (int j = 0; j < slDefFontComp.length; j++) {
			String s = slDefFontComp[j] + ".font";
			Object obj1 = UIManager.getFont(s);
			if (obj1 == null) {
				if (!isSynthLaF) {
					continue;
				}
				obj1 = fSynthDefault;
				if (obj1 == null) {
					obj1 = new FontUIResource("Dialog", 0, 12);
					fSynthDefault = (Font) obj1;
				}
			}
			if (shouldSetUIFont) {
				int k = 0;
				Integer nscale = (Integer) hmDefaultScale.get(s);
				if ((nscale instanceof Integer)) {
					k = nscale.intValue();
				} else {
					k = ((Font) obj1).getSize();
					hmDefaultScale.put(s, new Integer(k));
				}
				k = (int) (Math.pow(1.2D, i) * k);
				if (isMacOS) {
					obj = new FontUIResource(font.deriveFont(0, k));
				} else if (noFontUIResource) {
					obj = font.deriveFont(0, k);
				} else {
					obj = new FontUIResource(font.deriveFont(0, k));
				}
			} else {
				obj = null;
			}
			UIManager.put(s, obj);
		}

		if (isSynthLaF)
			UIManager.put("defaultFont", obj);
	}

	static boolean getSetUIFonts() {
		if (!V2CApp.isMacintosh())
			return true;
		V2CProperty v2cproperty = V2CApp.getUserProperty();
		if (v2cproperty != null) {
			return !v2cproperty.getBoolean("Main.UseJREFonts", true);
		}
		return false;
	}

	static void createNewBaseFonts() {
		if (V2CApp.isMacintosh())
			V2CApp.getUserProperty().putBoolean("Main.UseJREFonts", true);
		V2CBaseFont v2cbasefont;
		if (bfDefault != null)
			v2cbasefont = bfDefault;
		else
			v2cbasefont = checkJapaneseFont(true);
		if (!htBaseFont.containsKey("ThreadDefault"))
			if (V2CApp.isMacintosh()) {
				Font font = UIManager.getFont("Panel.font");
				if ((font != null) && (font.canDisplay('A'))
						&& (font.canDisplay('亜')))
					new V2CBaseFont("ThreadDefault", font.getFontName());
				else
					new V2CBaseFont("ThreadDefault", v2cbasefont);
			} else {
				if (!v2cbasefont.getFaceName().equals("MS PGothic")) {
					Font font1 = new Font("MS PGothic", 0, 1);
					if ("MS PGothic".equals(font1.getFontName()))
						new V2CBaseFont("ThreadDefault", "MS PGothic");
				}
				if (!htBaseFont.containsKey("ThreadDefault"))
					new V2CBaseFont("ThreadDefault", v2cbasefont);
			}
		classStateSaver.requestSave();
	}

	static V2CBaseFont checkJapaneseFont(boolean flag) {
		Font font = fOriginalMenuFont;
		V2CBaseFont v2cbasefont = null;
		boolean flag1 = (font != null) && (font.canDisplay('A'))
				&& (font.canDisplay('亜'));
		if ((flag1) && (!flag))
			return null;
		if (!flag1) {
			font = findJapanesePhysicalFont();
			if (font == null) {
				V2CBaseFontSP v2cbasefontsp = new V2CBaseFontSP(true);
				Object[] aobj = {" OK ", "Exit"};
				int i = JOptionPane.showOptionDialog(null, v2cbasefontsp,
						"BaseFont Setting", -1, 3, null, aobj, aobj[0]);
				if (i != 0)
					V2CApp.immediateExit(0);
				v2cbasefont = v2cbasefontsp.getSelectedBaseFont("Default");
				if (v2cbasefont == null) {
					V2CApp.immediateExit(1);
					return null;
				}
				font = v2cbasefont.getFont();
			}
			setUIFonts(font);
		} else if (V2CApp.isWinType()) {
			String s = V2CApp.isWindowsVista() ? "Meiryo" : "MS UI Gothic";
			Font font1 = new Font(s, 0, 1);
			if (s.equals(font1.getFontName())) {
				font = font1;
				setUIFonts(font);
			}
		}
		if (v2cbasefont == null)
			v2cbasefont = new V2CBaseFont("Default", font.getFontName());
		return v2cbasefont;
	}

	static Font findJapanesePhysicalFont() {
		if (V2CApp.isWinType()) {
			String s = V2CApp.isWindowsVista() ? "Meiryo" : "MS UI Gothic";
			Font font = new Font(s, 0, 1);
			if (s.equals(font.getFontName()))
				return font;
		}
		GraphicsEnvironment graphicsenvironment = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		String[] as = graphicsenvironment.getAvailableFontFamilyNames();
		String[] as1 = {"MS UI Gothic", "MS PGothic", "Kochi Gothic",
				"Kochi Mincho", "Mona", "TakaoPGothic"};
		for (int i = 0; i < as1.length; i++) {
			String s1 = as1[i];
			for (int k = 0; k < as.length; k++) {
				if (s1.equals(as[k])) {
					return new Font(s1, 0, 1);
				}
			}
		}
		Font[] afont = graphicsenvironment.getAllFonts();
		for (int j = 0; j < afont.length; j++) {
			if ((afont[j].canDisplay('A')) && (afont[j].canDisplay('亜')))
				return afont[j];
		}
		return null;
	}

	static void setSkipJPFontCheck(boolean flag) {
		V2CProperty v2cproperty = V2CApp.getUserProperty();
		if (flag)
			v2cproperty.putBoolean("Main.SkipJPFontCheck", true);
		else
			v2cproperty.remove("Main.SkipJPFontCheck");
	}

	static boolean getSkipJPFontCheck() {
		return V2CApp.getUserProperty().getBoolean("Main.SkipJPFontCheck");
	}

	static V2CFontSP getFontConfPanel(boolean flag) {
		V2CBaseFont v2cbasefont = getDefaultBaseFont();
		V2CBaseFont v2cbasefont1 = getThreadDefaultBaseFont();
		V2CBaseFont v2cbasefont2 = getThreadAltBaseFont(false);
		boolean flag1 = v2cbasefont2 == null;
		if (flag1)
			v2cbasefont2 = getThreadAltBaseFont(true);
		if ((v2cbasefont == null) || (v2cbasefont1 == null)
				|| (v2cbasefont2 == null)) {
			return null;
		}

		int i = V2CSSFont.getDefaultSize();
		Object obj = V2CThreadStyle.getTextAntialiasing();
		int j = V2CThreadStyle.getLeading();
		boolean flag2 = V2CFont.bCheckFontChars;
		boolean flag3 = V2CFont.bCheckPUAChars;
		boolean flag4 = getSetUIFonts();
		int k = getUIFontScale();
		int l = V2CThreadStyle.getAltFontSize();
		V2CFontSP v2cfontsp = new V2CFontSP(i, obj, (flag)
				|| (getSkipJPFontCheck()));
		return v2cfontsp;
	}

	static void changeFont(boolean flag) {
		V2CBaseFont v2cbasefont = getDefaultBaseFont();
		V2CBaseFont v2cbasefont1 = getThreadDefaultBaseFont();
		V2CBaseFont v2cbasefont2 = getThreadAltBaseFont(false);
		boolean flag1 = v2cbasefont2 == null;
		if (flag1)
			v2cbasefont2 = getThreadAltBaseFont(true);
		if ((v2cbasefont == null) || (v2cbasefont1 == null)
				|| (v2cbasefont2 == null)) {
			return;
		}

		int i = V2CSSFont.getDefaultSize();
		Object obj = V2CThreadStyle.getTextAntialiasing();
		int j = V2CThreadStyle.getLeading();
		boolean flag2 = V2CFont.bCheckFontChars;
		boolean flag3 = V2CFont.bCheckPUAChars;
		boolean flag4 = getSetUIFonts();
		int k = getUIFontScale();
		int l = V2CThreadStyle.getAltFontSize();
		V2CFontSP v2cfontsp = new V2CFontSP(i, obj, (flag)
				|| (getSkipJPFontCheck()));
		if (!v2cfontsp.showDialog()) {
			return;
		}
		V2CBaseFontSP v2cbasefontsp = v2cfontsp.getDefaultBaseFontSP();
		V2CBaseFont v2cbasefont3 = v2cbasefontsp.getSelectedBaseFont();
		boolean flag5 = !v2cbasefontsp.getUseSystemFont();
		if (V2CApp.isMacintosh())
			V2CApp.getUserProperty().putBoolean("Main.UseJREFonts", !flag5);
		boolean flag6 = (v2cbasefont3 != null)
				&& (v2cbasefont.copyFrom(v2cbasefont3, false));
		V2CApp.setUseSystemAAFontSettings(v2cbasefontsp
				.getUseSysAAFontSetting());
		boolean flag7 = false;
		boolean flag8 = false;
		V2CBaseFontSP v2cbasefontsp1 = v2cfontsp.getThreadDefaultBaseFontSP();
		V2CBaseFont v2cbasefont4 = v2cbasefontsp1.getSelectedBaseFont();
		boolean flag9 = (v2cbasefont4 != null)
				&& (v2cbasefont1.copyFrom(v2cbasefont4, false));
		boolean flag10 = false;
		boolean flag11 = false;
		int i1 = v2cbasefontsp1.getFontSize();
		if ((i1 > 0) && (i1 != i)) {
			V2CSSFont.setDefaultSize(i1);
			flag10 = true;
		}
		Object obj1 = v2cbasefontsp1.getTextAntialiasing();
		if (obj1 != obj) {
			V2CThreadStyle.setTextAntialiasing(obj1);
			flag10 = true;
		}
		int j1 = v2cbasefontsp1.getLeading();
		if (j1 != j) {
			V2CThreadStyle.setLeading(j1);
			flag11 = true;
		}
		boolean flag12 = v2cbasefontsp1.getCheckFontChars();
		boolean flag13 = v2cbasefontsp1.getCheckPUAChars();
		if ((flag12 != flag2) || ((flag12) && (flag13 != flag3))) {
			V2CFont.setCheckFontChars(flag12, flag13);
			flag10 = true;
		}
		if (flag10)
			V2CFont.clearAllWidthCache();
		flag7 = (flag10) || (flag11);
		V2CBaseFontSP v2cbasefontsp2 = v2cfontsp.getThreadAltBaseFontSP();
		V2CBaseFont v2cbasefont5 = v2cbasefontsp2.getSelectedBaseFont();
		boolean flag14 = (v2cbasefont5 != null)
				&& (v2cbasefont2.copyFrom(v2cbasefont5, flag1));
		int k1 = v2cbasefontsp2.getAltFontSize();
		if (k1 != l) {
			V2CThreadStyle.setAltFontSize(k1);
			flag8 = true;
		}
		if (flag6)
			V2CFont.baseFontChanged(v2cbasefont);
		if (flag9)
			V2CFont.baseFontChanged(v2cbasefont1);
		if (flag14)
			V2CFont.baseFontChanged(v2cbasefont2);
		int l1 = v2cbasefontsp.getUIFontScale();
		if (l1 != k)
			setUIFontScale(l1);
		if ((flag6) || (flag5 != flag4) || (l1 != k)) {
			setUIFonts(v2cbasefont.fFont);
			V2CMain.fontForUIChanged();
			if (V2CApp.isMacintosh()) {
				V2CListTable.clearRendererFont();
				V2CHistory.clearRendererFont();
			}
		}
		if ((flag14) || (flag8))
			V2CSSFont.altFontChanged();
		if ((flag9) || (flag7))
			V2CThreadPanel.notifyDefaultFontChanged();
	}

	static void applyChanges(V2CFontSP v2cfontsp) {
		V2CBaseFont v2cbasefont = getDefaultBaseFont();
		V2CBaseFont v2cbasefont1 = getThreadDefaultBaseFont();
		V2CBaseFont v2cbasefont2 = getThreadAltBaseFont(false);
		boolean flag1 = v2cbasefont2 == null;
		if (flag1)
			v2cbasefont2 = getThreadAltBaseFont(true);
		if ((v2cbasefont == null) || (v2cbasefont1 == null)
				|| (v2cbasefont2 == null)) {
			return;
		}

		int i = V2CSSFont.getDefaultSize();
		Object obj = v2cfontsp.textAntiAliasing;
		int j = V2CThreadStyle.getLeading();
		boolean flag2 = V2CFont.bCheckFontChars;
		boolean flag3 = V2CFont.bCheckPUAChars;
		boolean flag4 = getSetUIFonts();
		int k = getUIFontScale();
		int l = V2CThreadStyle.getAltFontSize();

		V2CBaseFontSP v2cbasefontsp = v2cfontsp.getDefaultBaseFontSP();
		V2CBaseFont v2cbasefont3 = v2cbasefontsp.getSelectedBaseFont();
		boolean flag5 = !v2cbasefontsp.getUseSystemFont();
		if (V2CApp.isMacintosh())
			V2CApp.getUserProperty().putBoolean("Main.UseJREFonts", !flag5);
		boolean flag6 = (v2cbasefont3 != null)
				&& (v2cbasefont.copyFrom(v2cbasefont3, false));
		V2CApp.setUseSystemAAFontSettings(v2cbasefontsp
				.getUseSysAAFontSetting());
		boolean flag7 = false;
		boolean flag8 = false;
		V2CBaseFontSP v2cbasefontsp1 = v2cfontsp.getThreadDefaultBaseFontSP();
		V2CBaseFont v2cbasefont4 = v2cbasefontsp1.getSelectedBaseFont();
		boolean flag9 = (v2cbasefont4 != null)
				&& (v2cbasefont1.copyFrom(v2cbasefont4, false));
		boolean flag10 = false;
		boolean flag11 = false;
		int i1 = v2cbasefontsp1.getFontSize();
		if ((i1 > 0) && (i1 != i)) {
			V2CSSFont.setDefaultSize(i1);
			flag10 = true;
		}
		Object obj1 = v2cbasefontsp1.getTextAntialiasing();
		if (obj1 != obj) {
			V2CThreadStyle.setTextAntialiasing(obj1);
			flag10 = true;
		}
		int j1 = v2cbasefontsp1.getLeading();
		if (j1 != j) {
			V2CThreadStyle.setLeading(j1);
			flag11 = true;
		}
		boolean flag12 = v2cbasefontsp1.getCheckFontChars();
		boolean flag13 = v2cbasefontsp1.getCheckPUAChars();
		if ((flag12 != flag2) || ((flag12) && (flag13 != flag3))) {
			V2CFont.setCheckFontChars(flag12, flag13);
			flag10 = true;
		}
		if (flag10)
			V2CFont.clearAllWidthCache();
		flag7 = (flag10) || (flag11);
		V2CBaseFontSP v2cbasefontsp2 = v2cfontsp.getThreadAltBaseFontSP();
		V2CBaseFont v2cbasefont5 = v2cbasefontsp2.getSelectedBaseFont();
		boolean flag14 = (v2cbasefont5 != null)
				&& (v2cbasefont2.copyFrom(v2cbasefont5, flag1));
		int k1 = v2cbasefontsp2.getAltFontSize();
		if (k1 != l) {
			V2CThreadStyle.setAltFontSize(k1);
			flag8 = true;
		}
		if (flag6)
			V2CFont.baseFontChanged(v2cbasefont);
		if (flag9)
			V2CFont.baseFontChanged(v2cbasefont1);
		if (flag14)
			V2CFont.baseFontChanged(v2cbasefont2);
		int l1 = v2cbasefontsp.getUIFontScale();
		if (l1 != k)
			setUIFontScale(l1);
		if ((flag6) || (flag5 != flag4) || (l1 != k)) {
			setUIFonts(v2cbasefont.fFont);
			V2CMain.fontForUIChanged();
			if (V2CApp.isMacintosh()) {
				V2CListTable.clearRendererFont();
				V2CHistory.clearRendererFont();
			}
		}
		if ((flag14) || (flag8))
			V2CSSFont.altFontChanged();
		if ((flag9) || (flag7))
			V2CThreadPanel.notifyDefaultFontChanged();
	}

	static V2CBaseFont getDefaultBaseFont() {
		return bfDefault;
	}

	static V2CBaseFont getThreadDefaultBaseFont() {
		return getBaseFont("ThreadDefault");
	}

	static V2CBaseFont getThreadAltBaseFont(boolean flag) {
		V2CBaseFont v2cbasefont = getBaseFont("ThreadAlt");
		if ((v2cbasefont == null) && (flag)) {
			V2CBaseFont v2cbasefont1 = getThreadDefaultBaseFont();
			if (v2cbasefont1 != null)
				v2cbasefont = new V2CBaseFont("ThreadAlt", v2cbasefont1);
		}
		return v2cbasefont;
	}

	static V2CBaseFont getBaseFont(String s) {
		Object obj = htBaseFont.get(s);
		if ((obj instanceof String))
			obj = htBaseFont.get(obj);
		if ((obj instanceof V2CBaseFont)) {
			return (V2CBaseFont) obj;
		}
		return null;
	}

	public V2CBaseFont(String s, String s1, int i) {
		this.nType = 2;
		this.nTTFontNumber = i;
		if ((s1 != null) && (s1.length() > 0)) {
			this.sTTFileName = V2CLocalFileHandler.parseV2CRelativePath(s1);
			this.fFont = createFontFromFile(this.sTTFileName, i);
		}
		if (this.fFont == null) {
			System.out.println("fFont==null");
			return;
		}
		if (s == null) {
			return;
		}

		this.sName = s;
		registerFont();
	}

	public V2CBaseFont(Font font) {
		this.nType = 1;
		this.fFont = font.deriveFont(0, 1.0F);
	}

	public V2CBaseFont(String s, String s1) {
		this.nType = 1;
		this.fFont = new Font(s1, 0, 1);
		if (s == null) {
			return;
		}

		this.sName = s;
		registerFont();
	}

	public V2CBaseFont(String s, V2CBaseFont v2cbasefont) {
		this.nType = v2cbasefont.nType;
		this.fFont = v2cbasefont.fFont;
		if (this.nType == 2) {
			this.sTTFileName = v2cbasefont.sTTFileName;
			this.nTTFontNumber = v2cbasefont.nTTFontNumber;
		}
		if (s == null) {
			return;
		}

		this.sName = s;
		registerFont();
	}

	private void registerFont() {
		htBaseFont.put(this.sName, this);
		if (this.sName.equals("Default"))
			bfDefault = this;
	}

	boolean copyFrom(V2CBaseFont v2cbasefont, boolean flag) {
		boolean flag1 = flag;
		if (this.nType != v2cbasefont.nType) {
			this.nType = v2cbasefont.nType;
			flag1 = true;
		}
		if (!this.fFont.equals(v2cbasefont.fFont)) {
			this.fFont = v2cbasefont.fFont;
			flag1 = true;
		}
		if (this.nType == 2) {
			if ((this.sTTFileName == null)
					|| (!this.sTTFileName.equals(v2cbasefont.sTTFileName))) {
				this.sTTFileName = v2cbasefont.sTTFileName;
				flag1 = true;
			}
			if (this.nTTFontNumber != v2cbasefont.nTTFontNumber) {
				this.nTTFontNumber = v2cbasefont.nTTFontNumber;
				flag1 = true;
			}
		}
		if (flag1)
			classStateSaver.requestSave();
		return flag1;
	}

	void getSaveString(StringBuffer stringbuffer) {
		stringbuffer.setLength(0);
		stringbuffer.append(this.sName);
		if (this.nType == 1) {
			stringbuffer.append(",N,");
			stringbuffer.append(getFaceName());
		} else if (this.nType == 2) {
			stringbuffer.append(",P,");
			stringbuffer.append(V2CLocalFileHandler
					.getV2CRelativePath(new File(this.sTTFileName)));
			stringbuffer.append(',');
			stringbuffer.append(String.valueOf(this.nTTFontNumber));
		} else {
			stringbuffer.setLength(0);
		}
	}

	boolean canDisplay(int i) {
		boolean[] aflag = this.blDisplayableChars;
		if (aflag == null) {
			aflag = V2CFont.createDispCharArray(getFont());
			this.blDisplayableChars = aflag;
		}
		return aflag[i];
	}

	public String getName() {
		return this.sName;
	}

	Font getFont() {
		return this.fFont;
	}

	Font deriveFont(int i, float f) {
		return this.fFont.deriveFont(i, f);
	}

	String getFamily() {
		return this.fFont.getFamily();
	}

	String getFaceName() {
		return this.fFont.getFontName();
	}

	String getFaceName(Locale locale) {
		return this.fFont.getFontName(locale);
	}

	int getType() {
		return this.nType;
	}

	String getTTFileName() {
		return this.sTTFileName;
	}

	int getNTTFontNumber() {
		return this.nTTFontNumber;
	}

	public int hashCode() {
		return this.sName.hashCode();
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof V2CBaseFont)) {
			return false;
		}
		return this.sName.equals(((V2CBaseFont) obj).getName());
	}

	public String toString() {
		return this.sName;
	}

	static class ClassStateSaver implements V2CPersistentState {
		static long lSaveRequestTime;

		void requestSave() {
			lSaveRequestTime = System.currentTimeMillis() + 10000L;
			V2CApp.registerStateSave(this);
		}

		public void doSaveState() {
			V2CBaseFont.saveBaseFonts();
		}

		public long getSaveRequestTime() {
			return lSaveRequestTime;
		}
	}

	static class ExchangeTTFInputStream extends FileInputStream {
		int iReadIndex;
		int nBuf;
		byte[] bBuf;

		public int read() throws IOException {
			if (this.iReadIndex < this.nBuf) {
				return this.bBuf[(this.iReadIndex++)];
			}
			return super.read();
		}

		public int read(byte[] abyte0) throws IOException {
			return read(abyte0, 0, abyte0.length);
		}

		public int read(byte[] abyte0, int i, int j) throws IOException {
			if (this.iReadIndex < this.nBuf) {
				if (j < this.nBuf - this.iReadIndex) {
					System.arraycopy(this.bBuf, this.iReadIndex, abyte0, i, j);
					this.iReadIndex += j;
					return j;
				}

				int k = this.nBuf - this.iReadIndex;
				System.arraycopy(this.bBuf, this.iReadIndex, abyte0, i, k);
				this.iReadIndex = this.nBuf;
				return k;
			}

			return super.read(abyte0, i, j);
		}

		public boolean markSupported() {
			return false;
		}

		ExchangeTTFInputStream(String s, int i) throws FileNotFoundException,
				IOException {
			super(s);
			this.nBuf = (16 + i * 4);
			this.bBuf = new byte[this.nBuf];

			int j = 0;
			int k;
			while (((k = super.read(this.bBuf, j, this.nBuf - j)) > 0)
					&& (k + j != this.nBuf));
			for (int l = 0; l < 4; l++) {
				byte byte0 = this.bBuf[(12 + l)];
				this.bBuf[(12 + l)] = this.bBuf[(12 + i * 4 + l)];
				this.bBuf[(12 + i * 4 + l)] = byte0;
			}
		}
	}
}