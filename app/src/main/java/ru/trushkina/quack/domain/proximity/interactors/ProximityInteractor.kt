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
package ru.trushkina.quack.domain.proximity.interactors

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.trushkina.quack.domain.exceptions.ProfileIsNotSetException
import ru.trushkina.quack.domain.models.Contact
import ru.trushkina.quack.domain.profile.repositories.IProfileRepository
import ru.trushkina.quack.domain.proximity.repositories.IProximityRepository

class ProximityInteractor(
    private val proximityRepository: IProximityRepository,
    private val profileRepository: IProfileRepository,
    private val ioDispatcher: CoroutineDispatcher
) : IProximityInteractor {

    override suspend fun startContactsScan() = proximityRepository.startContactsScan()

    override suspend fun stopContactsScan() = proximityRepository.stopContactsScan()

    override suspend fun getScannedContacts(): List<Contact> = proximityRepository.getScannedContacts()

    override suspend fun setProfileBroadcastingEnabled(enable: Boolean) {
        withContext(ioDispatcher) {
            if (enable) {
                val profile = profileRepository.getProfile()
                if (profile != null) {
                    proximityRepository.startContactBroadcasting(profile)
                } else {
                    throw ProfileIsNotSetException()
                }
            } else {
                proximityRepository.stopContactBroadcasting()
            }
        }
    }

    override suspend fun isProfileBroadcastingEnabled(): Boolean = proximityRepository.isContactBroadcastingStarted()
}