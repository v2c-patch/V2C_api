#V2C 2ch API patch
詳しくはhttp://v2c.kaz-ic.net/wiki/?2chAPIを参照
##ビルド方法
###必要なソフトウェア
- git
- jdk 1.5以上 (1.8推奨)
- ant 1.7以上
- eclipseまたはnetbeans (オプション）
###ビルド手順
コマンドラインの場合
```bash
$ git clone https://github.com/v2c-patch/V2C_api
$ cd V2C_api
$ ant
```

Netbeanの場合
- Team -> Git -> Clone
- Repository URLに「https://github.com/v2c-patch/V2C_api」を指定（他はそのまま）してFinishをクリック
- 完了後のダイアログで「Open Project」をクリック
- V2Cのプロジェクトを右クリックして、Build Projectを実行

いずれの場合もbuildディレクトリ以下にパッチのZIP, .jar, .appが生成されます
