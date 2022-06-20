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

import android.content.SharedPreferences
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.trushkina.quack.data.mappers.IStorageContactMapper
import ru.trushkina.quack.data.models.StorageContactModel
import ru.trushkina.quack.domain.models.Contact
import ru.trushkina.quack.domain.profile.repositories.IProfileRepository

private const val SP_PROFILE_KEY = "profile"

class ProfileRepository(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson,
    private val mapper: IStorageContactMapper,
    private val ioDispatcher: CoroutineDispatcher
): IProfileRepository {

    override suspend fun getProfile(): Contact? =
        withContext(ioDispatcher) {
            val json = sharedPreferences.getString(SP_PROFILE_KEY, "") ?: ""
            if (json.isNotEmpty()) {
                val contactModel = gson.fromJson(json, StorageContactModel::class.java)
                mapper.convert(contactModel)
            } else {
                null
            }
        }

    override suspend fun setProfile(contact: Contact) =
        withContext(ioDispatcher) {
            val contactModel = mapper.reverse(contact)
            val json = gson.toJson(contactModel)
            sharedPreferences.edit().putString(SP_PROFILE_KEY, json).apply()
        }
}