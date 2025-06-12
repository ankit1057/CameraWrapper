package com.dhwaniris.comera

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore

/**
 * Helper class for launching camera activities with proper permission handling.
 * This class provides a simple API similar to how Fotoapparat handled permissions.
 */
object CameraLauncher {

    /**
     * Launch Camera2Activity with enhanced permission handling.
     * The activity will automatically request permissions if not granted.
     * 
     * @param context The context to launch from
     * @param outputUri The URI where the photo should be saved
     * @param objectShape Optional shape overlay (RECTANGLE_SHAPE or CARD_SHAPE)
     * @param addTimestamp Whether to add timestamp to photo
     * @param addLocation Whether to add location to photo (requires location permission)
     * @param customText Optional custom text to add to photo
     * @param cameraOrientation Optional camera orientation preference
     */
    fun launchCamera2(
        context: Context,
        outputUri: Uri,
        objectShape: Int? = null,
        addTimestamp: Boolean = false,
        addLocation: Boolean = false,
        customText: String? = null,
        cameraOrientation: String? = null
    ): Intent {
        val intent = Intent(context, Camera2Activity::class.java).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
            
            objectShape?.let { 
                putExtra(Camera2Activity.EXTRA_OBJECT_SHAPE, it) 
            }
            
            putExtra(Camera2Activity.EXTRA_ADD_TIMESTAMP, addTimestamp)
            putExtra(Camera2Activity.EXTRA_ADD_LOCATION, addLocation)
            
            customText?.let { 
                putExtra(Camera2Activity.EXTRA_CUSTOM_TEXT, it) 
            }
            
            cameraOrientation?.let { 
                putExtra(Camera2Activity.CAMERA_ORIENTATION, it) 
            }
        }
        
        return intent
    }

    /**
     * Launch legacy CameraActivity (Fotoapparat-based) with enhanced permission handling.
     * Note: This is maintained for backwards compatibility but Camera2Activity is recommended.
     */
    fun launchLegacyCamera(
        context: Context,
        outputUri: Uri,
        objectShape: Int? = null,
        addTimestamp: Boolean = false,
        addLocation: Boolean = false,
        customText: String? = null,
        cameraOrientation: String? = null
    ): Intent {
        val intent = Intent(context, Camera2Activity::class.java).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
            
            objectShape?.let { 
                putExtra(Camera2Activity.EXTRA_OBJECT_SHAPE, it)
            }
            
            putExtra(Camera2Activity.EXTRA_ADD_TIMESTAMP, addTimestamp)
            putExtra(Camera2Activity.EXTRA_ADD_LOCATION, addLocation)
            
            customText?.let { 
                putExtra(Camera2Activity.EXTRA_CUSTOM_TEXT, it)
            }
            
            cameraOrientation?.let { 
                putExtra(Camera2Activity.CAMERA_ORIENTATION, it)
            }
        }
        
        return intent
    }

    /**
     * Check if all required permissions are available for basic camera functionality.
     * This only checks camera permission.
     */
    fun hasCameraPermissions(context: Context): Boolean {
        val permissionsDelegate = PermissionsDelegate(context as android.app.Activity)
        return permissionsDelegate.hasCameraPermission()
    }

    /**
     * Check if all permissions are available for enhanced camera functionality.
     * 
     * @param context The context to check permissions in
     * @param includeLocation Whether to check location permissions
     * @param includeStorage Whether to check storage permissions
     */
    fun hasAllPermissions(
        context: Context, 
        includeLocation: Boolean = false, 
        includeStorage: Boolean = false
    ): Boolean {
        val permissionsDelegate = PermissionsDelegate(context as android.app.Activity)
        return permissionsDelegate.hasAllPermissions(includeLocation, includeStorage)
    }

    /**
     * Request camera permission manually if you want to handle permissions before launching camera.
     * Generally not needed as the camera activities handle this automatically.
     */
    fun requestCameraPermission(context: Context) {
        val permissionsDelegate = PermissionsDelegate(context as android.app.Activity)
        permissionsDelegate.requestCameraPermission()
    }

    /**
     * Request all permissions manually if you want to handle permissions before launching camera.
     * Generally not needed as the camera activities handle this automatically.
     */
    fun requestAllPermissions(
        context: Context,
        includeLocation: Boolean = false,
        includeStorage: Boolean = false
    ) {
        val permissionsDelegate = PermissionsDelegate(context as android.app.Activity)
        permissionsDelegate.requestAllPermissions(includeLocation, includeStorage)
    }

    // Constants for object shapes
    const val RECTANGLE_SHAPE = 1
    const val CARD_SHAPE = 2
    
    // Constants for camera orientation  
    const val CAMERA_BACK = "CAMERA_BACK"
    const val CAMERA_FRONT = "CAMERA_FRONT"
} 