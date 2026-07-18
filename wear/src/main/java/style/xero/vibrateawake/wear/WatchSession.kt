package style.xero.vibrateawake.wear

import android.content.Context
import android.os.SystemClock
import style.xero.vibrateawake.core.PatternStyle
import style.xero.vibrateawake.core.RoadNoise
import style.xero.vibrateawake.core.VibrationConfig

// Persisted session state on the watch. The vibrate service can be killed and restarted
// by the tick alarm between buzzes, so it reads its config and start time from here rather
// than from the launch intent, which the alarm does not carry.
object WatchSession {

    private const val PREFS = "watch_session"
    private const val K_RUNNING = "running"
    private const val K_INTERVAL = "interval"
    private const val K_PATTERN = "pattern"
    private const val K_ROAD_NOISE = "road_noise"
    private const val K_DURATION = "duration_scale"
    private const val K_START_ELAPSED = "start_elapsed"

    fun begin(context: Context, config: VibrationConfig) {
        prefs(context).edit()
            .putBoolean(K_RUNNING, true)
            .putFloat(K_INTERVAL, config.intervalMinutes.toFloat())
            .putInt(K_PATTERN, config.pattern.ordinal)
            .putInt(K_ROAD_NOISE, config.roadNoise.ordinal)
            .putFloat(K_DURATION, config.durationScale.toFloat())
            .putLong(K_START_ELAPSED, SystemClock.elapsedRealtime())
            .apply()
    }

    fun end(context: Context) {
        prefs(context).edit().putBoolean(K_RUNNING, false).apply()
    }

    fun isRunning(context: Context): Boolean = prefs(context).getBoolean(K_RUNNING, false)

    fun config(context: Context): VibrationConfig {
        val p = prefs(context)
        return VibrationConfig(
            intervalMinutes = p.getFloat(K_INTERVAL, 3f).toDouble(),
            pattern = PatternStyle.entries.getOrElse(p.getInt(K_PATTERN, 0)) { PatternStyle.STACCATO },
            roadNoise = RoadNoise.entries.getOrElse(p.getInt(K_ROAD_NOISE, 0)) { RoadNoise.ADAPTIVE },
            durationScale = p.getFloat(K_DURATION, 2f).toDouble(),
        )
    }

    fun elapsedSinceStart(context: Context): Long =
        SystemClock.elapsedRealtime() - prefs(context).getLong(K_START_ELAPSED, SystemClock.elapsedRealtime())

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
