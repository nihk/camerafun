package nick.camerafun

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class QrCodeAnalyzer(private val onBarcodesReceived: (List<Barcode>) -> Unit) : ImageAnalysis.Analyzer {
    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val image = imageProxy.image ?: return
        val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(inputImage).addOnSuccessListener { barcodes: List<Barcode> ->
            imageProxy.close()
            onBarcodesReceived(barcodes)
        }
    }
}