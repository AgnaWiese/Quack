/*
 * Copyright (C) 2022. Evgenia Trushkina
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.trushkina.quack.di

import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import ru.trushkina.quack.data.datasources.IUuidBleServiceHolder
import ru.trushkina.quack.data.datasources.UuidBleServiceHolder
import ru.trushkina.quack.data.mappers.BleContactMapper
import ru.trushkina.quack.data.mappers.IBleContactMapper
import ru.trushkina.quack.data.repositories.BleProximityRepository
import ru.trushkina.quack.domain.profile.repositories.IProfileRepository
import ru.trushkina.quack.domain.proximity.interactors.IProximityInteractor
import ru.trushkina.quack.domain.proximity.interactors.ProximityInteractor
import ru.trushkina.quack.domain.proximity.repositories.IProximityRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ProximityModule {

    @Provides
    @Singleton
    fun provideBleContactMapper(): IBleContactMapper = BleContactMapper()

    @Provides
    @Singleton
    fun provideBluetoothLeScanSettings(): ScanSettings =
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

    @Provides
    @Singleton
    fun provideUuidBleServiceHolder(@ApplicationContext context: Context): IUuidBleServiceHolder =
        UuidBleServiceHolder(context)

    @Provides
    @Singleton
    fun provideBluetoothManager(@ApplicationContext context: Context): BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    @Provides
    @Singleton
    fun provideAdvertiseSettings(): AdvertiseSettings =
        AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

    @Provides
    @Singleton
    fun provideProximityRepository(
        ioDispatcher: CoroutineDispatcher,
        sharedPreferences: SharedPreferences,
        gson: Gson,
        bleContactMapper: IBleContactMapper,
        bluetoothManager: BluetoothManager,
        advertiseSettings: AdvertiseSettings,
        bluetoothLeScanSettings: ScanSettings,
        uuidBleServiceHolder: IUuidBleServiceHolder
    ): IProximityRepository = BleProximityRepository(
        ioDispatcher,
        sharedPreferences,
        gson,
        bleContactMapper,
        bluetoothManager,
        advertiseSettings,
        bluetoothLeScanSettings,
        uuidBleServiceHolder
    )

    @Provides
    @Singleton
    fun provideProximityInteractor(
        proximityRepository: IProximityRepository,
        profileRepository: IProfileRepository,
        ioDispatcher: CoroutineDispatcher
    ): IProximityInteractor = ProximityInteractor(proximityRepository, profileRepository, ioDispatcher)
}