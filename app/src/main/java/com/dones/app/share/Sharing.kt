package com.dones.app.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Demo: abre WhatsApp con los PDFs adjuntos y la persona toca Enviar.
 * Producción: el envío será automático con la API oficial de WhatsApp Business (desde el servidor).
 */
object Sharing {

    private val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")

    fun toWhatsApp(context: Context, files: List<File>, message: String) {
        require(files.isNotEmpty()) { "No hay documentos para enviar." }
        val uris = files.map { uriFor(context, it) }
        val base = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
        val intent = base.apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_TEXT, message)
            clipData = ClipData.newRawUri("", uris.first()).also { clip -> uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) } }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val whatsapp = whatsappPackages.firstOrNull { isInstalled(context, it) }
        if (whatsapp != null) {
            context.startActivity(intent.setPackage(whatsapp))
        } else {
            // Sin WhatsApp: se abre el menú normal de compartir de Android.
            context.startActivity(Intent.createChooser(intent, "Enviar mis documentos"))
        }
    }

    fun open(context: Context, file: File) {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uriFor(context, file), "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "Abrir PDF"))
    }

    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    @Suppress("DEPRECATION")
    private fun isInstalled(context: Context, pkg: String): Boolean =
        try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
}
