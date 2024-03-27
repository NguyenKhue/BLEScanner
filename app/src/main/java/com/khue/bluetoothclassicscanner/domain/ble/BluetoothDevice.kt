package com.khue.bluetoothclassicscanner.domain.ble

typealias BluetoothDeviceDomain = BluetoothDevice

data class BluetoothDevice(
    val name: String?,
    val address: String
)
