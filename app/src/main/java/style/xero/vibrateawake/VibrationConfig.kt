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
    val durationScale: Double = 2.0,            // 1.0 to 3.0
)
