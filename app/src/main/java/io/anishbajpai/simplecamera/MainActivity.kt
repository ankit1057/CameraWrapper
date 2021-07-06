package io.anishbajpai.simplecamera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.dhwaniris.comera.CameraActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


private const val REQUEST_IMAGE_CAPTURE = 92
private const val REQUEST_STORAGE_PERMISSION = 51

class MainActivity : AppCompatActivity() {

    private val imageView by lazy { findViewById<ImageView>(R.id.iv_thumb) }
    var photoURI: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.b_start_camera).setOnClickListener {
            val imageFile = createImageFile()
            if (imageFile != null) {
                val camera = Intent(this, CameraActivity::class.java)
                photoURI = FileProvider.getUriForFile(
                    this,
                    "io.anishbajpai.simplecamera.fileprovider",
                    imageFile
                )
                camera.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(camera, REQUEST_IMAGE_CAPTURE)
            }
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_STORAGE_PERMISSION
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK && data != null) {
            val findViewById = findViewById<ImageView>(R.id.iv_thumb)
            Glide.with(this).load(photoURI).into(findViewById)
        }
    }


    private fun createImageFile(): File? {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
        )
        return image
    }
}
