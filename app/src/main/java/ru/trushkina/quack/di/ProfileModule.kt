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

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import ru.trushkina.quack.data.mappers.StorageContactMapper
import ru.trushkina.quack.data.mappers.IStorageContactMapper
import ru.trushkina.quack.data.repositories.ProfileRepository
import ru.trushkina.quack.domain.profile.interactors.IProfileInteractor
import ru.trushkina.quack.domain.profile.interactors.ProfileInteractor
import ru.trushkina.quack.domain.profile.repositories.IProfileRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ProfileModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    @Provides
    @Singleton
    fun provideContactMapper(): IStorageContactMapper = StorageContactMapper()

    @Provides
    @Singleton
    fun provideProfileRepository(
        sharedPreferences: SharedPreferences,
        gson: Gson,
        contactMapper: IStorageContactMapper,
        ioDispatcher: CoroutineDispatcher
    ): IProfileRepository = ProfileRepository(
        sharedPreferences,
        gson,
        contactMapper,
        ioDispatcher
    )

    @Provides
    @Singleton
    fun provideProfileInteractor(profileRepository: IProfileRepository): IProfileInteractor =
        ProfileInteractor(profileRepository)
}