package com.dhwaniris.comera

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.text.LineBreaker
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristic
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import com.dhwaniris.comera.widgets.CameraSwitchView
import com.dhwaniris.comera.widgets.FlashSwitchView
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.selector.autoFlash
import io.fotoapparat.selector.back
import io.fotoapparat.selector.front
import io.fotoapparat.selector.off
import io.fotoapparat.selector.on
import io.fotoapparat.view.CameraView
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class CameraActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_OBJECT_SHAPE = "OBJECT_SHAPE"
        const val RECTANGLE_SHAPE = 1
        const val CARD_SHAPE = 2
        const val EXTRA_ADD_TIMESTAMP = "EXTRA_ADD_TIMESTAMP"
        const val EXTRA_ADD_LOCATION = "EXTRA_ADD_LOCATION"
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

    private val tvLatitude by lazy { findViewById<TextView>(R.id.latitude_textview) }
    private val tvLongitude by lazy { findViewById<TextView>(R.id.longitude_textview) }
    private val tvDate by lazy { findViewById<TextView>(R.id.jaitpur_date) }
    private val tvAccuracy by lazy { findViewById<TextView>(R.id.accuracytext) }
    private val ll_location_preview by lazy { findViewById<LinearLayout>(R.id.ll_location_preview) }
    private val cl_options by lazy { findViewById<ConstraintLayout>(R.id.cl_options) }

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
    private var addLocation = false
    private var customText: String? = null
    private var location:LocationUtils? = null
    private var loc: Location? = null

    private val locationListener = object:LocationUtilsListener(){
        override fun onProviderDisabled(provider: String){
            capture.isEnabled = false
        }
        override fun onProviderEnabled(provider: String){
            capture.isEnabled = true
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?){

        }
        override fun onLocationChanged(location: Location){
            loc = location
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        actionBar?.hide()
        supportActionBar?.hide()

        initWaterMarkLocationDateTimeBar()



        imageUri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT)
        if (intent.extras != null) {
            val objectShape = intent.getIntExtra(EXTRA_OBJECT_SHAPE, 0)
            if (objectShape != 0) {
                findViewById<TextView>(R.id.obj_mgs).visibility = View.VISIBLE
                findViewById<View>(R.id.object_layout).visibility = View.VISIBLE
                cameraSwitchView.visibility = View.GONE

                when (objectShape) {
                    RECTANGLE_SHAPE -> findViewById<View>(R.id.object_layout).setBackgroundResource(R.drawable.ic_blur)
                    CARD_SHAPE -> findViewById<View>(R.id.object_layout).setBackgroundResource(R.drawable.ic_blur_2)
                }
            }
            addTimeDate = intent.getBooleanExtra(EXTRA_ADD_TIMESTAMP, false)
            addLocation = intent.getBooleanExtra(EXTRA_ADD_LOCATION, false)
            customText = intent.getStringExtra(EXTRA_CUSTOM_TEXT)

        }

        if(addLocation) {
            location = LocationUtils(this, locationListener)
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

//        val display = windowManager.defaultDisplay
//        val size = Point()
//        display.getSize(size)
//        val longestSide = max(size.x, size.y)

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
            if(addLocation && loc==null){
                Toast.makeText(this, "Cannot add location", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(!addLocation)
                ll_location_preview.visibility = View.GONE

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
                        height = (it.height),
                        width = (it.width)
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
                        val locationText = "${loc?.longitude}, ${loc?.latitude} ${loc?.accuracy}"
                        addCustomTextOnImage(bitmap, locationText!!)
                    }
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    }
                    outputStream?.flush()
                    outputStream?.close()
                    bitmap
                }
                .whenAvailable { photo: Bitmap? ->
                    photo?.let {
                        tvLatitude.text = "${loc?.latitude}"
                        tvLongitude.text = "${loc?.longitude}"
                        tvDate.text = getDateTime()
                        tvAccuracy.text = "${loc?.accuracy}"
                        ivPreview.setImageBitmap(it)
                        clCamera.visibility = View.GONE
                        flPreview.visibility = View.VISIBLE


                    }
                    isProcessing = false
                }

            val handler = Handler(Looper.getMainLooper())

            bAccept.setOnClickListener {
                if (isProcessing) return@setOnClickListener

                cl_options.visibility = View.GONE
                isProcessing = true
                handler.post {
                    val result = Intent()
                    savefile(imageUri!!, viewToBitmap(flPreview))
                    result.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                    setResult(RESULT_OK, result)
                    finish()
                    isProcessing = false
                }


            }

            bReject.setOnClickListener {
                if (isProcessing) return@setOnClickListener
                contentResolver.delete(imageUri!!, null, null)
                val result = Intent()
                setResult(Activity.RESULT_CANCELED, result)
                finish()
            }

            bRetry.setOnClickListener {
                if (isProcessing) return@setOnClickListener
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
            when (flashManager.switch()) {
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

        val cameraOrientation = intent.getStringExtra(CAMERA_ORIENTATION)
        if (cameraOrientation != null) {
            fotoapparat.switchTo(front(), cameraConfiguration)
            cameraSwitchView.displayFrontCamera()
            isBackCamera = false
        }
    }
    private fun savefile(sourceuri: Uri, bitmap: Bitmap) {
        try {
            val stream  = contentResolver.openOutputStream(sourceuri) ?: return

            // Write the bitmap to the file
            val fOut = stream
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut)
            fOut.flush()
            fOut.close()
        } catch (e: IOException) {
            println("Error saving image: ${e.message}")
        }
    }
    fun viewToBitmap(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }


    private fun initWaterMarkLocationDateTimeBar() {

    }

    fun getMidRes(): Iterable<Resolution>.() -> Resolution? {
        return { this.mid(Resolution::area) }
    }

    private inline fun <T, R : Comparable<R>> Iterable<T>.mid(selector: (T) -> R): T? {
        val iterator = iterator()
        val list = iterator.asSequence().toList()
        return list[list.size / 2 + list.size % 2]
    }

    private val configuration = CameraConfiguration(
        pictureResolution = { nearestBy(Resolution(1280, 720), Resolution::area) }
    )

    private inline fun <T> Iterable<T>.nearestBy(ofValue: T, selector: (T) -> Int): T? {
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
        paint.color = Color.WHITE
        paint.textSize = (20 * scale)
        paint.setShadowLayer(1f, 0f, 1f, Color.DKGRAY)
        val bounds = Rect()
        paint.getTextBounds(mText, 0, mText.length, bounds)
        val x: Float = (bitmap.width - bounds.width()) - (20 * scale)
        val y: Float = (bitmap.height - bounds.height()) - (20 * scale)
        canvas.drawText(mText, x, y, paint)
    }

    private fun addLocationOnImage(bitmap: Bitmap) {
        val resources: Resources = this.resources
        val scale: Float = resources.displayMetrics.density
        val canvas = Canvas(bitmap)
        val mText = "longi=${loc?.longitude}"
        val mTextLat = "lat=${loc?.latitude}"
        val mTextAcc = "acc=${loc?.accuracy}"
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.textSize = (20 * scale)
        paint.setShadowLayer(1f, 0f, 1f, Color.DKGRAY)
        val bounds = Rect()
        paint.getTextBounds(mText, 0, mText.length, bounds)
        val x: Float = (bitmap.width - bounds.width()) - (20 * scale)
        val y: Float = (bitmap.height - (4*bounds.height()) ) - (20 * scale)
        val y2: Float = (bitmap.height - (3*bounds.height()) ) - (20 * scale)
        val y3: Float = (bitmap.height - (2*bounds.height()) ) - (20 * scale)
        canvas.drawText(mText, x, y, paint)
        canvas.drawText(mTextLat, x, y2, paint)
        canvas.drawText(mTextAcc, x, y3, paint)
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


    fun Canvas.drawMultilineText(
        text: CharSequence,
        textPaint: TextPaint,
        width: Int,
        x: Float,
        y: Float,
        start: Int = 0,
        end: Int = text.length,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        breakStrategy: Int = LineBreaker.BREAK_STRATEGY_BALANCED,
        textDir: TextDirectionHeuristic = TextDirectionHeuristics.LTR,
        spacingMult: Float = 1f,
        spacingAdd: Float = 0f,
        hyphenationFrequency: Int = Layout.HYPHENATION_FREQUENCY_NONE,
        justificationMode: Int = Layout.JUSTIFICATION_MODE_NONE) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val staticLayout =  StaticLayout.Builder
                .obtain(text, start, end, textPaint, width)
                .setAlignment(alignment)
                .setTextDirection(textDir)
                .setLineSpacing(spacingAdd, spacingMult)
                .setBreakStrategy(breakStrategy)
                .build()
            staticLayout.draw(this)
        }

    }

    private fun addCustomTextOnImage(bitmap: Bitmap, mText: String) {
        val resources: Resources = this.resources
        val scale: Float = resources.displayMetrics.density
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.textSize = (20 * scale)
        paint.setShadowLayer(1f, 0f, 1f, Color.DKGRAY)
        val bounds = Rect()
        paint.getTextBounds(mText, 0, mText.length, bounds)
        val x: Float = (20 * scale)
        val y: Float = (bitmap.height - bounds.height()) - (45 * scale)
        canvas.drawText(mText, x, y, paint)
    }


    override fun onStart() {
        super.onStart()
        if (permissionsDelegate.hasCameraPermission()) {
            fotoapparat.start()
        }
        location?.startLocationUpdates()

    }

    override fun onResume() {
        super.onResume()

    }

    override fun onPause() {
        super.onPause()

    }

    override fun onStop() {
        super.onStop()
        if (permissionsDelegate.hasCameraPermission()) {
            fotoapparat.stop()
        }
        location?.stopLocationUpdates()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        location?.onActivityResult(requestCode,resultCode,data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        location?.onRequestPermissionsResult(requestCode, permissions, grantResults)

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