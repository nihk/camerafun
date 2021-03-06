package nick.camerafun

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class PermissionsFragment : Fragment() {
    private val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            navigateToCamera()
        } else {
            Toast.makeText(requireContext(), "Permissions were not granted :(", Toast.LENGTH_LONG)
                .show()
            requireActivity().finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hasPermissions()) {
            navigateToCamera()
        } else {
            if (savedInstanceState == null) {
                launcher.launch(REQUESTED_PERMISSIONS)
            }
        }
    }

    private fun navigateToCamera() {
        findNavController().navigate(Navigation.Action.granted)
    }

    private fun hasPermissions(): Boolean {
        return REQUESTED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    object Navigation {
        object Destination {
            val id = IdGenerator.next()
        }
        object Action {
            val granted = IdGenerator.next()
        }
    }

    companion object {
        private val REQUESTED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}