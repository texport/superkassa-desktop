package kz.mybrain.superkassa.scan

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer

/**
 * Разбор кадра камеры: штрихкоды товаров, коды маркировки и QR.
 *
 * Кадр камеры лежит так, как его снимает матрица, — боком к экрану:
 * штрихкод, горизонтальный в видоискателе телефона, в кадре стоит
 * вертикально, а линейные коды ZXing читает только строками. Поэтому
 * кадр разбирается и как есть, и повёрнутым. Код отдаётся один раз:
 * следующие кадры с тем же кодом дописали бы товар в чек повторно.
 *
 * @param onCode код прочитан — зовётся в потоке анализа.
 */
internal class CodeAnalyzer(private val onCode: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader().apply { setHints(HINTS) }
    private var done = false

    override fun analyze(image: ImageProxy) {
        image.use { frame ->
            if (done) return
            val code = decode(frame) ?: return
            done = true
            onCode(code)
        }
    }

    private fun decode(frame: ImageProxy): String? {
        val plane = frame.planes.first()
        val bytes = ByteArray(plane.buffer.remaining()).also { plane.buffer.get(it) }
        val width = frame.width
        val height = frame.height
        val straight = PlanarYUVLuminanceSource(bytes, plane.rowStride, height, 0, 0, width, height, false)
        return read(straight) ?: read(rotated(bytes, plane.rowStride, width, height))
    }

    private fun read(source: PlanarYUVLuminanceSource): String? = try {
        reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
    } catch (_: ReaderException) {
        null
    } finally {
        reader.reset()
    }

    /** Яркость кадра, повёрнутая на четверть оборота: столбцы становятся строками. */
    private fun rotated(bytes: ByteArray, stride: Int, width: Int, height: Int): PlanarYUVLuminanceSource {
        val turned = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) turned[x * height + height - 1 - y] = bytes[y * stride + x]
        }
        return PlanarYUVLuminanceSource(turned, height, width, 0, 0, height, width, false)
    }

    private companion object {
        /** Коды товаров, маркировки и QR; искать настойчиво — кадр бывает смазан. */
        val HINTS = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39,
                BarcodeFormat.ITF,
                BarcodeFormat.DATA_MATRIX,
                BarcodeFormat.QR_CODE
            ),
            DecodeHintType.TRY_HARDER to true
        )
    }
}
