package ga.jundbits.clock_in_clock_out.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ga.jundbits.clock_in_clock_out.R
import ga.jundbits.clock_in_clock_out.data.AppDatabase
import ga.jundbits.clock_in_clock_out.data.Attendance
import ga.jundbits.clock_in_clock_out.data.Badge
import ga.jundbits.clock_in_clock_out.data.ClockRule
import ga.jundbits.clock_in_clock_out.data.ClockWatch
import ga.jundbits.clock_in_clock_out.data.Clocking
import ga.jundbits.clock_in_clock_out.data.Profile
import ga.jundbits.clock_in_clock_out.ui.theme.AppTheme
import kotlinx.coroutines.launch

private data class ProfileDetail(val profile: Profile?, val last: Attendance?)

/** One person on the gate phone: their badge to scan or share, and a manual record. */
@Composable
fun ProfileScreen(db: AppDatabase, id: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val detail by produceState<ProfileDetail?>(null, id) {
        value = ProfileDetail(db.profileDao().get(id), db.attendanceDao().lastFor(id))
    }
    var saving by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.background,
        topBar = { TopBar(stringResource(R.string.profile), onBack) },
    ) { padding ->
        val current = detail ?: return@Scaffold
        val profile = current.profile
        if (profile == null) {
            EmptyState(stringResource(R.string.profile), stringResource(R.string.profile_missing), Modifier.padding(padding))
            return@Scaffold
        }
        KeepScreenBright()
        val picked = ClockRule.next(current.last, System.currentTimeMillis())
        val other = if (picked == Clocking.IN) Clocking.OUT else Clocking.IN
        val recordedIn = stringResource(R.string.recorded_in, profile.name)
        val recordedOut = stringResource(R.string.recorded_out, profile.name)

        fun record(clocking: Clocking) {
            if (saving) return
            saving = true
            scope.launch {
                db.attendanceDao().insert(
                    Attendance(
                        employeeId = profile.id,
                        employeeName = profile.name,
                        employeeDepartment = profile.department,
                        clocking = clocking,
                        timestamp = System.currentTimeMillis(),
                        clockShift = ClockWatch.consume(context),
                    ),
                )
                Toast.makeText(context, if (clocking == Clocking.IN) recordedIn else recordedOut, Toast.LENGTH_SHORT).show()
                onBack()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BadgeCard(profile.name, profile.department, profile.id, Badge.encode(profile)) {
                Text(stringResource(R.string.profile_scan_hint), fontSize = 15.sp, color = colors.muted)
            }
            SecondaryButton(
                text = stringResource(R.string.share_badge),
                onClick = {
                    runCatching { Share.badge(context, profile) }
                        .onFailure { Toast.makeText(context, R.string.share_failed, Toast.LENGTH_SHORT).show() }
                },
                modifier = Modifier.fillMaxWidth(),
                icon = R.drawable.ic_share,
            )
            Text(
                stringResource(R.string.record_by_hand),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.muted,
                modifier = Modifier.padding(top = 10.dp),
            )
            PrimaryButton(
                text = stringResource(if (picked == Clocking.IN) R.string.record_clock_in else R.string.record_clock_out),
                onClick = { record(picked) },
                modifier = Modifier.fillMaxWidth(),
                container = if (picked == Clocking.IN) colors.inColor else colors.outColor,
                content = if (picked == Clocking.IN) colors.onIn else colors.onOut,
                icon = if (picked == Clocking.IN) R.drawable.ic_in else R.drawable.ic_out,
            )
            LinkButton(
                text = stringResource(if (other == Clocking.IN) R.string.record_other_in else R.string.record_other_out),
                onClick = { record(other) },
                color = colors.text,
            )
            Text(
                text = clockReason(context, current.last, picked),
                fontSize = 13.sp,
                color = colors.muted,
                textAlign = TextAlign.Start,
            )
        }
    }
}
