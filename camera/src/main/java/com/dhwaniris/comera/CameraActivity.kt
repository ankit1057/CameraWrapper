package com.dhwaniris.comera

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import com.dhwaniris.comera.widgets.CameraSwitchView
import com.dhwaniris.comera.widgets.FlashSwitchView
import io.fotoapparat.Fotoapparat
import io.fotoapparat.capability.Capabilities
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.Resolution
<<<<<<< Updated upstream
import io.fotoapparat.selector.autoFlash
import io.fotoapparat.selector.autoFocus
import io.fotoapparat.selector.back
import io.fotoapparat.selector.firstAvailable
import io.fotoapparat.selector.fixed
import io.fotoapparat.selector.front
import io.fotoapparat.selector.infinity
import io.fotoapparat.selector.lowestFps
import io.fotoapparat.selector.off
import io.fotoapparat.selector.on
import io.fotoapparat.view.CameraView
=======
import io.fotoapparat.parameter.Zoom
import io.fotoapparat.result.PhotoResult
import io.fotoapparat.result.transformer.scaled
import io.fotoapparat.selector.*
import io.fotoapparat.view.CameraView
import io.fotoapparat.view.FocusView
import kotlinx.android.synthetic.main.activity_camera.*
import java.text.SimpleDateFormat
import java.util.*
>>>>>>> Stashed changes
import kotlin.math.max

const val KEY_FLASH_STATE = "key_flash_state"
const val CAMERA_FLASH_AUTO = 0
const val CAMERA_FLASH_OFF = 1
const val CAMERA_FLASH_ON = 2

class CameraActivity : AppCompatActivity() {

  init {
    AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
  }

  private val cameraView by lazy { findViewById<CameraView>(R.id.cameraView) }
  private val capture by lazy { findViewById<View>(R.id.capture) }
  private val cameraSwitchView by lazy { findViewById<CameraSwitchView>(R.id.cameraSwitchView) }
  private val flashSwitchView by lazy { findViewById<FlashSwitchView>(R.id.flashSwitchView) }

  private val ivPreview by lazy { findViewById<ImageView>(R.id.iv_preview) }
  private val bRetry by lazy { findViewById<ImageView>(R.id.b_retry) }
  private val bAccept by lazy { findViewById<ImageView>(R.id.b_accept) }
  private val bReject by lazy { findViewById<ImageView>(R.id.b_reject) }

  private val flPreview by lazy { findViewById<FrameLayout>(R.id.fl_preview) }
  private val clCamera by lazy { findViewById<ConstraintLayout>(R.id.cl_camera) }

  private val sharedPrefs by lazy { getSharedPreferences("camera_prefs", Context.MODE_PRIVATE) }

  private lateinit var fotoapparat: Fotoapparat
  private val permissionsDelegate = PermissionsDelegate(this)
  private lateinit var imageUri: Uri
  private var isBackCamera = true
  private var flashState = CAMERA_FLASH_AUTO

  private var isProcessing = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN)

<<<<<<< Updated upstream
    setContentView(R.layout.activity_camera)
=======
        var backCameraId = -1
        for (i in 0 until Camera.getNumberOfCameras()) {
            val cameraInfo = CameraInfo()
            Camera.getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                backCameraId = i
                break
            }
        }

        if (backCameraId == -1) {
            cameraSwitchView.visibility = View.GONE
        }

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val longestSide = max(size.x, size.y)

        val cameraConfiguration = configuration

        fotoapparat = Fotoapparat(
            context = this,
            view = cameraView,         // view which will draw the camera preview
            logger = loggers(logcat()),
            cameraErrorCallback = {
                it.printStackTrace()
                Log.e("Camera Error Callback", "Camera Crashed", it)
            },
            cameraConfiguration = cameraConfiguration
        )

        capture.setOnClickListener {

            Log.d("Picture", isProcessing.toString())
            if (isProcessing) return@setOnClickListener
            isProcessing = true

            // var scale = 0f
            val photoResult = fotoapparat
                .autoFocus()
                .takePicture()

            photoResult
                .toBitmap {
                     scaled(0.50f).invoke(it);
                }
                .transform {
                    val outputStream = contentResolver.openOutputStream(imageUri!!)?.buffered()
                    val bitmap = it.bitmap.rotate(-it.rotationDegrees.toFloat())
                    if (addTimeDate) {
                        addTimeOnImage(bitmap)
                    }
                    if (!customText.isNullOrEmpty()) {
                        addCustomTextOnImage(bitmap, customText!!)
                    }
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream?.flush()
                    outputStream?.close()
                    bitmap
                }
                .whenAvailable { photo: Bitmap? ->
                    photo?.let {
                        ivPreview.setImageBitmap(it)
                        clCamera.visibility = View.GONE
                        flPreview.visibility = View.VISIBLE
                    }
                }
>>>>>>> Stashed changes

    actionBar?.hide()
    supportActionBar?.hide()

    imageUri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT)

    if (permissionsDelegate.hasCameraPermission()) {
      cameraView.visibility = View.VISIBLE
    } else {
      permissionsDelegate.requestCameraPermission()
    }

    var backCameraId = -1
    for (i in 0 until Camera.getNumberOfCameras()) {
      val cameraInfo = CameraInfo()
      Camera.getCameraInfo(i, cameraInfo)
      if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
        backCameraId = i
        break
      }
    }
    var hasflash = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    if (!hasflash) {
      flashSwitchView.visibility = View.GONE
    }

    flashState = sharedPrefs.getInt(KEY_FLASH_STATE, CAMERA_FLASH_AUTO)

    if (backCameraId == -1) {
      cameraSwitchView.visibility = View.GONE
    }

    val display = windowManager.defaultDisplay
    val size = Point()
    display.getSize(size)
    val longestSide = max(size.x, size.y)

    var cameraConfiguration =
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
          CameraConfiguration.default().copy(
              pictureResolution = {
                val sorted = sortedByDescending { it.area }
                var selected = sorted.first()
                sorted.forEach {
                  val longestWidthForResolution = max(it.height, it.width)
                  if (longestWidthForResolution >= longestSide) {
                    if (it.width <= selected.width && it.height <= selected.height)
                      selected = it
                  }
                }
                return@copy selected
              },
              previewFpsRange = lowestFps(),
              focusMode = firstAvailable(
                  autoFocus(),
                  fixed(),
                  infinity()
              )
          )
        } else CameraConfiguration.default()

    cameraConfiguration = setFlashState(flashState, cameraConfiguration)
    fotoapparat = Fotoapparat(
        context = this,
        view = cameraView,         // view which will draw the camera preview
        logger = loggers(logcat()),
        cameraErrorCallback = {
          it.printStackTrace()
          Log.e("Camera Error Callback", "Camera Crashed", it)
        },
        cameraConfiguration = cameraConfiguration
    )

    capture.setOnClickListener {

      Log.d("Picture", isProcessing.toString())
      if (isProcessing) return@setOnClickListener
      isProcessing = true

      val photoResult = fotoapparat
          .autoFocus()
          .takePicture()

//      photoResult.toPendingResult().transform { input ->
//        outputStream.use {
//          it.write(input.encodedImage)
//          it.flush()
//        }
//      }

//      photoResult
//          .toPendingResult()
//          .whenAvailable {
//            it?.encodedImage?.let {
//              val bitmap = decodeSampledBitmapFromByteArray(it, ivPreview.width, ivPreview.height)
//              ivPreview.setImageBitmap(bitmap)
//              ivPreview.setRotation(.rotationDegrees)
//              clCamera.visibility = View.GONE
//              flPreview.visibility = View.VISIBLE
//            }
//          }

      photoResult
          .toBitmap {
            val scale = max(ivPreview.height, ivPreview.width).toFloat() / max(it.height,
                it.width).toFloat()
            Resolution(height = (it.height * scale).toInt(), width = (it.width * scale).toInt())
          }
          .transform {
            val outputStream = contentResolver.openOutputStream(imageUri).buffered()
            val bitmap = it.bitmap.rotate(-it.rotationDegrees.toFloat())
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            bitmap
          }
          .whenAvailable { photo: Bitmap? ->
            photo?.let {
              ivPreview.setImageBitmap(it)
              clCamera.visibility = View.GONE
              flPreview.visibility = View.VISIBLE
            }
          }

<<<<<<< Updated upstream
    }
    bAccept.setOnClickListener {
      val result = Intent()
      result.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
      setResult(RESULT_OK, result)
      finish()
=======
    val configuration = CameraConfiguration(
        pictureResolution = { nearestBy(Resolution(1280, 720), Resolution::area) }
    )

    inline fun <T> Iterable<T>.nearestBy(ofValue: T, selector: (T) -> Int): T? {
        val iterator = iterator()
        if (!iterator.hasNext()) return null
        val valueToCompare = selector(ofValue)
        var nearestElem = iterator.next()
        var nearestRange = Math.abs(selector(nearestElem) - valueToCompare)
        var currentRange: Int
        while (iterator.hasNext()) {
            val e = iterator.next()
            val v = selector(e)
            currentRange = Math.abs(v - valueToCompare)
            if (currentRange < nearestRange) {
                nearestElem = e
                nearestRange = currentRange
            }
        }
        return nearestElem
    }

    private fun addTimeOnImage(bitmap: Bitmap) {
        val resources: Resources = this.resources
        val scale: Float = resources.displayMetrics.density
        val canvas = Canvas(bitmap)
        val mText = getDateTime()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE;
        paint.textSize = (20 * scale);
        paint.setShadowLayer(1f, 0f, 1f, Color.DKGRAY)
        val bounds = Rect()
        paint.getTextBounds(mText, 0, mText.length, bounds)
        val x: Float = (bitmap.width - bounds.width()) - (20 * scale)
        val y: Float = (bitmap.height - bounds.height()) - (20 * scale)
        canvas.drawText(mText, x, y, paint);
>>>>>>> Stashed changes
    }

    bReject.setOnClickListener {
      contentResolver.delete(imageUri, null, null)
      val result = Intent()
      setResult(Activity.RESULT_CANCELED, result)
      finish()
    }

    bRetry.setOnClickListener {
      isProcessing = false
      clCamera.visibility = View.VISIBLE
      flPreview.visibility = View.GONE
      if (ivPreview.drawable != null && ivPreview.drawable is BitmapDrawable) {
        (ivPreview.drawable as BitmapDrawable).bitmap?.recycle()
      }
    }

    cameraSwitchView.setOnClickListener {

      if (isBackCamera) {
        fotoapparat.switchTo(front(), cameraConfiguration)
        cameraSwitchView.displayFrontCamera()
      } else {
        fotoapparat.switchTo(back(), cameraConfiguration)
        cameraSwitchView.displayBackCamera()
      }

      isBackCamera = !isBackCamera
    }

    flashSwitchView.setOnClickListener {
      switchCameraFlash()
      sharedPrefs.edit().putInt(KEY_FLASH_STATE, flashState).apply()
      fotoapparat.updateConfiguration(setFlashState(flashState, cameraConfiguration))
    }
  }

  private fun switchCameraFlash(): Int {
    flashState = (flashState + 1) % 3
    return flashState
  }

  private fun setFlashState(current: Int, cameraConfiguration: CameraConfiguration): CameraConfiguration {
    return when (current) {
      CAMERA_FLASH_AUTO -> {

        flashSwitchView.displayFlashAuto()
        cameraConfiguration.copy(flashMode = autoFlash())
      }
      CAMERA_FLASH_ON -> {
        flashSwitchView.displayFlashOn()
        cameraConfiguration.copy(flashMode = on())
      }
      else -> {
        flashSwitchView.displayFlashOff()
        cameraConfiguration.copy(flashMode = off())
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
      isProcessing = false
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

fun Bitmap.rotate(angle: Float): Bitmap {
  val matrix = Matrix()
  matrix.postRotate(angle)
  val result = Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
  this.recycle()
  return result
}