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
package ru.trushkina.quack.presentation.lake

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.trushkina.quack.domain.exceptions.ProfileIsNotSetException
import ru.trushkina.quack.domain.proximity.interactors.IProximityInteractor
import ru.trushkina.quack.domain.models.Contact
import javax.inject.Inject

@HiltViewModel
class LakeViewModel @Inject constructor(
    private val proximityInteractor: IProximityInteractor
) : ViewModel() {

    private val _uiScanState = MutableStateFlow(ScanContactsUiState())
    val uiScanState: StateFlow<ScanContactsUiState> = _uiScanState.asStateFlow()

    private val _uiBroadcastingState = MutableStateFlow(BroadcastingUiState())
    val uiBroadcastingState: StateFlow<BroadcastingUiState> = _uiBroadcastingState.asStateFlow()

    private var isContactsObservingStarted = false

    fun startContactsScan() {
        viewModelScope.launch {
            try {
                proximityInteractor.startContactsScan()

                isContactsObservingStarted = true

                while(isContactsObservingStarted) {
                    val contacts = proximityInteractor.getScannedContacts()
                    _uiScanState.update {
                        it.copy(
                            scannedContacts =  contacts
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("LakeViewModel", e.message ?: "")
                _uiScanState.update {
                    it.copy(
                        isScanningError = true
                    )
                }
            }
        }
    }

    fun stopContactsScan() {
        viewModelScope.launch {
            try {
                isContactsObservingStarted = false
                proximityInteractor.stopContactsScan()
            } catch (e: Exception) {
                Log.e("LakeViewModel", e.message ?: "")
            }
        }
    }

    fun observeProfileBroadcastingState() {
        viewModelScope.launch {
            val enabled = proximityInteractor.isProfileBroadcastingEnabled()
            _uiBroadcastingState.update {
                it.copy(
                    isBroadcastingSwitchEnabled = enabled
                )
            }
        }
    }

    fun switchBroadcastingState(enabled: Boolean) {
        viewModelScope.launch {
            try {
                proximityInteractor.setProfileBroadcastingEnabled(enabled)
            } catch (e: ProfileIsNotSetException) {
                Log.e("LakeViewModel", e.message ?: "")
                _uiBroadcastingState.update {
                    it.copy(
                        profileIsNotSet = true
                    )
                }
            } catch (e: Exception) {
                Log.e("LakeViewModel", e.message ?: "")
                _uiBroadcastingState.update {
                    it.copy(
                        isBroadcastingError = true
                    )
                }
            }
        }
    }

    fun userMessageShown() {
        _uiScanState.update {
            it.copy(
                isScanningError = false
            )
        }
        _uiBroadcastingState.update {
            it.copy(
                profileIsNotSet = false,
                isBroadcastingError = false
            )
        }
    }
}

data class ScanContactsUiState(
    val isScanningError: Boolean = false,
    val scannedContacts: List<Contact> = emptyList()
)

data class BroadcastingUiState(
    val profileIsNotSet: Boolean = false,
    val isBroadcastingError: Boolean = false,
    val isBroadcastingSwitchEnabled: Boolean = false
)