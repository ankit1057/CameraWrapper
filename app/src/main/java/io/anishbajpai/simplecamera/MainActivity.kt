package io.anishbajpai.simplecamera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.dhwaniris.comera.Camera2Activity
import com.dhwaniris.comera.CameraLauncher
import com.dhwaniris.comera.MigrationTestActivity
import com.dhwaniris.comera.PerformanceMetrics
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val REQUEST_CAMERA2_BASIC = 101
private const val REQUEST_CAMERA2_FEATURES = 102
private const val REQUEST_CAMERA2_RECTANGLE = 103
private const val REQUEST_CAMERA2_CARD = 104
private const val REQUEST_CAMERA2_FRONT = 105
private const val REQUEST_MIGRATION_TEST = 106
private const val REQUEST_STORAGE_PERMISSION = 51

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // UI Components
    private val imageView by lazy { findViewById<ImageView>(R.id.iv_thumb) }
    private val tvResults by lazy { findViewById<TextView>(R.id.tv_results) }
    private val progressBar by lazy { findViewById<ProgressBar>(R.id.progress_bar) }
    private val switchTimestamp by lazy { findViewById<Switch>(R.id.switch_timestamp) }
    private val switchLocation by lazy { findViewById<Switch>(R.id.switch_location) }
    private val etCustomText by lazy { findViewById<EditText>(R.id.et_custom_text) }
    
    private var photoURI: Uri? = null
    private var performanceStartTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupUI()
        setupClickListeners()
        requestPermissions()
        
        // Initialize performance tracking
        PerformanceMetrics.clearMetrics()
        
        // Show expected improvements
        tvResults.text = PerformanceMetrics.getExpectedImprovements()
    }

    private fun setupUI() {
        title = "Camera2 Demo App"
        
        // Set default values
        switchTimestamp.isChecked = true
        switchLocation.isChecked = true
        etCustomText.setText("Camera2 Demo")
    }

    private fun setupClickListeners() {
        // Camera2 Basic Demo
        findViewById<Button>(R.id.btn_camera2_basic).setOnClickListener {
            launchCamera2Basic()
        }
        
        // Camera2 Full Features Demo
        findViewById<Button>(R.id.btn_camera2_features).setOnClickListener {
            launchCamera2FullFeatures()
        }
        
        // Camera2 Rectangle Shape Demo
        findViewById<Button>(R.id.btn_camera2_rectangle).setOnClickListener {
            launchCamera2Rectangle()
        }
        
        // Camera2 Card Shape Demo
        findViewById<Button>(R.id.btn_camera2_card).setOnClickListener {
            launchCamera2Card()
        }
        
        // Camera2 Front Camera Demo
        findViewById<Button>(R.id.btn_camera2_front).setOnClickListener {
            launchCamera2Front()
        }
        
        // Migration Test Activity
        findViewById<Button>(R.id.btn_migration_test).setOnClickListener {
            launchMigrationTest()
        }
        
        // Performance Report
        findViewById<Button>(R.id.btn_performance_report).setOnClickListener {
            showPerformanceReport()
        }
        
        // Clear Results
        findViewById<Button>(R.id.btn_clear_results).setOnClickListener {
            clearResults()
        }
    }

    private fun launchCamera2Basic() {
        updateResults("🚀 Launching Camera2 Basic...")
        performanceStartTime = PerformanceMetrics.startTracking("Camera2")
        
        val imageFile = createImageFile()
        if (imageFile != null) {
            photoURI = FileProvider.getUriForFile(
                this,
                "io.anishbajpai.simplecamera.fileprovider",
                imageFile
            )
            
            val intent = CameraLauncher.launchCamera2(
                context = this,
                outputUri = photoURI!!,
                addTimestamp = switchTimestamp.isChecked,
                addLocation = switchLocation.isChecked,
                customText = etCustomText.text.toString().takeIf { it.isNotEmpty() }
            )
            
            startActivityForResult(intent, REQUEST_CAMERA2_BASIC)
        }
    }

    private fun launchCamera2FullFeatures() {
        updateResults("🎯 Launching Camera2 Full Features...")
        performanceStartTime = PerformanceMetrics.startTracking("Camera2")
        
        val imageFile = createImageFile()
        if (imageFile != null) {
            photoURI = FileProvider.getUriForFile(
                this,
                "io.anishbajpai.simplecamera.fileprovider",
                imageFile
            )
            
            val intent = CameraLauncher.launchCamera2(
                context = this,
                outputUri = photoURI!!,
                addTimestamp = true,
                addLocation = true,
                customText = etCustomText.text.toString().takeIf { it.isNotEmpty() } ?: "Full Features Demo"
            )
            
            startActivityForResult(intent, REQUEST_CAMERA2_FEATURES)
        }
    }

    private fun launchCamera2Rectangle() {
        updateResults("📐 Launching Camera2 Rectangle Shape...")
        performanceStartTime = PerformanceMetrics.startTracking("Camera2")
        
        val imageFile = createImageFile()
        if (imageFile != null) {
            photoURI = FileProvider.getUriForFile(
                this,
                "io.anishbajpai.simplecamera.fileprovider",
                imageFile
            )
            
            val intent = CameraLauncher.launchCamera2(
                context = this,
                outputUri = photoURI!!,
                objectShape = CameraLauncher.RECTANGLE_SHAPE,
                addTimestamp = switchTimestamp.isChecked,
                addLocation = switchLocation.isChecked,
                customText = etCustomText.text.toString().takeIf { it.isNotEmpty() }
            )
            
            startActivityForResult(intent, REQUEST_CAMERA2_RECTANGLE)
        }
    }

    private fun launchCamera2Card() {
        updateResults("💳 Launching Camera2 Card Shape...")
        performanceStartTime = PerformanceMetrics.startTracking("Camera2")
        
        val imageFile = createImageFile()
        if (imageFile != null) {
            photoURI = FileProvider.getUriForFile(
                this,
                "io.anishbajpai.simplecamera.fileprovider",
                imageFile
            )
            
            val intent = CameraLauncher.launchCamera2(
                context = this,
                outputUri = photoURI!!,
                objectShape = CameraLauncher.CARD_SHAPE,
                addTimestamp = switchTimestamp.isChecked,
                addLocation = switchLocation.isChecked,
                customText = etCustomText.text.toString().takeIf { it.isNotEmpty() }
            )
            
            startActivityForResult(intent, REQUEST_CAMERA2_CARD)
        }
    }

    private fun launchCamera2Front() {
        updateResults("🤳 Launching Camera2 Front Camera...")
        performanceStartTime = PerformanceMetrics.startTracking("Camera2")
        
        val imageFile = createImageFile()
        if (imageFile != null) {
            photoURI = FileProvider.getUriForFile(
                this,
                "io.anishbajpai.simplecamera.fileprovider",
                imageFile
            )
            
            val intent = CameraLauncher.launchCamera2(
                context = this,
                outputUri = photoURI!!,
                addTimestamp = switchTimestamp.isChecked,
                addLocation = switchLocation.isChecked,
                customText = etCustomText.text.toString().takeIf { it.isNotEmpty() },
                cameraOrientation = CameraLauncher.CAMERA_FRONT
            )
            
            startActivityForResult(intent, REQUEST_CAMERA2_FRONT)
        }
    }

    private fun launchMigrationTest() {
        updateResults("🧪 Launching Migration Test Activity...")
        val intent = Intent(this, MigrationTestActivity::class.java)
        startActivityForResult(intent, REQUEST_MIGRATION_TEST)
    }

    private fun showPerformanceReport() {
        val report = PerformanceMetrics.comparePerformance()
        updateResults(report)
        PerformanceMetrics.logAllMetrics()
    }

    private fun clearResults() {
        tvResults.text = PerformanceMetrics.getExpectedImprovements()
        imageView.setImageResource(android.R.color.transparent)
        PerformanceMetrics.clearMetrics()
    }

    private fun updateResults(message: String) {
        tvResults.text = message
        Log.i(TAG, message)
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            REQUEST_STORAGE_PERMISSION
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // Record performance metrics
        if (performanceStartTime > 0) {
            PerformanceMetrics.recordTotal("Camera2", performanceStartTime)
            performanceStartTime = 0
        }
        
        when (requestCode) {
            REQUEST_CAMERA2_BASIC,
            REQUEST_CAMERA2_FEATURES,
            REQUEST_CAMERA2_RECTANGLE,
            REQUEST_CAMERA2_CARD,
            REQUEST_CAMERA2_FRONT -> {
                if (resultCode == Activity.RESULT_OK) {
                    updateResults("✅ Camera2 operation completed successfully!")
                    displayCapturedImage()
                } else {
                    updateResults("❌ Camera2 operation was cancelled")
                }
            }
            REQUEST_MIGRATION_TEST -> {
                if (resultCode == Activity.RESULT_OK) {
                    updateResults("✅ Migration test completed")
                } else {
                    updateResults("ℹ️ Migration test finished")
                }
            }
        }
    }

    private fun displayCapturedImage() {
        photoURI?.let { uri ->
            try {
                Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(imageView)
                updateResults(tvResults.text.toString() + "\n📸 Image loaded and displayed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error displaying image", e)
                updateResults(tvResults.text.toString() + "\n❌ Error displaying image: ${e.message}")
            }
        }
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "CAMERA2_DEMO_${timeStamp}"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile(imageFileName, ".jpg", storageDir)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating image file", e)
            null
        }
    }
}
