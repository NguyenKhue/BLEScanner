package com.khue.bluetoothclassicscanner

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.khue.bluetoothclassicscanner.ui.theme.BluetoothClassicScannerTheme
import java.io.IOException
import java.util.UUID

const val PERMISSION_BLUETOOTH_SCAN = "android.permission.BLUETOOTH_SCAN"
const val PERMISSION_BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT"

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    val foundDevices =   mutableStateListOf<BluetoothDevice>()

    private val requestEnableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {}
        else {}
    }

    private val bleStateChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                Log.d("BLEClient", "state: ${intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)}")
            }
        }
    }

    private val bleDiscoverDeviceReceiver = object : BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        val deviceName = device.name
                        val deviceHardwareAddress = device.address // MAC address
                        if(!deviceName.isNullOrEmpty() && !foundDevices.contains(device)) {
                            foundDevices.add(device)
                            Log.d("BLEClient", "ble device: name: $deviceName, address: $deviceHardwareAddress")
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter ?: throw Exception("Bluetooth is not supported by this device")

        if(haveAllPermissions(this)) {
            registerReceiver(bleStateChangeReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
            registerReceiver(bleDiscoverDeviceReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        }

        setContent {
            BluetoothClassicScannerTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val context = LocalContext.current
                    var allPermissionsGranted by remember {
                        mutableStateOf (haveAllPermissions(context))
                    }
                    var isScanning by remember {
                        mutableStateOf(false)
                    }

                    if (!allPermissionsGranted) {
                        PermissionsRequiredScreen {
                            allPermissionsGranted = true
                            enableBluetooth()
                            registerReceiver(bleStateChangeReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
                            registerReceiver(bleDiscoverDeviceReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
                        }
                    }
                    else {
                        ScanningScreen(
                            isScanning = isScanning,
                            foundDevices = foundDevices,
                            startScanning = {
                                foundDevices.clear()
                                bluetoothAdapter.startDiscovery()
                                isScanning = true
                            },
                            stopScanning = {
                                bluetoothAdapter.cancelDiscovery()
                                isScanning = false
                            },
                            selectDevice = { device ->
                                device.createBond().also {
                                    Log.d("BLEClient", "ble device: name: ${device.name} create bond: $it")
                                }
                            },
                            connectSocket = {
                                isScanning = false
                                ConnectThread(it).start()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun enableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestEnableBluetoothLauncher.launch(enableBtIntent)
        }
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(private val device: BluetoothDevice) : Thread() {
        private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(MY_UUID)
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery()

            mmSocket?.connect()
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e("BLEClient", "Could not close the client socket", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bleStateChangeReceiver)
        unregisterReceiver(bleDiscoverDeviceReceiver)
    }
}

val ALL_BLE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    )
}
else {
    arrayOf(
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
}
fun haveAllPermissions(context: Context) =
    ALL_BLE_PERMISSIONS
        .all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

@Composable
fun PermissionsRequiredScreen(onPermissionGranted: () -> Unit) {
    Box {
        Column(
            modifier = Modifier.align(Alignment.Center)
        ) {
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { granted ->
                if (granted.values.all { it }) {
                    onPermissionGranted()
                }
            }
            Button(onClick = { launcher.launch(ALL_BLE_PERMISSIONS) }) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
@RequiresPermission(allOf = [PERMISSION_BLUETOOTH_SCAN, PERMISSION_BLUETOOTH_CONNECT])
fun ScanningScreen(
    isScanning: Boolean,
    foundDevices: List<BluetoothDevice>,
    startScanning: () -> Unit,
    stopScanning: () -> Unit,
    selectDevice: (BluetoothDevice) -> Unit,
    connectSocket: (BluetoothDevice) -> Unit
) {
    Column (
        Modifier.padding(horizontal = 10.dp)
    ){
        if (isScanning) {
            Text("Scanning...")

            Button(onClick = stopScanning) {
                Text("Stop Scanning")
            }
        }
        else {
            Button(onClick = startScanning) {
                Text("Start Scanning")
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(foundDevices) { device ->
                DeviceItem(
                    deviceName = device.name,
                    selectDevice = { selectDevice(device) },
                    connectSocket = { connectSocket(device) }
                )
            }
        }
    }
}

@Composable
fun DeviceItem(deviceName: String?, selectDevice: () -> Unit, connectSocket: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = deviceName ?: "[Unnamed]",
                textAlign = TextAlign.Center,
            )
            Row {
                Button(onClick = selectDevice) {
                    Text("Connect")
                }
                Button(onClick = connectSocket) {
                    Text("Connect socket")
                }
            }

        }
    }
}