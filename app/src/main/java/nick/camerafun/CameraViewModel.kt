package nick.camerafun

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CameraViewModel(
    private val appContext: Context,
    private val mainExecutor: Executor = Dispatchers.Main.asExecutor()
) : ViewModel() {

    private val cameraProvider = MutableLiveData<ProcessCameraProvider>()
    fun cameraProvider(): LiveData<ProcessCameraProvider> = cameraProvider

    private val outputDirectory: File by lazy {
        val mediaDirectory = appContext.externalMediaDirs.firstOrNull()?.let {
            File(it, appContext.getString(R.string.app_name))
                .apply { mkdirs() }
        }
        if (mediaDirectory?.exists() == true) {
            mediaDirectory
        } else {
            appContext.filesDir
        }
    }

    init {
        viewModelScope.launch {
            cameraProvider.value = getCameraProvider()
        }
    }

    private suspend fun getCameraProvider(): ProcessCameraProvider = suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(appContext)
        future.addListener({
            continuation.resume(future.get())
        }, mainExecutor)

        continuation.invokeOnCancellation {
            future.cancel(false)
        }
    }

    suspend fun takePhoto(imageCapture: ImageCapture): ImageCapture.OutputFileResults {
        val date = SimpleDateFormat(FILENAME_FORMAT, Locale.CANADA)
            .format(System.currentTimeMillis())
        val photoFile = File(
            outputDirectory,
            "$date.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        return imageCapture.takePicture(outputOptions)
    }

    private suspend fun ImageCapture.takePicture(
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


        takePicture(outputFileOptions, mainExecutor, callback)
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return CameraViewModel(appContext) as T
        }
    }
}