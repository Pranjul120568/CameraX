package com.pdinc.camerax

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.pdinc.camerax.databinding.ActivityMainBinding
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var imageAnalysis: ImageAnalysis? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null
    private var imagePreview: Preview? = null
    private lateinit var outputDirectory: File

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
            )
        }
        outputDirectory = getOutputDirectory()

        binding.captureBtn.setOnClickListener {
            takePicture()
        }
    }
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun takePicture() {
        val file = createFile(
                outputDirectory,
                FILENAME,
                PHOTO_EXTENSION
        )
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture?.takePicture(outputFileOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val msg = "Photo capture succeeded: ${file.absolutePath}"
                binding.previewview.post {
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                val msg = "Photo capture failed: ${exception.message}"
                binding.previewview.post {
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                }
            }
        })
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        cameraProviderFuture.addListener({
            imageCapture = ImageCapture.Builder().apply {
                setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                setFlashMode(ImageCapture.FLASH_MODE_AUTO)
            }.build()
            imagePreview = Preview.Builder().apply {
                setTargetAspectRatio(AspectRatio.RATIO_16_9)
                setTargetRotation(binding.previewview.display.rotation)
                imageAnalysis = ImageAnalysis.Builder().apply {
                    setImageQueueDepth(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                }.build()
            }.build()
            imageAnalysis?.setAnalyzer(cameraExecutor, LuminosityAnalyzer())

            val cameraProvider = cameraProviderFuture.get()
            val camera = cameraProvider.bindToLifecycle(this, cameraSelector, imagePreview,imageAnalysis,imageCapture)
            binding.previewview.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            imagePreview?.setSurfaceProvider(binding.previewview.surfaceProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                finish()
            }
        }
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
    companion object {
        private const val TAG = "MainActivity"
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

        fun createFile(baseFolder: File, format: String, extension: String) =
                File(baseFolder, SimpleDateFormat(format, Locale.US)
                        .format(System.currentTimeMillis()) + extension)
    }
}
private class LuminosityAnalyzer : ImageAnalysis.Analyzer {
    private var lastAnalyzedTimestamp = 0L

    /**
     * Helper extension function used to extract a byte array from an
     * image plane buffer
     */
    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }
    override fun analyze(image: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        // Calculate the average luma no more often than every second
        if (currentTimestamp - lastAnalyzedTimestamp >=
                java.util.concurrent.TimeUnit.SECONDS.toMillis(1)) {
            // Since format in ImageAnalysis is YUV, image.planes[0]
            // contains the Y (luminance) plane
            val buffer = image.planes[0].buffer
            // Extract image data from callback object
            val data = buffer.toByteArray()
            // Convert the data into an array of pixel values
            val pixels = data.map { it.toInt() and 0xFF }
            // Compute average luminance for the image
            val luma = pixels.average()
            // Log the new luma value
            Log.d("CameraXApp", "Average luminosity: $luma")
            // Update timestamp of last analyzed frame
            lastAnalyzedTimestamp = currentTimestamp
        }
        image.close()
    }
    }