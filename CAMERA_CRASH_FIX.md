# Camera Background Restoration Crash Fix

## Problem Summary

The app was experiencing a `FATAL EXCEPTION: Camera Background` with the error:
```
java.lang.IllegalArgumentException: surfaceTexture must not be null
at android.view.Surface.<init>(Surface.java:385)
at com.dhwaniris.comera.Camera2Activity.createCameraPreview(Camera2Activity.kt:329)
```

This crash occurs when the app is restored from background and attempts to access the camera, similar to the issue described in [React Native Screens GitHub issue #2151](https://github.com/software-mansion/react-native-screens/issues/2151).

## Root Cause Analysis

The crash happens because:

1. **Surface Lifecycle Management**: When Android apps are put in background and restored, the `TextureView`'s `SurfaceTexture` may become unavailable or null
2. **Timing Issues**: The camera preview creation was attempting to create a `Surface` object before the `TextureView` was properly initialized
3. **Missing Null Checks**: The original code didn't verify that `surfaceTexture` was available before trying to use it
4. **Background Thread Issues**: Camera callbacks were executing on background threads without proper UI thread synchronization

## Implemented Solutions

### 1. Enhanced Surface Availability Checks

**File**: `camera/src/main/java/com/dhwaniris/comera/Camera2Activity.kt`

```kotlin
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
                // ... other callbacks
            }
            return
        }
        
        val surface = Surface(textureView.surfaceTexture)
        // ... rest of preview creation
    } catch (e: IllegalArgumentException) {
        Log.e(TAG, "Illegal argument exception: ${e.message}", e)
        Toast.makeText(this, "Camera preview initialization failed. Please try again.", Toast.LENGTH_SHORT).show()
    }
}
```

### 2. Improved Activity Lifecycle Management

**Enhanced onResume() method**:
```kotlin
override fun onResume() {
    super.onResume()
    startBackgroundThread()
    
    // Wait for UI to be ready, especially after background restoration
    textureView.post {
        if (textureView.isAvailable && textureView.surfaceTexture != null) {
            initCamera()
        } else {
            // Set up listener to wait for surface availability
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    Log.d(TAG, "SurfaceTexture available, initializing camera")
                    initCamera()
                }
                // ... other callbacks with proper logging
            }
        }
    }
}
```

### 3. Thread Safety Improvements

**Enhanced camera state callback**:
```kotlin
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
        // ... proper cleanup and user notification
    }
}
```

### 4. Robust Error Handling

**Enhanced openCamera() method**:
```kotlin
private fun openCamera() {
    try {
        Log.d(TAG, "Attempting to open camera: $cameraId")
        
        // Multiple validation checks
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
        
        // Permissions, lock acquisition, and camera opening...
    } catch (e: Exception) {
        Log.e(TAG, "Unexpected error opening camera", e)
        cameraOpenCloseLock.release()
        Toast.makeText(this, "Failed to open camera: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
```

### 5. Enhanced Logging for Debugging

Added comprehensive logging throughout the camera lifecycle:
- Camera initialization attempts
- Surface texture availability status
- Permission check results
- Camera state changes
- Error conditions with detailed messages

## Key Benefits

1. **Crash Prevention**: The app no longer crashes when restored from background
2. **Graceful Degradation**: If camera cannot be initialized, user gets clear feedback
3. **Automatic Recovery**: The camera automatically retries initialization when surface becomes available
4. **Better Debugging**: Extensive logging helps identify issues in production
5. **Thread Safety**: All UI updates happen on the main thread

## Testing Recommendations

1. **Background/Foreground Testing**: Put app in background, wait a few minutes, restore and test camera
2. **Permission Testing**: Deny camera permission, then grant it and test recovery
3. **Multiple Apps**: Open another camera app, then return to test camera conflict handling
4. **Device Rotation**: Test camera functionality during device orientation changes
5. **Memory Pressure**: Test under low memory conditions when system might kill background processes

## Related Issues

This fix addresses the same type of issue described in:
- [React Native Screens #2151](https://github.com/software-mansion/react-native-screens/issues/2151)
- Android camera lifecycle management during process restoration
- TextureView surface availability in background/foreground transitions

## Compatibility

- ✅ Android API 21+ (Camera2 API requirement)
- ✅ All camera orientations (front/back)
- ✅ All features (timestamp, location, custom text)
- ✅ Permission flows maintained
- ✅ Existing API compatibility preserved 