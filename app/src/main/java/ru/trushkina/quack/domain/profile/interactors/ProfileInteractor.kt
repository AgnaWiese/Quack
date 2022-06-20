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
package ru.trushkina.quack.domain.profile.interactors

import ru.trushkina.quack.domain.models.Contact
import ru.trushkina.quack.domain.profile.repositories.IProfileRepository

class ProfileInteractor(
    private val profileRepository: IProfileRepository
) : IProfileInteractor{

    override suspend fun getProfile(): Contact? = profileRepository.getProfile()

    @Throws(IllegalArgumentException::class)
    override suspend fun setProfile(contact: Contact) =
        if (validateProfile(contact)) {
            profileRepository.setProfile(contact)
        } else {
            throw IllegalArgumentException("Contact is not valid. First Name should be specified")
        }

    private fun validateProfile(contact: Contact): Boolean =
        contact.firstName.isNotBlank()
}