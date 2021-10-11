package com.learner.scopedstoragedemoapp

import android.content.ContentUris
import android.os.Bundle
import android.provider.MediaStore
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.learner.scopedstoragedemoapp.databinding.ActivityBrowseAlbumBinding
import kotlin.concurrent.thread

class BrowseAlbum : AppCompatActivity() {
    private val imageList = ArrayList<Image>()

    private val checkedImages = HashMap<String, Image>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityBrowseAlbumBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val pickFiles = intent.getBooleanExtra("pick_files", false)
        title = if (pickFiles) "Pick Images" else "Browse Album"
        binding.recyclerView.viewTreeObserver.addOnPreDrawListener(object :
            ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                binding.recyclerView.viewTreeObserver.removeOnPreDrawListener(this)
                val columns = 3
                val imageSize = binding.recyclerView.width / columns
                val adapter =
                    AlbumAdapter(this@BrowseAlbum, imageList, checkedImages, imageSize, pickFiles)
                binding.recyclerView.layoutManager = GridLayoutManager(this@BrowseAlbum, columns)
                binding.recyclerView.adapter = adapter
                loadImages(adapter)
                return false
            }
        })

    }

    private fun loadImages(adapter: AlbumAdapter) {
        thread {
            val projection = arrayOf(
                MediaStore.MediaColumns.DATA,
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
            )
            val cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                null, null, "${MediaStore.MediaColumns.DATE_ADDED} desc"
            )
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val id =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    val uri =
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val imageSize =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
                    val imageWidth =
                        cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH))
                    val imageHeight =
                        cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT))
                    imageList.add(
                        Image(
                            uri = uri,
                            size = imageSize,
                            width = imageWidth,
                            height = imageHeight,
                            checked = false
                        )
                    )
                }
                cursor.close()
            }
            runOnUiThread {
                adapter.notifyDataSetChanged()
            }
        }
    }
}