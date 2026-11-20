package ga.jundbits.clock_in_clock_out.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object Qr {

    /**
     * Always black on white with a quiet zone, whatever the app theme: many
     * scanners can't read light-on-dark codes.
     */
    fun bitmap(content: String, modulePixels: Int = 1): Bitmap {
        val hints = mapOf(EncodeHintType.MARGIN to 2, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.CHARACTER_SET to "UTF-8")
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
        val width = matrix.width * modulePixels
        val height = matrix.height * modulePixels
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (matrix[x / modulePixels, y / modulePixels]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}

@Composable
fun QrCode(content: String, size: Dp, contentDescription: String, modifier: Modifier = Modifier) {
    val image = remember(content) { Qr.bitmap(content).asImageBitmap() }
    // Nearest-neighbour scaling keeps the modules sharp.
    Image(
        bitmap = image,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        filterQuality = FilterQuality.None,
    )
}
