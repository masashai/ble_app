package com.example.ble_app

import android.os.Bundle

import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugins.GeneratedPluginRegistrant
import java.time.LocalDateTime

class MainActivity: FlutterActivity() {
  companion object {
      // main.dartでMethodChannelのコンストラクタで指定した文字列です
      private const val CHANNEL = "package.name/sample"
      // main.dartでinvokeMethodの第一引数に指定したmethodの文字列です
      private const val METHOD_TEST = "test"
  } 
  
  private lateinit var bleGattCharacteristic: BluetoothGattCharacteristic
  private lateinit var bleGattServer: BluetoothGattServer
  private lateinit var bleLeAdvertiser: BluetoothLeAdvertiser
  private lateinit var bleAdapter: BluetoothAdapter
  private lateinit var bleManager: BluetoothManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    GeneratedPluginRegistrant.registerWith(this)

    bleManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    bleAdapter = bleManager.getAdapter()

    bleLeAdvertiser = bleAdapter.getBluetoothLeAdvertiser()
    if (bleLeAdvertiser != null) {
        val btGattService = BluetoothGattService(UUID.fromString(getString(R.string.uuid_service)), BluetoothGattService.SERVICE_TYPE_PRIMARY)

        bleGattCharacteristic = BluetoothGattCharacteristic(UUID.fromString(getString(R.string.uuid_characteristic)), BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattDescriptor.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ)
        bleGattCharacteristic.setValue("BT");
        btGattService.addCharacteristic(bleGattCharacteristic)

        bleGattServer = bleManager.openGattServer(this, gattServerCallback)
        bleGattServer.addService(btGattService)
        val dataBuilder = AdvertiseData.Builder()
        val settingsBuilder = AdvertiseSettings.Builder()
        dataBuilder.setIncludeTxPowerLevel(false)
        dataBuilder.addServiceUuid(ParcelUuid.fromString(getString(R.string.uuid_service)))
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
        // Start Bluetooth LE Advertising
        bleLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {

            }

            override fun onStartFailure(errorCode: Int) {

            }
        })
    } else {
        Toast.makeText(this, "Bluetooth Le Advertiser not supported", Toast.LENGTH_SHORT).show()
    }
    // MethodChannelからのメッセージを受け取ります
    // （flutterViewはFlutterActivityのプロパティ、CHANNELはcompanion objectで定義しています）
    MethodChannel(this.flutterView, CHANNEL)
            .setMethodCallHandler { methodCall: MethodCall, result: MethodChannel.Result ->
                if (methodCall.method == METHOD_TEST) {
                    result.success("Hello from kt")
                }
            }
  }
}
