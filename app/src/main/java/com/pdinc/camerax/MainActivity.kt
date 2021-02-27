package com.pdinc.camerax

import android.Manifest
import android.content.pm.PackageManager
import android.media.CamcorderProfile
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.camera.core.CameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pdinc.camerax.databinding.ActivityMainBinding
import java.io.File
import java.lang.Exception
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor:ExecutorService
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        if (ActivityCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startCamera()
        }
        else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 1234)
        }
        binding.captureBtn.setOnClickListener { takePhoto() }
        outputDirectory=getOutputDirectory()
        cameraExecutor= Executors.newSingleThreadExecutor()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    companion object{
        private const val TAG="CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 1234
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private fun getOutputDirectory(): File {
val mediaDir=externalMediaDirs.firstOrNull()?.let {
    File(it,resources.getString(R.string.app_name).apply { mkdirs() })
}
        return if (mediaDir!=null && mediaDir.exists()) {mediaDir}
        else{
            filesDir
        }
    }


    private fun mkdirs() {

    }
    private fun takePhoto() {

    }

    private fun startCamera() {
val cameraProviderFuture=ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
val cameraProvider:ProcessCameraProvider=cameraProviderFuture.get()
            //Preview
            val preview=Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.previewview.surfaceProvider)
                    }
            // Select back camera as a default
val cameraSelector=CameraSelector.DEFAULT_BACK_CAMERA
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this,cameraSelector,preview)
            }catch (e:Exception){
                Log.e(TAG,"Use case Binding failed",e)
            }
        },ContextCompat.getMainExecutor(this))

    }
}