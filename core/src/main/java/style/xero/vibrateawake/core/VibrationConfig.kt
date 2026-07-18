package style.xero.vibrateawake.core

import kotlin.math.roundToLong

// The configuration knobs and the physical waveform data for each rhythm. Shared by
// the phone app and the Wear OS companion so the two never drift as the waveforms are tuned.

enum class PatternStyle {
    STACCATO,   // escalating heartbeat: soft, medium, then hard
    SIREN,      // continuous wave rising then falling (UI label: Continuous Wave)
    SOS_PULSE;  // three fast max-intensity jabs

    // (timings, amplitudes) for VibrationEffect.createWaveform. Index 0 is an
    // initial delay at amplitude 0.
    fun waveform(): Pair<LongArray, IntArray> = when (this) {
        STACCATO -> longArrayOf(0, 150, 100, 150, 100, 400) to intArrayOf(0, 100, 0, 180, 0, 255)
        SIREN -> longArrayOf(0, 200, 200, 200, 400, 200, 200) to intArrayOf(0, 80, 140, 200, 255, 180, 90)
        SOS_PULSE -> longArrayOf(0, 80, 80, 80, 80, 80) to intArrayOf(0, 255, 0, 255, 0, 255)
    }
}

// Vibration intensity. Enum name kept for stable DataStore/Intent ordinals.
enum class RoadNoise {
    ADAPTIVE,   // Standard: use each pattern's natural amplitude curve (soft to strong)
    MAX_HEAVY,  // Maximum: force every active pulse to full 255
}

data class VibrationConfig(
    val intervalMinutes: Double = 3.0,          // 1.0 to 25.0
    val pattern: PatternStyle = PatternStyle.STACCATO,
    val roadNoise: RoadNoise = RoadNoise.ADAPTIVE,
    // Uniformly time-stretches the selected rhythm so each buzz runs longer (and,
    // via temporal summation, feels stronger). 1.0 is the raw waveform; default 2.0
    // is twice as long, matching tester feedback that the alerts were too short.
    val durationScale: Double = 2.0,            // 1.0 to 8.0
)

// Builds the final (timings, amplitudes) actually handed to VibrationEffect.createWaveform:
// the whole waveform is uniformly time-stretched by durationScale (buzz and gap alike, so
// the rhythm's proportions hold), and Maximum forces every active step to full 255.
fun VibrationConfig.buildWaveform(): Pair<LongArray, IntArray> {
    val (baseTimings, amplitudes) = pattern.waveform()
    val timings = LongArray(baseTimings.size) { i -> (baseTimings[i] * durationScale).roundToLong() }
    val finalAmplitudes = if (roadNoise == RoadNoise.MAX_HEAVY) {
        IntArray(amplitudes.size) { i -> if (amplitudes[i] > 0) 255 else 0 }
    } else {
        amplitudes
    }
    return timings to finalAmplitudes
}

// Collapses a (timings, amplitudes) waveform into a pure on/off timing array, merging
// consecutive segments of the same state and always starting with an OFF segment. Used on
// Wear devices that report no amplitude control, where only on/off timing survives. A
// varying-amplitude rhythm like the siren becomes one sustained buzz rather than fragments.
fun onOffTimings(timings: LongArray, amplitudes: IntArray): LongArray {
    val merged = ArrayList<Long>()
    var currentOn = false   // durations alternate OFF, ON, OFF, ...
    var currentDur = 0L
    for (i in timings.indices) {
        val on = amplitudes[i] > 0
        if (on == currentOn) {
            currentDur += timings[i]
        } else {
            merged.add(currentDur)
            currentOn = on
            currentDur = timings[i]
        }
    }
    merged.add(currentDur)
    return merged.toLongArray()
}

// Scheduling constants shared by both engines so the phone and watch cycle identically.
object VibrationTiming {
    const val RANDOM_WINDOW_MS = 15_000L   // +/- jitter around the interval
    const val MIN_DELAY_MS = 30_000L       // never fire sooner than 30s
    const val PRE_WARN_LEAD_MS = 15_000L   // faint pulse this far ahead of the alert
    const val PRE_WARN_MS = 100L           // faint pulse duration
    const val PRE_WARN_AMPLITUDE = 60      // low amplitude (of 255)
}
