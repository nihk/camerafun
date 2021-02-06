package nick.camerafun

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

typealias LumaListener = (luma: Double) -> Unit

class LuminosityAnalyzer(
    private val listener: LumaListener
) : ImageAnalysis.Analyzer {

    override fun analyze(imageProxy: ImageProxy) {
        val luma = imageProxy.planes[0]
            .buffer
            .toByteArray()
            .map { it.toInt() and 0xFF }
            .average()

        listener(luma)

        imageProxy.close()
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }
}