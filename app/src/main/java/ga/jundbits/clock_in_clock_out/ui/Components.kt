package ga.jundbits.clock_in_clock_out.ui

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ga.jundbits.clock_in_clock_out.R
import ga.jundbits.clock_in_clock_out.data.Clocking
import ga.jundbits.clock_in_clock_out.ui.theme.AppTheme

@Composable
fun AppIcon(
    @DrawableRes id: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
) {
    Icon(painterResource(id), contentDescription, modifier.size(size), tint)
}

@Composable
fun RoundIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = Color.Transparent,
    content: Color = AppTheme.colors.text,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        colors = IconButtonDefaults.iconButtonColors(containerColor = container, contentColor = content),
    ) {
        AppIcon(icon, contentDescription, size = 22.dp)
    }
}

/** A top bar action with a visible label, for actions an icon alone doesn't explain. */
@Composable
fun TopBarAction(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
        AppIcon(icon, null, tint = colors.inColor, size = 20.dp)
        Spacer(Modifier.width(6.dp))
        Text(label, color = colors.inColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(60.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) RoundIconButton(R.drawable.ic_back, stringResource(R.string.back), onBack)
        else Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppTheme.colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )
        actions()
    }
}

@Composable
fun Avatar(name: String, size: Dp) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.avatar),
        contentAlignment = Alignment.Center,
    ) {
        Text(Formats.initials(name), color = colors.onAvatar, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.36f).sp)
    }
}

/** "IT · 36512666", with the ID in a fixed-width font. */
@Composable
fun idLine(department: String, id: String): AnnotatedString = buildAnnotatedString {
    if (department.isNotBlank()) append("$department · ")
    withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(id) }
}

@Composable
fun PersonRow(name: String, department: String, id: String, avatarSize: Dp = 60.dp, nameSize: Int = 24) {
    val colors = AppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(name, avatarSize)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(name, fontSize = nameSize.sp, fontWeight = FontWeight.ExtraBold, color = colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(idLine(department, id), fontSize = 15.sp, color = colors.muted)
        }
    }
}

/** The lanyard-style ID card with the badge QR code. */
@Composable
fun BadgeCard(
    name: String,
    department: String,
    id: String,
    qrContent: String,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(colors.inColor),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                Modifier
                    .padding(top = 14.dp)
                    .size(width = 56.dp, height = 10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(colors.background),
            )
        }
        Box(Modifier.padding(start = 22.dp, end = 22.dp, top = 20.dp)) { PersonRow(name, department, id) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(if (colors.isDark) 10.dp else 0.dp),
            ) {
                QrCode(qrContent, 240.dp, stringResource(R.string.badge_qr_description, name))
            }
        }
        if (footer != null) {
            HorizontalDivider(color = colors.border)
            Box(Modifier.padding(horizontal = 22.dp, vertical = 16.dp)) { footer() }
        }
    }
}

/** The decided clocking with the time and why it was picked. */
@Composable
fun ClockingBlock(clocking: Clocking, time: String, reason: String?) {
    val colors = AppTheme.colors
    val background = if (clocking == Clocking.IN) colors.inContainer else colors.outContainer
    val content = if (clocking == Clocking.IN) colors.onInContainer else colors.onOutContainer
    val caption = if (clocking == Clocking.IN) colors.inCaption else colors.outCaption
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(if (clocking == Clocking.IN) R.string.clock_in else R.string.clock_out), color = content, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(time, color = content, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        if (reason != null) Text(reason, color = caption, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ClockChip(clocking: Clocking, time: String) {
    val colors = AppTheme.colors
    val background = if (clocking == Clocking.IN) colors.inContainer else colors.outContainer
    val content = if (clocking == Clocking.IN) colors.onInContainer else colors.onOutContainer
    val label = stringResource(if (clocking == Clocking.IN) R.string.chip_in else R.string.chip_out)
    Text(
        text = "$label $time",
        color = content,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = AppTheme.colors.inColor,
    content: Color = AppTheme.colors.onIn,
    @DrawableRes icon: Int? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 60.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            AppIcon(icon, null, size = 22.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null) {
    val colors = AppTheme.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 60.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colors.border),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.surface, contentColor = colors.text),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            AppIcon(icon, null, size = 22.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun LinkButton(text: String, onClick: () -> Unit, color: Color = AppTheme.colors.inColor) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 10.dp)) {
        Text(text, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)
    }
}

/** A tappable row with an icon, title, subtitle and chevron. */
@Composable
fun NavRow(@DrawableRes icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.inContainer),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(icon, null, tint = colors.onInContainer, size = 22.dp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.text)
            Text(subtitle, fontSize = 14.sp, color = colors.muted)
        }
        AppIcon(R.drawable.ic_chevron, null, tint = colors.muted, size = 20.dp)
    }
}

@Composable
fun CardColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape),
    ) { content() }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = colors.text, textAlign = TextAlign.Center)
        Text(body, fontSize = 15.sp, color = colors.muted, textAlign = TextAlign.Center, lineHeight = 22.sp)
        if (action != null) {
            Spacer(Modifier.height(8.dp))
            action()
        }
    }
}

/** Full brightness while a QR code is on screen, so the gate camera reads it first time. */
@Composable
fun KeepScreenBright() {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity) {
        val window = activity.window
        val previous = window.attributes.screenBrightness
        window.attributes = window.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window.attributes = window.attributes.apply { screenBrightness = previous }
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
