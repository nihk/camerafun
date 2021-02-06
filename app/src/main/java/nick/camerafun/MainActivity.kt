package nick.camerafun

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import nick.camerafun.databinding.MainActivityBinding
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity() {
    lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            lifecycleScope.launch { startCamera() }
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.cameraCaptureButton.setOnClickListener { takePhoto() }
    }

    private fun takePhoto() {

    }

    private suspend fun startCamera() {
        val cameraProvider = getCameraProvider(this)
        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview)
        } catch (throwable: Throwable) {
            Log.d(TAG, "Use case binding failed", throwable)
        }
    }

    private suspend fun getCameraProvider(context: Context): ProcessCameraProvider = suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            continuation.resume(future.get())
        }, ContextCompat.getMainExecutor(context))

        continuation.invokeOnCancellation {
            future.cancel(true)
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

    fun getOutputFileDirectory(): File {
        val mediaDirectory = externalMediaDirs.firstOrNull()?.let {
            File(it, getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDirectory?.exists() == true) {
            mediaDirectory
        } else {
            filesDir
        }
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}