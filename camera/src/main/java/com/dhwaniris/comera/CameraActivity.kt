package com.dhwaniris.comera

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.graphics.*
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
import android.widget.FrameLayout
import android.widget.ImageView
import com.dhwaniris.comera.widgets.CameraSwitchView
import com.dhwaniris.comera.widgets.FlashSwitchView
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.result.transformer.scaled
import io.fotoapparat.selector.*
import io.fotoapparat.view.CameraView
import kotlinx.android.synthetic.main.activity_camera.*
import java.text.SimpleDateFormat
import java.time.Clock
import java.util.*
import kotlin.math.max


class CameraActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_OBJECT_SHAPE = "OBJECT_SHAPE"
        const val RECTANGLE_SHAPE = 1
        const val CARD_SHAPE = 2

        const val EXTRA_ADD_TIMESTAMP = "EXTRA_ADD_TIMESTAMP"
        const val EXTRA_CUSTOM_TEXT = "EXTRA_CUSTOM_TEXT"

    }


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

    private lateinit var fotoapparat: Fotoapparat
    private val permissionsDelegate = PermissionsDelegate(this)
    private var imageUri: Uri? = null
    private var isBackCamera = true
    private val flashManager = FlashManager()

    private var isProcessing = false
    private var addTimeDate = false
    private var customText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        actionBar?.hide()
        supportActionBar?.hide()

        imageUri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT)
        if (intent.extras != null) {
            val objectShape = intent.getIntExtra(EXTRA_OBJECT_SHAPE, 0)
            if (objectShape != 0) {
                obj_mgs.visibility = View.VISIBLE
                object_layout.visibility = View.VISIBLE
                cameraSwitchView.visibility = View.GONE

                when (objectShape) {
                    RECTANGLE_SHAPE -> object_layout.setBackgroundResource(R.drawable.ic_blur)
                    CARD_SHAPE -> object_layout.setBackgroundResource(R.drawable.ic_blur_2)
                }
            }
            addTimeDate = intent.getBooleanExtra(EXTRA_ADD_TIMESTAMP, false)
            customText = intent.getStringExtra(EXTRA_CUSTOM_TEXT)

        }

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
//                    val scale = max(ivPreview.height, ivPreview.width).toFloat() / max(
//                        it.height,
//                        it.width
//                    ).toFloat()
                    Resolution(
                        height = (it.height ).toInt(),
                        width = (it.width ).toInt()
                    )
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

            bAccept.setOnClickListener {
                val result = Intent()
                result.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                setResult(RESULT_OK, result)
                finish()
            }

            bReject.setOnClickListener {
                contentResolver.delete(imageUri!!, null, null)
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
            val current = flashManager.switch()
            when (current) {
                is CameraFlashAuto -> {
                    fotoapparat.updateConfiguration(
                        cameraConfiguration.copy(flashMode = autoFlash())
                    )
                    flashSwitchView.displayFlashAuto()
                }
                is CameraFlashOn -> {
                    fotoapparat.updateConfiguration(cameraConfiguration.copy(flashMode = on()))
                    flashSwitchView.displayFlashOn()
                }
                is CameraFlashOff -> {
                    fotoapparat.updateConfiguration(cameraConfiguration.copy(flashMode = off()))
                    flashSwitchView.displayFlashOff()
                }
            }
        }
    }

    fun getMidRes(): Iterable<Resolution>.() -> Resolution? {
        return { this.mid(Resolution::area) }
    }

    public inline fun <T, R : Comparable<R>> Iterable<T>.mid(selector: (T) -> R): T? {
        val iterator = iterator()
        val list = iterator.asSequence().toList()
        return list.get(list.size/2 + list.size % 2)
    }

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
    }

    private fun getDateTime(): String {
        val timestamp = System.currentTimeMillis()
        val date = Date(timestamp)
        val df = SimpleDateFormat(
            "MMM dd, yyyy hh:mm a",
            Locale.ENGLISH
        )
        return df.format(date)
    }

    private fun addCustomTextOnImage(bitmap: Bitmap, mText: String) {
        val resources: Resources = this.resources
        val scale: Float = resources.displayMetrics.density
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE;
        paint.textSize = (20 * scale);
        paint.setShadowLayer(1f, 0f, 1f, Color.DKGRAY)
        val bounds = Rect()
        paint.getTextBounds(mText, 0, mText.length, bounds)
        val x: Float = (20 * scale)
        val y: Float = (bitmap.height - bounds.height()) - (45 * scale)
        canvas.drawText(mText, x, y, paint);
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

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissionsDelegate.resultGranted(requestCode, permissions, grantResults)) {
            fotoapparat.start()
            cameraView.visibility = View.VISIBLE
        }
    }
}

sealed class CameraFlashState

object CameraFlashAuto : CameraFlashState()
object CameraFlashOff : CameraFlashState()
object CameraFlashOn : CameraFlashState()

class FlashManager {
    private val modes = listOf(CameraFlashAuto, CameraFlashOn, CameraFlashOff)
    private var current = 0

    fun switch(): CameraFlashState {
        current = (++current) % modes.size
        return modes[current]
    }

    fun current(): CameraFlashState {
        return modes[current]
    }
}

fun Bitmap.rotate(angle: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(angle)
    val result = Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
    this.recycle()
    return result
}