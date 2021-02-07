package nick.camerafun

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.SeekBar
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.barcode.Barcode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import nick.camerafun.databinding.CameraFragmentBinding
import kotlin.math.roundToInt

class CameraFragment : Fragment(R.layout.camera_fragment) {

    lateinit var binding: CameraFragmentBinding
    lateinit var viewModel: CameraViewModel
    var imageCapture: ImageCapture? = null
    var videoCapture: VideoCapture? = null
    var camera: Camera? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = CameraFragmentBinding.bind(view)

        val factory = CameraViewModel.Factory(requireContext().applicationContext)
        viewModel = ViewModelProvider(this, factory).get(CameraViewModel::class.java)
        viewModel.cameraProvider().onEach { cameraProvider ->
            bindToCamera(cameraProvider)
            enableGestures()
            enableZoomSlider()
            binding.imageCapture.setOnClickListener {
                takePicture()
            }
            binding.videoCapture.setOnClickListener {
                if (binding.videoCapture.text == getString(R.string.start_recording)) {
                    recordVideo()
                } else {
                    stopRecording()
                }
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        binding.viewFinder.previewStreamState.observe(viewLifecycleOwner) { state: PreviewView.StreamState? ->
            Log.d(TAG, "StreamState == $state")
            binding.progressBar.visibility = if (state == PreviewView.StreamState.IDLE) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        viewModel.directions().onEach { directions ->
            binding.viewContent.apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    val args = bundleOf(KEY_URI to directions.uri)
                    findNavController().navigate(directions.destination, args)
                }
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun bindToCamera(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(binding.viewFinder.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder().build()

        val qrCodeAnalysis = ImageAnalysis.Builder().build().apply {
            setAnalyzer(Dispatchers.Default.asExecutor(), QrCodeAnalyzer { barcodes ->
                handleBarcodes(barcodes)
            })
        }

        videoCapture = VideoCapture.Builder().build()

        try {
            cameraProvider.unbindAll()
            // N.B. the max allowed UseCases that can be passed into this method is 3
            camera = cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture,
                videoCapture
            )
        } catch (throwable: Throwable) {
            Log.d(TAG, "Use case binding failed", throwable)
        }
    }

    private fun handleBarcodes(barcodes: List<Barcode>) {
        if (barcodes.isNotEmpty()) {
            binding.qrCard.visibility = View.VISIBLE
        }

        barcodes.forEach { barcode ->
            binding.qrCodeText.text = when (barcode.valueType) {
                Barcode.TYPE_URL -> barcode.url?.url
                else -> barcode.rawValue
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun enableGestures() {
        val pinchToZoom = getPinchToZoomDetector()

        binding.viewFinder.setOnTouchListener { view, event ->
            if (event.pointerCount > 1) {
                pinchToZoom.onTouchEvent(event)
            } else {
                tapToFocus(event)
            }
        }
    }

    private fun tapToFocus(event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> true
            MotionEvent.ACTION_UP -> {
                val point = binding.viewFinder.meteringPointFactory.createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(point).build()
                camera?.cameraControl?.startFocusAndMetering(action)
                true
            }
            else -> false
        }
    }

    private fun getPinchToZoomDetector(): ScaleGestureDetector {
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoomRatio = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 0f
                val delta = detector.scaleFactor
                val zoomRatio = currentZoomRatio * delta
                camera?.cameraControl?.setZoomRatio(zoomRatio)
                // Match seekbar progress with pinch-to-zoom progress.
                camera?.cameraInfo?.zoomState?.value?.linearZoom?.let { linearZoom ->
                    binding.zoomSlider.progress = (linearZoom * 100).roundToInt()
                }
                return true
            }
        }
        return ScaleGestureDetector(requireContext(), listener)
    }

    private fun enableZoomSlider() {
        binding.zoomSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    camera?.cameraControl?.setLinearZoom(progress / 100f)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun takePicture() {
        val imageCapture = imageCapture ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val savedUri = viewModel.takePicture(imageCapture)
            Log.d(TAG, "Saved pic to: $savedUri")
            val directions = Directions(PictureFragment.Navigation.Destination.id, savedUri)
            viewModel.setDirections(directions)
        }
    }

    private fun recordVideo() {
        val videoCapture = videoCapture ?: return
        binding.videoCapture.text = getString(R.string.stop_recording)
        viewLifecycleOwner.lifecycleScope.launch {
            val results = viewModel.startRecording(videoCapture)
            Log.d(TAG, results.savedUri.toString())
            val directions = Directions(VideoFragment.Navigation.Destination.id, results.savedUri!!)
            viewModel.setDirections(directions)

            // There's currently no "cancel recording" mechanism in CameraX, so this Job will always
            // finish and come here. Check whether the Job is active before accessing any leaked Context.
            ensureActive()

            binding.videoCapture.text = getString(R.string.start_recording)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun stopRecording() {
        val videoCapture = videoCapture ?: return
        videoCapture.stopRecording()
    }

    companion object {
        private const val TAG = "asdf"
    }

    object Navigation {
        object Destination {
            val id = IdGenerator.next()
        }
    }
}