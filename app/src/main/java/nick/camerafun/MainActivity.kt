package nick.camerafun

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.createGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.fragment

// todo: tap to focus: https://medium.com/androiddevelopers/whats-new-in-camerax-fb8568d6ddc
//       pinch to zoom ^
//       zoom slider ^
// todo: scan barcodes (QR?): https://developers.google.com/ml-kit/vision/barcode-scanning/android
class MainActivity : AppCompatActivity(R.layout.main_activity) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNavGraph()
    }

    private fun createNavGraph() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostContainer) as NavHostFragment

        navHostFragment.navController.apply {
            graph = createGraph(
                id = Navigation.id,
                startDestination = CameraPermissionFragment.Navigation.Destination.id
            ) {
                fragment<CameraPermissionFragment>(CameraPermissionFragment.Navigation.Destination.id) {
                    action(CameraPermissionFragment.Navigation.Action.permissionGranted) {
                        destinationId = CameraFragment.Navigation.Destination.id
                        navOptions {
                            popUpTo(CameraPermissionFragment.Navigation.Destination.id) {
                                inclusive = true
                            }
                        }
                    }
                }
                fragment<CameraFragment>(CameraFragment.Navigation.Destination.id)
            }
        }
    }

    object Navigation {
        val id = IdGenerator.next()
    }
}