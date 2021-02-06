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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import nick.camerafun.databinding.CameraFragmentBinding

@SuppressLint("LogNotTimber")
class CameraFragment : Fragment(R.layout.camera_fragment) {

    lateinit var binding: CameraFragmentBinding
    private lateinit var viewModel: CameraViewModel
    var imageCapture: ImageCapture? = null
    var camera: Camera? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = CameraFragmentBinding.bind(view)

        val factory = CameraViewModel.Factory(requireContext().applicationContext)
        viewModel = ViewModelProvider(this, factory).get(CameraViewModel::class.java)
        viewModel.cameraProvider().observe(viewLifecycleOwner) {
            bindToCamera(it)
            binding.cameraCaptureButton.isEnabled = true
        }

        binding.cameraCaptureButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch { takePicture() }
        }

        binding.viewFinder.previewStreamState.observe(viewLifecycleOwner) { state: PreviewView.StreamState? ->
            Log.d(TAG, "StreamState == $state")
            binding.progressBar.visibility = if (state == PreviewView.StreamState.IDLE) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        enableGestures()
        enableZoomSlider()
    }

    private fun bindToCamera(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(binding.viewFinder.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder().build()

        val imageAnalyzer = ImageAnalysis.Builder().build().apply {
            setAnalyzer(Dispatchers.Default.asExecutor(), LuminosityAnalyzer { luma ->
                Log.d(TAG, "Average luminosity: $luma")
            })
        }

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
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
                camera?.cameraControl?.setZoomRatio(currentZoomRatio * delta)
                // todo: set zoom slider as side effect
                return true
            }
        }
        return ScaleGestureDetector(requireContext(), listener)
    }

    private fun enableZoomSlider() {
        binding.zoomSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                camera?.cameraControl?.setLinearZoom(progress / 100f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun takePicture() {
        val imageCapture = imageCapture ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val results = viewModel.takePicture(imageCapture)
            Log.d(TAG, "Saved pic to: ${results.savedUri}")
        }
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