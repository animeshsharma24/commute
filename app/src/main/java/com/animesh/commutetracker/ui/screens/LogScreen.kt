package com.animesh.commutetracker.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.animesh.commutetracker.ui.viewmodel.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val logs by viewModel.recentLogs.collectAsState(initial = emptyList())
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostic Logs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = {
                        val logText = logs.joinToString("\n") { log ->
                            "${timeFormat.format(Date(log.timestamp))} [${log.event}] ${log.message}"
                        }
                        clipboardManager.setText(AnnotatedString(logText))
                    }) { Icon(Icons.Default.ContentCopy, "Copy") }
                    
                    IconButton(onClick = {
                        val csvHeader = "Timestamp,Event,Message\n"
                        val csvBody = logs.joinToString("\n") { log ->
                            "${timeFormat.format(Date(log.timestamp))},${log.event},\"${log.message}\""
                        }
                        val fileName = "tracker_logs_${System.currentTimeMillis()}.csv"
                        val file = File(context.cacheDir, fileName)
                        file.writeText(csvHeader + csvBody)
                        
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Export Logs"))
                    }) { Icon(Icons.Default.Share, "Export") }

                    IconButton(onClick = { viewModel.clearLogs() }) { 
                        Icon(Icons.Default.ClearAll, "Clear") 
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(logs) { log ->
                Column(modifier = Modifier.padding(8.dp)) {
                    Row {
                        Text(timeFormat.format(Date(log.timestamp)), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(log.event, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                    if (log.message.isNotEmpty()) {
                        Text(log.message, fontSize = 14.sp)
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
