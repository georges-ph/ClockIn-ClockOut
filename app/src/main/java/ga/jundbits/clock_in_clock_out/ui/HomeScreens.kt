package ga.jundbits.clock_in_clock_out.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableLongStateOf
import androidx.lifecycle.compose.LifecycleResumeEffect
import ga.jundbits.clock_in_clock_out.data.ClockWatch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import ga.jundbits.clock_in_clock_out.data.Badge
import ga.jundbits.clock_in_clock_out.data.BadgeData
import ga.jundbits.clock_in_clock_out.data.EmployeeStore
import ga.jundbits.clock_in_clock_out.ui.theme.AppTheme

@Composable
private fun AppHeader(menu: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(60.dp)
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.app_name), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = AppTheme.colors.text, modifier = Modifier.weight(1f))
        menu()
    }
}

@Composable
private fun OverflowMenu(items: List<Pair<String, () -> Unit>>) {
    var open by remember { mutableStateOf(false) }
    Box {
        RoundIconButton(R.drawable.ic_more, stringResource(R.string.more_options), { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { (label, action) ->
                DropdownMenuItem(text = { Text(label, fontSize = 15.sp) }, onClick = { open = false; action() })
            }
        }
    }
}

@Composable
fun WelcomeScreen(onEmployee: () -> Unit, onGate: () -> Unit) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.inColor),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(R.drawable.ic_badge, null, tint = colors.onIn, size = 32.dp)
        }
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.welcome_title), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = colors.text, lineHeight = 36.sp)
        Text(stringResource(R.string.welcome_body), fontSize = 16.sp, color = colors.muted, lineHeight = 23.sp)
        Spacer(Modifier.height(12.dp))
        CardColumn {
            NavRow(R.drawable.ic_badge, stringResource(R.string.welcome_employee), stringResource(R.string.welcome_employee_body), onEmployee)
            HorizontalDivider(color = colors.border)
            NavRow(R.drawable.ic_scan, stringResource(R.string.welcome_gate), stringResource(R.string.welcome_gate_body), onGate)
        }
    }
}

/** The employee's home: the badge itself. */
@Composable
fun BadgeScreen(
    store: EmployeeStore,
    onBack: (() -> Unit)?,
    onScanAnother: () -> Unit,
    onGateMode: () -> Unit,
    onMissing: () -> Unit,
) {
    val colors = AppTheme.colors
    val badge: BadgeData? = remember { store.read() }
    if (badge == null) {
        LaunchedEffect(Unit) { onMissing() }
        return
    }
    KeepScreenBright()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .navigationBarsPadding(),
    ) {
        val menuItems = listOf(
            stringResource(R.string.menu_scan_another) to onScanAnother,
            stringResource(R.string.menu_gate_mode) to onGateMode,
        )
        if (onBack != null) {
            TopBar(stringResource(R.string.menu_my_badge), onBack) { OverflowMenu(menuItems) }
        } else {
            AppHeader { OverflowMenu(menuItems) }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BadgeCard(badge.name, badge.department, badge.id, Badge.encode(badge.id, badge.name, badge.department))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                AppIcon(R.drawable.ic_clock, null, tint = colors.inColor, size = 22.dp)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.badge_hint), fontSize = 15.sp, color = colors.text, lineHeight = 22.sp)
            }
        }
    }
}

/** The gate phone's home: one big scan button, then the lists. */
@Composable
fun GuardHomeScreen(
    db: AppDatabase,
    hasBadge: Boolean,
    onScan: () -> Unit,
    onProfiles: () -> Unit,
    onAttendance: () -> Unit,
    onMyBadge: () -> Unit,
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val profileCount by remember { db.profileDao().observeCount() }.collectAsStateWithLifecycle(initialValue = -1)
    val todayStart = remember { Formats.startOfDay(System.currentTimeMillis()) }
    val todayCount by remember { db.attendanceDao().observeCountSince(todayStart) }.collectAsStateWithLifecycle(initialValue = 0)
    val latest by remember { db.attendanceDao().observeLatest() }.collectAsStateWithLifecycle(initialValue = null)
    val myBadgeLabel = stringResource(R.string.menu_my_badge)

    // Checked each time the gate screen opens or comes back, so a time change
    // shows up straight away instead of only on the next record.
    var clockShift by remember { mutableLongStateOf(0L) }
    var dismissedShift by remember { mutableLongStateOf(0L) }
    LifecycleResumeEffect(Unit) {
        clockShift = ClockWatch.pending(context)
        onPauseOrDispose { }
    }
    if (clockShift != 0L && clockShift != dismissedShift) {
        AlertDialog(
            onDismissRequest = { dismissedShift = clockShift },
            icon = { AppIcon(R.drawable.ic_alert, null, tint = colors.outCaption, size = 28.dp) },
            title = { Text(stringResource(R.string.clock_changed_title)) },
            text = { Text(Formats.clockChangedBody(context, clockShift)) },
            confirmButton = { TextButton({ dismissedShift = clockShift }) { Text(stringResource(R.string.ok), fontWeight = FontWeight.Bold) } },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .navigationBarsPadding(),
    ) {
        AppHeader { if (hasBadge) OverflowMenu(listOf(myBadgeLabel to onMyBadge)) }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                profileCount < 0 -> Unit
                profileCount == 0 -> CardColumn {
                    EmptyState(
                        title = stringResource(R.string.guard_empty_title),
                        body = stringResource(R.string.guard_empty_body),
                        action = { PrimaryButton(stringResource(R.string.import_list), onProfiles, icon = R.drawable.ic_import) },
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 168.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(colors.inColor)
                            .clickable(onClick = onScan)
                            .padding(22.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        AppIcon(R.drawable.ic_scan, null, tint = colors.onIn, size = 36.dp)
                        Spacer(Modifier.height(28.dp))
                        Text(stringResource(R.string.guard_scan), color = colors.onIn, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)                    }
                    CardColumn {
                        NavRow(R.drawable.ic_people, stringResource(R.string.guard_profiles), pluralStringResource(R.plurals.guard_profiles_count, profileCount, profileCount), onProfiles)
                        HorizontalDivider(color = colors.border)
                        NavRow(R.drawable.ic_list, stringResource(R.string.guard_attendance), pluralStringResource(R.plurals.guard_today_count, todayCount, todayCount), onAttendance)
                    }
                    latest?.let { record ->
                        CardColumn {
                            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(stringResource(R.string.guard_last), fontSize = 14.sp, color = colors.muted, fontWeight = FontWeight.SemiBold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(record.employeeName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.text)
                                        Text(idLine(record.employeeDepartment, record.employeeId), fontSize = 14.sp, color = colors.muted)
                                    }
                                    ClockChip(record.clocking, Formats.whenText(context, record.timestamp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
