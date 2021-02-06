package nick.camerafun

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
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
            viewLifecycleOwner.lifecycleScope.launch { takePhoto() }
        }

        binding.viewFinder.previewStreamState.observe(viewLifecycleOwner) { state: PreviewView.StreamState? ->
            Log.d(TAG, "StreamState == $state")
            binding.progressBar.visibility = if (state == PreviewView.StreamState.IDLE) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
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

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val results = viewModel.takePhoto(imageCapture)
            Log.d(TAG, "Saved pic to: ${results.savedUri}")
        }
    }

    companion object {
        private const val TAG = "asdf"
    }
}