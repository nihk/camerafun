package nick.camerafun

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.*
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
    private val mainExecutor: Executor
) : ViewModel() {

    private val cameraProvider = MutableStateFlow<ProcessCameraProvider?>(null)
    fun cameraProvider(): Flow<ProcessCameraProvider> = cameraProvider.filterNotNull()

    private val directions = MutableSharedFlow<Directions>()
    fun directions(): SharedFlow<Directions> = directions

    // Adapted from: https://codelabs.developers.google.com/codelabs/camerax-getting-started#1
    // TODO: Use MediaStore, instead.
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

    suspend fun setDirections(directions: Directions) {
        this.directions.emit(directions)
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

    suspend fun takePicture(imageCapture: ImageCapture): Uri {
        val file = createFile("jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        // The URI in these results will be null unless the the CameraX API created the File; use the File
        // created earlier to reference the picture location, instead.
        // See: https://github.com/android/camera-samples/issues/234#issuecomment-629536782
        val results = imageCapture.takePicture(outputOptions)
        return Uri.fromFile(file)
    }

    private fun createFile(extension: String): File {
        val date = SimpleDateFormat(FILENAME_FORMAT, Locale.CANADA)
            .format(System.currentTimeMillis())
        return File(
            outputDirectory,
            "$date.$extension"
        )
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

    suspend fun startRecording(videoCapture: VideoCapture): VideoCapture.OutputFileResults {
        val file = createFile("mp4")
        val outputOptions = VideoCapture.OutputFileOptions.Builder(file).build()
        return videoCapture.startRecording(outputOptions)
    }

    @SuppressLint("RestrictedApi")
    private suspend fun VideoCapture.startRecording(
        outputFileOptions: VideoCapture.OutputFileOptions
    ): VideoCapture.OutputFileResults = suspendCoroutine { continuation ->
        val callback = object : VideoCapture.OnVideoSavedCallback {
            override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                continuation.resume(outputFileResults)
            }

            override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                val exception = VideoCaptureException(videoCaptureError, message, cause)
                continuation.resumeWithException(exception)
            }
        }

        startRecording(outputFileOptions, mainExecutor, callback)
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    class Factory(
        private val appContext: Context,
        private val mainExecutor: Executor = Dispatchers.Main.asExecutor()
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return CameraViewModel(appContext, mainExecutor) as T
        }
    }
}

class VideoCaptureException(
    val error: Int,
    message: String,
    cause: Throwable?
) : Exception(message, cause)