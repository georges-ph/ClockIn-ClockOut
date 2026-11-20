package ga.jundbits.clock_in_clock_out.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import ga.jundbits.clock_in_clock_out.R
import ga.jundbits.clock_in_clock_out.data.Badge
import ga.jundbits.clock_in_clock_out.data.Profile
import java.io.File

object Share {

    private fun sharedDir(context: Context): File = File(context.cacheDir, "shared").apply { mkdirs() }

    /** Removes earlier files with the same prefix, so exports don't pile up in the cache. */
    private fun freshFile(context: Context, prefix: String, name: String): File {
        val dir = sharedDir(context)
        dir.listFiles { file -> file.name.startsWith(prefix) }?.forEach { it.delete() }
        return File(dir, name)
    }

    private fun uri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun send(context: Context, uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            // Without this, the receiving app can't open the file.
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    fun csv(context: Context, fileName: String, content: String) {
        val file = freshFile(context, "attendance_", fileName)
        file.writeText(content, Charsets.UTF_8)
        send(context, uri(context, file), "text/csv")
    }

    /** A printable badge image: name, department and ID above the QR code. */
    fun badge(context: Context, profile: Profile) {
        val width = 1080
        val height = 1440
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val typeface = ResourcesCompat.getFont(context, R.font.manrope) ?: Typeface.DEFAULT
        val strip = Paint().apply { color = 0xFF1D4ED8.toInt() }
        canvas.drawRect(0f, 0f, width.toFloat(), 120f, strip)

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111827.toInt()
            textSize = 72f
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subtitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF5B6478.toInt()
            textSize = 44f
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(fit(profile.name, title, width - 120f), width / 2f, 270f, title)
        canvas.drawText(fit("${profile.department} · ${profile.id}", subtitle, width - 120f), width / 2f, 350f, subtitle)

        val qr = Qr.bitmap(Badge.encode(profile))
        val qrSize = 840
        val left = (width - qrSize) / 2
        val top = 440
        val scaled = Paint().apply { isFilterBitmap = false }
        canvas.drawBitmap(qr, null, Rect(left, top, left + qrSize, top + qrSize), scaled)

        val safeId = profile.id.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = freshFile(context, "badge_", "badge_$safeId.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        send(context, uri(context, file), "image/png")
    }

    private fun fit(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.take(end) + "…") > maxWidth) end--
        return text.take(end) + "…"
    }
}
