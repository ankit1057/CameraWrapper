package com.dhwaniris.comera

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.View

private const val REQUEST_CODE = 10

const val CAMERA_BACK = "CAMERA_BACK"
const val CAMERA_FRONT = "CAMERA_FRONT"
const val CAMERA_ORIENTATION = "CAMERA_ORIENTATION"

internal class PermissionsDelegate(private val activity: Activity) {

    fun hasCameraPermission(): Boolean {
        val permissionCheckResult = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        )
        return permissionCheckResult == PackageManager.PERMISSION_GRANTED
    }

    fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CODE
        )
    }

    fun resultGranted(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {

        if (requestCode != REQUEST_CODE) {
            return false
        }

        if (grantResults.isEmpty()) {
            return false
        }
        if (permissions[0] != Manifest.permission.CAMERA) {
            return false
        }

        val noPermissionView = activity.findViewById<View>(R.id.no_permission)

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            noPermissionView.visibility = View.GONE
            return true
        }

        requestCameraPermission()
        noPermissionView.visibility = View.VISIBLE
        return false
    }
}