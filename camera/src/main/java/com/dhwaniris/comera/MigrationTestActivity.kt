package com.dhwaniris.comera

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
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * MigrationTestActivity provides a test interface to compare 
 * the legacy Fotoapparat implementation with the new Camera2 implementation
 */
class MigrationTestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MigrationTest"
        private const val REQUEST_LEGACY_CAMERA = 1001
        private const val REQUEST_CAMERA2 = 1002
        private const val REQUEST_CAMERA2_FEATURES = 1003
    }

    private lateinit var btnLegacyCamera: Button
    private lateinit var btnCamera2: Button
    private lateinit var btnCamera2Features: Button
    private lateinit var btnCamera2Rectangle: Button
    private lateinit var btnCamera2Card: Button
    private lateinit var btnCamera2Front: Button
    private lateinit var tvResults: TextView
    private lateinit var ivResult: ImageView
    private lateinit var switchTimestamp: Switch
    private lateinit var switchLocation: Switch
    private lateinit var etCustomText: EditText

    private var currentImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createLayout())
        
        setupViews()
        setupClickListeners()
    }

    private fun createLayout(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            
            // Title
            addView(TextView(this@MigrationTestActivity).apply {
                text = "Camera Migration Test"
                textSize = 24f
                setPadding(0, 0, 0, 32)
            })
            
            // Settings section
            addView(TextView(this@MigrationTestActivity).apply {
                text = "Test Settings:"
                textSize = 18f
                setPadding(0, 0, 0, 16)
            })
            
            switchTimestamp = Switch(this@MigrationTestActivity).apply {
                text = "Add Timestamp"
                isChecked = true
            }
            addView(switchTimestamp)
            
            switchLocation = Switch(this@MigrationTestActivity).apply {
                text = "Add Location"
                isChecked = true
            }
            addView(switchLocation)
            
            etCustomText = EditText(this@MigrationTestActivity).apply {
                hint = "Custom watermark text"
                setText("Migration Test")
            }
            addView(etCustomText)
            
            // Buttons section
            addView(TextView(this@MigrationTestActivity).apply {
                text = "Camera Tests:"
                textSize = 18f
                setPadding(0, 32, 0, 16)
            })
            
            btnLegacyCamera = Button(this@MigrationTestActivity).apply {
                text = "Test Legacy Camera (Fotoapparat)"
            }
            addView(btnLegacyCamera)
            
            btnCamera2 = Button(this@MigrationTestActivity).apply {
                text = "Test Camera2 Basic"
            }
            addView(btnCamera2)
            
            btnCamera2Features = Button(this@MigrationTestActivity).apply {
                text = "Test Camera2 Full Features"
            }
            addView(btnCamera2Features)
            
            btnCamera2Rectangle = Button(this@MigrationTestActivity).apply {
                text = "Test Camera2 Rectangle Shape"
            }
            addView(btnCamera2Rectangle)
            
            btnCamera2Card = Button(this@MigrationTestActivity).apply {
                text = "Test Camera2 Card Shape"
            }
            addView(btnCamera2Card)
            
            btnCamera2Front = Button(this@MigrationTestActivity).apply {
                text = "Test Camera2 Front Camera"
            }
            addView(btnCamera2Front)
            
            // Results section
            addView(TextView(this@MigrationTestActivity).apply {
                text = "Results:"
                textSize = 18f
                setPadding(0, 32, 0, 16)
            })
            
            tvResults = TextView(this@MigrationTestActivity).apply {
                text = "No tests run yet"
                setPadding(0, 0, 0, 16)
            }
            addView(tvResults)
            
            ivResult = ImageView(this@MigrationTestActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = LinearLayout.LayoutParams(400, 400)
                setBackgroundColor(android.graphics.Color.LTGRAY)
            }
            addView(ivResult)
        }
    }

    private fun setupViews() {
        title = "Camera Migration Test"
    }

    private fun setupClickListeners() {
        btnLegacyCamera.setOnClickListener {
            testLegacyCamera()
        }
        
        btnCamera2.setOnClickListener {
            testCamera2Basic()
        }
        
        btnCamera2Features.setOnClickListener {
            testCamera2Features()
        }
        
        btnCamera2Rectangle.setOnClickListener {
            testCamera2Rectangle()
        }
        
        btnCamera2Card.setOnClickListener {
            testCamera2Card()
        }
        
        btnCamera2Front.setOnClickListener {
            testCamera2Front()
        }
    }

    private fun createImageUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "CAMERA_TEST_${timeStamp}.jpg"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File(storageDir, imageFileName)
        
        return FileProvider.getUriForFile(
            this,
            "io.anishbajpai.simplecamera.fileprovider",
            imageFile
        )
    }

    private fun testLegacyCamera() {
        try {
            currentImageUri = createImageUri()
            val intent = CameraLauncher.launchLegacyCamera(
                this,
                currentImageUri!!,
                addTimestamp = switchTimestamp.isChecked,
                addLocation = switchLocation.isChecked,
                customText = etCustomText.text.toString().takeIf { it.isNotEmpty() }
            )
            startActivityForResult(intent, REQUEST_LEGACY_CAMERA)
            updateResults("Testing Legacy Camera (Fotoapparat)...")
        } catch (e: Exception) {
            Log.e(TAG, "Error launching legacy camera", e)
            updateResults("Error: ${e.message}")
        }
    }

    private fun testCamera2Basic() {
        try {
            currentImageUri = createImageUri()
            val intent = CameraLauncher.launchCamera2(
                this,
                currentImageUri!!,
                addTimestamp = switchTimestamp.isChecked,
                addLocation = switchLocation.isChecked,
                customText = etCustomText.text.toString().takeIf { it.isNotEmpty() }
            )
            startActivityForResult(intent, REQUEST_CAMERA2)
            updateResults("Testing Camera2 Basic...")
        } catch (e: Exception) {
            Log.e(TAG, "Error launching Camera2", e)
            updateResults("Error: ${e.message}")
        }
    }

    private fun testCamera2Features() {
        try {
            currentImageUri = createImageUri()
            val intent = CameraLauncher.launchCamera2(
                this,
                currentImageUri!!,
                addTimestamp = true,
                addLocation = true,
                customText = etCustomText.text.toString().takeIf { it.isNotEmpty() } ?: "Full Features Test"
            )
            startActivityForResult(intent, REQUEST_CAMERA2_FEATURES)
            updateResults("Testing Camera2 Full Features...")
        } catch (e: Exception) {
            Log.e(TAG, "Error launching Camera2 features", e)
            updateResults("Error: ${e.message}")
        }
    }

    private fun testCamera2Rectangle() {
        try {
            currentImageUri = createImageUri()
            val intent = CameraLauncher.launchCamera2(
                this,
                currentImageUri!!,
                objectShape = CameraLauncher.RECTANGLE_SHAPE,
                addTimestamp = switchTimestamp.isChecked,
                addLocation = switchLocation.isChecked,
                customText = etCustomText.text.toString().takeIf { it.isNotEmpty() }
            )
            startActivityForResult(intent, REQUEST_CAMERA2)
            updateResults("Testing Camera2 Rectangle Shape...")
        } catch (e: Exception) {
            Log.e(TAG, "Error launching Camera2 rectangle", e)
            updateResults("Error: ${e.message}")
        }
    }

    private fun testCamera2Card() {
        try {
            currentImageUri = createImageUri()
            val intent = CameraLauncher.launchCamera2(
                this,
                currentImageUri!!,
                objectShape = CameraLauncher.CARD_SHAPE,
                addTimestamp = switchTimestamp.isChecked,
                addLocation = switchLocation.isChecked,
                customText = etCustomText.text.toString().takeIf { it.isNotEmpty() }
            )
            startActivityForResult(intent, REQUEST_CAMERA2)
            updateResults("Testing Camera2 Card Shape...")
        } catch (e: Exception) {
            Log.e(TAG, "Error launching Camera2 card", e)
            updateResults("Error: ${e.message}")
        }
    }

    private fun testCamera2Front() {
        try {
            currentImageUri = createImageUri()
            val intent = CameraLauncher.launchCamera2(
                this,
                currentImageUri!!,
                addTimestamp = switchTimestamp.isChecked,
                addLocation = switchLocation.isChecked,
                customText = etCustomText.text.toString().takeIf { it.isNotEmpty() },
                cameraOrientation = CameraLauncher.CAMERA_FRONT
            )
            startActivityForResult(intent, REQUEST_CAMERA2)
            updateResults("Testing Camera2 Front Camera...")
        } catch (e: Exception) {
            Log.e(TAG, "Error launching Camera2 front", e)
            updateResults("Error: ${e.message}")
        }
    }

    private fun updateResults(message: String) {
        tvResults.text = message
        Log.i(TAG, message)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_LEGACY_CAMERA -> {
                if (resultCode == Activity.RESULT_OK) {
                    updateResults("✅ Legacy Camera SUCCESS")
                    displayCapturedImage()
                } else {
                    updateResults("❌ Legacy Camera CANCELLED")
                }
            }
            REQUEST_CAMERA2, REQUEST_CAMERA2_FEATURES -> {
                if (resultCode == Activity.RESULT_OK) {
                    updateResults("✅ Camera2 SUCCESS")
                    displayCapturedImage()
                } else {
                    updateResults("❌ Camera2 CANCELLED")
                }
            }
        }
    }

    private fun displayCapturedImage() {
        currentImageUri?.let { uri ->
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                ivResult.setImageBitmap(bitmap)
                updateResults(tvResults.text.toString() + "\nImage captured and displayed")
            } catch (e: Exception) {
                Log.e(TAG, "Error displaying image", e)
                updateResults(tvResults.text.toString() + "\nError displaying image: ${e.message}")
            }
        }
    }
} 