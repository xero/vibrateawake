package style.xero.vibrateawake.core

import com.google.android.gms.wearable.DataMap

// The phone->watch wire format, carried in a single DataClient item at PATH. The phone
// writes STARTED (with the active config) when a session begins and STOPPED when it ends;
// the watch reacts. Kept here so both sides serialize identically.
object SessionSync {

    const val PATH = "/session"

    private const val KEY_STATE = "state"
    private const val KEY_INTERVAL = "interval_minutes"
    private const val KEY_PATTERN = "pattern"
    private const val KEY_ROAD_NOISE = "road_noise"
    private const val KEY_DURATION_SCALE = "duration_scale"
    private const val KEY_STARTED_AT = "started_at"

    fun writeStarted(map: DataMap, config: VibrationConfig, startedAt: Long) {
        map.putInt(KEY_STATE, SessionState.STARTED.ordinal)
        map.putDouble(KEY_INTERVAL, config.intervalMinutes)
        map.putInt(KEY_PATTERN, config.pattern.ordinal)
        map.putInt(KEY_ROAD_NOISE, config.roadNoise.ordinal)
        map.putDouble(KEY_DURATION_SCALE, config.durationScale)
        // A fresh timestamp per start guarantees the item's bytes change, so onDataChanged fires.
        map.putLong(KEY_STARTED_AT, startedAt)
    }

    fun writeStopped(map: DataMap) {
        map.putInt(KEY_STATE, SessionState.STOPPED.ordinal)
        map.putLong(KEY_STARTED_AT, 0L)
    }

    fun readState(map: DataMap): SessionState =
        SessionState.entries.getOrElse(map.getInt(KEY_STATE, SessionState.STOPPED.ordinal)) {
            SessionState.STOPPED
        }

    fun readConfig(map: DataMap): VibrationConfig = VibrationConfig(
        intervalMinutes = map.getDouble(KEY_INTERVAL, 3.0),
        pattern = PatternStyle.entries.getOrElse(map.getInt(KEY_PATTERN, 0)) { PatternStyle.STACCATO },
        roadNoise = RoadNoise.entries.getOrElse(map.getInt(KEY_ROAD_NOISE, 0)) { RoadNoise.ADAPTIVE },
        durationScale = map.getDouble(KEY_DURATION_SCALE, 2.0),
    )

    fun readStartedAt(map: DataMap): Long = map.getLong(KEY_STARTED_AT, 0L)
}

enum class SessionState { STARTED, STOPPED }
