package org.monazilla.v2c;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

public class V2CAPISettings extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final String HMKey_Key = "2chAPI.HMKey";
	private static final String AppKey_Key = "2chAPI.AppKey";
	private static final String UAName_Key = "2chAPI.UAName";
	private static final String UAPost_Key = "2chAPI.UAPost";
	private static final String UAAuth_Key = "2chAPI.UAAuth";
	private static final String XUAName_Key = "2chAPI.X2chUA";
	private static final String UseAPI_Key = "2chAPI.UseAPI";
	private static final String UseSC_Key = "2chAPI.UseSC";
	private static final String UseHTML_Key = "2chAPI.UseHTML";

	JPanel jPanel;
	JTextField[] config = new JTextField[6];

	JCheckBox useAPI = new JCheckBox("2ch APIを使用する", V2CHttpUtil.apiProperty.getBoolean(UseAPI_Key));
	JCheckBox useHTML = new JCheckBox("read.cgiを使用する", V2CHttpUtil.apiProperty.getBoolean(UseHTML_Key, true));
	JCheckBox useSC = new JCheckBox("2ch.scを使用する", V2CHttpUtil.apiProperty.getBoolean(UseSC_Key));

	public V2CAPISettings() {
		this.jPanel = new JPanel(new GridBagLayout());

		GridBagConstraints constraints = V2CSwingUtil
				.createGridBagConstraints();
		JPanel keyConfigPanel = new JPanel(new GridBagLayout());
		keyConfigPanel.setBorder(new TitledBorder("鍵設定"));
		constraints.insets = new Insets(2, 7, 2, 7);

		keyConfigPanel.add(new JLabel("API共通鍵（HMKey）："),
				constraints);
		constraints.gridx += 1;
		config[0] = new JTextField(V2CHttpUtil.getHMKey(), 25);
		keyConfigPanel.add(config[0], constraints);
		constraints.gridx = 0;
		constraints.gridy += 1;
		keyConfigPanel
				.add(new JLabel("API鍵（AppKey）："), constraints);
		constraints.gridx += 1;
		config[1] = new JTextField(V2CHttpUtil.getAppKey(), 25);
		keyConfigPanel.add(config[1], constraints);

		JPanel uaConfigPanel = new JPanel(new GridBagLayout());
		uaConfigPanel.setBorder(new TitledBorder("UA設定"));
		constraints.insets = new Insets(2, 7, 2, 7);
		constraints.gridx = 0;
		constraints.gridy = 0;
		uaConfigPanel.add(new JLabel("認証用UA (X-2ch-UA) :"),
				constraints);
		config[2] = new JTextField(V2CHttpUtil.X2CHUA, 25);
		constraints.gridx = 1;
		uaConfigPanel.add(config[2], constraints);
		constraints.gridx = 0;
		constraints.gridy += 1;
		uaConfigPanel.add(new JLabel("ユーザーエージェント(DAT取得) :"),
				constraints);
		config[3] = new JTextField(V2CHttpUtil.UAName, 25);
		constraints.gridx = 1;
		uaConfigPanel.add(config[3], constraints);

                constraints.gridx = 0;
		constraints.gridy += 1;
		uaConfigPanel.add(new JLabel("ユーザーエージェント(書き込み) :"),
				constraints);
		config[4] = new JTextField(V2CHttpUtil.UAPost, 25);
		constraints.gridx = 1;
		uaConfigPanel.add(config[4], constraints);

		constraints.gridx = 0;
		constraints.gridy += 1;
		uaConfigPanel.add(new JLabel("ユーザーエージェント(認証) :"),
				constraints);
		config[5] = new JTextField(V2CHttpUtil.UAAuth, 25);
		constraints.gridx = 1;
		uaConfigPanel.add(config[5], constraints);

        constraints.gridx = 0;
		constraints.gridy = 0;
		this.jPanel.add(useAPI, constraints);
		constraints.gridy++;		
		this.jPanel.add(useHTML, constraints);
		constraints.gridy++;
		if (!V2CHttpUtil.apiProperty.getBoolean("I.hate.Tarako")){
			this.jPanel.add(useSC, constraints);
			constraints.gridy++;
		}
		this.jPanel.add(keyConfigPanel, constraints);
		constraints.gridy++;
		this.jPanel.add(uaConfigPanel, constraints);
		JLabel l = new JLabel("V2C API Patch - B10");
		constraints.gridy++;
		constraints.gridwidth = 300;
		this.jPanel.add(l, constraints);
	}

	JPanel getPanel() {
		return this.jPanel;
	}

	public void saveChanges() {
		V2CProperty apiProperty = V2CHttpUtil.apiProperty;
		apiProperty.put(HMKey_Key, config[0].getText());
		apiProperty.put(AppKey_Key, config[1].getText());
		apiProperty.put(XUAName_Key, config[2].getText());
		apiProperty.put(UAName_Key, config[3].getText());
		apiProperty.put(UAPost_Key, config[4].getText());
		apiProperty.put(UAAuth_Key, config[5].getText());
		apiProperty.putBoolean(UseAPI_Key, useAPI.isSelected());
		apiProperty.putBoolean(UseSC_Key, useSC.isSelected());
		apiProperty.putBoolean(UseHTML_Key, useHTML.isSelected());
		V2CHttpUtil.HMKey = config[0].getText();
		V2CHttpUtil.AppKey = config[1].getText();
		V2CHttpUtil.X2CHUA = config[2].getText();
		V2CHttpUtil.UAName = config[3].getText();
		V2CHttpUtil.UAPost = config[4].getText();
		V2CHttpUtil.UAAuth= config[5].getText();
		V2CHttpUtil.useAPI = useAPI.isSelected();
		V2CHttpUtil.useSC = useSC.isSelected();
		V2CHttpUtil.useHTML = useHTML.isSelected();
		V2CHttpUtil.SID_updater.interrupt();
		apiProperty.doSaveState();
	}
}
