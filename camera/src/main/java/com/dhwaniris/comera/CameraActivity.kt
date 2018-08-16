package com.dhwaniris.comera

import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import io.fotoapparat.Fotoapparat
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.result.BitmapPhoto
import io.fotoapparat.result.transformer.scaled
import io.fotoapparat.view.CameraView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date


class CameraActivity : AppCompatActivity() {

  private val cameraView by lazy { findViewById<CameraView>(R.id.cameraView) }
  private val capture by lazy { findViewById<View>(R.id.capture) }
  private lateinit var fotoapparat: Fotoapparat
  private val permissionsDelegate = PermissionsDelegate(this)
  private lateinit var imageUri: Uri

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_camera)

    actionBar?.hide()

    imageUri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT)

    if (permissionsDelegate.hasCameraPermission()) {
      cameraView.visibility = View.VISIBLE
    } else {
      permissionsDelegate.requestCameraPermission()
    }

    fotoapparat = Fotoapparat(
        context = this,
        view = cameraView         // view which will draw the camera preview
    )

    capture.setOnClickListener {
      val photoResult = fotoapparat
          .autoFocus()
          .takePicture()

      val outputStream = contentResolver.openOutputStream(imageUri).buffered()
      photoResult.toPendingResult().transform { input ->
        outputStream.use {
          it.write(input.encodedImage)
          it.flush()
        }
      }

      photoResult
//          .toBitmap(scaled(0.20f))
          .toBitmap {
            val scale = 128f / (Math.max(it.height, it.width)).toFloat()
            Resolution(height = (it.height * scale).toInt(), width = (it.width * scale).toInt())
          }
          .whenAvailable { photo: BitmapPhoto? ->
            photo?.let {
              val result = Intent()
              val bitmap = it.bitmap
              if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                Log.d("Image_SIZE", bitmap.allocationByteCount.toString())
              }
              result.putExtra("data", bitmap)
              setResult(RESULT_OK, result)
              finish()
            }
          }
    }
  }

  override fun onStart() {
    super.onStart()
    if (permissionsDelegate.hasCameraPermission()) {
      fotoapparat.start()
    }
  }

  override fun onStop() {
    super.onStop()
    if (permissionsDelegate.hasCameraPermission()) {
      fotoapparat.stop()
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
      grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (permissionsDelegate.resultGranted(requestCode, permissions, grantResults)) {
      fotoapparat.start()
      cameraView.visibility = View.VISIBLE
    }
  }
}