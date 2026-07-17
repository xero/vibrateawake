package style.xero.vibrateawake

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import style.xero.vibrateawake.ui.theme.SliderTrackGrey
import style.xero.vibrateawake.ui.theme.Thunderline
import style.xero.vibrateawake.ui.theme.TitleOrange
import style.xero.vibrateawake.ui.theme.TitleOrangeDim
import style.xero.vibrateawake.ui.theme.VibrateAwakeTheme
import style.xero.vibrateawake.ui.theme.buttonContainerColor
import style.xero.vibrateawake.ui.theme.pureContentColor
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VibrateAwakeTheme {
                val vm: MainViewModel = viewModel()
                val config by vm.config.collectAsState()
                val isRunning by vm.isRunning.collectAsState()

                VibrateAwakeScreen(
                    config = config,
                    isRunning = isRunning,
                    onIntervalChange = vm::setInterval,
                    onPatternChange = vm::setPattern,
                    onRoadNoiseChange = vm::setRoadNoise,
                    onStart = vm::start,
                    onStop = vm::stop,
                )
            }
        }
    }
}

@Composable
private fun VibrateAwakeScreen(
    config: VibrationConfig,
    isRunning: Boolean,
    onIntervalChange: (Double) -> Unit,
    onPatternChange: (PatternStyle) -> Unit,
    onRoadNoiseChange: (RoadNoise) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val context = LocalContext.current

    // Requesting POST_NOTIFICATIONS is only meaningful on Android 13+. Whatever the
    // grant result, start the service (the notification just stays hidden if denied).
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { startServiceThenBattery(context, onStart) }

    val onStartClick = {
        if (hasNotificationPermission(context)) {
            startServiceThenBattery(context, onStart)
        } else {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    // System back closes the privacy view first instead of leaving the app.
    BackHandler(enabled = showPrivacy) { showPrivacy = false }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {

            Text(
                text = "VIBRATE AWAKE",
                fontFamily = Thunderline,
                fontSize = 54.sp,
                color = TitleOrange,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 20.dp),
            )
            HorizontalDivider()

            if (showPrivacy) {
                PrivacyScreen(
                    modifier = Modifier.weight(1f),
                    onClose = { showPrivacy = false },
                    onOpenUrl = { url -> openUrl(context, url) },
                )
            } else {
                // Scrolling middle so the form never pushes the button off small screens.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    FatigueSection(
                        intervalMinutes = config.intervalMinutes,
                        enabled = !isRunning,
                        onIntervalChange = onIntervalChange,
                    )
                    RadioSection(
                        title = "Vibration Rhythm",
                        options = listOf(
                            PatternStyle.STACCATO to "Staccato",
                            PatternStyle.SOS_PULSE to "SOS Pulse",
                            PatternStyle.SIREN to "Continuous Wave",
                        ),
                        selected = config.pattern,
                        enabled = !isRunning,
                        onSelect = onPatternChange,
                    )
                    RadioSection(
                        title = "Vibration Intensity",
                        options = listOf(
                            RoadNoise.ADAPTIVE to "Standard",
                            RoadNoise.MAX_HEAVY to "Maximum",
                        ),
                        selected = config.roadNoise,
                        enabled = !isRunning,
                        onSelect = onRoadNoiseChange,
                    )
                }

                // Privacy shield: bottom-right, below the form, above the Start/Stop button.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_privacy),
                        contentDescription = "Privacy policy",
                        modifier = Modifier
                            .size(56.dp)
                            .clickable { showPrivacy = true },
                    )
                }

                HorizontalDivider()
                Button(
                    onClick = { if (isRunning) onStop() else onStartClick() },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        // Running turns the button orange so it reads as the one live control.
                        containerColor = if (isRunning) TitleOrange else buttonContainerColor(),
                        contentColor = pureContentColor(),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .height(72.dp),
                ) {
                    Text(
                        text = if (isRunning) "STOP" else "START",
                        fontFamily = Thunderline,
                        fontSize = 32.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun FatigueSection(
    intervalMinutes: Double,
    enabled: Boolean,
    onIntervalChange: (Double) -> Unit,
) {
    val presets = listOf(3.0 to "3 Min", 5.0 to "5 Min", 10.0 to "10 Min")
    SectionHeader("Fatigue Level", enabled)

    RadioColumn(
        options = presets,
        selected = presets.map { it.first }.firstOrNull { it == intervalMinutes },
        enabled = enabled,
        onSelect = onIntervalChange,
    )

    val onPreset = presets.any { it.first == intervalMinutes }
    Text(
        text = if (onPreset) {
            "Drag the slider for a custom time interval"
        } else {
            "Custom time interval: ${formatMinutes(intervalMinutes)} " +
                if (intervalMinutes == 1.0) "min" else "mins"
        },
        color = dim(MaterialTheme.colorScheme.onBackground, enabled),
        modifier = Modifier.padding(top = 8.dp),
    )
    Slider(
        value = intervalMinutes.toFloat(),
        onValueChange = { onIntervalChange((it * 2).roundToInt() / 2.0) },
        valueRange = 1f..25f,
        // No tick marks; the value still snaps to 30-second steps via the rounding above.
        enabled = enabled,
        colors = SliderDefaults.colors(
            thumbColor = TitleOrange,
            activeTrackColor = TitleOrange,
            inactiveTrackColor = SliderTrackGrey,
        ),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Caption("Extreme Fatigue", enabled)
        Caption("Preventative", enabled)
    }
}

@Composable
private fun <T> RadioSection(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    enabled: Boolean,
    onSelect: (T) -> Unit,
) {
    SectionHeader(title, enabled)
    RadioColumn(options = options, selected = selected, enabled = enabled, onSelect = onSelect)
}

@Composable
private fun <T> RadioColumn(
    options: List<Pair<T, String>>,
    selected: T?,
    enabled: Boolean,
    onSelect: (T) -> Unit,
) {
    Column {
        options.forEach { (value, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = value == selected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onSelect(value) },
                    )
                    .padding(vertical = 4.dp),
            ) {
                RadioButton(
                    selected = value == selected,
                    onClick = null,
                    enabled = enabled,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = TitleOrange,
                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledSelectedColor = TitleOrangeDim,
                        disabledUnselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    ),
                )
                Text(
                    text = label,
                    color = dim(MaterialTheme.colorScheme.onBackground, enabled),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, enabled: Boolean = true) {
    Text(
        text = text,
        color = dim(pureContentColor(), enabled),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun Caption(text: String, enabled: Boolean = true) {
    Text(
        text = text,
        color = dim(MaterialTheme.colorScheme.onBackground, enabled),
        style = MaterialTheme.typography.bodySmall,
    )
}

private fun formatMinutes(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

// Fade a color toward the background when a control is locked (service running).
private fun dim(color: Color, enabled: Boolean): Color =
    if (enabled) color else color.copy(alpha = 0.38f)

@Composable
private fun PrivacyScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text = "‹ Back",
            color = TitleOrange,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable { onClose() }
                .padding(vertical = 4.dp),
        )
        Text(
            text = "Privacy Policy",
            color = pureContentColor(),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Image(
            painter = painterResource(R.drawable.ic_launcher_art),
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp),
        )

        PrivacyH2("TLDR")
        PrivacyBody(
            "Vibrate Awake is a vibration timer to help keep a drowsy driver awake. It does " +
                "not collect, store off-device, use, sell, or transfer any personal data. It " +
                "has no accounts, no analytics, no advertising, no network access, is totally free.",
        )

        PrivacyRule()
        PrivacyH2("What we collect")
        PrivacyBody(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                    append("Nothing")
                }
                append(
                    ". The app gathers no personal information, usage data, device identifiers, " +
                        "nor location. It does not contain analytics, crash reporting, advertising, " +
                        "or any third-party SDK that would gather data. The app declares no internet " +
                        "permission, so it has no ability to send data off your device by design.",
                )
            },
        )

        PrivacyRule()
        PrivacyH2("What stays on your device")
        PrivacyBody(
            "Your three settings (interval, vibration rhythm, and intensity level) are saved " +
                "locally so the app remembers your last choices. This preference file lives in the " +
                "app's private storage on your phone. It never leaves the device, and it is removed " +
                "when you uninstall the app.",
        )

        PrivacyRule()
        PrivacyH2("Permissions")
        PrivacyBody("Each permission serves the core timer, not data collection:")
        PrivacyBullet("Vibrate", "Drives the vibration motor.")
        PrivacyBullet(
            "Wake lock",
            "Keeps the processor awake so the timer fires on schedule while the screen is off.",
        )
        PrivacyBullet(
            "Foreground service (and special-use type)",
            "Lets the alert keep running while the app is in the background or the phone is locked.",
        )
        PrivacyBullet(
            "Post notifications",
            "Shows the ongoing \"Vibrate Awake is active\" notification while the timer runs.",
        )
        PrivacyBullet(
            "Request ignore battery optimizations",
            "Lets the app ask you to exempt it from battery restrictions so the system does not " +
                "throttle the timer. You can decline, and the app still runs.",
        )
        Spacer(Modifier.height(8.dp))
        PrivacyBody("None of these permissions read contacts, files, location, or any personal data.")

        PrivacyRule()
        PrivacyH2("Ads, tracking, and third parties")
        PrivacyBody(
            "There are no ads, no tracking, no in-app purchases, and no third-party services. No " +
                "data is shared with anyone, because none is collected.",
        )

        PrivacyRule()
        PrivacyH2("Contact, feedback, and bug reports")
        PrivacyBody("Create an issue on the project's GitHub:")
        PrivacyLink("https://github.com/xero/vibrateawake/issues", onOpenUrl)
        Spacer(Modifier.height(8.dp))
        PrivacyBody("Join and post to the project's mailing list group:")
        PrivacyLink("https://groups.google.com/g/vibrate-awake-testing/", onOpenUrl)

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PrivacyH2(text: String) {
    Text(
        text = text,
        color = pureContentColor(),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun PrivacyBody(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun PrivacyBody(text: AnnotatedString) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun PrivacyBullet(lead: String, rest: String) {
    Row(modifier = Modifier.padding(top = 6.dp)) {
        Text(
            text = "•  ",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$lead. ") }
                append(rest)
            },
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PrivacyLink(url: String, onOpenUrl: (String) -> Unit) {
    Text(
        text = url,
        color = TitleOrange,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .clickable { onOpenUrl(url) }
            .padding(vertical = 2.dp),
    )
}

@Composable
private fun PrivacyRule() {
    HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
}

// Opens a URL in the user's default browser, in its own task, with no app referrer leaked.
private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(Intent.EXTRA_REFERRER, Uri.EMPTY)
    }
    context.startActivity(intent)
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun startServiceThenBattery(context: Context, onStart: () -> Unit) {
    onStart()
    requestBatteryExemption(context)
}

// One-time system prompt to exempt the app from Doze so timers fire reliably.
private fun requestBatteryExemption(context: Context) {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        )
        context.startActivity(intent)
    }
}
