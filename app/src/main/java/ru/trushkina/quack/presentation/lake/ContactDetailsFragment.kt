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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import ru.trushkina.quack.R
import ru.trushkina.quack.databinding.FragmentContactDetailsBinding
import ru.trushkina.quack.domain.models.Contact
import ru.trushkina.quack.presentation.utis.hideBottomNav

class ContactDetailsFragment : Fragment() {

    private val args: ContactDetailsFragmentArgs by navArgs()

    private var _binding: FragmentContactDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hideBottomNav()

        val contact = args.contact
        with(binding) {
            toolbar.apply {
                setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
                setNavigationOnClickListener { findNavController().navigateUp() }
                title = "${contact?.firstName} ${contact?.lastName ?: ""}"
            }
            btnShare.setOnClickListener { shareContact(contact) }
            Glide
                .with(root.context)
                .load(contact?.avatarUrl)
                .placeholder(R.drawable.ic_baseline_person_avatar)
                .into(ivAvatar)
            with(contactOrganization) {
                showItem(tvOrganization, sectionOrganization, contact?.organization)
                showItem(tvJobTitle, sectionJobTitle, contact?.jobTitle)
            }
            with(contactContacts) {
                showItem(tvPhoneNumber, sectionPhone, contact?.phoneNumber)
                showItem(tvTelegram, sectionTelegram, contact?.telegram)
            }
            with(contactWeb) {
                showItem(tvVK, sectionVK, contact?.vkProfileUrl)
                showItem(tvWebPage, sectionWeb, contact?.webPage)
            }
        }
    }

    private fun showItem(textView: TextView, section: View, value: String?) {
        value?.let {
            textView.text = it
            section.visibility = View.VISIBLE
        }
    }

    private fun shareContact(contact: Contact?) {
        contact?.let {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, contactToString(contact))
            }

            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        }
    }

    private fun contactToString(contact: Contact?):String =
        """
                Name: ${contact?.firstName} ${contact?.lastName ?: ""}
                Organization: ${contact?.organization ?: ""},
                Job Title: ${contact?.jobTitle ?: ""},
                Phone Number: ${contact?.phoneNumber ?: ""},
                Telegram: ${contact?.telegram ?: ""},
                VK: ${contact?.vkProfileUrl ?: ""},
                Web Page: ${contact?.webPage ?: ""} 
        """.trimIndent()
}