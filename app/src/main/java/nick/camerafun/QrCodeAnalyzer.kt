package nick.camerafun

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

class QrCodeAnalyzer(private val block: (Deferred<List<Barcode>>) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val image = imageProxy.image ?: return
        val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
        val task = scanner.process(inputImage)
        val deferred = CompletableDeferred<List<Barcode>>()

        task.addOnCompleteListener {
            imageProxy.close()

            if (task.exception == null) {
                if (task.isCanceled) {
                    deferred.cancel()
                } else {
                    deferred.complete(task.result)
                }
            } else {
                deferred.completeExceptionally(task.exception!!)
            }
        }

        block(deferred)
    }
}