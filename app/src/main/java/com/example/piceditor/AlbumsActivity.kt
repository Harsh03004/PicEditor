package com.example.piceditor

import android.content.ContentResolver
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AlbumsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AlbumsAdapter
    private var albums: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_albums)

        recyclerView = findViewById(R.id.album_recycler)
        adapter = AlbumsAdapter(this, albums) { albumName ->
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("albumName", albumName)
            startActivity(intent)
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadAlbums()
    }

    private fun loadAlbums() {
        loadDeviceAlbums()
        loadAppCreatedAlbums()
        adapter.notifyDataSetChanged()
    }

    private fun loadDeviceAlbums() {
        val contentResolver: ContentResolver = contentResolver
        val projection = arrayOf(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " ASC"
        )

        if (cursor != null) {
            val albumSet = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                val albumName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))
                albumSet.add(albumName)
            }
            cursor.close()

            albums.addAll(albumSet)
        } else {
            Toast.makeText(this, "No albums found on device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAppCreatedAlbums() {
        val sharedPreferences = getSharedPreferences("image_editor_prefs", MODE_PRIVATE)
        val existingAlbums = sharedPreferences.getStringSet("albums", mutableSetOf())

        if (existingAlbums != null) {
            albums.addAll(existingAlbums)
        }
    }
}
