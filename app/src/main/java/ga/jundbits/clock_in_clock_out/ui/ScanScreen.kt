package ga.jundbits.clock_in_clock_out.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import ga.jundbits.clock_in_clock_out.R
import ga.jundbits.clock_in_clock_out.data.AppDatabase
import ga.jundbits.clock_in_clock_out.data.Attendance
import ga.jundbits.clock_in_clock_out.data.Badge
import ga.jundbits.clock_in_clock_out.data.ClockRule
import ga.jundbits.clock_in_clock_out.data.ClockWatch
import ga.jundbits.clock_in_clock_out.data.Clocking
import ga.jundbits.clock_in_clock_out.data.EmployeeStore
import ga.jundbits.clock_in_clock_out.data.Profile
import ga.jundbits.clock_in_clock_out.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

enum class ScanMode { GUARD, SETUP }

private sealed interface ScanState {
    data object Scanning : ScanState
    data class Found(val profile: Profile, val last: Attendance?, val picked: Clocking, val scannedAt: Long) : ScanState
    data class NotOnList(val id: String) : ScanState
    data object NotABadge : ScanState
    data object SetupIncomplete : ScanState
}

private fun hasCameraPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/** The reason line under the decided clocking. */
internal fun clockReason(context: Context, last: Attendance?, picked: Clocking): String = when {
    last == null -> context.getString(R.string.scan_reason_first)
    last.clocking == Clocking.OUT -> context.getString(R.string.scan_reason_last_out, Formats.whenText(context, last.timestamp))
    picked == Clocking.OUT -> context.getString(R.string.scan_reason_last_in, Formats.whenText(context, last.timestamp))
    else -> context.getString(R.string.scan_reason_old_in)
}

/**
 * Scans one badge, then turns the camera off: the preview leaves the screen as
 * soon as a code is read, which releases the camera.
 */
@Composable
fun ScanScreen(
    mode: ScanMode,
    db: AppDatabase,
    store: EmployeeStore,
    onClose: () -> Unit,
    onSetupDone: () -> Unit,
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ScanState>(ScanState.Scanning) }
    var permitted by remember { mutableStateOf(hasCameraPermission(context)) }
    var asked by remember { mutableStateOf(false) }
    var cameraFailed by remember { mutableStateOf(false) }
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    LightStatusBarIcons()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permitted = granted
        asked = true
    }
    LaunchedEffect(Unit) {
        if (hasCamera && !permitted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    // Picks up a permission granted from the system settings screen.
    LifecycleResumeEffect(Unit) {
        permitted = hasCameraPermission(context)
        onPauseOrDispose { }
    }

    val handleCode: (String) -> Unit = { text ->
        scope.launch {
            val badge = Badge.decode(text)
            when (mode) {
                ScanMode.SETUP -> when {
                    badge == null -> state = ScanState.NotABadge
                    !badge.isComplete -> state = ScanState.SetupIncomplete
                    else -> {
                        store.save(badge)
                        Toast.makeText(context, R.string.badge_saved, Toast.LENGTH_SHORT).show()
                        onSetupDone()
                    }
                }
                ScanMode.GUARD -> {
                    if (badge == null) {
                        state = ScanState.NotABadge
                    } else {
                        // The name shown comes from the imported list, not from the code.
                        val profile = db.profileDao().get(badge.id)
                        state = if (profile == null) {
                            ScanState.NotOnList(badge.id)
                        } else {
                            val now = System.currentTimeMillis()
                            val last = db.attendanceDao().lastFor(profile.id)
                            ScanState.Found(profile, last, ClockRule.next(last, now), now)
                        }
                    }
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.camera),
    ) {
        when (val current = state) {
            ScanState.Scanning -> {
                if (hasCamera && permitted && !cameraFailed) {
                    CameraPreview(onCode = handleCode, onError = { cameraFailed = true })
                }
                Column(Modifier.fillMaxSize()) {
                    CameraTopRow(
                        label = stringResource(if (mode == ScanMode.SETUP) R.string.scan_point_setup else R.string.scan_point),
                        onClose = onClose,
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            !hasCamera -> CameraMessage(stringResource(R.string.no_camera), null)
                            cameraFailed -> CameraMessage(stringResource(R.string.camera_error), null)
                            !permitted && asked -> CameraMessage(stringResource(R.string.camera_needed_body), stringResource(R.string.camera_needed_title)) {
                                PrimaryButton(stringResource(R.string.allow_camera), { permissionLauncher.launch(Manifest.permission.CAMERA) }, Modifier.fillMaxWidth())
                                SecondaryButton(stringResource(R.string.open_settings), {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }, Modifier.fillMaxWidth())
                            }
                            permitted -> ScanFrame(null, Color.Transparent)
                            else -> Unit
                        }
                    }
                }
            }
            is ScanState.Found -> FoundResult(current, db, onClose)
            is ScanState.NotOnList -> ResultLayout(R.drawable.ic_close, colors.danger, Color.White, onClose) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.dangerContainer)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(stringResource(R.string.not_on_list), color = colors.onDangerContainer, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(R.string.nothing_recorded), color = colors.onDangerContainer, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.scanned_id), color = colors.muted, fontSize = 15.sp)
                    Text(current.id, color = colors.text, fontSize = 20.sp, fontFamily = FontFamily.Monospace)
                }
                Text(stringResource(R.string.not_on_list_body), color = colors.text, fontSize = 15.sp, lineHeight = 22.sp)
                RetryButtons(onClose) { state = ScanState.Scanning }
            }
            ScanState.NotABadge, ScanState.SetupIncomplete -> ResultLayout(R.drawable.ic_close, colors.danger, Color.White, onClose) {
                Text(
                    stringResource(if (current == ScanState.SetupIncomplete) R.string.setup_incomplete_title else R.string.not_a_badge),
                    color = colors.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    stringResource(if (current == ScanState.SetupIncomplete) R.string.setup_incomplete else R.string.not_a_badge_body),
                    color = colors.muted,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
                RetryButtons(onClose) { state = ScanState.Scanning }
            }
        }
    }
}

/** The camera screen is dark in both themes, so the status bar icons stay light on it. */
@Composable
private fun LightStatusBarIcons() {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
}

@Composable
private fun RetryButtons(onClose: () -> Unit, onRetry: () -> Unit) {
    val colors = AppTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SecondaryButton(stringResource(R.string.close), onClose, Modifier.weight(1f))
        PrimaryButton(stringResource(R.string.scan_again), onRetry, Modifier.weight(1f), container = colors.ink, content = colors.onInk)
    }
}

@Composable
private fun FoundResult(found: ScanState.Found, db: AppDatabase, onClose: () -> Unit) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var chosen by remember(found) { mutableStateOf(found.picked) }
    var saving by remember { mutableStateOf(false) }
    val accent = if (chosen == Clocking.IN) colors.inColor else colors.outColor
    val onAccent = if (chosen == Clocking.IN) colors.onIn else colors.onOut
    val profile = found.profile
    val recordedMessage = stringResource(if (chosen == Clocking.IN) R.string.recorded_in else R.string.recorded_out, profile.name)

    ResultLayout(R.drawable.ic_check, accent, onAccent, onClose) {
        PersonRow(profile.name, profile.department, profile.id)
        ClockingBlock(
            clocking = chosen,
            time = Formats.time(context, found.scannedAt),
            reason = if (chosen == found.picked) clockReason(context, found.last, found.picked) else null,
        )
        if (ClockRule.isRecent(found.last, found.scannedAt)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.outContainer)
                    .padding(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                AppIcon(R.drawable.ic_alert, null, tint = colors.outCaption, size = 20.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.scan_recent, Formats.time(context, found.last!!.timestamp)),
                    color = colors.outCaption,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Box(Modifier.offset(y = (-6).dp)) {
            LinkButton(
                text = stringResource(if (chosen == Clocking.IN) R.string.scan_switch_to_out else R.string.scan_switch_to_in),
                onClick = { chosen = if (chosen == Clocking.IN) Clocking.OUT else Clocking.IN },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(stringResource(R.string.cancel), onClose, Modifier.weight(1f))
            PrimaryButton(
                text = stringResource(R.string.record),
                onClick = {
                    if (saving) return@PrimaryButton
                    saving = true
                    scope.launch {
                        db.attendanceDao().insert(
                            Attendance(
                                employeeId = profile.id,
                                employeeName = profile.name,
                                employeeDepartment = profile.department,
                                clocking = chosen,
                                timestamp = found.scannedAt,
                                clockShift = ClockWatch.consume(context),
                            ),
                        )
                        Toast.makeText(context, recordedMessage, Toast.LENGTH_SHORT).show()
                        onClose()
                    }
                },
                modifier = Modifier.weight(1f),
                container = accent,
                content = onAccent,
            )
        }
    }
}

/** Dark camera area with the result mark on top, and a sheet below it. */
@Composable
private fun ResultLayout(
    @DrawableRes mark: Int,
    markColor: Color,
    onMark: Color,
    onClose: () -> Unit,
    sheet: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            CameraTopRow(stringResource(R.string.scan_captured), onClose)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                ScanFrame(mark, markColor, onMark)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = 0.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(colors.surface)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.border),
            )
            sheet()
        }
    }
}

@Composable
private fun CameraTopRow(label: String, onClose: () -> Unit) {
    val colors = AppTheme.colors
    // The label is centred on the screen; the close button sits on the left.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        RoundIconButton(
            R.drawable.ic_close,
            stringResource(R.string.close),
            onClose,
            modifier = Modifier.align(Alignment.CenterStart),
            container = colors.cameraChip,
            content = colors.onCameraChip,
        )
        Text(
            label,
            color = colors.onCameraChip,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 56.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.cameraChip)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/** Four rounded corners around the scan area, with an optional result mark inside. */
@Composable
private fun ScanFrame(@DrawableRes mark: Int?, markColor: Color, onMark: Color = Color.White) {
    val frame = AppTheme.colors.cameraFrame
    Box(Modifier.size(232.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val arm = 40.dp.toPx()
            val radius = 14.dp.toPx()
            val inset = stroke / 2
            val w = size.width
            val h = size.height
            val corners = listOf(
                Offset(inset, inset) to Offset(1f, 1f),
                Offset(w - inset, inset) to Offset(-1f, 1f),
                Offset(inset, h - inset) to Offset(1f, -1f),
                Offset(w - inset, h - inset) to Offset(-1f, -1f),
            )
            for ((corner, dir) in corners) {
                val path = Path().apply {
                    moveTo(corner.x, corner.y + dir.y * arm)
                    lineTo(corner.x, corner.y + dir.y * radius)
                    quadraticTo(corner.x, corner.y, corner.x + dir.x * radius, corner.y)
                    lineTo(corner.x + dir.x * arm, corner.y)
                }
                drawPath(path, frame, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        if (mark != null) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(markColor),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(mark, null, tint = onMark, size = 34.dp)
            }
        }
    }
}

@Composable
private fun CameraMessage(body: String, title: String?, actions: (@Composable ColumnScope.() -> Unit)? = null) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .padding(24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(R.drawable.ic_camera_off, null, tint = colors.muted, size = 28.dp)
        if (title != null) Text(title, color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(body, color = colors.muted, fontSize = 15.sp, lineHeight = 22.sp)
        actions?.invoke(this)
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(onCode: (String) -> Unit, onError: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCode by rememberUpdatedState(onCode)
    val currentOnError by rememberUpdatedState(onError)
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
        val executor = Executors.newSingleThreadExecutor()
        val delivered = AtomicBoolean(false)
        var provider: ProcessCameraProvider? = null

        providerFuture.addListener({
            try {
                val cameraProvider = providerFuture.get()
                provider = cameraProvider
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { proxy ->
                    val image = proxy.image
                    if (image == null || delivered.get()) {
                        proxy.close()
                        return@setAnalyzer
                    }
                    scanner.process(InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees))
                        .addOnSuccessListener { codes ->
                            val text = codes.firstNotNullOfOrNull { it.rawValue }
                            if (text != null && delivered.compareAndSet(false, true)) currentOnCode(text)
                        }
                        .addOnCompleteListener { proxy.close() }
                }
                val selector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            } catch (e: Exception) {
                currentOnError()
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            provider?.unbindAll()
            scanner.close()
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}
