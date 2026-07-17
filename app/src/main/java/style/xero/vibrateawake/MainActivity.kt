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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import style.xero.vibrateawake.ui.theme.Thunderline
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

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {

            Text(
                text = "VIBRATE AWAKE",
                fontFamily = Thunderline,
                fontSize = 54.sp,
                color = pureContentColor(),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 20.dp),
            )
            HorizontalDivider()

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
                    title = "Road Noise Level",
                    options = listOf(
                        RoadNoise.ADAPTIVE to "Standard (Adaptive)",
                        RoadNoise.MAX_HEAVY to "Heavy Cabin Noise (Max Power)",
                    ),
                    selected = config.roadNoise,
                    enabled = !isRunning,
                    onSelect = onRoadNoiseChange,
                )
            }

            HorizontalDivider()
            Button(
                onClick = { if (isRunning) onStop() else onStartClick() },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonContainerColor(),
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

@Composable
private fun FatigueSection(
    intervalMinutes: Double,
    enabled: Boolean,
    onIntervalChange: (Double) -> Unit,
) {
    val presets = listOf(3.0 to "3 Min", 5.0 to "5 Min", 10.0 to "10 Min")
    SectionHeader("Fatigue Level")

    RadioColumn(
        options = presets,
        selected = presets.map { it.first }.firstOrNull { it == intervalMinutes },
        enabled = enabled,
        onSelect = onIntervalChange,
    )

    Text(
        text = "Custom: ${formatMinutes(intervalMinutes)} min",
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 8.dp),
    )
    Slider(
        value = intervalMinutes.toFloat(),
        onValueChange = { onIntervalChange((it * 2).roundToInt() / 2.0) },
        valueRange = 1f..10f,
        steps = 17, // 0.5-minute (30s) increments across 1..10
        enabled = enabled,
        // Explicit greyscale; the default inactive track is a purple-tinted role.
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.onBackground,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            activeTickColor = MaterialTheme.colorScheme.onPrimary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Caption("Extreme Fatigue")
        Caption("Preventative")
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
    SectionHeader(title)
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
                RadioButton(selected = value == selected, onClick = null, enabled = enabled)
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = pureContentColor(),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun Caption(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.bodySmall,
    )
}

private fun formatMinutes(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

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
