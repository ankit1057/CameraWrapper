package com.dhwaniris.comera

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.View
import androidx.annotation.RequiresApi

private const val REQUEST_CODE_CAMERA = 10
private const val REQUEST_CODE_STORAGE = 11
private const val REQUEST_CODE_LOCATION = 12
private const val REQUEST_CODE_ALL_PERMISSIONS = 13

const val CAMERA_BACK = "CAMERA_BACK"
const val CAMERA_FRONT = "CAMERA_FRONT"
const val CAMERA_ORIENTATION = "CAMERA_ORIENTATION"

internal class PermissionsDelegate(private val activity: Activity) {

    // Check individual permissions
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ doesn't need WRITE_EXTERNAL_STORAGE for MediaStore
            true
        } else {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Check if all required permissions are granted
    fun hasAllRequiredPermissions(): Boolean {
        return hasCameraPermission()
    }

    // Check if all permissions (including optional ones) are granted
    fun hasAllPermissions(includeLocation: Boolean = false, includeStorage: Boolean = false): Boolean {
        val cameraGranted = hasCameraPermission()
        val storageGranted = if (includeStorage) hasStoragePermission() else true
        val locationGranted = if (includeLocation) hasLocationPermission() else true
        
        return cameraGranted && storageGranted && locationGranted
    }

    // Request individual permissions
    fun requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
            showPermissionRationaleDialog(
                "Camera Permission Required",
                "This app needs camera permission to take photos. Please grant the permission to continue.",
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CODE_CAMERA
            )
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CODE_CAMERA
            )
        }
    }

    fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showPermissionRationaleDialog(
                    "Storage Permission Required",
                    "This app needs storage permission to save photos. Please grant the permission to continue.",
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_STORAGE
                )
            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_STORAGE
                )
            }
        }
    }

    fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            showPermissionRationaleDialog(
                "Location Permission Required",
                "This app needs location permission to add location information to photos. Please grant the permission to continue.",
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_CODE_LOCATION
            )
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_CODE_LOCATION
            )
        }
    }

    // Request all permissions at once
    fun requestAllPermissions(includeLocation: Boolean = false, includeStorage: Boolean = false) {
        val permissions = mutableListOf<String>()
        
        if (!hasCameraPermission()) {
            permissions.add(Manifest.permission.CAMERA)
        }
        
        if (includeStorage && !hasStoragePermission() && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        if (includeLocation && !hasLocationPermission()) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissions.toTypedArray(),
                REQUEST_CODE_ALL_PERMISSIONS
            )
        }
    }

    // Handle permission results
    fun resultGranted(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (grantResults.isEmpty()) {
            return false
        }

        val noPermissionView = activity.findViewById<View>(R.id.no_permission)

        when (requestCode) {
            REQUEST_CODE_CAMERA -> {
                if (permissions.contains(Manifest.permission.CAMERA) && 
                    grantResults[permissions.indexOf(Manifest.permission.CAMERA)] == PackageManager.PERMISSION_GRANTED) {
                    noPermissionView?.visibility = View.GONE
                    return true
                } else {
                    handlePermissionDenied(Manifest.permission.CAMERA)
                    return false
                }
            }
            REQUEST_CODE_STORAGE -> {
                val storageGranted = permissions.indices.all { i ->
                    grantResults[i] == PackageManager.PERMISSION_GRANTED
                }
                return storageGranted
            }
            REQUEST_CODE_LOCATION -> {
                val locationGranted = permissions.indices.any { i ->
                    permissions[i] in arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED
                }
                return locationGranted
            }
            REQUEST_CODE_ALL_PERMISSIONS -> {
                var allGranted = true
                var cameraGranted = false
                
                for (i in permissions.indices) {
                    when (permissions[i]) {
                        Manifest.permission.CAMERA -> {
                            cameraGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED
                            if (!cameraGranted) allGranted = false
                        }
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE -> {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                // Storage permission is optional, don't fail
                            }
                        }
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION -> {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                // Location permission is optional, don't fail
                            }
                        }
                    }
                }
                
                if (cameraGranted) {
                    noPermissionView?.visibility = View.GONE
                } else {
                    handlePermissionDenied(Manifest.permission.CAMERA)
                }
                
                return cameraGranted
            }
        }
        
        return false
    }

    private fun showPermissionRationaleDialog(
        title: String,
        message: String,
        permissions: Array<String>,
        requestCode: Int
    ) {
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Grant Permission") { _, _ ->
                ActivityCompat.requestPermissions(activity, permissions, requestCode)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                activity.finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun handlePermissionDenied(permission: String) {
        val noPermissionView = activity.findViewById<View>(R.id.no_permission)
        noPermissionView?.visibility = View.VISIBLE
        
        if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            // Permission permanently denied, show dialog to go to settings
            showGoToSettingsDialog()
        } else {
            // Show rationale and request again
            when (permission) {
                Manifest.permission.CAMERA -> requestCameraPermission()
                Manifest.permission.WRITE_EXTERNAL_STORAGE -> requestStoragePermission()
                Manifest.permission.ACCESS_FINE_LOCATION -> requestLocationPermission()
            }
        }
    }

    private fun showGoToSettingsDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Permission Required")
            .setMessage("Camera permission is required to use this feature. Please enable it in the app settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity.packageName, null)
                }
                activity.startActivity(intent)
                activity.finish()
            }
            .setNegativeButton("Cancel") { _, _ ->
                activity.finish()
            }
            .setCancelable(false)
            .show()
    }

    // Utility methods for checking specific permission combinations
    fun canTakePhoto(): Boolean = hasCameraPermission()
    
    fun canSavePhoto(): Boolean = hasStoragePermission()
    
    fun canAddLocation(): Boolean = hasLocationPermission()
}