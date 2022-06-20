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
package ru.trushkina.quack.presentation.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.trushkina.quack.domain.models.Contact
import ru.trushkina.quack.domain.profile.interactors.IProfileInteractor
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileInteractor: IProfileInteractor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun getProfileAsync() {
        viewModelScope.launch {
            try {
                val profile = profileInteractor.getProfile()
                _uiState.update {
                    it.copy(
                        profile = profile
                    )
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", e.message ?: "")
                _uiState.update {
                    it.copy(
                        showErrorLoading = true
                    )
                }
            }
        }
    }

    fun saveProfileAsync(profile: Contact) {
        viewModelScope.launch {
            try {
                profileInteractor.setProfile(profile)
                _uiState.update {
                    it.copy(
                        showSavingSuccess = true
                    )
                }
            } catch (e: IllegalArgumentException) {
                Log.e("ProfileViewModel", e.message ?: "")
                _uiState.update {
                    it.copy(
                        showErrorInput = true
                    )
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", e.message ?: "")
                _uiState.update {
                    it.copy(
                        showErrorSaving = true
                    )
                }
            }
        }
    }

    fun userMessageShown() {
        _uiState.update {
            it.copy(
                showErrorLoading = false,
                showErrorSaving = false,
                showSavingSuccess = false,
                showErrorInput = false,
                profile = null
            )
        }
    }
}

data class ProfileUiState(
    val showErrorLoading: Boolean = false,
    val showErrorSaving: Boolean = false,
    val showSavingSuccess: Boolean = false,
    val showErrorInput: Boolean = false,
    val profile: Contact? = null
)