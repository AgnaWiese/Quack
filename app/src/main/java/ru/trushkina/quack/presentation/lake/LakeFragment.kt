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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.trushkina.quack.R
import ru.trushkina.quack.databinding.FragmentLakeBinding
import ru.trushkina.quack.presentation.lake.adapter.ScannedContactsAdapter
import ru.trushkina.quack.presentation.utis.showBottomNav
import ru.trushkina.quack.presentation.utis.switchBottomNavigation

@AndroidEntryPoint
class LakeFragment : Fragment() {

    private val lakeViewModel: LakeViewModel by viewModels()

    private var _binding: FragmentLakeBinding? = null
    private val binding get() = _binding!!

    private val requestScanPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            when {
                isGranted -> {
                    binding.btnGetPermission.visibility = View.GONE
                    lakeViewModel.startContactsScan()
                    launchScanFlow()
                }
                else -> showNoPermissionDialog(R.string.alert_no_perm_msg)
            }
        }

    private val requestBroadcastPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            when {
                isGranted -> lakeViewModel.switchBroadcastingState(true)
                else -> showNoPermissionDialog(R.string.alert_no_perm_broadcasting_msg)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLakeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            toolbar.title = getString(R.string.people_nearby)
            switchBroadcasting.setOnCheckedChangeListener { _, enabled ->
                onSwitchBroadcastingClick(enabled)
            }
        }

        launchBroadcastingFlow()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                launchScanFlow()
            } else {
                showPermissionScanRationaleUiBle()
            }
        } else {
            if (checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                launchScanFlow()
            } else {
                showPermissionScanRationaleUiLocation()
            }
        }

        lakeViewModel.observeProfileBroadcastingState()

        showBottomNav()
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                lakeViewModel.startContactsScan()
            }
        } else {
            if (checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                lakeViewModel.startContactsScan()
            }
            lakeViewModel.startContactsScan()
        }
    }

    override fun onStop() {
        lakeViewModel.stopContactsScan()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onSwitchBroadcastingClick(enabled: Boolean) {
        val hasPermission = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
            else -> true
        }
        val shouldSwitchBroadcastingState = when {
            enabled -> hasPermission
            else -> true
        }
        when {
            shouldSwitchBroadcastingState -> lakeViewModel.switchBroadcastingState(enabled)
            else -> {
                binding.switchBroadcasting.isChecked = !enabled
                if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestBroadcastPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADVERTISE)
                }
            }
        }
    }

    private fun showSnackBar(@StringRes message: Int) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        lakeViewModel.userMessageShown()
    }

    private fun showProfileSetAlert() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.alert_profile_creation))
            .setMessage(getString(R.string.alert_profile_creation_msg))
            .setPositiveButton(getString(R.string.alert_lets_go)) { dialog, _ ->
                switchBottomNavigation(R.id.navigation_profile)
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
        lakeViewModel.userMessageShown()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun showPermissionScanRationaleUiBle() {
        with(binding) {
            tvQuackMsg.text = getString(R.string.grant_scan_perm_rationale_ble)
            btnGetPermission.setOnClickListener {
                requestScanPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
            }
            btnGetPermission.visibility = View.VISIBLE
            quackMsgView.visibility = View.VISIBLE
        }
    }

    private fun showPermissionScanRationaleUiLocation() {
        with(binding) {
            tvQuackMsg.text = getString(R.string.grant_scan_perm_rationale_location)
            btnGetPermission.setOnClickListener {
                requestScanPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            btnGetPermission.visibility = View.VISIBLE
            quackMsgView.visibility = View.VISIBLE
        }
    }

    private fun hideHintUI() {
        with(binding) {
            quackMsgView.visibility = View.GONE
            btnGetPermission.visibility = View.GONE
            progressIndicator.visibility = View.GONE
            tvQuackMsg.text = ""
        }
    }

    private fun showNoContactsNearbyHint() {
        with(binding) {
            tvQuackMsg.text = getString(R.string.no_quack_quack_so_far)
            quackMsgView.visibility = View.VISIBLE
            progressIndicator.visibility = View.VISIBLE
        }
    }

    private fun checkPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

    private fun launchScanFlow() =
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                lakeViewModel.uiScanState.collectLatest {
                    Log.d("LakeFragment", "UI update: $it")

                    binding.scannedContactsList.adapter = ScannedContactsAdapter(it.scannedContacts)

                    if (it.scannedContacts.isEmpty()) {
                        showNoContactsNearbyHint()
                    } else {
                        hideHintUI()
                    }

                    when {
                        it.isScanningError -> {
                            showSnackBar(R.string.error_scanning)
                        }
                    }
                }
            }
        }

    private fun launchBroadcastingFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                lakeViewModel.uiBroadcastingState.collectLatest {
                    Log.d("LakeFragment", "UI update: $it")

                    binding.switchBroadcasting.isChecked = it.isBroadcastingSwitchEnabled

                    when {
                        it.profileIsNotSet -> {
                            showProfileSetAlert()
                        }
                        it.isBroadcastingError -> {
                            binding.switchBroadcasting.isChecked = false
                            showSnackBar(R.string.error_broadcasting)
                        }
                    }
                }
            }
        }
    }

    private fun showNoPermissionDialog(@StringRes messageId: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.alert_no_perm)
            .setMessage(messageId)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}