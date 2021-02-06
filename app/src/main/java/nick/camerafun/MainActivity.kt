package nick.camerafun

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import nick.camerafun.databinding.MainActivityBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity() {
    lateinit var binding: MainActivityBinding
    var imageCapture: ImageCapture? = null

    val outputDirectory: File by lazy {
        val mediaDirectory = externalMediaDirs.firstOrNull()?.let {
            File(it, getString(R.string.app_name)).apply { mkdirs() }
        }
        if (mediaDirectory?.exists() == true) {
            mediaDirectory
        } else {
            filesDir
        }
    }

    // todo: how to not use an Executor?
    val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            lifecycleScope.launch { startCamera() }
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.cameraCaptureButton.setOnClickListener {
            lifecycleScope.launch { takePhoto() }
        }
    }

    private suspend fun startCamera() {
        val cameraProvider = getCameraProvider(this)
        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }

        imageCapture = ImageCapture.Builder().build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .build()
            .apply {
                setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                    Log.d(TAG, "Average luminosity: $luma")
                })
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture,
                imageAnalyzer
            )
        } catch (throwable: Throwable) {
            Log.d(TAG, "Use case binding failed", throwable)
        }
    }

    private suspend fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val date = SimpleDateFormat(FILENAME_FORMAT, Locale.CANADA)
            .format(System.currentTimeMillis())
        val photoFile = File(
            outputDirectory,
            "$date.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        try {
            val results = imageCapture.takePicture(this, outputOptions)
            val savedUri = Uri.fromFile(photoFile)
            val msg = "Photo capture succeeded: $savedUri"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            Log.d(TAG, msg)
        } catch (throwable: Throwable) {
            Log.d(TAG, "Photo capture failed: ${throwable.message}", throwable)
        }
    }

    private suspend fun ImageCapture.takePicture(
        context: Context,
        outputFileOptions: ImageCapture.OutputFileOptions
    ): ImageCapture.OutputFileResults = suspendCoroutine { continuation ->
        val callback = object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                continuation.resume(outputFileResults)
            }

            override fun onError(exception: ImageCaptureException) {
                continuation.resumeWithException(exception)
            }
        }

        takePicture(outputFileOptions, ContextCompat.getMainExecutor(context), callback)
    }

    private suspend fun getCameraProvider(context: Context): ProcessCameraProvider = suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            continuation.resume(future.get())
        }, ContextCompat.getMainExecutor(context))

        continuation.invokeOnCancellation {
            future.cancel(false)
        }
    }

    fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_CODE_PERMISSIONS) {
            return
        }

        if (allPermissionsGranted()) {
            lifecycleScope.launch { startCamera() }
        } else {
            Toast.makeText(this, "Permissions were not granted :(", Toast.LENGTH_LONG)
                .show()
            finish()
        }
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}