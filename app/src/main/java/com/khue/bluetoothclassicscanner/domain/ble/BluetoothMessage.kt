package com.khue.bluetoothclassicscanner.domain.ble

data class BluetoothMessage(
    val message: String,
    val senderName: String,
    val isFromLocalUser: Boolean
)
