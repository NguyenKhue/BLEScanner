package com.khue.bluetoothclassicscanner.domain.ble

import java.io.IOException

class TransferFailedException: IOException("Reading incoming data failed")