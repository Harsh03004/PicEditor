package com.example.piceditor

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

class GalleryAdapter(private val context: Context, private val images_list: MutableList<String>) :
    RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.gallery_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val image_file = File(images_list[position])

        if (image_file.exists()) {
            Glide.with(context).load(image_file).into(holder.image)
        }

        holder.image.setOnClickListener {
            val intent = Intent(context, ViewPicture::class.java)
            intent.putExtra("image_file", images_list[position])
            context.startActivity(intent)
        }
    }

    private fun deleteImage(imagePath: String): Boolean {
        val file = File(imagePath)
        return if (file.exists()) {
            file.delete() // Attempt to delete the file
        } else {
            false // File doesn't exist
        }
    }

    override fun getItemCount(): Int {
        return images_list.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById<ImageView>(R.id.gallery_image)
    }
}
