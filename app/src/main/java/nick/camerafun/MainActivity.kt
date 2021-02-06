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
                id = AppNavGraph.id,
                startDestination = AppNavGraph.Destination.permission
            ) {
                fragment<CameraPermissionFragment>(AppNavGraph.Destination.permission) {
                    action(AppNavGraph.Action.permissionGranted) {
                        destinationId = AppNavGraph.Destination.camera
                        navOptions {
                            popUpTo(AppNavGraph.Destination.permission) {
                                inclusive = true
                            }
                        }
                    }
                }
                fragment<CameraFragment>(AppNavGraph.Destination.camera)
            }
        }
    }
}

object AppNavGraph {
    private var count = 1
    val id = count++

    object Destination {
        val permission = count++
        val camera = count++
    }

    object Action {
        val permissionGranted = count++
    }
}