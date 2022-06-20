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
package ru.trushkina.quack.data.mappers

import ru.trushkina.quack.data.models.StorageContactModel
import ru.trushkina.quack.domain.models.Contact

class StorageContactMapper: IStorageContactMapper {

    override fun convert(contact: StorageContactModel): Contact =
        Contact(
            firstName = contact.firstName ?: "",
            lastName = contact.lastName,
            organization = contact.organization,
            jobTitle = contact.jobTitle,
            avatarUrl = contact.avatarUrl,
            phoneNumber = contact.phoneNumber,
            telegram = contact.telegram,
            vkProfileUrl = contact.vkProfileUrl,
            webPage = contact.webPage
        )

    override fun reverse(contact: Contact): StorageContactModel =
        StorageContactModel(
            firstName = contact.firstName,
            lastName = contact.lastName,
            organization = contact.organization,
            jobTitle = contact.jobTitle,
            avatarUrl = contact.avatarUrl,
            phoneNumber = contact.phoneNumber,
            telegram = contact.telegram,
            vkProfileUrl = contact.vkProfileUrl,
            webPage = contact.webPage
        )
}