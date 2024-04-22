package com.hw20.presentation.adapter

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hw20.data.Photo
import com.hw20.databinding.ItemPhotoBinding

class PhotoViewHolder(private val binding: ItemPhotoBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(photo: Photo) {
        Glide.with(binding.photo.context)
            .load(photo.imgUri)
            .centerCrop()
            .into(binding.photo)

        val fileName = photo.imgUri
            .substringAfterLast("/")
            .substringBeforeLast('.')
        binding.textPhoto.text = fileName
    }
}
