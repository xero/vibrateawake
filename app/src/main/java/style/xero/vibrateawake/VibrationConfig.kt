package style.xero.vibrateawake

// The three configuration knobs, plus the physical waveform data for each rhythm.

enum class PatternStyle {
    STACCATO,   // escalating heartbeat: soft, medium, then hard
    SIREN,      // continuous wave rising then falling (UI label: Continuous Wave)
    SOS_PULSE;  // three fast max-intensity jabs

    // (timings, amplitudes) for VibrationEffect.createWaveform. Index 0 is an
    // initial delay at amplitude 0. Values adapted from TASK.md.
    fun waveform(): Pair<LongArray, IntArray> = when (this) {
        STACCATO -> longArrayOf(0, 150, 100, 150, 100, 400) to intArrayOf(0, 100, 0, 180, 0, 255)
        SIREN -> longArrayOf(0, 200, 200, 200, 400, 200, 200) to intArrayOf(0, 80, 140, 200, 255, 180, 90)
        SOS_PULSE -> longArrayOf(0, 80, 80, 80, 80, 80) to intArrayOf(0, 255, 0, 255, 0, 255)
    }
}

enum class RoadNoise {
    ADAPTIVE,   // use the pattern's ascending amplitudes as-is
    MAX_HEAVY,  // force every active pulse to full 255 to punch through cabin noise
}

data class VibrationConfig(
    val intervalMinutes: Double = 3.0,          // 1.0 to 10.0
    val pattern: PatternStyle = PatternStyle.STACCATO,
    val roadNoise: RoadNoise = RoadNoise.ADAPTIVE,
)
