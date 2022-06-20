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
package ru.trushkina.quack.presentation.lake.adapter

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import ru.trushkina.quack.R
import ru.trushkina.quack.databinding.ListItemScannedContactBinding
import ru.trushkina.quack.domain.models.Contact
import ru.trushkina.quack.presentation.lake.LakeFragmentDirections

internal class ScannedContactsAdapter(private val contacts: List<Contact>) :
    RecyclerView.Adapter<ScannedContactsAdapter.ScannedContactsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScannedContactsViewHolder =
        ScannedContactsViewHolder(
            ListItemScannedContactBinding.inflate(
                LayoutInflater.from(parent.context)
            )
        )

    override fun onBindViewHolder(holder: ScannedContactsViewHolder, position: Int) =
        holder.bindView(contacts[position])

    override fun getItemCount(): Int = contacts.size

    internal class ScannedContactsViewHolder(private val binding: ListItemScannedContactBinding) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bindView(scannedContact: Contact) {
            with(binding) {
                contactHeadline.text = "${scannedContact.firstName} ${scannedContact.lastName ?: ""}"
                contactSubhead.text = scannedContact.organization
                contactSupportingText.text = scannedContact.jobTitle
                Glide
                    .with(root.context)
                    .load(scannedContact.avatarUrl)
                    .placeholder(R.drawable.ic_baseline_person_avatar)
                    .into(contactAvatarImage)
                materialCardContainer.setOnClickListener {
                    val direction = LakeFragmentDirections.actionOpenContact(scannedContact)
                    Navigation.findNavController(it).navigate(direction)
                }
            }
        }
    }
}