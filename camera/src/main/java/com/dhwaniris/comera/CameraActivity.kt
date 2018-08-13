package com.dhwaniris.comera

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import io.fotoapparat.Fotoapparat
import io.fotoapparat.result.transformer.scaled
import io.fotoapparat.view.CameraView

class CameraActivity : AppCompatActivity() {

  private val cameraView by lazy { findViewById<CameraView>(R.id.cameraView) }
  private val capture by lazy { findViewById<View>(R.id.capture) }
  private lateinit var fotoapparat: Fotoapparat
  private val permissionsDelegate = PermissionsDelegate(this)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_camera)

    actionBar?.hide()

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

//      val bitmapPhoto = photoResult.toBitmap().await()
//      val result = Intent()
//      result.putExtra("data", bitmapPhoto.bitmap)
//      setResult(RESULT_OK, result)
//      finish()

      photoResult
          .toBitmap(scaled(scaleFactor = 0.10f))
          .whenAvailable { photo ->
            photo?.let {
              val result = Intent()
              result.putExtra("data", it.bitmap)
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