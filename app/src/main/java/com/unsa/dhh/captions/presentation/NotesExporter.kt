package com.unsa.dhh.captions.presentation

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Módulo 5: Exportador de apuntes de clase para el estudiante con discapacidad auditiva.
 * Permite almacenar y compartir la transcripción limpia y corregida en formatos .txt y .pdf.
 */
class NotesExporter(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    /**
     * Exporta el texto transcrito a un archivo de texto plano (.txt).
     */
    fun exportToTxt(subject: String, transcriptText: String): File? {
        return try {
            val timestamp = dateFormat.format(Date())
            val filename = "Apuntes_${subject}_$timestamp.txt"
            val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val file = File(targetDir, filename)

            FileWriter(file).use { writer ->
                writer.write("====================================================\n")
                writer.write("APUNTES DE CLASE - DHH CAPTIONS (UNSA)\n")
                writer.write("Materia: $subject\n")
                writer.write("Fecha: ${displayFormat.format(Date())}\n")
                writer.write("====================================================\n\n")
                writer.write(transcriptText)
            }
            Log.d(TAG, "Archivo TXT guardado exitosamente: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error al exportar a TXT: ${e.message}", e)
            null
        }
    }

    /**
     * Exporta los apuntes de la clase a formato PDF nativo de Android.
     */
    fun exportToPdf(subject: String, transcriptText: String): File? {
        val pdfDocument = PdfDocument()
        return try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Formato A4
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 16f
                isFakeBoldText = true
            }

            val metaPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 11f
            }

            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                isAntiAlias = true
            }

            var yPos = 50f
            canvas.drawText("APUNTES DE CLASE - DHH CAPTIONS", 40f, yPos, titlePaint)
            yPos += 20f
            canvas.drawText("Materia: $subject   |   Fecha: ${displayFormat.format(Date())}", 40f, yPos, metaPaint)
            yPos += 15f
            canvas.drawLine(40f, yPos, 555f, yPos, metaPaint)
            yPos += 30f

            // Partir el texto en líneas ajustadas al ancho de página
            val words = transcriptText.split(" ")
            var currentLine = StringBuilder()

            for (w in words) {
                if (textPaint.measureText(currentLine.toString() + " " + w) < 515f) {
                    currentLine.append(" ").append(w)
                } else {
                    canvas.drawText(currentLine.toString().trim(), 40f, yPos, textPaint)
                    yPos += 18f
                    currentLine = StringBuilder(w)
                    if (yPos > 790f) {
                        break // En prototipo de clase estándar 1 página o expandible
                    }
                }
            }
            if (currentLine.isNotEmpty() && yPos <= 800f) {
                canvas.drawText(currentLine.toString().trim(), 40f, yPos, textPaint)
            }

            pdfDocument.finishPage(page)

            val timestamp = dateFormat.format(Date())
            val filename = "Apuntes_${subject}_$timestamp.pdf"
            val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val file = File(targetDir, filename)

            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            Log.d(TAG, "Archivo PDF guardado exitosamente: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error exportando a PDF: ${e.message}", e)
            null
        } finally {
            pdfDocument.close()
        }
    }

    companion object {
        private const val TAG = "NotesExporter"
    }
}
