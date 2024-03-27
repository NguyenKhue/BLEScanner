package com.khue.bluetoothclassicscanner.di

import android.content.Context
import com.khue.bluetoothclassicscanner.data.ble.AndroidBluetoothController
import com.khue.bluetoothclassicscanner.domain.ble.BluetoothController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBluetoothController(@ApplicationContext context: Context): BluetoothController {
        return AndroidBluetoothController(context)
    }
}