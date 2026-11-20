package ga.jundbits.clock_in_clock_out.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ga.jundbits.clock_in_clock_out.R
import ga.jundbits.clock_in_clock_out.data.AppDatabase
import ga.jundbits.clock_in_clock_out.data.Csv
import ga.jundbits.clock_in_clock_out.data.ImportResult
import ga.jundbits.clock_in_clock_out.data.Profile
import ga.jundbits.clock_in_clock_out.data.SkipReason
import ga.jundbits.clock_in_clock_out.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the import dialog shows after reading a file. */
private sealed interface ImportDialog {
    data class Confirm(val result: ImportResult.Parsed, val currentCount: Int) : ImportDialog
    data class Problem(val message: Int) : ImportDialog
}

@Composable
fun ProfilesScreen(db: AppDatabase, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val profiles by remember { db.profileDao().observeAll() }.collectAsStateWithLifecycle(initialValue = null)
    var query by rememberSaveable { mutableStateOf("") }
    var dialog by remember { mutableStateOf<ImportDialog?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull()
            }
            dialog = when (val result = text?.let(Csv::parseProfiles)) {
                null -> ImportDialog.Problem(R.string.import_read_error)
                ImportResult.MissingHeader -> ImportDialog.Problem(R.string.import_no_header)
                is ImportResult.Parsed ->
                    if (result.profiles.isEmpty()) ImportDialog.Problem(R.string.import_no_rows)
                    else ImportDialog.Confirm(result, db.profileDao().count())
            }
        }
    }
    val startImport = { picker.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")) }

    Scaffold(
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopBar(stringResource(R.string.profiles), onBack) {
                TopBarAction(R.drawable.ic_import, stringResource(R.string.import_text), startImport)
            }
        },
    ) { padding ->
        val list = profiles
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (list == null) return@Column
            if (list.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.no_profiles_title),
                    body = stringResource(R.string.no_profiles_body),
                    action = { PrimaryButton(stringResource(R.string.import_list), startImport, icon = R.drawable.ic_import) },
                )
                return@Column
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { AppIcon(R.drawable.ic_search, null, tint = colors.muted, size = 20.dp) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    unfocusedBorderColor = colors.border,
                    focusedBorderColor = colors.inColor,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
            val needle = query.trim().lowercase()
            val shown = if (needle.isEmpty()) list else list.filter {
                it.id.lowercase().contains(needle) ||
                    it.name.lowercase().contains(needle) ||
                    it.department.lowercase().contains(needle)
            }
            if (shown.isEmpty()) {
                Text(
                    stringResource(R.string.no_match, query.trim()),
                    color = colors.muted,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                )
            }
            LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)) {
                itemsIndexed(shown, key = { _, profile -> profile.id }) { index, profile ->
                    ProfileRow(profile, first = index == 0, last = index == shown.lastIndex) { onOpen(profile.id) }
                }
            }
        }
    }

    when (val current = dialog) {
        null -> Unit
        is ImportDialog.Problem -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.import_none_title)) },
            text = { Text(stringResource(current.message)) },
            confirmButton = { TextButton({ dialog = null }) { Text(stringResource(R.string.ok)) } },
        )
        is ImportDialog.Confirm -> {
            val count = current.result.profiles.size
            val importedMessage = pluralStringResource(R.plurals.imported, count, count)
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text(stringResource(R.string.import_title)) },
                text = { ImportSummary(current) },
                confirmButton = {
                    TextButton({
                        dialog = null
                        scope.launch {
                            db.profileDao().replaceAll(current.result.profiles)
                            query = ""
                            snackbar.showSnackbar(importedMessage)
                        }
                    }) { Text(stringResource(R.string.replace), fontWeight = FontWeight.Bold) }
                },
                dismissButton = { TextButton({ dialog = null }) { Text(stringResource(R.string.cancel)) } },
            )
        }
    }
}

@Composable
private fun ImportSummary(confirm: ImportDialog.Confirm) {
    val count = confirm.result.profiles.size
    val skipped = confirm.result.skipped
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(pluralStringResource(R.plurals.import_found, count, count))
        if (confirm.currentCount > 0) Text(pluralStringResource(R.plurals.import_replaces, confirm.currentCount, confirm.currentCount))
        if (skipped.isNotEmpty()) {
            Text(stringResource(R.string.import_skipped_header), fontWeight = FontWeight.Bold)
            skipped.take(5).forEach { line ->
                Text(
                    when (line.reason) {
                        SkipReason.MISSING_FIELD -> stringResource(R.string.skip_missing, line.line)
                        SkipReason.DUPLICATE_ID -> stringResource(R.string.skip_duplicate, line.line, line.id)
                    },
                    fontSize = 14.sp,
                )
            }
            if (skipped.size > 5) Text(pluralStringResource(R.plurals.import_more, skipped.size - 5, skipped.size - 5), fontSize = 14.sp)
        }
    }
}

@Composable
private fun ProfileRow(profile: Profile, first: Boolean, last: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(
        topStart = if (first) 20.dp else 0.dp, topEnd = if (first) 20.dp else 0.dp,
        bottomStart = if (last) 20.dp else 0.dp, bottomEnd = if (last) 20.dp else 0.dp,
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
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(profile.name, 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(profile.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)
                Text(idLine(profile.department, profile.id), fontSize = 14.sp, color = colors.muted)
            }
            AppIcon(R.drawable.ic_chevron, null, tint = colors.muted, size = 20.dp)
        }
        if (!last) HorizontalDivider(color = colors.border)
    }
}
