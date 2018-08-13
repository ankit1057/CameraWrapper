package io.anishbajpai.simplecamera

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import com.dhwaniris.comera.CameraActivity

private const val REQUEST_IMAGE_CAPTURE = 92

class MainActivity : AppCompatActivity() {

  private val imageView by lazy { findViewById<ImageView>(R.id.iv_thumb) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    findViewById<Button>(R.id.b_start_camera).setOnClickListener {
      val camera = Intent(this, CameraActivity::class.java)
      startActivityForResult(camera, REQUEST_IMAGE_CAPTURE)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK && data != null) {
      val extras = data.extras
      val imageBitmap = extras?.get("data") as Bitmap
      imageView.setImageBitmap(imageBitmap)
    }
  }
}
