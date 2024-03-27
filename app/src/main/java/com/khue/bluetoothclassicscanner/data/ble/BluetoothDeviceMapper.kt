package com.khue.bluetoothclassicscanner.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.khue.bluetoothclassicscanner.domain.ble.BluetoothDeviceDomain

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain(): BluetoothDeviceDomain {
    return BluetoothDeviceDomain(
        name = name,
        address = address
    )
}