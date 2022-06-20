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
package ru.trushkina.quack.data.repositories

import android.annotation.SuppressLint
import android.bluetooth.le.*
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ru.trushkina.quack.data.datasources.IUuidBleServiceHolder
import ru.trushkina.quack.data.mappers.IBleContactMapper
import ru.trushkina.quack.data.models.BleContactModel
import ru.trushkina.quack.data.models.BleContactPartModel
import ru.trushkina.quack.domain.models.Contact
import ru.trushkina.quack.domain.proximity.repositories.IProximityRepository
import java.lang.StringBuilder
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

private const val SP_BROADCAST_STARTED_KEY = "profileBroadcastStarted"
private const val SCAN_PERIOD = 3_000L
private const val BROADCAST_PERIOD_BETWEEN_PACKAGES = 200L
private const val BROADCAST_PERIOD_BETWEEN_CIRCLES = 2_000L
private const val BLE_PACKET_DATA_LENGTH = 9
private const val BLE_PACKET_DEVICE_ID_LENGTH = 2
private const val CHARSET_START = 0
private const val CHARSET_END = 127

class BleProximityRepository(
    private val ioDispatcher: CoroutineDispatcher,
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson,
    private val bleContactMapper: IBleContactMapper,
    private val bluetoothLeAdvertiser: BluetoothLeAdvertiser,
    private val advertiseSettings: AdvertiseSettings,
    private val bluetoothLeScanner: BluetoothLeScanner,
    private val bluetoothLeScanSettings: ScanSettings,
    private val uuidBleServiceHolder: IUuidBleServiceHolder
) : IProximityRepository {

    private var isBroadcastingTurnedOn = false

    private val protocolCache: MutableMap<String, MutableSet<BleContactPartModel>> = ConcurrentHashMap()

    private val advertisingCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d("BleProximityRepository", "Advertising onStartSuccess")
            super.onStartSuccess(settingsInEffect)
        }

        override fun onStartFailure(errorCode: Int) {
            val errorMsg = when(errorCode) {
                ADVERTISE_FAILED_ALREADY_STARTED -> "ADVERTISE_FAILED_ALREADY_STARTED"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "ADVERTISE_FAILED_FEATURE_UNSUPPORTED"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "ADVERTISE_FAILED_INTERNAL_ERROR"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS"
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "ADVERTISE_FAILED_DATA_TOO_LARGE"
                else -> "Unknown error"
            }

            Log.e("BleProximityRepository", "Advertising onStartFailure: $errorMsg")
            super.onStartFailure(errorCode)
        }
    }

    private val bleContactPartComparator: Comparator<BleContactPartModel> =
        Comparator<BleContactPartModel> { o1, o2 -> o1.index.compareTo(o2.index) }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val bytes = result.scanRecord?.getServiceData(uuidBleServiceHolder.getUuid())
            bytes?.let {
                val data = String(bytes, Charsets.UTF_8)
                Log.d("BleProximityRepository", "Found data: $data")
                try {
                    val deviceId = data.substring(0, 2)
                    var seq = protocolCache[deviceId]
                    if (seq == null) {
                        seq = sortedSetOf(bleContactPartComparator)
                        protocolCache[deviceId] = seq
                    }
                    seq.add(
                        BleContactPartModel(
                            deviceId = deviceId,
                            index = data[2].code - CHARSET_START,
                            count = data[3].code - CHARSET_START,
                            value = data.substring(4)
                        )
                    )
                } catch (e: Exception) {
                    Log.e("BleProximityRepository", "Incorrect BLE package: $data - skipping")
                }
            }

            super.onScanResult(callbackType, result)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun startContactsScan() {
        withContext(ioDispatcher) {
            val filter = ScanFilter.Builder()
                .setServiceData(uuidBleServiceHolder.getUuid(), null)
                .build()

            bluetoothLeScanner.startScan(listOf(filter), bluetoothLeScanSettings, scanCallback)
            Log.d("BleProximityRepository", "startContactsScan")
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopContactsScan() {
        withContext(ioDispatcher) {
//            protocolCache.clear()
            bluetoothLeScanner.stopScan(scanCallback)
            Log.d("BleProximityRepository", "stopContactsScan")
        }
    }

    override suspend fun getScannedContacts() =
        withContext(ioDispatcher) {
            delay(SCAN_PERIOD)
            val contacts = collectContacts()
            Log.d("BleProximityRepository", "getScannedContacts: $contacts")
            contacts
        }

    @SuppressLint("MissingPermission")
    override suspend fun startContactBroadcasting(contact: Contact) =
        withContext(ioDispatcher) {
            if (isContactBroadcastingStarted()) {
                return@withContext
            }

            sharedPreferences.edit().putBoolean(SP_BROADCAST_STARTED_KEY, true).apply()
            isBroadcastingTurnedOn = true

            val deviceId = getRandomDeviceId()

            while (isBroadcastingTurnedOn) {
                val strContact = gson.toJson(bleContactMapper.reverse(contact))
                val optimizedStrContact = optimizeStrContact(strContact)
                val strLength = optimizedStrContact.length
                val totalPackages = ceil((strLength.toDouble() / BLE_PACKET_DATA_LENGTH)).toInt()
                Log.d("BleProximityRepository", "Prepared data for broadcast.\nData: $strContact\nOptimized Data: $optimizedStrContact\nPackages: $totalPackages")
                val end = Char(totalPackages)
                for (i in 0 until totalPackages) {
                    val start = (i + CHARSET_START).toChar()

                    val step = if (i == totalPackages - 1) {
                        val rest = (totalPackages - 1) * BLE_PACKET_DATA_LENGTH
                        if (strLength >= rest) {
                            strLength - rest
                        } else {
                            rest - strLength
                        }
                    } else {
                        BLE_PACKET_DATA_LENGTH
                    }
                    val strData = "${deviceId}${start}${end}${optimizedStrContact.substring(i * BLE_PACKET_DATA_LENGTH, i * BLE_PACKET_DATA_LENGTH + step)}"
                    val data = AdvertiseData.Builder()
                        .setIncludeDeviceName(false)
                        .setIncludeTxPowerLevel(false)
                        .addServiceData(
                            uuidBleServiceHolder.getUuid(),
                            strData.toByteArray(Charsets.UTF_8)
                        )
                        .build()

                    bluetoothLeAdvertiser.startAdvertising(advertiseSettings, data, advertisingCallback)
                    Log.d("BleProximityRepository", "Started broadcasting data part: $strData")
                    delay(BROADCAST_PERIOD_BETWEEN_PACKAGES)
                    bluetoothLeAdvertiser.stopAdvertising(advertisingCallback)
                    Log.d("BleProximityRepository", "Stopped broadcasting data part")
                }
                delay(BROADCAST_PERIOD_BETWEEN_CIRCLES)
            }
        }


    @SuppressLint("MissingPermission")
    override suspend fun stopContactBroadcasting() =
        withContext(ioDispatcher) {
            sharedPreferences.edit().putBoolean(SP_BROADCAST_STARTED_KEY, false).apply()
            isBroadcastingTurnedOn = false
            bluetoothLeAdvertiser.stopAdvertising(object : AdvertiseCallback() {})
        }

    override suspend fun isContactBroadcastingStarted(): Boolean =
        withContext(ioDispatcher) {
            sharedPreferences.getBoolean(SP_BROADCAST_STARTED_KEY, false)
        }

    private fun collectContacts(): List<Contact> {
        val result = mutableListOf<Contact>()
        for (set in protocolCache.values) {
            val msgBuilder = StringBuilder()
            for (parts in set) {
                msgBuilder.append(parts.value)
            }
            val json = revertOptimizedStrContact(msgBuilder.toString())
            Log.d("BleProximityRepository", "Collected json: $json")
            try {
                result.add(bleContactMapper.convert(gson.fromJson(json, BleContactModel::class.java)))
            } catch (e: Exception) {
                Log.e("BleProximityRepository", "Could not parse BLE data: $msgBuilder")
            }
        }

        return result
    }

    private fun getRandomDeviceId() : String {
        val charsetBuilder = StringBuilder()
        for (i in CHARSET_START..CHARSET_END) {
            charsetBuilder.append(Char(i))
        }

        return List(BLE_PACKET_DEVICE_ID_LENGTH) { charsetBuilder.toString().random() }
            .joinToString("")
    }

    private fun optimizeStrContact(strContact: String): String =
        strContact
            .replaceFirst("\"n\":\"", "n:")
            .replaceFirst("\"s\":\"", "s:")
            .replaceFirst("\"p\":\"", "p:")
            .replaceFirst("\"t\":\"", "t:")
            .replaceFirst("\"o\":\"", "o:")
            .replaceFirst("\"j\":\"", "j:")
            .replaceFirst("\"w\":\"", "w:")
            .replaceFirst("\"v\":\"", "v:")
            .replaceFirst("\"a\":\"", "a:")
            .replace("\",", ",")
            .replace("\"}", "")
            .replaceFirst("{", "")

    private fun revertOptimizedStrContact(strContact: String): String =
        "{" + strContact
            .replaceFirst("n:", "\"n\":\"")
            .replaceFirst("s:", "\"s\":\"")
            .replaceFirst("p:", "\"p\":\"")
            .replaceFirst("t:", "\"t\":\"")
            .replaceFirst("o:", "\"o\":\"")
            .replaceFirst("j:", "\"j\":\"")
            .replaceFirst("w:", "\"w\":\"")
            .replaceFirst("v:", "\"v\":\"")
            .replaceFirst("a:", "\"a\":\"")
            .replace(",", "\",") + "\"}"
}