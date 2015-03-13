package org.monazilla.v2c;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class V2CExtCommandSP extends JPanel {
	static int nLastSelectedIndex;
	JTabbedPane jTabbedPane;
	final V2CMiscExtCommandSP vMiscExtCommandSP;
	final V2CURLCommandSP vURLCommandSP;
	V2CAPISettings apiSettings = new V2CAPISettings();
	V2CScriptSP vScriptSP;

	static void setExtCommand() {
		V2CExtCommandSP localV2CExtCommandSP = new V2CExtCommandSP();
		localV2CExtCommandSP.showDialog();
	}

	public V2CExtCommandSP() {
		super(new GridBagLayout());
		GridBagConstraints localGridBagConstraints = V2CSwingUtil
				.createGridBagConstraints();
		localGridBagConstraints.insets = new Insets(1, 1, 1, 1);
		JTabbedPane localJTabbedPane = new JTabbedPane();
		this.jTabbedPane = localJTabbedPane;
		localJTabbedPane.setFocusable(false);
		this.vMiscExtCommandSP = new V2CMiscExtCommandSP();
		localJTabbedPane.addTab("一般", this.vMiscExtCommandSP);
		this.vURLCommandSP = new V2CURLCommandSP();
		localJTabbedPane.addTab("ブラウザ", this.vURLCommandSP);
		if (V2CApp.javaVersionEqualOrGreaterThan(1, 6)) {
			this.vScriptSP = V2CScriptUtil.createScriptSP();
			localJTabbedPane.addTab("スクリプト", this.vScriptSP.getPanel());
		}
		localJTabbedPane.addTab("2chAPI設定", this.apiSettings.getPanel());
		localJTabbedPane.setSelectedIndex(nLastSelectedIndex);
		localGridBagConstraints.weightx = 1.0D;
		localGridBagConstraints.weighty = 1.0D;
		localGridBagConstraints.fill = 1;
		add(localJTabbedPane, localGridBagConstraints);
	}

	void showDialog() {
		this.vMiscExtCommandSP.beforeConfig();
		try {
			if (!V2CSwingUtil.showOKCancelDialog(this, "外部コマンドの設定"))
				return;
		} finally {
			this.vMiscExtCommandSP.afterConfig();
		}
		this.vMiscExtCommandSP.saveChange();
		this.vURLCommandSP.saveChange();
		if (this.vScriptSP != null)
			this.vScriptSP.postConfig();
		this.apiSettings.saveChanges();
		nLastSelectedIndex = this.jTabbedPane.getSelectedIndex();
	}
	public void beforeConfig() {
		this.vMiscExtCommandSP.beforeConfig();
	}
	public void afterConfig() {
		this.vMiscExtCommandSP.afterConfig();
	}
	void applyChanges() {
		this.vMiscExtCommandSP.saveChange();
		this.vURLCommandSP.saveChange();
		if (this.vScriptSP != null)
			this.vScriptSP.postConfig();
		nLastSelectedIndex = this.jTabbedPane.getSelectedIndex();
		this.apiSettings.saveChanges();
	}
}