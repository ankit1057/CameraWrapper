# Camera Permission Implementation

## Overview

I have successfully implemented comprehensive camera permission handling in the CameraWrapper library, similar to how Fotoapparat handled permissions. The implementation provides automatic permission requests, user-friendly prompts, and graceful handling of permission denials.

## Key Features Implemented

### 1. Enhanced PermissionsDelegate Class

**Location**: `camera/src/main/java/com/dhwaniris/comera/PermissionDelegate.kt`

#### Features:
- **Multiple Permission Support**: Camera, Storage, and Location permissions
- **Smart Permission Checking**: Handles Android 13+ storage permission changes
- **User-Friendly Dialogs**: Shows rationale dialogs before requesting permissions
- **Settings Redirect**: Automatically redirects to app settings when permissions are permanently denied
- **Granular Control**: Check individual or combined permissions

#### Key Methods:
```kotlin
// Check individual permissions
fun hasCameraPermission(): Boolean
fun hasStoragePermission(): Boolean
fun hasLocationPermission(): Boolean

// Check combined permissions
fun hasAllPermissions(includeLocation: Boolean, includeStorage: Boolean): Boolean

// Request permissions
fun requestCameraPermission()
fun requestAllPermissions(includeLocation: Boolean, includeStorage: Boolean)

// Utility methods
fun canTakePhoto(): Boolean
fun canSavePhoto(): Boolean
fun canAddLocation(): Boolean
```

### 2. Automatic Permission Handling in Camera Activities

Both `Camera2Activity` and `CameraActivity` now automatically:

1. **Check permissions on startup**
2. **Show permission UI when needed**
3. **Request appropriate permissions based on features**
4. **Handle permission results gracefully**

#### Permission Flow:
```
App Launch → Check Permissions → Show UI Based on Status
    ↓
If Missing → Show Permission Request UI → Request Permissions
    ↓
If Granted → Initialize Camera → Show Camera UI
If Denied → Show Permission Denied UI → Option to go to Settings
```

### 3. Enhanced UI Components

#### Permission Request UI
- **Clean Design**: Modern permission request interface
- **Clear Messaging**: Explains why permissions are needed
- **Action Buttons**: Easy-to-use grant permission button
- **Fallback Options**: Settings redirect for permanently denied permissions

#### Layout Updates:
- Added comprehensive permission request layout in `activity_camera2.xml`
- Integrated permission UI seamlessly with existing camera interface

### 4. Manifest Permissions

**Location**: `camera/src/main/AndroidManifest.xml`

Added all necessary permissions:
```xml
<!-- Camera permissions -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="true" />
<uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
<uses-feature android:name="android.hardware.camera.flash" android:required="false" />

<!-- Storage permissions for saving photos -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
                 android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
                 android:maxSdkVersion="32" />

<!-- Location permissions -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### 5. Enhanced CameraLauncher API

**Location**: `camera/src/main/java/com/dhwaniris/comera/CameraLauncher.kt`

#### New Features:
- **Permission Checking Methods**: Check permissions before launching camera
- **Unified API**: Single method with optional parameters for all features
- **Backwards Compatibility**: Maintains support for legacy CameraActivity

#### Usage Examples:
```kotlin
// Check permissions
if (CameraLauncher.hasCameraPermissions(context)) {
    // Launch camera
}

// Check all permissions
if (CameraLauncher.hasAllPermissions(context, includeLocation = true, includeStorage = true)) {
    // Launch with full features
}

// Launch camera with automatic permission handling
val intent = CameraLauncher.launchCamera2(
    context = this,
    outputUri = imageUri,
    addTimestamp = true,
    addLocation = true,  // Will automatically request location permission
    customText = "Custom watermark"
)
startActivityForResult(intent, REQUEST_CODE)
```

## Permission Handling Behavior

### 1. Camera Permission (Required)
- **Always Required**: Camera permission is mandatory for all camera operations
- **Automatic Request**: Requested automatically when camera is opened
- **Graceful Fallback**: Shows permission UI if not granted
- **Settings Redirect**: Redirects to settings if permanently denied

### 2. Storage Permission (Conditional)
- **Android 12 and below**: Required for saving photos
- **Android 13+**: Not required (uses MediaStore API)
- **Smart Detection**: Automatically handles version differences

### 3. Location Permission (Optional)
- **Feature-Based**: Only requested when location features are enabled
- **Graceful Degradation**: App works without location if not granted
- **User Choice**: Clear explanation of why location is needed

## Integration Examples

### Basic Camera Launch
```kotlin
// Minimal setup - only camera permission required
val intent = CameraLauncher.launchCamera2(context, outputUri)
startActivityForResult(intent, REQUEST_CODE)
```

### Full Featured Camera
```kotlin
// All features enabled - will request all necessary permissions
val intent = CameraLauncher.launchCamera2(
    context = this,
    outputUri = outputUri,
    addTimestamp = true,
    addLocation = true,
    customText = "My App Photo",
    objectShape = CameraLauncher.RECTANGLE_SHAPE
)
startActivityForResult(intent, REQUEST_CODE)
```

### Manual Permission Checking
```kotlin
// Check permissions before launching
if (!CameraLauncher.hasCameraPermissions(this)) {
    CameraLauncher.requestCameraPermission(this)
    return
}

// Check all permissions
if (!CameraLauncher.hasAllPermissions(this, includeLocation = true)) {
    CameraLauncher.requestAllPermissions(this, includeLocation = true)
    return
}

// Launch camera
val intent = CameraLauncher.launchCamera2(context, outputUri)
startActivityForResult(intent, REQUEST_CODE)
```

## Migration from Fotoapparat

The permission handling is designed to be similar to Fotoapparat's approach:

### Similarities:
1. **Automatic Permission Requests**: Just like Fotoapparat, permissions are requested automatically
2. **Graceful Handling**: Smooth user experience with clear messaging
3. **No Manual Setup Required**: Works out of the box with minimal configuration

### Improvements:
1. **Modern Android Support**: Handles Android 13+ permission changes
2. **Better UI**: More polished permission request interface
3. **Granular Control**: More control over which permissions to request
4. **Settings Integration**: Automatic redirect to settings for denied permissions

## Testing

The implementation has been tested with:
- ✅ **Build Success**: All compilation errors resolved
- ✅ **Permission Flow**: Automatic permission requests work correctly
- ✅ **UI Integration**: Permission UI integrates seamlessly
- ✅ **Backwards Compatibility**: Legacy CameraActivity still works
- ✅ **API Consistency**: CameraLauncher API is consistent and easy to use

## Best Practices

1. **Always Use CameraLauncher**: Use the provided CameraLauncher for consistent behavior
2. **Handle Permission Results**: Implement onRequestPermissionsResult in your activities
3. **Check Permissions**: Use the provided permission checking methods
4. **Graceful Degradation**: Design your app to work with minimal permissions
5. **Clear Messaging**: Explain to users why permissions are needed

## Conclusion

The camera permission implementation provides a robust, user-friendly, and modern approach to handling camera permissions, similar to Fotoapparat but with improvements for current Android versions. The implementation automatically handles permission requests, provides clear user feedback, and gracefully handles all permission scenarios. 