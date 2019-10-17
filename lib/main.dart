import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_blue/flutter_blue.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  //---------------Androidメソッド呼び出し実装START---------------//
  static const _platform = const MethodChannel("package.name/sample");

  void _getDataFromPlatform() async {
    try {
      var result = await _platform.invokeMethod("test");
      print(result);
    } catch (e) {
      print(e);
    }
  }
  //---------------Androidメソッド呼び出し実装 END ---------------//

  @override
  Widget build(BuildContext context) {
    // BT接続可能デバイスにデータ送信
    _getDataFromPlatform();

    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // Try running your application with "flutter run". You'll see the
        // application has a blue toolbar. Then, without quitting the app, try
        // changing the primarySwatch below to Colors.green and then invoke
        // "hot reload" (press "r" in the console where you ran "flutter run",
        // or simply save your changes to "hot reload" in a Flutter IDE).
        // Notice that the counter didn't reset back to zero; the application
        // is not restarted.
        primarySwatch: Colors.blue,
      ),
      home: MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.title}) : super(key: key);

  // This widget is the home page of your application. It is stateful, meaning
  // that it has a State object (defined below) that contains fields that affect
  // how it looks.

  // This class is the configuration for the state. It holds the values (in this
  // case the title) provided by the parent (in this case the App widget) and
  // used by the build method of the State. Fields in a Widget subclass are
  // always marked "final".

  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  int _counter = 0;

  void _incrementCounter() {
    //scanDevices();
    scanBTDevices();
    setState(() {
      // This call to setState tells the Flutter framework that something has
      // changed in this State, which causes it to rerun the build method below
      // so that the display can reflect the updated values. If we changed
      // _counter without calling setState(), then the build method would not be
      // called again, and so nothing would appear to happen.
      _counter++;
    });
  }

  //---------------BT接続周りの実装START---------------//
  final int timeout = 4;
  final FlutterBlue _flutterBlue = FlutterBlue.instance;
  final String _serviceUuid = '47968aa6-eb7e-11e9-81b4-2a2ae2dbcce4';
  final String _characteristicUuid = '47968d76-eb7e-11e9-81b4-2a2ae2dbcce4';
  BluetoothDeviceState lastState;
  bool isCommunicating = false;
  List<BluetoothDevice> _devices = [];
  BluetoothDevice _device;
  List<ScanResult> _scanResults = [];
  String _connectedDeviceName = '';
  /// ログ出力
  void log (String tag, String text) {
    print ('$tag $text');
  }

  /// BLEメイン処理
  void bleExecute() {

  }

  /// 端末のスキャン
  void scanBTDevices() {
    isCommunicating = false;
    _flutterBlue.scan(timeout: Duration(seconds: timeout)).listen((scanResult) async {
      _scanResults.add(scanResult);
    }, onDone: postFunc);
  }

  /// スキャン後の処理
  void postFunc() {
    // スキャンの終了
    _flutterBlue.stopScan().then((result) {
      // 対象端末の抽出
      List<BluetoothDevice> targetDevices = _scanResults
          .where((s) => s.advertisementData.connectable)
          .where((s) => s.advertisementData.serviceUuids
          .where((u) => u == _serviceUuid).length > 0)
          .map((s) => s.device).toSet().toList();
      log("対象端末の抽出", '対象端末数: ${targetDevices.length}');
      // 対象端末に接続
      var timeout = 30;
      targetDevices.forEach((device) {
        var deviceInfo = "${device.name}(${device.id})";
        if (lastState != BluetoothDeviceState.connected) {
          device.connect(timeout: Duration(seconds: timeout));
        }
        else {
          log(deviceInfo, '端末との接続が完了しているため接続処理は実施しません。');
          // 接続端末とデータのやり取り
          communicate(device);
        }

        device.state.listen((state) {
          if (state != lastState) {
            lastState = state;
            // 接続完了時の処理
            if (state == BluetoothDeviceState.connected) {
              log(deviceInfo, '端末との接続が完了しました。');
              // 接続端末とデータのやり取り
              communicate(device);
            }

            // 切断完了時の処理
            if (state == BluetoothDeviceState.disconnected) {
              log(deviceInfo, '端末との接続が切断されました。');
              // スキャン再開
              //scanBTDevices();
              setState(() {
                _connectedDeviceName = '';
              });
            }

            if (state == BluetoothDeviceState.connecting) {
              log(deviceInfo, '端末との接続を試行中。');
            }

            if (state == BluetoothDeviceState.disconnecting) {
              log(deviceInfo, '端末との接続切断を試行中。');
            }
          }
          else {
            log(deviceInfo, '接続状態に変更なし。接続状態: ${lastState.toString()}');
          }
        });
      });
    });
  }

  void communicate(BluetoothDevice device) {
    var deviceInfo = "${device.name}(${device.id})";

    if (isCommunicating) {
      log(deviceInfo, '既に実行中のため接続端末とのやり取りは実施しません。');
      return;
    }

    isCommunicating = true;

    device.discoverServices().then((services) {
      BluetoothService service = services.where((s) => s.uuid.toString() == _serviceUuid).toList()?.first;
      log(deviceInfo, '対象サービス: ${service.uuid.toString()}');
      if (service != null) {
        var characteristic = service.characteristics.where((s) => s.uuid.toString() == _characteristicUuid).toList()?.first;
        log(deviceInfo, '対象キャラクタリスティック: ${characteristic.uuid.toString()}');
        setState(() {
          _connectedDeviceName = device.name;
        });
        if (characteristic != null) {
          characteristic.read().then((val) {
            log(deviceInfo, 'キャラクタリスティック内容: $val');
          });
        }
        else {
          log(deviceInfo, 'サービス内に対象キャラクタリスティックが見つかりませんでした。');
        }
      }
      else {
        log(deviceInfo, '端末内に対象サービスが見つかりませんでした。');
      }

      // 端末との切断
      device.disconnect();
      isCommunicating = false;
    });
  }

  void onDone() {
    _flutterBlue.stopScan();
//    _devices = _devices.toSet().toList();
//    _devices.forEach ((device) {
//      print('found device: ${device.name}(${device.id})');
//    });

//    _scanResults = _scanResults.toSet().toList();
//    _scanResults.forEach ((scanResult) {
//      print('---------------------------------------------------');
//      print('device: ${scanResult.device.name}(${scanResult.device.id})');
//      print('advertisementData: ${scanResult.advertisementData}(connectable: ${scanResult.advertisementData.connectable})');
//      print('serviceUuids: ${scanResult.advertisementData.serviceUuids}');
//    });
  }
  //---------------BT接続周りの実装 END ---------------//

  @override
  Widget build(BuildContext context) {
    // This method is rerun every time setState is called, for instance as done
    // by the _incrementCounter method above.
    //
    // The Flutter framework has been optimized to make rerunning build methods
    // fast, so that you can just rebuild anything that needs updating rather
    // than having to individually change instances of widgets.
    return Scaffold(
      appBar: AppBar(
        // Here we take the value from the MyHomePage object that was created by
        // the App.build method, and use it to set our appbar title.
        title: Text(widget.title),
      ),
      body: Center(
        // Center is a layout widget. It takes a single child and positions it
        // in the middle of the parent.
        child: Column(
          // Column is also a layout widget. It takes a list of children and
          // arranges them vertically. By default, it sizes itself to fit its
          // children horizontally, and tries to be as tall as its parent.
          //
          // Invoke "debug painting" (press "p" in the console, choose the
          // "Toggle Debug Paint" action from the Flutter Inspector in Android
          // Studio, or the "Toggle Debug Paint" command in Visual Studio Code)
          // to see the wireframe for each widget.
          //
          // Column has various properties to control how it sizes itself and
          // how it positions its children. Here we use mainAxisAlignment to
          // center the children vertically; the main axis here is the vertical
          // axis because Columns are vertical (the cross axis would be
          // horizontal).
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Text(
              'Connected Device Name: $_connectedDeviceName',
            ),
            Text(
              '$_counter',
              style: Theme.of(context).textTheme.display1,
            ),
          ],
        ),
      ),
      floatingActionButton: StreamBuilder<bool>(
        stream: FlutterBlue.instance.isScanning,
        initialData: false,
        builder: (c, snapshot) {
          if (snapshot.data) {
            return FloatingActionButton(
              child: Icon(Icons.stop),
              onPressed: () => FlutterBlue.instance.stopScan(),
              backgroundColor: Colors.red,
            );
          } else {
            return FloatingActionButton(
                child: Icon(Icons.search),
                onPressed: () => FlutterBlue.instance
                    .startScan(timeout: Duration(seconds: 4)));
          }
        },
      ), // This trailing comma makes auto-formatting nicer for build methods.
    );
  }
}
