package ga.jundbits.clock_in_clock_out.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ga.jundbits.clock_in_clock_out.R
import ga.jundbits.clock_in_clock_out.data.AppDatabase
import ga.jundbits.clock_in_clock_out.data.Attendance
import ga.jundbits.clock_in_clock_out.data.Clocking
import ga.jundbits.clock_in_clock_out.data.Csv
import ga.jundbits.clock_in_clock_out.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface AttendanceItem {
    data class Header(val label: String, val key: Long) : AttendanceItem
    data class Row(val record: Attendance, val first: Boolean, val last: Boolean) : AttendanceItem
}

private fun exportFileName(): String =
    "attendance_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".csv"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AttendanceScreen(db: AppDatabase, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val records by remember { db.attendanceDao().observeAll() }.collectAsStateWithLifecycle(initialValue = null)
    var menuOpen by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Attendance?>(null) }

    val savedMessage = stringResource(R.string.saved_csv)
    val saveFailedMessage = stringResource(R.string.save_failed)
    val shareFailedMessage = stringResource(R.string.share_failed)
    val emptyMessage = stringResource(R.string.nothing_to_export)
    val deletedMessage = stringResource(R.string.deleted)

    suspend fun exportText(): String? {
        val all = db.attendanceDao().getAllForExport()
        return if (all.isEmpty()) null else Csv.exportAttendance(all)
    }

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = exportText() ?: return@launch
            val ok = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null }.getOrDefault(false)
            }
            snackbar.showSnackbar(if (ok) savedMessage else saveFailedMessage)
        }
    }

    Scaffold(
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopBar(stringResource(R.string.attendance), onBack) {
                Box {
                    TopBarAction(R.drawable.ic_export, stringResource(R.string.export_text)) { menuOpen = true }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.share_csv)) },
                            leadingIcon = { AppIcon(R.drawable.ic_share, null, size = 20.dp) },
                            onClick = {
                                menuOpen = false
                                scope.launch {
                                    val text = exportText()
                                    if (text == null) {
                                        snackbar.showSnackbar(emptyMessage)
                                        return@launch
                                    }
                                    val shared = runCatching { Share.csv(context, exportFileName(), text) }.isSuccess
                                    if (!shared) snackbar.showSnackbar(shareFailedMessage)
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.save_csv)) },
                            leadingIcon = { AppIcon(R.drawable.ic_download, null, size = 20.dp) },
                            onClick = {
                                menuOpen = false
                                scope.launch {
                                    if (exportText() == null) snackbar.showSnackbar(emptyMessage)
                                    else saver.launch(exportFileName())
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        val list = records ?: return@Scaffold
        if (list.isEmpty()) {
            EmptyState(stringResource(R.string.no_attendance_title), stringResource(R.string.no_attendance_body), Modifier.padding(padding))
            return@Scaffold
        }
        val items = remember(list) {
            val out = mutableListOf<AttendanceItem>()
            list.groupBy { Formats.startOfDay(it.timestamp) }.forEach { (day, dayRecords) ->
                out += AttendanceItem.Header(Formats.dayLabel(context, day), day)
                dayRecords.forEachIndexed { i, record -> out += AttendanceItem.Row(record, i == 0, i == dayRecords.lastIndex) }
            }
            out
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            item(key = "hint") {
                Text(stringResource(R.string.hold_to_delete), fontSize = 13.sp, color = colors.muted, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
            }
            items(items, key = { item ->
                when (item) {
                    is AttendanceItem.Header -> "day-${item.key}"
                    is AttendanceItem.Row -> "rec-${item.record.id}"
                }
            }) { item ->
                when (item) {
                    is AttendanceItem.Header -> Text(
                        item.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.muted,
                        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
                    )
                    is AttendanceItem.Row -> {
                        val shape = RoundedCornerShape(
                            topStart = if (item.first) 20.dp else 0.dp, topEnd = if (item.first) 20.dp else 0.dp,
                            bottomStart = if (item.last) 20.dp else 0.dp, bottomEnd = if (item.last) 20.dp else 0.dp,
                        )
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(colors.surface),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(onClick = {}, onLongClick = { toDelete = item.record })
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.record.employeeName.ifBlank { item.record.employeeId }, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)
                                    Text(idLine(item.record.employeeDepartment, item.record.employeeId), fontSize = 14.sp, color = colors.muted)
                                    if (item.record.clockShift != 0L) {
                                        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            AppIcon(R.drawable.ic_alert, null, tint = colors.outCaption, size = 16.dp)
                                            Spacer(Modifier.width(6.dp))
                                            Text(Formats.clockShift(context, item.record.clockShift), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.outCaption)
                                        }
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                ClockChip(item.record.clocking, Formats.time(context, item.record.timestamp))
                            }
                            if (!item.last) HorizontalDivider(color = colors.border)
                        }
                    }
                }
            }
        }
    }

    toDelete?.let { record ->
        val label = stringResource(if (record.clocking == Clocking.IN) R.string.chip_in else R.string.chip_out)
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = {
                Text(stringResource(R.string.delete_body, record.employeeName.ifBlank { record.employeeId }, label, Formats.whenText(context, record.timestamp)))
            },
            confirmButton = {
                TextButton({
                    toDelete = null
                    scope.launch {
                        db.attendanceDao().delete(record)
                        snackbar.showSnackbar(deletedMessage)
                    }
                }) { Text(stringResource(R.string.delete), color = colors.danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton({ toDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
