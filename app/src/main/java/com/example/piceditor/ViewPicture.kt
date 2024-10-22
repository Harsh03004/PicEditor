package com.example.piceditor

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.jsibbold.zoomage.ZoomageView
import java.io.File

class ViewPicture : AppCompatActivity() {
    private var image: ZoomageView? = null
    private var image_file: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_picture)

        image = findViewById(R.id.image)

        image_file = intent.getStringExtra("image_file") ?: return // Exit if null

        val file = File(image_file)
        if (file.exists()) {
            // Use safe call to avoid NullPointerException
            image?.let { zoomageView ->
                Glide.with(this)
                    .load(file) // Ensure you are loading the file object
                    .into(zoomageView)
            } ?: run {
                Log.e("ViewPicture", "ZoomageView is null")
            }
        } else {
            Log.e("ViewPicture", "File does not exist: ${file.absolutePath}")
        }
    }


}
