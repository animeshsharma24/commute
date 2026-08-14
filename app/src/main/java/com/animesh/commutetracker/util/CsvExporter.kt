package com.animesh.commutetracker.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.animesh.commutetracker.data.model.CommuteRecord
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {
    fun exportRecords(context: Context, records: List<CommuteRecord>) {
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val csvHeader = "Date,Direction,Start,Arrival,Duration_Min,Transport,Cost,Detection_Method\n"
        val csvBody = records.joinToString("\n") { record ->
            "${record.date},${record.direction},${dateFormat.format(Date(record.startTimestamp))},${dateFormat.format(Date(record.arrivalTimestamp))},${record.durationMinutes},${record.transportMode},${record.cost ?: 0},${record.detectionMethod}"
        }

        val fileName = "Commute_History_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        file.writeText(csvHeader + csvBody)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Commute History"))
    }
}
