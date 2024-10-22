package com.example.piceditor

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var recycler: RecyclerView
    private var images: MutableList<String> = mutableListOf()
    private lateinit var adapter: GalleryAdapter
    private lateinit var manager: GridLayoutManager
    private var currentOffset = 0 // Current offset for pagination
    private val chunkSize = 100 // Number of images to load at a time
    private var isLoading = false // To prevent multiple loads
    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize RecyclerView, Adapter, and LayoutManager
        recycler = findViewById(R.id.gallery_recycler)
        adapter = GalleryAdapter(this, images)
        manager = GridLayoutManager(this, 3)

        // Initialize Spinner
        val sortSpinner: Spinner = findViewById(R.id.sort_spinner)
        val sortOptions = arrayOf("Sort", "Size", "Date", "Location")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = spinnerAdapter

        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (sortOptions[position]) {
                    "Size" -> sortImagesAsync { sortBySize() }
                    "Date" -> sortImagesAsync { sortByDate() }
                    "Location" -> sortImagesAsync { sortByLocation() }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Add a button or an option to open AlbumsActivity
        val albumsButton: Button = findViewById(R.id.albums_button)
        albumsButton.setOnClickListener {
            loadExistingAlbums()
        }

        recycler.adapter = adapter
        recycler.layoutManager = manager

        // Add scroll listener for pagination
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!isLoading && manager.findLastVisibleItemPosition() >= images.size - 5) {
                    loadMoreImages()
                }
            }
        })

        // Set up swipe gestures for deleting or adding photos to an album
        setUpSwipeGestures()

        // Check permissions and load images
        checkPermissions()
    }

    private fun sortImagesAsync(sortFunction: () -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            sortFunction()
            withContext(Dispatchers.Main) {
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun setUpSwipeGestures() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        // Handle left swipe (delete)
                        deletePhoto(position)
                    }
                    ItemTouchHelper.RIGHT -> {
                        // Handle right swipe (add to album)
                        showAlbumOptions(position)
                    }
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recycler)
    }

    private fun showAlbumOptions(position: Int) {
        val options = arrayOf("Create New Album", "Add to Existing Album")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Album Option")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> createNewAlbum(images[position]) // Create new album
                1 -> loadExistingAlbums() // Select existing album
            }
        }
        builder.show()
    }

    private fun createNewAlbum(imagePath: String) {
        val input = EditText(this)
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Create New Album")
        builder.setMessage("Enter album name:")
        builder.setView(input)
        builder.setPositiveButton("Create") { dialog, which ->
            val albumName = input.text.toString()
            if (albumName.isNotBlank()) {
                // Logic to create the album and move the image
                Toast.makeText(this, "Album '$albumName' created and image added.", Toast.LENGTH_SHORT).show()
                // You can add logic here to actually create the album
            } else {
                Toast.makeText(this, "Album name cannot be empty.", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun loadExistingAlbums() {
        CoroutineScope(Dispatchers.IO).launch {
            val albumList = mutableListOf<String>()
            val uri: Uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Audio.Albums.ALBUM)
            val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)

            cursor?.use {
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
                while (it.moveToNext()) {
                    val albumName = it.getString(albumColumn)
                    if (albumName != null) {
                        albumList.add(albumName)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                // Pass the album list to AlbumsActivity
                val intent = Intent(this@MainActivity, AlbumsActivity::class.java)
                intent.putStringArrayListExtra("ALBUM_LIST", ArrayList(albumList))
                startActivity(intent)
            }
        }
    }

    private fun checkPermissions() {
        val readPermission = ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.READ_EXTERNAL_STORAGE)
        val writePermission = ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED) {
            loadImages()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadImages()
        } else {
            Toast.makeText(this, "You have denied the permissions.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadImages() {
        val albumPath = intent.getStringExtra("albumPath")

        val selection = if (albumPath != null) "${MediaStore.Images.Media.DATA} like ?" else null
        val selectionArgs = if (albumPath != null) arrayOf("$albumPath%") else null

        val columns = arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID)
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            columns,
            selection,
            selectionArgs,
            null
        )

        if (cursor != null) {
            while (cursor.moveToNext()) {
                val columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                images.add(cursor.getString(columnIndex))
            }
            cursor.close()
        }

        adapter.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadMoreImages() {
        isLoading = true
        val SDCard = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        if (SDCard) {
            val columns = arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID)
            val cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                columns,
                null,
                null,
                null
            )

            cursor?.let {
                val count = it.count
                if (count > 0) {
                    while (it.moveToNext() && currentOffset < chunkSize) {
                        val columnIndex = it.getColumnIndex(MediaStore.Images.Media.DATA)
                        images.add(it.getString(columnIndex))
                        currentOffset++
                    }
                }
                it.close()
            }
        }
        isLoading = false
        adapter.notifyDataSetChanged()
    }

    private fun deletePhoto(position: Int) {
        // Logic to delete photo
        val photoToDelete = images[position]
        val file = File(photoToDelete)

        if (file.exists()) {
            if (file.delete()) {
                images.removeAt(position)
                adapter.notifyItemRemoved(position)
                Toast.makeText(this, "Photo deleted.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to delete photo.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Photo does not exist.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sortBySize() {
        images.sortBy { File(it).length() }
    }

    private fun sortByDate() {
        images.sortBy { getFileDate(File(it)) }
    }

    private fun sortByLocation() {
        // Assuming you have a way to get location data for the images
        // For demonstration, we will just sort by filename (you may want to modify this)
        images.sortBy { File(it).name }
    }

    private fun getFileDate(file: File): Long {
        return try {
            val exif = ExifInterface(file.absolutePath)
            exif.getAttribute(ExifInterface.TAG_DATETIME)?.let {
                // Convert to timestamp
                SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).parse(it)?.time ?: 0
            } ?: 0
        } catch (e: IOException) {
            Log.e("MainActivity", "Error reading EXIF data: ${e.message}")
            0
        }
    }
}
