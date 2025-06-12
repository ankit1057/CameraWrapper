package com.dhwaniris.comera

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
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
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Legacy CameraActivity - kept for backwards compatibility
 * Note: This uses deprecated Camera API and provides basic functionality
 * For full camera functionality, use Camera2Activity instead
 */
class CameraActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_OBJECT_SHAPE = "OBJECT_SHAPE"
        const val RECTANGLE_SHAPE = 1
        const val CARD_SHAPE = 2
        const val EXTRA_ADD_TIMESTAMP = "EXTRA_ADD_TIMESTAMP"
        const val EXTRA_ADD_LOCATION = "EXTRA_ADD_LOCATION"
        const val EXTRA_CUSTOM_TEXT = "EXTRA_CUSTOM_TEXT"
        const val CAMERA_ORIENTATION = "CAMERA_ORIENTATION"
    }

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    // UI Components
    private val cameraView by lazy { findViewById<android.view.SurfaceView>(R.id.cameraView) }
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

    // Camera and other components
    private var camera: Camera? = null
    private val permissionsDelegate = PermissionsDelegate(this)
    private var imageUri: Uri? = null
    private var isBackCamera = true
    private val flashManager = FlashManager()
    private var isProcessing = false
    private var addTimeDate = false
    private var addLocation = false
    private var customText: String? = null
    private var location: LocationUtils? = null
    private var loc: Location? = null

    private val locationListener = object : LocationUtilsListener() {
        override fun onProviderDisabled(provider: String) {
            capture.isEnabled = false
        }

        override fun onProviderEnabled(provider: String) {
            capture.isEnabled = true
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onLocationChanged(location: Location) {
            loc = location
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        actionBar?.hide()
        supportActionBar?.hide()

        initWaterMarkLocationDateTimeBar()
        setupIntentExtras()
        setupUIComponents()
        checkAndRequestPermissions()
    }

    private fun setupIntentExtras() {
        imageUri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT)
        intent.extras?.let { extras ->
            val objectShape = extras.getInt(EXTRA_OBJECT_SHAPE, 0)
            if (objectShape != 0) {
                findViewById<TextView>(R.id.obj_mgs).visibility = View.VISIBLE
                findViewById<View>(R.id.object_layout).visibility = View.VISIBLE
                cameraSwitchView.visibility = View.GONE

                when (objectShape) {
                    RECTANGLE_SHAPE -> findViewById<View>(R.id.object_layout)
                        .setBackgroundResource(R.drawable.ic_blur)
                    CARD_SHAPE -> findViewById<View>(R.id.object_layout)
                        .setBackgroundResource(R.drawable.ic_blur_2)
                }
            }
            addTimeDate = extras.getBoolean(EXTRA_ADD_TIMESTAMP, false)
            addLocation = extras.getBoolean(EXTRA_ADD_LOCATION, false)
            customText = extras.getString(EXTRA_CUSTOM_TEXT)
        }

        if (addLocation) {
            location = LocationUtils(this, locationListener)
        }

        // Check camera availability
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

        val cameraOrientation = intent.getStringExtra(CAMERA_ORIENTATION)
        if (cameraOrientation != null) {
            isBackCamera = false
            cameraSwitchView.displayFrontCamera()
        }
    }

    private fun setupUIComponents() {
        capture.setOnClickListener { takePicture() }
        cameraSwitchView.setOnClickListener { switchCamera() }
        flashSwitchView.setOnClickListener { toggleFlash() }
        bAccept.setOnClickListener { acceptPhoto() }
        bReject.setOnClickListener { rejectPhoto() }
        bRetry.setOnClickListener { retryPhoto() }
    }

    private fun checkAndRequestPermissions() {
        val needsLocation = addLocation
        val needsStorage = true // Usually needed for saving photos
        
        if (permissionsDelegate.hasAllPermissions(needsLocation, needsStorage)) {
            initializeCameraUI()
        } else {
            showPermissionUI()
        }
    }

    private fun initializeCameraUI() {
        findViewById<View>(R.id.no_permission)?.visibility = View.GONE
        cameraView.visibility = View.VISIBLE
        initCamera()
    }

    private fun showPermissionUI() {
        findViewById<View>(R.id.no_permission)?.visibility = View.VISIBLE
        cameraView.visibility = View.GONE
    }

    private fun initWaterMarkLocationDateTimeBar() {
        // Initialize watermark setup if needed
    }

    private fun initCamera() {
        // Basic camera initialization - simplified for backwards compatibility
        // For full camera functionality, use Camera2Activity instead
    }

    private fun takePicture() {
        if (addLocation && loc == null) {
            Toast.makeText(this, "Cannot add location", Toast.LENGTH_SHORT).show()
            return
        }
        if (!addLocation) {
            ll_location_preview.visibility = View.GONE
        }

        Log.d("Picture", isProcessing.toString())
        if (isProcessing) return
        isProcessing = true

        // Simplified picture taking - creates a placeholder bitmap
        // For actual camera capture, use Camera2Activity instead
        val placeholderBitmap = createPlaceholderBitmap()
        
        tvLatitude.text = "${loc?.latitude}"
        tvLongitude.text = "${loc?.longitude}"
        tvDate.text = getDateTime()
        tvAccuracy.text = "${loc?.accuracy}"
        ivPreview.setImageBitmap(placeholderBitmap)
        clCamera.visibility = View.GONE
        flPreview.visibility = View.VISIBLE
        isProcessing = false
    }

    private fun createPlaceholderBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.GRAY)
        
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 48f
            textAlign = Paint.Align.CENTER
        }
        
        canvas.drawText("Camera Preview", bitmap.width / 2f, bitmap.height / 2f, paint)
        canvas.drawText("Use Camera2Activity for full functionality", bitmap.width / 2f, bitmap.height / 2f + 60f, paint)
        
        return bitmap
    }

    private fun acceptPhoto() {
        if (isProcessing) return
        cl_options.visibility = View.GONE
        isProcessing = true
        
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            val result = Intent()
            savefile(imageUri!!, viewToBitmap(flPreview))
            result.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            setResult(RESULT_OK, result)
            finish()
            isProcessing = false
        }
    }

    private fun rejectPhoto() {
        if (isProcessing) return
        contentResolver.delete(imageUri!!, null, null)
        val result = Intent()
        setResult(Activity.RESULT_CANCELED, result)
        finish()
    }

    private fun retryPhoto() {
        if (isProcessing) return
        isProcessing = false
        clCamera.visibility = View.VISIBLE
        flPreview.visibility = View.GONE
        if (ivPreview.drawable != null && ivPreview.drawable is BitmapDrawable) {
            (ivPreview.drawable as BitmapDrawable).bitmap?.recycle()
        }
    }

    private fun switchCamera() {
        if (isBackCamera) {
            cameraSwitchView.displayFrontCamera()
        } else {
            cameraSwitchView.displayBackCamera()
        }
        isBackCamera = !isBackCamera
    }

    private fun toggleFlash() {
        when (flashManager.switch()) {
            is CameraFlashAuto -> flashSwitchView.displayFlashAuto()
            is CameraFlashOn -> flashSwitchView.displayFlashOn()
            is CameraFlashOff -> flashSwitchView.displayFlashOff()
        }
    }

    private fun savefile(sourceuri: Uri, bitmap: Bitmap) {
        try {
            val stream = contentResolver.openOutputStream(sourceuri) ?: return
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            Log.e("CameraActivity", "Error saving image: ${e.message}")
        }
    }

    private fun viewToBitmap(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
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
        val y: Float = (bitmap.height - (4 * bounds.height())) - (20 * scale)
        val y2: Float = (bitmap.height - (3 * bounds.height())) - (20 * scale)
        val y3: Float = (bitmap.height - (2 * bounds.height())) - (20 * scale)
        canvas.drawText(mText, x, y, paint)
        canvas.drawText(mTextLat, x, y2, paint)
        canvas.drawText(mTextAcc, x, y3, paint)
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

    private fun getDateTime(): String {
        val timestamp = System.currentTimeMillis()
        val date = Date(timestamp)
        val df = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.ENGLISH)
        return df.format(date)
    }

    override fun onStart() {
        super.onStart()
        location?.startLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        // Camera resume logic would go here
    }

    override fun onPause() {
        super.onPause()
        // Camera pause logic would go here
    }

    override fun onStop() {
        super.onStop()
        location?.stopLocationUpdates()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        location?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        location?.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (permissionsDelegate.resultGranted(requestCode, permissions, grantResults)) {
            initializeCameraUI()
        } else {
            showPermissionUI()
        }
    }
}

// Flash management classes
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

// Extension function for bitmap rotation
fun Bitmap.rotate(angle: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(angle)
    val result = Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
    this.recycle()
    return result
}