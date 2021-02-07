package nick.camerafun

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import nick.camerafun.databinding.PictureFragmentBinding

class PictureFragment : Fragment(R.layout.picture_fragment) {

    private val uri: Uri get() = arguments?.getParcelable(KEY_URI)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = PictureFragmentBinding.bind(view)
        binding.picture.setImageURI(uri)
    }

    object Navigation {
        object Destination {
            val id = IdGenerator.next()
        }
    }
}