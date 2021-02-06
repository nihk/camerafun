package nick.camerafun

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class CameraPermissionFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hasCameraPermission()) {
            navigateToCamera()
        } else {
            val permissionRequest =
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                    if (isGranted) {
                        navigateToCamera()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Permissions were not granted :(",
                            Toast.LENGTH_LONG
                        ).show()
                        requireActivity().finish()
                    }
                }

            permissionRequest.launch(Manifest.permission.CAMERA)
        }
    }

    private fun navigateToCamera() {
        findNavController().navigate(Navigation.Action.permissionGranted)
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    object Navigation {
        object Destination {
            val id = IdGenerator.next()
        }
        object Action {
            val permissionGranted = IdGenerator.next()
        }
    }
}