package nick.camerafun

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.createGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.fragment

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
                startDestination = PermissionsFragment.Navigation.Destination.id
            ) {
                fragment<PermissionsFragment>(PermissionsFragment.Navigation.Destination.id) {
                    action(PermissionsFragment.Navigation.Action.granted) {
                        destinationId = CameraFragment.Navigation.Destination.id
                        navOptions {
                            popUpTo(PermissionsFragment.Navigation.Destination.id) {
                                inclusive = true
                            }
                        }
                    }
                }
                fragment<CameraFragment>(CameraFragment.Navigation.Destination.id)
                fragment<PictureFragment>(PictureFragment.Navigation.Destination.id)
                fragment<VideoFragment>(VideoFragment.Navigation.Destination.id)
            }
        }
    }

    object Navigation {
        val id = IdGenerator.next()
    }
}