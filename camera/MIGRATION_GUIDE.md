# Migration Guide: From Fotoapparat to Camera2 APIs

## Overview

This guide documents the migration from `io.fotoapparat:fotoapparat:2.7.0` to Android's native Camera2 APIs. The migration provides better performance, reduced dependencies, and access to advanced camera features.

## Key Changes

### 1. Dependencies Updated

**Before (Fotoapparat):**
```gradle
implementation 'io.fotoapparat:fotoapparat:2.7.0'
```

**After (Camera2):**
```gradle
// Camera2 API dependencies
implementation 'androidx.camera:camera-core:1.3.1'
implementation 'androidx.camera:camera-camera2:1.3.1'
implementation 'androidx.camera:camera-lifecycle:1.3.1'
implementation 'androidx.camera:camera-view:1.3.1'
```

### 2. UI Components

**Before:** Used `io.fotoapparat.view.CameraView`
**After:** Uses Android's `TextureView`

The new layout file `activity_camera2.xml` replaces the CameraView with TextureView for better performance and control.

### 3. Camera Initialization

**Before (Fotoapparat):**
```kotlin
fotoapparat = Fotoapparat(
    context = this,
    view = cameraView,
    logger = loggers(logcat()),
    cameraConfiguration = cameraConfiguration
)
```

**After (Camera2):**
```kotlin
cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
// Direct camera device management with state callbacks
cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
```

### 4. Photo Capture

**Before (Fotoapparat):**
```kotlin
val photoResult = fotoapparat.autoFocus().takePicture()
photoResult.toBitmap().transform { ... }
```

**After (Camera2):**
```kotlin
// Uses ImageReader and CaptureRequest
val captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
cameraCaptureSessions.capture(captureBuilder.build(), captureCallback, backgroundHandler)
```

### 5. Camera Switching

**Before (Fotoapparat):**
```kotlin
fotoapparat.switchTo(front(), cameraConfiguration)
fotoapparat.switchTo(back(), cameraConfiguration)
```

**After (Camera2):**
```kotlin
// Close current camera and open new one
closeCamera()
isBackCamera = !isBackCamera
initCamera() // Opens camera with new facing direction
```

### 6. Flash Control

**Before (Fotoapparat):**
```kotlin
fotoapparat.updateConfiguration(cameraConfiguration.copy(flashMode = autoFlash()))
fotoapparat.updateConfiguration(cameraConfiguration.copy(flashMode = on()))
```

**After (Camera2):**
```kotlin
captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE)
captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF)
```

## New Files Created

1. **`Camera2Activity.kt`** - New activity using Camera2 APIs
2. **`Camera2Helper.kt`** - Utility class for camera operations
3. **`LocationUtilsListener.kt`** - Interface for location callbacks
4. **`activity_camera2.xml`** - Layout using TextureView
5. **`MIGRATION_GUIDE.md`** - This documentation

## Features Preserved

✅ **Photo capture with timestamp overlay**
✅ **Location data embedding**
✅ **Custom text overlay**
✅ **Front/back camera switching**
✅ **Flash control (on/off/auto)**
✅ **Preview functionality**
✅ **Permission handling**
✅ **Object shape overlays (rectangle/card)**

## Benefits of Camera2 Migration

### Performance
- **Lower latency**: Direct API access eliminates third-party overhead
- **Better memory management**: More control over camera resources
- **Efficient preview**: TextureView provides better rendering performance

### Reliability
- **No external dependencies**: Reduces risk of compatibility issues
- **Google maintained**: Guaranteed support and updates
- **Device compatibility**: Better support across Android devices

### Features
- **Advanced controls**: Manual exposure, focus, white balance
- **Hardware access**: Latest camera hardware features
- **Professional features**: RAW capture, burst mode, HDR

### Maintenance
- **Reduced dependency conflicts**: No third-party library conflicts
- **Smaller APK size**: Elimination of external library
- **Future-proof**: Access to latest Android camera APIs

## Usage Instructions

### To use the new Camera2 implementation:

1. **Update your intent to use Camera2Activity:**
   ```kotlin
   val intent = Intent(context, Camera2Activity::class.java)
   // Add same extras as before
   intent.putExtra(Camera2Activity.EXTRA_ADD_TIMESTAMP, true)
   intent.putExtra(Camera2Activity.EXTRA_ADD_LOCATION, true)
   ```

2. **All existing intent extras work the same:**
   - `EXTRA_OBJECT_SHAPE`
   - `EXTRA_ADD_TIMESTAMP`
   - `EXTRA_ADD_LOCATION`
   - `EXTRA_CUSTOM_TEXT`
   - `CAMERA_ORIENTATION`

3. **The API remains backward compatible** - no changes needed in calling code.

## Testing Recommendations

1. **Test camera switching** on devices with multiple cameras
2. **Verify flash functionality** across different devices
3. **Test permission handling** for camera and location
4. **Validate image quality** and timestamp/location overlays
5. **Test on various Android versions** (API 21+)

## Troubleshooting

### Common Issues:

1. **Camera permissions**: Ensure CAMERA permission is requested
2. **TextureView not showing preview**: Check camera initialization in onResume
3. **Image capture fails**: Verify ImageReader configuration
4. **Flash not working**: Check device flash support using Camera2Helper

### Performance Tips:

1. Use background thread for camera operations
2. Properly close camera resources in lifecycle methods
3. Handle orientation changes correctly
4. Optimize image processing operations

## Migration Timeline

- **Phase 1**: New Camera2Activity available alongside existing CameraActivity
- **Phase 2**: Test and validate Camera2Activity functionality
- **Phase 3**: Switch default to Camera2Activity
- **Phase 4**: Remove Fotoapparat dependency and old CameraActivity

This migration provides a solid foundation for future camera enhancements while maintaining all existing functionality. 