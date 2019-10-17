package com.example.ble_app

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast

import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugins.GeneratedPluginRegistrant
import java.util.*

class MainActivity: FlutterActivity() {
  companion object {
      // main.dartでMethodChannelのコンストラクタで指定した文字列です
      private const val CHANNEL = "package.name/sample"
      // main.dartでinvokeMethodの第一引数に指定したmethodの文字列です
      private const val METHOD_TEST = "test"
      // UUID
      private const val UUID_SERVICE = "47968aa6-eb7e-11e9-81b4-2a2ae2dbcce4"
      private const val UUID_CHARACTERISTIC = "47968d76-eb7e-11e9-81b4-2a2ae2dbcce4"
  } 
  
  private lateinit var bleGattCharacteristic: BluetoothGattCharacteristic
  private lateinit var bleGattServer: BluetoothGattServer
  private lateinit var bleLeAdvertiser: BluetoothLeAdvertiser
  private lateinit var bleAdapter: BluetoothAdapter
  private lateinit var bleManager: BluetoothManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    GeneratedPluginRegistrant.registerWith(this)

    // MethodChannelからのメッセージを受け取ります
    // （flutterViewはFlutterActivityのプロパティ、CHANNELはcompanion objectで定義しています）
    MethodChannel(this.flutterView, CHANNEL)
            .setMethodCallHandler { methodCall: MethodCall, result: MethodChannel.Result ->
                if (methodCall.method == METHOD_TEST) {
                    advertise()
                    result.success("Hello from kt")
                }
            }
  }

  private fun advertise() {
      val tag = "MainActivity.advertise"
      bleManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
      bleAdapter = bleManager.adapter

      bleLeAdvertiser = bleAdapter.bluetoothLeAdvertiser
      if (bleLeAdvertiser != null) {
          val btGattService = BluetoothGattService(UUID.fromString(UUID_SERVICE), BluetoothGattService.SERVICE_TYPE_PRIMARY)

          bleGattCharacteristic = BluetoothGattCharacteristic(UUID.fromString(UUID_CHARACTERISTIC), BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattDescriptor.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ)
          bleGattCharacteristic.setValue("advertised")
          btGattService.addCharacteristic(bleGattCharacteristic)

          bleGattServer = bleManager.openGattServer(this, object : BluetoothGattServerCallback() {
              override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                  if (newState == BluetoothProfile.STATE_CONNECTED) {
                      Log.d(tag, "call onConnectionStateChange STATE_CONNECTED: device is ${device.name}")
                  } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                      Log.d(tag, "call onConnectionStateChange STATE_DISCONNECTED: device is ${device.name}")
                  }
              }
          })
          bleGattServer.addService(btGattService)
          val dataBuilder = AdvertiseData.Builder()
          val settingsBuilder = AdvertiseSettings.Builder()
          dataBuilder.setIncludeTxPowerLevel(false)
          dataBuilder.addServiceUuid(ParcelUuid.fromString(UUID_SERVICE))
          settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
          settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
          // Start Bluetooth LE Advertising
          Log.d(tag, "startAdvertising")
          bleLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), object : AdvertiseCallback() {
              override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                  Log.d(tag, "startSuccess")
              }

              override fun onStartFailure(errorCode: Int) {

              }
          })
      } else {
          Toast.makeText(this, "Bluetooth Le Advertiser not supported", Toast.LENGTH_SHORT).show()
      }
  }
}
