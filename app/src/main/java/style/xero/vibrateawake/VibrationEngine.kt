package style.xero.vibrateawake

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.roundToLong
import kotlin.random.Random

// Drives the vibration schedule for one active session. All scheduling runs on
// the main looper via Handler callbacks, which is the timer source.
class VibrationEngine(context: Context) {

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var config: VibrationConfig = VibrationConfig()

    @Volatile
    private var running = false

    // USAGE_ALARM so the system plays these at full strength from a background
    // service and with the screen off, and lets them through Do Not Disturb.
    // The default (USAGE_UNKNOWN) is attenuated to a barely-perceptible buzz.
    private val alarmAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // Faint heads-up pulse so the main burst never startles the driver mid-steer.
    private val preWarn = Runnable { firePreWarnPulse() }

    // Main alert, then queue the next cycle.
    private val mainAlert = Runnable {
        fireMainVibration()
        scheduleNextCycle()
    }

    fun start(initial: VibrationConfig) {
        config = initial
        running = true
        // Fire the full pattern immediately so the driver feels their chosen
        // rhythm/intensity right away and can adjust before setting off, then
        // fall into the normal randomized cycle.
        fireMainVibration()
        scheduleNextCycle()
    }

    fun stop() {
        running = false
        handler.removeCallbacks(preWarn)
        handler.removeCallbacks(mainAlert)
        vibrator.cancel()
    }

    private fun scheduleNextCycle() {
        if (!running) return
        handler.removeCallbacks(preWarn)
        handler.removeCallbacks(mainAlert)

        val baseMs = (config.intervalMinutes * 60_000).toLong()
        // Anti-habituation: +/- up to 15s so the brain can't anticipate the pulse.
        val jitter = Random.nextLong(-RANDOM_WINDOW_MS, RANDOM_WINDOW_MS + 1)
        val nextDelay = (baseMs + jitter).coerceAtLeast(MIN_DELAY_MS)

        val preDelay = nextDelay - PRE_WARN_LEAD_MS
        if (preDelay > 0) handler.postDelayed(preWarn, preDelay)
        handler.postDelayed(mainAlert, nextDelay)
    }

    private fun firePreWarnPulse() {
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createOneShot(PRE_WARN_MS, PRE_WARN_AMPLITUDE), alarmAttributes)
    }

    private fun fireMainVibration() {
        if (!vibrator.hasVibrator()) return
        val (baseTimings, amplitudes) = config.pattern.waveform()
        // Uniform stretch: scale every segment (buzz and gap alike) so the rhythm's
        // proportions are preserved and only its overall length changes.
        val scale = config.durationScale
        val timings = LongArray(baseTimings.size) { i -> (baseTimings[i] * scale).roundToLong() }
        val finalAmplitudes = if (config.roadNoise == RoadNoise.MAX_HEAVY) {
            // Strip the gentle ascending slopes; every active step fires at full power.
            IntArray(amplitudes.size) { i -> if (amplitudes[i] > 0) 255 else 0 }
        } else {
            amplitudes
        }
        vibrator.vibrate(VibrationEffect.createWaveform(timings, finalAmplitudes, -1), alarmAttributes)
    }

    companion object {
        private const val RANDOM_WINDOW_MS = 15_000L   // +/- jitter around the interval
        private const val MIN_DELAY_MS = 30_000L       // never fire sooner than 30s
        private const val PRE_WARN_LEAD_MS = 15_000L   // faint pulse this far ahead of the alert
        private const val PRE_WARN_MS = 100L           // faint pulse duration
        private const val PRE_WARN_AMPLITUDE = 60      // low amplitude (of 255)
    }
}
