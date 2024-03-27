package com.khue.bluetoothclassicscanner.presentation

import com.khue.bluetoothclassicscanner.domain.ble.BluetoothDevice
import com.khue.bluetoothclassicscanner.domain.ble.BluetoothMessage

data class BluetoothUiState(
    val scannedDevices: List<BluetoothDevice> = emptyList(),
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val messages: List<BluetoothMessage> = emptyList()
)
