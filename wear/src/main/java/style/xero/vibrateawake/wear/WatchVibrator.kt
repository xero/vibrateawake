package style.xero.vibrateawake.wear

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import style.xero.vibrateawake.core.VibrationConfig
import style.xero.vibrateawake.core.VibrationTiming
import style.xero.vibrateawake.core.buildWaveform
import style.xero.vibrateawake.core.onOffTimings

// Fires the watch's own vibrator. USAGE_ALARM so it plays strongly with the screen off and
// through most suppression, mirroring the phone. Watch actuators frequently report no
// amplitude control, so we fall back to an on/off timing pattern at the actuator's fixed
// strength rather than letting the tuned envelope silently degrade.
object WatchVibrator {

    private val alarmAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun fireMain(context: Context, config: VibrationConfig) {
        val vibrator = vibrator(context)
        if (!vibrator.hasVibrator()) return
        val (timings, amplitudes) = config.buildWaveform()
        val effect = if (vibrator.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(onOffTimings(timings, amplitudes), -1)
        }
        withWakeLock(context, WAKE_MS_MAIN) { vibrator.vibrate(effect, alarmAttributes) }
    }

    fun firePreWarn(context: Context) {
        val vibrator = vibrator(context)
        if (!vibrator.hasVibrator()) return
        val amplitude = if (vibrator.hasAmplitudeControl()) {
            VibrationTiming.PRE_WARN_AMPLITUDE
        } else {
            VibrationEffect.DEFAULT_AMPLITUDE
        }
        val effect = VibrationEffect.createOneShot(VibrationTiming.PRE_WARN_MS, amplitude)
        withWakeLock(context, WAKE_MS_PRE) { vibrator.vibrate(effect, alarmAttributes) }
    }

    private inline fun withWakeLock(context: Context, timeoutMs: Long, block: () -> Unit) {
        val pm = context.getSystemService(PowerManager::class.java)
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_TAG)
        wl.acquire(timeoutMs)
        try {
            block()
        } finally {
            if (wl.isHeld) wl.release()
        }
    }

    private fun vibrator(context: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    private const val WAKE_TAG = "VibrateAwake::WatchBuzz"
    private const val WAKE_MS_MAIN = 12_000L
    private const val WAKE_MS_PRE = 1_000L
}
