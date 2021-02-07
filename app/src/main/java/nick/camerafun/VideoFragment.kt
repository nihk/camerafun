package nick.camerafun

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import nick.camerafun.databinding.VideoFragmentBinding

class VideoFragment : Fragment(R.layout.video_fragment) {

    private val uri: Uri get() = arguments?.getParcelable(KEY_URI)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = VideoFragmentBinding.bind(view)
        binding.playerView.player = SimpleExoPlayer.Builder(requireContext()).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            play()
        }
    }

    object Navigation {
        object Destination {
            val id = IdGenerator.next()
        }
    }
}