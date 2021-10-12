package com.learner.scopedstoragedemoapp

import android.app.Activity
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.learner.scopedstoragedemoapp.databinding.ActivityMainBinding
import java.io.*

const val OPEN_FILE_REQUEST_CODE = 2001
const val PICK_IMAGE_TO_COMPRESS_REQUEST_CODE = 2002
const val CHOOSE_FILE = 2003
const val READ_EXTERNAL_STORAGE_PERMISSION = 2004
const val CREATE_FILE_REQUEST_CODE = 2005

var downloadUrl = "https://picsum.photos/id/237/200/300"

class MainActivity : AppCompatActivity() {
    lateinit var mContentViewBinding: ActivityMainBinding
    var fileDownloadReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContentViewBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        clickListeners()
    }

    private fun clickListeners() {

        // To pick a pdf from storage
        mContentViewBinding.openFilePdfBt.setOnClickListener {
            pickPdfFile()
        }

        // To pick an image from storage
        mContentViewBinding.openFileImageBt.setOnClickListener {
            pickImageFile()
        }

        // To pick a doc from storage
        mContentViewBinding.openFileDocBt.setOnClickListener {
            pickDocFile()
        }

        // To download an image
        mContentViewBinding.downloadImageDownloadBt.setOnClickListener {
            downloadImage()
        }

        // To save an image to external storage
        mContentViewBinding.saveImageExternalStorage.setOnClickListener {
            val bitmap =
                (ContextCompat.getDrawable(this, R.drawable.test_image) as BitmapDrawable).bitmap
            saveImageToStorage(bitmap = bitmap, filename = "MyTempImages.jpg")
        }

        mContentViewBinding.btFetchGalleryImages.setOnClickListener {
            val intent = Intent(this, BrowseAlbum::class.java)
            startActivity(intent)
        }

        mContentViewBinding.btFetchCompressImage.setOnClickListener {
            pickImageFileForCompress()
        }
    }

    private fun saveImageToStorage(
        bitmap: Bitmap,
        isToShowToast: Boolean = true,
        filename: String = "screenshot.jpg",
        mimeType: String = "image/jpeg",
        directory: String = Environment.DIRECTORY_PICTURES,
        mediaContentUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    ) {

        val imageOutStream: OutputStream
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, directory)
            }

            contentResolver.run {
                val uri = contentResolver.insert(mediaContentUri, values) ?: return
                imageOutStream = openOutputStream(uri) ?: return
            }
        } else {
            val imagePath = Environment.getExternalStoragePublicDirectory(directory).absolutePath
            val image = File(imagePath, filename)
            imageOutStream = FileOutputStream(image)
        }

        imageOutStream.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        if (isToShowToast)
            Toast.makeText(this, "Image saved successfully", Toast.LENGTH_LONG).show()
    }

    private fun createFile() {

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TITLE, "Test.txt")
        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE)
    }

    private fun writeFileContent(uri: Uri?) {
        try {
            val file = uri?.let { this.contentResolver.openFileDescriptor(it, "w") }

            file?.let {
                val fileOutputStream = FileOutputStream(
                    it.fileDescriptor
                )
                val textContent = "This is the dummy text."

                fileOutputStream.write(textContent.toByteArray())

                fileOutputStream.close()
                it.close()
            }

        } catch (e: FileNotFoundException) {
            //print logs
        } catch (e: IOException) {
            //print logs
        }

    }

    private fun downloadImage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (permissionToReadWrite) {
                downloadImageToDownloadFolder()
            } else {
                permissionForReadWrite()
            }

        } else {
            downloadImageToDownloadFolder()
        }
    }

    private fun downloadImageToAppFolder() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (permissionToReadWrite) {
                downloadToAppFolder()
            } else {
                permissionForReadWrite()
            }

        } else {
            downloadToAppFolder()
        }
    }

    //Downloading file to Internal Folder
    private fun downloadToAppFolder() {
        try {
            val file = File(getExternalFilesDir(null), "testImage.png")

            if (!file.exists())
                file.createNewFile()

            val fileOutputStream = FileOutputStream(file)

            val bitmap =
                (ContextCompat.getDrawable(this, R.drawable.test_image) as BitmapDrawable).bitmap
            bitmap?.compress(Bitmap.CompressFormat.PNG, 80, fileOutputStream)

            Toast.makeText(
                this,
                getString(R.string.download_successful) + file.absolutePath,
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var permissionToReadWrite: Boolean = false
        get() {
            val permissionGrantedResult: Int = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
            return permissionGrantedResult == PackageManager.PERMISSION_GRANTED
        }

    //Request Permission For Read Storage
    private fun permissionForReadWrite() =
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
            READ_EXTERNAL_STORAGE_PERMISSION
        )


    private fun downloadImageToDownloadFolder() {
        val mgr = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val downloadUri = Uri.parse(downloadUrl)
        val request = DownloadManager.Request(downloadUri)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setAllowedOverRoaming(false)
            .setTitle("Image Sample")
            .setDescription("Testing")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "testingImage.jpg")

        Toast.makeText(
            applicationContext,
            "Downloaded successfully to ${downloadUri?.path}",
            Toast.LENGTH_LONG
        ).show()

        val downloadReferenceId = mgr.enqueue(request)
        startDownloadBroadcastReceiver(downloadReferenceId, mgr)
    }

    private fun startDownloadBroadcastReceiver(
        downloadRefId: Long,
        downloadManager: DownloadManager
    ) {

        fileDownloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (referenceId == downloadRefId) {
                    val myDownloadQuery = DownloadManager.Query()

                    //set the query filter to our previously Enqueued download
                    myDownloadQuery.setFilterById(referenceId)
                    val cursor = downloadManager.query(myDownloadQuery)
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            when (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    Toast.makeText(
                                        applicationContext,
                                        "Download Successful",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                DownloadManager.STATUS_FAILED -> {
                                    Toast.makeText(
                                        applicationContext,
                                        "Download Failed",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                    unregisterReceiver(fileDownloadReceiver)
                }
            }
        }

        val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        registerReceiver(fileDownloadReceiver, intentFilter)
    }

    private fun pickPdfFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            //If we want to open PDF file
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
            //Adding Read URI permission
            flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivityForResult(intent, OPEN_FILE_REQUEST_CODE)
    }

    private fun pickDocFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            //If we want to open PDF/Doc file
            type = "application/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            //Adding Read URI permission
            flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivityForResult(intent, OPEN_FILE_REQUEST_CODE)
    }

    private fun pickImageFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, OPEN_FILE_REQUEST_CODE)
    }

    private fun pickImageFileForCompress() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, PICK_IMAGE_TO_COMPRESS_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == OPEN_FILE_REQUEST_CODE) {
                data?.data?.also { documentUri ->
                    //Permission needed if you want to retain access even after reboot
                    contentResolver.takePersistableUriPermission(
                        documentUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    Toast.makeText(this, documentUri.path.toString(), Toast.LENGTH_LONG).show()
                }
            } else if (requestCode == PICK_IMAGE_TO_COMPRESS_REQUEST_CODE) {
                data?.data?.also { documentUri ->
                    //Permission needed if you want to retain access even after reboot
                    contentResolver.takePersistableUriPermission(
                        documentUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    val inputStream = contentResolver.openInputStream(documentUri)

                    val newBitmap = BitmapFactory.decodeStream(inputStream)

                    var bitmap = if(Build.VERSION.SDK_INT < 28) {
                        MediaStore.Images.Media.getBitmap(contentResolver, documentUri)
                    } else {
                        val source = ImageDecoder.createSource(contentResolver, documentUri)
                        ImageDecoder.decodeBitmap(source)
                    }

                    bitmap?.let {
                        bitmap = Bitmap.createScaledBitmap(it, 100, 100, true)

                        saveImageToStorage(it, false)
                        Toast.makeText(this, "Compressed image is stored to external storage", Toast.LENGTH_LONG).show()
                    }
                }
            } else if (requestCode == CHOOSE_FILE)
            else if (requestCode == CREATE_FILE_REQUEST_CODE) {
                if (data != null) {
                    writeFileContent(data.data)
                }

            }
        }
    }
}