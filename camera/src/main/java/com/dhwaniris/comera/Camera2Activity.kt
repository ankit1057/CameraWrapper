package com.dhwaniris.comera

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.MeteringRectangle
import android.location.Location
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import com.dhwaniris.comera.widgets.CameraSwitchView
import com.dhwaniris.comera.widgets.FlashSwitchView
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit


class Camera2Activity : AppCompatActivity() {
    companion object {
        const val EXTRA_OBJECT_SHAPE = "OBJECT_SHAPE"
        const val RECTANGLE_SHAPE = 1
        const val CARD_SHAPE = 2
        const val EXTRA_ADD_TIMESTAMP = "EXTRA_ADD_TIMESTAMP"
        const val EXTRA_ADD_LOCATION = "EXTRA_ADD_LOCATION"  
        const val EXTRA_CUSTOM_TEXT = "EXTRA_CUSTOM_TEXT"
        const val CAMERA_ORIENTATION = "CAMERA_ORIENTATION"
        private const val TAG = "Camera2Activity"
    }

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    // UI Components
    private val textureView by lazy { findViewById<TextureView>(R.id.textureView) }
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
    private val btnGrantPermission by lazy { findViewById<Button>(R.id.btn_grant_permission) }

    // Camera2 components
    private var cameraDevice: CameraDevice? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var cameraCaptureSessions: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private var cameraManager: CameraManager? = null
    private var cameraId: String = ""
    private var isBackCamera = true
    private var flashMode = CameraMetadata.FLASH_MODE_OFF
    
    // Surface management for LEGACY device compatibility
    private var previewSurface: Surface? = null
    private var isSessionConfigured = false
    private var isCameraInitializing = false
    private var isLegacyDevice = false
    private var surfaceConfigurationLock = Object()
    private var captureImageReader: ImageReader? = null
    
    // Image reader listener for photo capture
    private val readerListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage()
        backgroundHandler?.post(ImageSaver(image))
    }
    
    // Synchronization
    private val cameraOpenCloseLock = Semaphore(1)
    
    // Other components
    private val permissionsDelegate = PermissionsDelegate(this)
    private lateinit var camera2Helper: Camera2Helper
    private lateinit var deviceConfig: Map<String, Any>
    private var imageUri: Uri? = null
    private var isProcessing = false
    private var addTimeDate = false
    private var addLocation = false
    private var customText: String? = null
    private var location: LocationUtils? = null
    private var loc: Location? = null
    private var processedBitmap: Bitmap? = null // Store the final processed bitmap

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
        setContentView(R.layout.activity_camera2)

        actionBar?.hide()
        supportActionBar?.hide()

        Log.d(TAG, "onCreate called, savedInstanceState: ${if (savedInstanceState != null) "not null" else "null"}")

        // Initialize production-ready camera helper
        camera2Helper = Camera2Helper(this)
        deviceConfig = camera2Helper.getRecommendedConfiguration()
        
        // Log device compatibility for debugging
        val compatibility = camera2Helper.checkDeviceCompatibility()
        Log.i(TAG, "Device compatibility: ${compatibility.supportLevel}, Issues: ${compatibility.deviceIssues}")

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

                // Set up proper crop overlay guides
                val objectLayout = findViewById<View>(R.id.object_layout)
                when (objectShape) {
                    RECTANGLE_SHAPE -> {
                        // Rectangle shape (4:3 aspect ratio)
                        findViewById<TextView>(R.id.obj_mgs).text = "Image will be cropped to 4:3 rectangle"
                        objectLayout.setBackgroundResource(R.drawable.ic_blur)
                        // Adjust layout params to show actual crop area
                        setupRectangleCropGuide(objectLayout)
                    }
                    CARD_SHAPE -> {
                        // Card shape (credit card aspect ratio)
                        findViewById<TextView>(R.id.obj_mgs).text = "Image will be cropped to card format (1.586:1)"
                        objectLayout.setBackgroundResource(R.drawable.ic_blur_2)
                        // Adjust layout params to show actual crop area
                        setupCardCropGuide(objectLayout)
                    }
                }
            }
            addTimeDate = extras.getBoolean(EXTRA_ADD_TIMESTAMP, false)
            addLocation = extras.getBoolean(EXTRA_ADD_LOCATION, false)
            customText = extras.getString(EXTRA_CUSTOM_TEXT)
        }

        if (addLocation) {
            location = LocationUtils(this, locationListener)
        }

        // Check camera availability and multiple camera support
        val compatibility = camera2Helper.checkDeviceCompatibility()
        val hasMultipleCameras = compatibility.backCameras.isNotEmpty() && compatibility.frontCameras.isNotEmpty()
        if (!hasMultipleCameras) {
            cameraSwitchView.visibility = View.GONE
        }
    }

    private fun setupUIComponents() {
        capture.setOnClickListener { captureStillPicture() }
        cameraSwitchView.setOnClickListener { switchCamera() }
        flashSwitchView.setOnClickListener { toggleFlash() }
        bAccept.setOnClickListener { acceptPhoto() }
        bReject.setOnClickListener { rejectPhoto() }
        bRetry.setOnClickListener { retryPhoto() }
        btnGrantPermission.setOnClickListener { 
            val needsLocation = addLocation
            val needsStorage = true // Usually needed for saving photos
            permissionsDelegate.requestAllPermissions(
                includeLocation = needsLocation,
                includeStorage = needsStorage
            )
        }
        
        // Add touch-to-focus functionality
        textureView.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                try {
                    // Validate touch coordinates
                    if (event.x >= 0 && event.y >= 0 && 
                        event.x <= textureView.width && event.y <= textureView.height) {
                        focusOnTouch(event.x, event.y)
                    } else {
                        Log.w(TAG, "Invalid touch coordinates: (${event.x}, ${event.y}) for view size: ${textureView.width}x${textureView.height}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling touch event", e)
                }
                true
            } else {
                false
            }
        }
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
        textureView.visibility = View.VISIBLE
        initCamera()
    }

    private fun showPermissionUI() {
        findViewById<View>(R.id.no_permission)?.visibility = View.VISIBLE
        textureView.visibility = View.GONE
    }

    private fun focusOnTouch(x: Float, y: Float) {
        try {
            if (cameraDevice == null || cameraCaptureSessions == null) {
                Log.d(TAG, "Cannot focus: camera device or session is null")
                return
            }
            
            if (!isSessionConfigured) {
                Log.d(TAG, "Cannot focus: session not configured")
                return
            }
            
            // Convert touch coordinates to camera coordinates
            val focusArea = calculateFocusArea(x, y)
            
            val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            if (previewSurface != null) {
                captureBuilder.addTarget(previewSurface!!)
            } else {
                Log.w(TAG, "Preview surface is null, using texture surface")
                captureBuilder.addTarget(Surface(textureView.surfaceTexture))
            }
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO)
            captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
            
            // Set focus area if supported
            val characteristics = cameraManager!!.getCameraCharacteristics(cameraId)
            val maxRegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) ?: 0
            if (maxRegions > 0) {
                captureBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(focusArea))
            }
            
            cameraCaptureSessions!!.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    super.onCaptureCompleted(session, request, result)
                    // Focus completed, restart preview
                    updatePreview()
                }
            }, backgroundHandler)
            
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error during touch to focus", e)
        }
    }

    private fun calculateFocusArea(x: Float, y: Float): MeteringRectangle {
        val textureWidth = textureView.width
        val textureHeight = textureView.height
        
        // Validate input coordinates
        if (textureWidth <= 0 || textureHeight <= 0 || x < 0 || y < 0) {
            Log.w(TAG, "Invalid texture dimensions or coordinates: ${textureWidth}x${textureHeight}, touch: ($x, $y)")
            // Return a default center focus area
            return MeteringRectangle(-100, -100, 200, 200, MeteringRectangle.METERING_WEIGHT_MAX)
        }
        
        // Convert to camera coordinate system (-1000 to 1000)
        val cameraX = ((x / textureWidth) * 2000 - 1000).toInt()
        val cameraY = ((y / textureHeight) * 2000 - 1000).toInt()
        
        // Create a 200x200 focus area
        val rectSize = 100
        val left = Math.max(cameraX - rectSize, -1000)
        val top = Math.max(cameraY - rectSize, -1000)
        val right = Math.min(cameraX + rectSize, 1000)
        val bottom = Math.min(cameraY + rectSize, 1000)
        
        // Ensure width and height are positive
        val width = Math.max(right - left, 1)
        val height = Math.max(bottom - top, 1)
        
        // Ensure coordinates are within valid range
        val validLeft = Math.max(left, -1000)
        val validTop = Math.max(top, -1000)
        
        Log.d(TAG, "Focus area: left=$validLeft, top=$validTop, width=$width, height=$height")
        return MeteringRectangle(validLeft, validTop, width, height, MeteringRectangle.METERING_WEIGHT_MAX)
    }

    private fun initCamera() {
        try {
            Log.d(TAG, "Initializing camera, isBackCamera: $isBackCamera")
            
            // Prevent multiple simultaneous initializations
            if (isCameraInitializing) {
                Log.w(TAG, "Camera initialization already in progress")
                return
            }
            isCameraInitializing = true
            
            // Check if we have camera permission
            if (!permissionsDelegate.hasCameraPermission()) {
                Log.e(TAG, "Camera permission not granted")
                showPermissionUI()
                isCameraInitializing = false
                return
            }
            
            // Clean up any existing camera resources first
            closeCamera()
            
            cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
            startBackgroundThread()
            
            // Use production-ready camera ID selection with fallback
            cameraId = if (isBackCamera) {
                camera2Helper.getBackCameraId()
            } else {
                camera2Helper.getFrontCameraId()
            }
            
            Log.d(TAG, "Selected camera ID: $cameraId")
            
            // For LEGACY devices, add extra delay before opening camera
            val compatibility = camera2Helper.checkDeviceCompatibility()
            isLegacyDevice = compatibility.supportLevel == "LEGACY"
            if (isLegacyDevice) {
                Log.w(TAG, "LEGACY device detected - using conservative initialization")
                backgroundHandler?.postDelayed({
                    openCamera()
                }, 500) // 500ms delay for LEGACY devices
            } else {
                openCamera()
            }
            
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Camera access exception in initCamera", e)
            Toast.makeText(this, "Failed to access camera: ${e.message}", Toast.LENGTH_SHORT).show()
            isCameraInitializing = false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in initCamera", e)
            Toast.makeText(this, "Failed to initialize camera: ${e.message}", Toast.LENGTH_SHORT).show()
            isCameraInitializing = false
        }
    }

    private fun openCamera() {
        try {
            Log.d(TAG, "Attempting to open camera: $cameraId")
            
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                val errorMsg = "Time out waiting to lock camera opening."
                Log.e(TAG, errorMsg)
                throw RuntimeException(errorMsg)
            }
            
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != 
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Camera permission not granted")
                cameraOpenCloseLock.release()
                return
            }
            
            // Additional checks before opening camera
            if (cameraManager == null) {
                Log.e(TAG, "Camera manager is null")
                cameraOpenCloseLock.release()
                return
            }
            
            if (backgroundHandler == null) {
                Log.e(TAG, "Background handler is null")
                cameraOpenCloseLock.release()
                return
            }
            
            Log.d(TAG, "Opening camera with ID: $cameraId")
            cameraManager!!.openCamera(cameraId, stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Cannot open camera", e)
            cameraOpenCloseLock.release()
            Toast.makeText(this, "Cannot open camera: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: InterruptedException) {
            val errorMsg = "Interrupted while trying to lock camera opening."
            Log.e(TAG, errorMsg, e)
            throw RuntimeException(errorMsg, e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error opening camera", e)
            cameraOpenCloseLock.release()
            Toast.makeText(this, "Failed to open camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "Camera opened successfully")
            cameraOpenCloseLock.release()
            cameraDevice = camera
            
            // Post to main thread to ensure UI is ready
            runOnUiThread {
                createCameraPreview()
            }
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.w(TAG, "Camera disconnected")
            cameraOpenCloseLock.release()
            
            // Safely close disconnected camera
            try {
                camera.close()
                Log.d(TAG, "Disconnected camera closed successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Exception while closing disconnected camera: ${e.message}")
            }
            
            // Clean up capture session if it exists
            cameraCaptureSessions?.let { session ->
                try {
                    session.close()
                    Log.d(TAG, "Capture session closed after camera disconnect")
                } catch (e: Exception) {
                    Log.w(TAG, "Exception while closing capture session after disconnect: ${e.message}")
                }
            }
            cameraCaptureSessions = null
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            val errorMsg = when (error) {
                CameraDevice.StateCallback.ERROR_CAMERA_IN_USE -> "Camera in use"
                CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE -> "Max cameras in use"
                CameraDevice.StateCallback.ERROR_CAMERA_DISABLED -> "Camera disabled"
                CameraDevice.StateCallback.ERROR_CAMERA_DEVICE -> "Camera device error"
                CameraDevice.StateCallback.ERROR_CAMERA_SERVICE -> "Camera service error"
                else -> "Unknown camera error: $error"
            }
            
            Log.e(TAG, "Camera error: $errorMsg (code: $error)")
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
            
            runOnUiThread {
                Toast.makeText(this@Camera2Activity, "Camera error: $errorMsg", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun createCameraPreview() {
        try {
            // Check if camera device is available
            if (cameraDevice == null) {
                Log.e(TAG, "Camera device is null, cannot create preview")
                return
            }
            
            // Check if TextureView and its SurfaceTexture are available
            if (!textureView.isAvailable || textureView.surfaceTexture == null) {
                Log.e(TAG, "TextureView surface is not available, cannot create preview")
                // Retry after TextureView is ready
                textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        createCameraPreview()
                    }
                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false
                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
                return
            }
            
            synchronized(surfaceConfigurationLock) {
                // Clean up any existing surfaces
                previewSurface?.release()
                captureImageReader?.close()
                
                // Create preview surface
                previewSurface = Surface(textureView.surfaceTexture)
                
                // Create image reader for photo capture (required for session configuration)
                val size = getOptimalSize()
                captureImageReader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 1)
                captureImageReader!!.setOnImageAvailableListener(readerListener, backgroundHandler)
                
                captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequestBuilder!!.addTarget(previewSurface!!)
                
                Log.d(TAG, "Creating capture session, LEGACY device: $isLegacyDevice")
                
                // For LEGACY devices, include both surfaces from the start to avoid reconfiguration
                val surfaces = listOf(previewSurface!!, captureImageReader!!.surface)
                
                try {
                    cameraDevice!!.createCaptureSession(
                        surfaces,
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                                synchronized(surfaceConfigurationLock) {
                                    if (cameraDevice == null) {
                                        Log.w(TAG, "Camera device null during session configuration")
                                        return
                                    }
                                    
                                    Log.d(TAG, "Camera capture session configured successfully")
                                    cameraCaptureSessions = cameraCaptureSession
                                    isSessionConfigured = true
                                    isCameraInitializing = false
                                    
                                    // For LEGACY devices, add extra delay before starting preview
                                    val delay = if (isLegacyDevice) 300L else 50L
                                    backgroundHandler?.postDelayed({
                                        updatePreview()
                                    }, delay)
                                }
                            }

                            override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                                synchronized(surfaceConfigurationLock) {
                                    Log.e(TAG, "Camera capture session configuration failed")
                                    isSessionConfigured = false
                                    isCameraInitializing = false
                                    runOnUiThread {
                                        Toast.makeText(this@Camera2Activity, "Camera configuration failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            
                            override fun onClosed(session: CameraCaptureSession) {
                                synchronized(surfaceConfigurationLock) {
                                    Log.d(TAG, "Camera capture session closed")
                                    isSessionConfigured = false
                                }
                            }
                        },
                        backgroundHandler
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create capture session", e)
                    isSessionConfigured = false
                    isCameraInitializing = false
                    throw e
                }
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Camera access exception in createCameraPreview", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Illegal argument exception in createCameraPreview: ${e.message}", e)
            // This handles the "surfaceTexture must not be null" error
            Toast.makeText(this, "Camera preview initialization failed. Please try again.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception in createCameraPreview", e)
        }
    }

    private fun updatePreview() {
        if (cameraDevice == null) {
            Log.w(TAG, "Cannot update preview - camera device is null")
            return
        }
        
        if (cameraCaptureSessions == null) {
            Log.w(TAG, "Cannot update preview - capture session is null")
            return
        }
        
        try {
            captureRequestBuilder!!.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            captureRequestBuilder!!.set(CaptureRequest.FLASH_MODE, flashMode)
            
            cameraCaptureSessions!!.setRepeatingRequest(
                captureRequestBuilder!!.build(),
                null,
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Camera access exception in updatePreview", e)
            if (e.reason == CameraAccessException.CAMERA_DISCONNECTED) {
                Log.w(TAG, "Camera disconnected during preview update")
                // Camera is disconnected, clean up
                cameraDevice = null
                cameraCaptureSessions = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception in updatePreview", e)
        }
    }

    private fun captureStillPicture() {
        if (addLocation && loc == null) {
            Toast.makeText(this, "Cannot add location", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isProcessing) {
            Log.w(TAG, "Photo capture already in progress")
            return
        }
        
        synchronized(surfaceConfigurationLock) {
            // Check if camera session is properly configured
            if (!isSessionConfigured || cameraDevice == null || cameraCaptureSessions == null || captureImageReader == null) {
                Log.e(TAG, "Camera not properly configured for capture")
                runOnUiThread {
                    Toast.makeText(this@Camera2Activity, "Camera not ready. Please wait and try again.", Toast.LENGTH_SHORT).show()
                }
                return
            }
            
            isProcessing = true

            if (!addLocation) {
                ll_location_preview.visibility = View.GONE
            }

            try {
                // Use the pre-configured image reader that was included in session configuration
                val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                captureBuilder.addTarget(captureImageReader!!.surface)
                captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                captureBuilder.set(CaptureRequest.FLASH_MODE, flashMode)

                val captureCallback = object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)
                        Log.d(TAG, "Image captured successfully")
                    }
                    
                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {
                        super.onCaptureFailed(session, request, failure)
                        Log.e(TAG, "Image capture failed: ${failure.reason}")
                        isProcessing = false
                        runOnUiThread {
                            Toast.makeText(this@Camera2Activity, "Image capture failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                Log.d(TAG, "Attempting to capture image")
                cameraCaptureSessions!!.capture(captureBuilder.build(), captureCallback, backgroundHandler)
            } catch (e: CameraAccessException) {
                Log.e(TAG, "Camera access exception in captureStillPicture", e)
                isProcessing = false
                runOnUiThread {
                    Toast.makeText(this@Camera2Activity, "Camera error during capture: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected exception in captureStillPicture", e)
                isProcessing = false
                runOnUiThread {
                    Toast.makeText(this@Camera2Activity, "Unexpected error during capture: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private inner class ImageSaver(private val image: Image) : Runnable {
        override fun run() {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            
            try {
                // Save the raw image data temporarily for processing
                runOnUiThread {
                    processImageAndShowPreview(bytes)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image", e)
                runOnUiThread {
                    Toast.makeText(this@Camera2Activity, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                image.close()
                isProcessing = false
            }
        }
    }

    private fun processImageAndShowPreview(imageBytes: ByteArray) {
        try {
            // Convert bytes to bitmap with proper orientation handling
            var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            
            if (bitmap != null) {
                // Apply shape cropping if specified (without orientation changes)
                val objectShape = intent.getIntExtra(EXTRA_OBJECT_SHAPE, 0)
                if (objectShape != 0) {
                    bitmap = cropImageToShape(bitmap, objectShape)
                }
                
                // Apply other image processing
                if (addTimeDate) {
                    bitmap = addTimeOnImage(bitmap)
                }
                
                if (addLocation && loc != null) {
                    bitmap = addLocationOnImage(bitmap)
                }
                
                if (!customText.isNullOrEmpty()) {
                    bitmap = addCustomTextOnImage(bitmap, customText!!)
                }
                
                // Store the processed bitmap for saving
                processedBitmap = bitmap
                
                // Update UI with processed image
                runOnUiThread {
                    tvLatitude.text = if (loc?.latitude != null) String.format("%.6f", loc?.latitude) else "N/A"
                    tvLongitude.text = if (loc?.longitude != null) String.format("%.6f", loc?.longitude) else "N/A"
                    tvDate.text = getDateTime()
                    tvAccuracy.text = if (loc?.accuracy != null) String.format("%.1f m", loc?.accuracy) else "N/A"
                    
                    ivPreview.setImageBitmap(bitmap)
                    clCamera.visibility = View.GONE
                    flPreview.visibility = View.VISIBLE
                }
            } else {
                Log.e(TAG, "Failed to decode image bytes")
                runOnUiThread {
                    Toast.makeText(this@Camera2Activity, "Failed to process image", Toast.LENGTH_SHORT).show()
                    isProcessing = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
            runOnUiThread {
                Toast.makeText(this@Camera2Activity, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
                isProcessing = false
            }
        }
    }


    
    /**
     * Crops image to specified shape (rectangle or card)
     */
    private fun cropImageToShape(bitmap: Bitmap, shape: Int): Bitmap {
        return when (shape) {
            RECTANGLE_SHAPE -> cropToRectangle(bitmap)
            CARD_SHAPE -> cropToCard(bitmap)
            else -> bitmap
        }
    }
    
    /**
     * Crops image to rectangle shape (4:3 aspect ratio, centered)
     */
    private fun cropToRectangle(bitmap: Bitmap): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        
        // Calculate rectangle dimensions (4:3 aspect ratio)
        val targetAspectRatio = 4f / 3f
        val originalAspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
        
        val cropWidth: Int
        val cropHeight: Int
        
        // Always perform some cropping to make it visually distinct
        if (originalAspectRatio > targetAspectRatio) {
            // Image is wider than target, crop width
            cropHeight = (originalHeight * 0.85f).toInt() // Crop 15% from height
            cropWidth = (cropHeight * targetAspectRatio).toInt()
        } else {
            // Image is taller than target, crop height  
            cropWidth = (originalWidth * 0.85f).toInt() // Crop 15% from width
            cropHeight = (cropWidth / targetAspectRatio).toInt()
        }
        
        // Center the crop
        val startX = (originalWidth - cropWidth) / 2
        val startY = (originalHeight - cropHeight) / 2
        
        val croppedBitmap = Bitmap.createBitmap(
            bitmap, startX, startY, cropWidth, cropHeight
        )
        
        Log.d(TAG, "Rectangle crop: ${originalWidth}x${originalHeight} -> ${cropWidth}x${cropHeight}")
        
        if (croppedBitmap != bitmap) {
            bitmap.recycle()
        }
        
        return croppedBitmap
    }
    
    /**
     * Crops image to card shape (credit card aspect ratio 85.60 × 53.98 mm ≈ 1.586:1)
     */
    private fun cropToCard(bitmap: Bitmap): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        
        // Calculate card dimensions (credit card aspect ratio)
        val targetAspectRatio = 1.586f // Credit card ratio
        val originalAspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
        
        val cropWidth: Int
        val cropHeight: Int
        
        // Always perform visible cropping to card dimensions
        if (originalAspectRatio > targetAspectRatio) {
            // Image is wider than target, crop width significantly
            cropHeight = (originalHeight * 0.7f).toInt() // Crop 30% from height for card shape
            cropWidth = (cropHeight * targetAspectRatio).toInt()
        } else {
            // Image is taller than target, crop height significantly
            cropWidth = (originalWidth * 0.7f).toInt() // Crop 30% from width for card shape
            cropHeight = (cropWidth / targetAspectRatio).toInt()
        }
        
        // Center the crop
        val startX = (originalWidth - cropWidth) / 2
        val startY = (originalHeight - cropHeight) / 2
        
        val croppedBitmap = Bitmap.createBitmap(
            bitmap, startX, startY, cropWidth, cropHeight
        )
        
        Log.d(TAG, "Card crop: ${originalWidth}x${originalHeight} -> ${cropWidth}x${cropHeight}")
        
        if (croppedBitmap != bitmap) {
            bitmap.recycle()
        }
        
        return croppedBitmap
    }

    private fun acceptPhoto() {
        if (isProcessing) return
        
        cl_options.visibility = View.GONE
        isProcessing = true
        
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            try {
                // Save the actual processed bitmap (with cropping and effects applied)
                val finalBitmap = processedBitmap ?: throw IllegalStateException("No processed bitmap available")
                saveImageToUri(finalBitmap, imageUri!!)
                
                val result = Intent()
                result.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                setResult(RESULT_OK, result)
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving final image", e)
                Toast.makeText(this@Camera2Activity, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
                isProcessing = false
            }
        }
    }

    private fun saveImageToUri(bitmap: Bitmap, uri: Uri) {
        try {
            val outputStream = contentResolver.openOutputStream(uri)
            outputStream?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                stream.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap to URI", e)
            throw e
        }
    }

    private fun rejectPhoto() {
        if (isProcessing) return
        contentResolver.delete(imageUri!!, null, null)
        setResult(Activity.RESULT_CANCELED)
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
        
        // Clean up processed bitmap
        processedBitmap?.recycle()
        processedBitmap = null
    }

    private fun switchCamera() {
        closeCamera()
        isBackCamera = !isBackCamera
        
        if (isBackCamera) {
            cameraSwitchView.displayBackCamera()
        } else {
            cameraSwitchView.displayFrontCamera()
        }
        
        initCamera()
    }

    private fun toggleFlash() {
        when (flashMode) {
            CameraMetadata.FLASH_MODE_OFF -> {
                flashMode = CameraMetadata.FLASH_MODE_SINGLE
                flashSwitchView.displayFlashOn()
            }
            CameraMetadata.FLASH_MODE_SINGLE -> {
                flashMode = CameraMetadata.FLASH_MODE_OFF
                flashSwitchView.displayFlashOff()
            }
        }
        updatePreview()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("Camera Background")
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted while stopping background thread", e)
        }
    }

    private fun safelyStopRepeating() {
        if (!isSessionConfigured) {
            Log.d(TAG, "Session not configured, skipping stop repeating")
            return
        }
        
        cameraCaptureSessions?.let { session ->
            // For LEGACY devices, always skip stopRepeating to avoid crashes
            val compatibility = camera2Helper.checkDeviceCompatibility()
            val isLegacy = compatibility.supportLevel == "LEGACY"
            val useGracefulStop = deviceConfig["useGracefulStopRepeating"] as? Boolean ?: isLegacy
            
            if (!useGracefulStop && camera2Helper.supportsFeatureReliably("STOP_REPEATING")) {
                // Device supports stop repeating reliably
                try {
                    session.stopRepeating()
                    Log.d(TAG, "Stopped repeating requests successfully")
                    // Don't return here, still need to close session
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to stop repeating, continuing with cleanup: ${e.message}")
                }
            } else {
                Log.w(TAG, "Skipping stopRepeating for device compatibility (LEGACY: $isLegacy)")
            }
            
            // Always close the session after stopping repeating (or skipping it)
            try {
                session.close()
                Log.d(TAG, "Session closed successfully")
            } catch (e: CameraAccessException) {
                when (e.reason) {
                    CameraAccessException.CAMERA_DISCONNECTED -> {
                        Log.w(TAG, "Camera already disconnected during session close")
                    }
                    CameraAccessException.CAMERA_ERROR -> {
                        Log.w(TAG, "Camera error during session close (device compatibility issue): ${e.message}")
                    }
                    CameraAccessException.CAMERA_IN_USE -> {
                        Log.w(TAG, "Camera in use during session close: ${e.message}")
                    }
                    CameraAccessException.MAX_CAMERAS_IN_USE -> {
                        Log.w(TAG, "Max cameras in use during session close: ${e.message}")
                    }
                    CameraAccessException.CAMERA_DISABLED -> {
                        Log.w(TAG, "Camera disabled during session close: ${e.message}")
                    }
                    else -> {
                        Log.e(TAG, "Unknown camera access exception during session close: ${e.message}")
                    }
                }
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Session already closed: ${e.message}")
            } catch (e: Exception) {
                Log.w(TAG, "Unexpected exception during session close: ${e.message}")
            }
        }
        
        isSessionConfigured = false
    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            
            // Safely stop repeating requests and close session (handles device compatibility)
            safelyStopRepeating()
            cameraCaptureSessions = null
            
            // Close camera device safely
            cameraDevice?.let { device ->
                try {
                    device.close()
                    Log.d(TAG, "Camera device closed successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "Exception while closing camera device: ${e.message}")
                }
            }
            cameraDevice = null
            
            // Close image reader safely
            imageReader?.let { reader ->
                try {
                    reader.close()
                    Log.d(TAG, "Image reader closed successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "Exception while closing image reader: ${e.message}")
                }
            }
            imageReader = null
            
            // Clean up preview surface
            previewSurface?.let { surface ->
                try {
                    surface.release()
                    Log.d(TAG, "Preview surface released successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "Exception while releasing preview surface: ${e.message}")
                }
            }
            previewSurface = null
            
            // Clean up capture image reader
            captureImageReader?.let { reader ->
                try {
                    reader.close()
                    Log.d(TAG, "Capture image reader closed successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "Exception while closing capture image reader: ${e.message}")
                }
            }
            captureImageReader = null
            
            // Reset state flags
            isSessionConfigured = false
            isCameraInitializing = false
            
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted while trying to lock camera closing", e)
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception while closing camera", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    private fun getDateTime(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun initWaterMarkLocationDateTimeBar() {
        // Initialize watermark setup here if needed
    }
    
    private fun getOptimalSize(): Size {
        return try {
            val manager = getSystemService(CAMERA_SERVICE) as CameraManager
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            
            // Get the largest available JPEG size
            val jpegSizes = map?.getOutputSizes(ImageFormat.JPEG)
            if (jpegSizes != null && jpegSizes.isNotEmpty()) {
                // Return the largest available size
                jpegSizes.maxByOrNull { it.width * it.height } ?: Size(1920, 1080)
            } else {
                // Fallback to a common size
                Size(1920, 1080)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting optimal size: ${e.message}")
            // Return a safe default size
            Size(1920, 1080)
        }
    }

    private fun addTimeOnImage(bitmap: Bitmap): Bitmap {
        val resources = this.resources
        val scale = resources.displayMetrics.density
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val mText = getDateTime()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.textSize = (18 * scale)
        paint.setShadowLayer(2f, 0f, 2f, Color.BLACK)
        val bounds = Rect()
        paint.getTextBounds(mText, 0, mText.length, bounds)
        // Position at top-right corner with proper padding
        val x = (bitmap.width - bounds.width()) - (16 * scale)
        val y = bounds.height() + (16 * scale)
        canvas.drawText(mText, x, y, paint)
        return mutableBitmap
    }

    private fun addCustomTextOnImage(bitmap: Bitmap, mText: String): Bitmap {
        val resources = this.resources
        val scale = resources.displayMetrics.density
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.textSize = (16 * scale)
        paint.setShadowLayer(2f, 0f, 2f, Color.BLACK)
        val bounds = Rect()
        paint.getTextBounds(mText, 0, mText.length, bounds)
        // Position at top-left corner with proper padding
        val x = (16 * scale)
        val y = bounds.height() + (16 * scale)
        canvas.drawText(mText, x, y, paint)
        return mutableBitmap
    }

    private fun addLocationOnImage(bitmap: Bitmap): Bitmap {
        val resources = this.resources
        val scale = resources.displayMetrics.density
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        
        val mTextLat = "Lat: ${String.format("%.6f", loc?.latitude ?: 0.0)}"
        val mTextLon = "Lon: ${String.format("%.6f", loc?.longitude ?: 0.0)}"
        val mTextAcc = "Acc: ${String.format("%.1f", loc?.accuracy ?: 0.0)}m"
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.textSize = (14 * scale)
        paint.setShadowLayer(2f, 0f, 2f, Color.BLACK)
        
        val bounds = Rect()
        paint.getTextBounds(mTextLat, 0, mTextLat.length, bounds)
        val lineHeight = bounds.height() + (4 * scale)
        
        // Position at bottom-left corner with proper spacing
        val x = (16 * scale)
        val baseY = bitmap.height - (16 * scale)
        
        canvas.drawText(mTextLat, x, baseY - (2 * lineHeight), paint)
        canvas.drawText(mTextLon, x, baseY - lineHeight, paint)
        canvas.drawText(mTextAcc, x, baseY, paint)
        
        return mutableBitmap
    }

    private fun viewToBitmap(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    override fun onStart() {
        super.onStart()
        location?.startLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        
        // Wait a bit for the UI to be ready, especially after background restoration
        textureView.post {
            if (textureView.isAvailable && textureView.surfaceTexture != null) {
                initCamera()
            } else {
                // Set up listener to wait for surface to be available
                textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        Log.d(TAG, "SurfaceTexture available, initializing camera")
                        initCamera()
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                        Log.d(TAG, "SurfaceTexture size changed: ${width}x${height}")
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        Log.d(TAG, "SurfaceTexture destroyed")
                        return false
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                        // Handle frame updates if needed
                    }
                }
            }
        }
    }

    override fun onPause() {
        Log.d(TAG, "onPause called - closing camera and stopping background thread")
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        location?.stopLocationUpdates()
    }

    override fun onDestroy() {
        location?.stopLocationUpdates()
        super.onDestroy()
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

    /**
     * Sets up rectangle crop guide overlay to match actual crop area
     */
    private fun setupRectangleCropGuide(objectLayout: View) {
        textureView.post {
            val previewWidth = textureView.width
            val previewHeight = textureView.height
            
            if (previewWidth > 0 && previewHeight > 0) {
                val targetAspectRatio = 4f / 3f
                val previewAspectRatio = previewWidth.toFloat() / previewHeight.toFloat()
                
                val layoutParams = objectLayout.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                
                if (previewAspectRatio > targetAspectRatio) {
                    // Preview is wider, crop will constrain width
                    layoutParams.height = previewHeight
                    layoutParams.width = (previewHeight * targetAspectRatio).toInt()
                } else {
                    // Preview is taller, crop will constrain height
                    layoutParams.width = previewWidth
                    layoutParams.height = (previewWidth / targetAspectRatio).toInt()
                }
                
                objectLayout.layoutParams = layoutParams
                Log.d(TAG, "Rectangle guide: ${layoutParams.width}x${layoutParams.height}")
            }
        }
    }
    
    /**
     * Sets up card crop guide overlay to match actual crop area
     */
    private fun setupCardCropGuide(objectLayout: View) {
        textureView.post {
            val previewWidth = textureView.width
            val previewHeight = textureView.height
            
            if (previewWidth > 0 && previewHeight > 0) {
                val targetAspectRatio = 1.586f // Credit card ratio
                val previewAspectRatio = previewWidth.toFloat() / previewHeight.toFloat()
                
                val layoutParams = objectLayout.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                
                if (previewAspectRatio > targetAspectRatio) {
                    // Preview is wider, crop will constrain width
                    layoutParams.height = previewHeight
                    layoutParams.width = (previewHeight * targetAspectRatio).toInt()
                } else {
                    // Preview is taller, crop will constrain height
                    layoutParams.width = previewWidth
                    layoutParams.height = (previewWidth / targetAspectRatio).toInt()
                }
                
                objectLayout.layoutParams = layoutParams
                Log.d(TAG, "Card guide: ${layoutParams.width}x${layoutParams.height}")
            }
        }
    }
}

// Note: Flash management classes and Bitmap.rotate extension are defined in CameraActivity.kt to avoid duplication 