package com.dhwaniris.comera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.internal.ConnectionCallbacks
import com.google.android.gms.common.api.internal.OnConnectionFailedListener
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import java.util.concurrent.Executors


const val LOCATION_PERMISSION_REQUEST_CODE = 1
const val LOCATION_REQUEST_LOCATION = 34

// Define a class that implements the LocationListener interface
class LocationUtils(private val context: AppCompatActivity,private val listener:LocationUtilsListener): LocationListener {

    // Declare a variable for the LocationManager
    private var locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    init{
        // Initialize the LocationManager

        // Check if the app has the location permission
//        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // Request the location permission
//            ActivityCompat.requestPermissions(context,
//                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                LOCATION_PERMISSION_REQUEST_CODE)
//        } else {
//            // Start receiving location updates
//            startLocationUpdates()
//        }
    }

    // Handle the result of the permission request
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start receiving location updates
                startLocationUpdates()
            } else {
                // Permission denied, show a message to the user
                // TODO: Handle the permission denial gracefully
            }
        }
    }

    // Start receiving location updates from the GPS provider
    fun startLocationUpdates() {
        // Check if the app has the location permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Register the location listener with the LocationManager

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                requestLocationUpdates()
            }else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
            }
        } else {
            // Request the location permission
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )

        }
    }

    private fun checkPlayServices(): Boolean {
        val googleAPI = GoogleApiAvailability.getInstance()
        val result = googleAPI.isGooglePlayServicesAvailable(context)
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(
                    context, result,1231
                )?.show()
            }
            return false
        }
        return true
    }
    var mGoogleApiClient:GoogleApiClient? = null
    class FailedListener: OnConnectionFailedListener, GoogleApiClient.OnConnectionFailedListener {
        override fun onConnectionFailed(p0: ConnectionResult) {
            Log.e("Location",p0.errorMessage?:p0.isSuccess.toString())
        }

    }
    private val failedListener = FailedListener()

    private val locationRequest: LocationRequest = LocationRequest().apply{
        priority = Priority.PRIORITY_HIGH_ACCURACY// the desired priority
        interval = 30_000// the desired interval in milliseconds
        isWaitForAccurateLocation = true
    }

        /*.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, // the desired priority
        30_000 // the desired interval in milliseconds
    ).apply{
        setWaitForAccurateLocation(true) // whether to wait for an accurate location
        setMaxUpdates(5_000) // the maximum number of location updates
    }.build()*/

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            if (locationResult == null) {
                return
            }
            Log.e("Location","$locationResult")
            if(locationResult.locations.isNotEmpty()){
                val location = locationResult.locations.minBy { it.accuracy }
                onLocationChanged(location)

            }
        }
    }

    @RequiresPermission(
        anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION]
    )
    private fun requestLocationUpdates(){
        if(checkPlayServices()){

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && locationManager.hasProvider(LocationManager.FUSED_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
                requestEnableGPSProvider(locationRequest)
            }else{
                requestEnableGPSProvider(locationRequest)
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        }else{
            //TODO: Navigate to location settings
        }
    }

    private fun requestEnableGPSProvider(locationRequest: LocationRequest) {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)

        if (mGoogleApiClient == null) {
            mGoogleApiClient = GoogleApiClient.Builder(context)
                .enableAutoManage(context, 0, failedListener)
                .addConnectionCallbacks(object: ConnectionCallbacks, GoogleApiClient.ConnectionCallbacks {
                    override fun onConnected(p0: Bundle?) {
                        Log.e("Location","onConnected")
                    }

                    override fun onConnectionSuspended(p0: Int) {
                        Log.e("Location","onConnectionSuspended")
                        mGoogleApiClient?.connect()
                    }

                })
                .addOnConnectionFailedListener(failedListener)
                .addApi(LocationServices.API)
                .build()
            mGoogleApiClient?.connect()
        }

        val result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient!!, builder.build())
        result.setResultCallback { result ->
            val status = result.status
            when (status.statusCode) {
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    status.startResolutionForResult(context, LOCATION_REQUEST_LOCATION)
                } catch (e: SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    // Stop receiving location updates
    fun stopLocationUpdates() {
        // Unregister the location listener from the LocationManager
        locationManager.removeUpdates(this)
        if(checkPlayServices()){
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    // Implement the LocationListener methods
    override fun onLocationChanged(location: Location) {
        listener.onLocationChanged(location)
        // This method is called when a new location is available
        // TODO: Do something with the location data
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        listener.onStatusChanged(provider, status, extras)
        // This method is called when the provider status changes
        // TODO: Handle the status change
    }

    override fun onProviderEnabled(provider: String) {
        listener.onProviderEnabled(provider)
        // This method is called when the provider is enabled
        // TODO: Handle the provider enablement
    }

    override fun onProviderDisabled(provider: String) {
        listener.onProviderDisabled(provider)
        // This method is called when the provider is disabled
        // TODO: Handle the provider disablement
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(LOCATION_REQUEST_LOCATION==requestCode){
            if(resultCode != Activity.RESULT_OK) {
                onProviderDisabled(LocationManager.FUSED_PROVIDER)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && locationManager.hasProvider(LocationManager.FUSED_PROVIDER) && !locationManager.isProviderEnabled(
                        LocationManager.FUSED_PROVIDER
                    )
                ) {
                    requestEnableGPSProvider(locationRequest)
                } else {
                    requestEnableGPSProvider(locationRequest)
                }
            }else{
                onProviderEnabled(LocationManager.FUSED_PROVIDER)
            }
        }
//        Log.e("Location","requestCode=$requestCode  resultCode=$resultCode")
    }
}

open class LocationUtilsListener{
    open fun onProviderDisabled(provider: String){}
    open fun onProviderEnabled(provider: String){}
    open fun onStatusChanged(provider: String?, status: Int, extras: Bundle?){}
    open fun onLocationChanged(location: Location){}
}