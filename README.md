#V2C 2ch API patch
詳しくは[V2C wiki](http://v2c.kaz-ic.net/wiki/?2chAPI)を参照
##ビルド方法
###必要なソフトウェア
- git
- jdk 1.5以上 (1.8推奨)
- ant 1.7以上
- eclipseまたはnetbeans (オプション）

###ビルド手順
コマンドラインの場合(全OS共通）
```bash
$ git clone https://github.com/v2c-patch/V2C_api
$ cd V2C_api
$ ant
```

Netbeanの場合
- Team -> Git -> Clone
- Repository URLに「[https://github.com/v2c-patch/V2C_api](https://github.com/v2c-patch/V2C_api)」を指定（他はそのまま）してFinishをクリック
- 完了後のダイアログで「Open Project」をクリック
- V2Cのプロジェクトを右クリックして、Build Projectを実行

いずれの場合もbuildディレクトリ以下にパッチのZIP, .jar, .appが生成されます

###起動確認
コマンドラインの場合(Windows)
```cmd
D:\V2C_api\> java -classpath build\classes;lib\V2C_S20150206.jar V2C
```
コマンドラインの場合(Unix/Linux/MacOS)
```bash
$ java -classpath build/classes:lib/V2C_S20150206.jar V2C
```
Eclipse, Netbeansの場合
* default packageのMain.javaを右クリックしてRun
