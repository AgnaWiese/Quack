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

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.trushkina.quack.R
import ru.trushkina.quack.databinding.FragmentProfileBinding
import ru.trushkina.quack.domain.models.Contact
import ru.trushkina.quack.presentation.utis.hideKeyboard
import ru.trushkina.quack.presentation.utis.showBottomNav

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private val profileViewModel: ProfileViewModel by viewModels()

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showBottomNav()

        with(binding) {
            toolbar.title = getString(R.string.your_profile)
            btnSave.setOnClickListener {
                profileViewModel.saveProfileAsync(getContactFromEditTexts())
                clearFocus()
                hideKeyboard()
            }
        }

        getProfileAsync()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getContactFromEditTexts(): Contact =
        Contact(
            firstName = binding.profileName.tfFirstName.editText?.text.toString(),
            lastName = binding.profileName.tfLastName.editText?.text.toString(),
            organization = binding.profileOrganization.tfOrganization.editText?.text.toString(),
            jobTitle = binding.profileOrganization.tfJobTitle.editText?.text.toString(),
            phoneNumber = binding.profileContacts.tfPhoneNumber.editText?.text.toString(),
            telegram = binding.profileContacts.tfTelegram.editText?.text.toString(),
            vkProfileUrl = binding.profileWeb.tfVK.editText?.text.toString(),
            webPage = binding.profileWeb.tfWebPage.editText?.text.toString()
        )

    private fun clearFocus() {
        binding.apply {
            with(profileName) {
                tfFirstName.editText?.clearFocus()
                tfLastName.editText?.clearFocus()
            }
            with(profileOrganization) {
                tfOrganization.editText?.clearFocus()
                tfJobTitle.editText?.clearFocus()
            }
            with(profileContacts) {
                tfPhoneNumber.editText?.clearFocus()
                tfTelegram.editText?.clearFocus()
            }
            with(profileWeb) {
                tfVK.editText?.clearFocus()
                tfWebPage.editText?.clearFocus()
            }
        }
    }

    private fun clearErrors() {
        binding.profileName.tfFirstName.error = null
    }

    private fun showSnackBar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        profileViewModel.userMessageShown()
    }

    private fun updateTextFields(profile: Contact) {
        with(binding) {
            with(profileName) {
                tfFirstName.editText?.setText(profile.firstName)
                tfLastName.editText?.setText(profile.lastName)
            }
            with(profileOrganization) {
                tfOrganization.editText?.setText(profile.organization)
                tfJobTitle.editText?.setText(profile.jobTitle)
            }
            with(profileContacts) {
                tfPhoneNumber.editText?.setText(profile.phoneNumber)
                tfTelegram.editText?.setText(profile.telegram)
            }
            with(profileWeb) {
                tfVK.editText?.setText(profile.vkProfileUrl)
                tfWebPage.editText?.setText(profile.webPage)
            }
        }
    }

    private fun getProfileAsync() {
        profileViewModel.getProfileAsync()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.uiState.collectLatest {
                    Log.d("LakeFragment", "UI update: $it")
                    when {
                        it.showSavingSuccess -> {
                            clearErrors()
                            showSnackBar(getString(R.string.saved_profile))
                        }
                        it.showErrorSaving -> {
                            showSnackBar(getString(R.string.error_saving_profile))
                        }
                        it.showErrorLoading -> {
                            showSnackBar(getString(R.string.error_loading_profile))
                        }
                        it.showErrorInput -> {
                            showSnackBar(getString(R.string.error_invalid_profile_input))
                            binding.profileName.tfFirstName.error = getString(R.string.required_parameter)
                        }
                        it.profile != null -> {
                            clearErrors()
                            updateTextFields(it.profile)
                        }
                    }
                }
            }
        }
    }
}