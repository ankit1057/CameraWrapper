package com.dhwaniris.comera

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.Surface
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * Production-ready Camera2 helper with comprehensive device compatibility
 * and error handling for robust camera operations across different devices.
 */
class Camera2Helper(private val context: Context) {
    
    companion object {
        private const val TAG = "Camera2Helper"
        private const val MAX_PREVIEW_WIDTH = 1920
        private const val MAX_PREVIEW_HEIGHT = 1080
    }
    
    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    
    data class CameraInfo(
        val id: String,
        val facing: Int,
        val isSupported: Boolean,
        val hasFlash: Boolean,
        val supportLevel: Int,
        val capabilities: List<Int>
    )
    
    data class DeviceCompatibility(
        val isCamera2Supported: Boolean,
        val supportLevel: String,
        val backCameras: List<CameraInfo>,
        val frontCameras: List<CameraInfo>,
        val hasReliableStopRepeating: Boolean,
        val deviceIssues: List<String>
    )
    
    /**
     * Comprehensive device compatibility check for production use
     */
    fun checkDeviceCompatibility(): DeviceCompatibility {
        val issues = mutableListOf<String>()
        var hasReliableStopRepeating = true
        
        // Check for known problematic devices
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".lowercase()
        
        // Known devices with camera API issues
        val problematicDevices = listOf(
            "samsung sm-a", // Some Samsung A series
            "huawei honor", // Some Honor devices
            "xiaomi redmi", // Some Redmi devices
            "generic", // Emulator (always LEGACY)
            "android_x86", // x86 emulator
            "goldfish", // Android emulator
        )
        
        if (problematicDevices.any { deviceModel.contains(it) }) {
            hasReliableStopRepeating = false
            issues.add("Device may have camera API implementation issues")
        }
        
        // Check Android version compatibility
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            issues.add("Camera2 API requires Android 5.0+")
            return DeviceCompatibility(
                isCamera2Supported = false,
                supportLevel = "UNSUPPORTED",
                backCameras = emptyList(),
                frontCameras = emptyList(),
                hasReliableStopRepeating = false,
                deviceIssues = issues
            )
        }
        
        try {
            val cameraIds = cameraManager.cameraIdList
            val backCameras = mutableListOf<CameraInfo>()
            val frontCameras = mutableListOf<CameraInfo>()
            var overallSupportLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
            
            for (cameraId in cameraIds) {
                try {
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING) ?: continue
                    val supportLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ?: 
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
                    val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                    val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toList() ?: emptyList()
                    
                    overallSupportLevel = maxOf(overallSupportLevel, supportLevel)
                    
                    val cameraInfo = CameraInfo(
                        id = cameraId,
                        facing = facing,
                        isSupported = true,
                        hasFlash = hasFlash,
                        supportLevel = supportLevel,
                        capabilities = capabilities
                    )
                    
                    when (facing) {
                        CameraCharacteristics.LENS_FACING_BACK -> backCameras.add(cameraInfo)
                        CameraCharacteristics.LENS_FACING_FRONT -> frontCameras.add(cameraInfo)
                    }
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Error checking camera $cameraId: ${e.message}")
                    issues.add("Camera $cameraId has access issues")
                }
            }
            
            // Check support level
            val supportLevelString = when (overallSupportLevel) {
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED" 
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                else -> "UNKNOWN"
            }
            
                    if (overallSupportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            issues.add("Device has legacy camera support - some features may be unreliable")
            hasReliableStopRepeating = false
            issues.add("LEGACY device: stopRepeating() calls will be skipped for stability")
            issues.add("LEGACY device: extended timeouts and conservative session management enabled")
        }
            
            return DeviceCompatibility(
                isCamera2Supported = backCameras.isNotEmpty() || frontCameras.isNotEmpty(),
                supportLevel = supportLevelString,
                backCameras = backCameras,
                frontCameras = frontCameras,
                hasReliableStopRepeating = hasReliableStopRepeating,
                deviceIssues = issues
            )
            
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Camera access exception during compatibility check", e)
            issues.add("Camera access denied or unavailable")
            return DeviceCompatibility(
                isCamera2Supported = false,
                supportLevel = "ERROR",
                backCameras = emptyList(),
                frontCameras = emptyList(),
                hasReliableStopRepeating = false,
                deviceIssues = issues
            )
        }
    }
    
    /**
     * Get back camera ID with fallback logic
     */
    fun getBackCameraId(): String {
        return try {
            val cameraIds = cameraManager.cameraIdList
            for (cameraId in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return cameraId
                }
            }
            // Fallback to first available camera
            if (cameraIds.isNotEmpty()) {
                Log.w(TAG, "No back camera found, using first available camera")
                cameraIds[0]
            } else {
                throw RuntimeException("No cameras available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting back camera ID", e)
            "0" // Last resort fallback
        }
    }
    
    /**
     * Get front camera ID with fallback logic
     */
    fun getFrontCameraId(): String {
        return try {
            val cameraIds = cameraManager.cameraIdList
            for (cameraId in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId
                }
            }
            // Fallback to back camera if no front camera
            Log.w(TAG, "No front camera found, falling back to back camera")
            getBackCameraId()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting front camera ID", e)
            getBackCameraId() // Fallback to back camera
        }
    }
    
    /**
     * Check if device supports specific camera features reliably
     */
    fun supportsFeatureReliably(feature: String): Boolean {
        val compatibility = checkDeviceCompatibility()
        
        return when (feature) {
            "STOP_REPEATING" -> compatibility.hasReliableStopRepeating
            "FLASH" -> compatibility.backCameras.any { it.hasFlash } || compatibility.frontCameras.any { it.hasFlash }
            "AUTOFOCUS" -> compatibility.supportLevel != "LEGACY"
            "MANUAL_SENSOR" -> compatibility.supportLevel in listOf("FULL", "LEVEL_3")
            else -> true
        }
    }
    
    /**
     * Get recommended camera configuration for this device
     */
    fun getRecommendedConfiguration(): Map<String, Any> {
        val compatibility = checkDeviceCompatibility()
        
        return mapOf(
            "useGracefulStopRepeating" to !compatibility.hasReliableStopRepeating,
            "maxRetryAttempts" to if (compatibility.supportLevel == "LEGACY") 3 else 1,
            "useBackgroundHandlerForCleanup" to true,
            "enableExtensiveLogging" to compatibility.deviceIssues.isNotEmpty(),
            "supportLevel" to compatibility.supportLevel,
            "hasKnownIssues" to compatibility.deviceIssues.isNotEmpty()
        )
    }
    
    fun getOptimalPreviewSize(cameraId: String, targetWidth: Int, targetHeight: Int): Size? {
        return try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val supportedSizes = map?.getOutputSizes(SurfaceTexture::class.java)
            
            supportedSizes?.let { sizes ->
                chooseOptimalSize(sizes, targetWidth, targetHeight, Size(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT))
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error getting optimal preview size", e)
            null
        }
    }
    
    fun getLargestImageSize(cameraId: String): Size? {
        return try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val jpegSizes = map?.getOutputSizes(ImageFormat.JPEG)
            
            jpegSizes?.let { sizes ->
                Collections.max(sizes.toList()) { lhs, rhs ->
                    java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
                }
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error getting largest image size", e)
            null
        }
    }
    
    fun isFlashSupported(cameraId: String): Boolean {
        return try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error checking flash support", e)
            false
        }
    }
    
    fun hasMultipleCameras(): Boolean {
        return try {
            cameraManager.cameraIdList.size > 1
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error checking multiple cameras", e)
            false
        }
    }
    
    private fun chooseOptimalSize(
        choices: Array<Size>,
        textureViewWidth: Int,
        textureViewHeight: Int,
        maxSize: Size
    ): Size {
        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough = mutableListOf<Size>()
        // Collect the supported resolutions that are smaller than the preview Surface
        val notBigEnough = mutableListOf<Size>()
        
        val w = textureViewWidth
        val h = textureViewHeight
        
        for (option in choices) {
            if (option.width <= maxSize.width && option.height <= maxSize.height) {
                if (option.height == option.width * h / w) {
                    if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }
        }
        
        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        return when {
            bigEnough.size > 0 -> Collections.min(bigEnough) { lhs, rhs ->
                java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
            }
            notBigEnough.size > 0 -> Collections.max(notBigEnough) { lhs, rhs ->
                java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
            }
            else -> {
                Log.e(TAG, "Couldn't find any suitable preview size")
                choices[0]
            }
        }
    }
} 