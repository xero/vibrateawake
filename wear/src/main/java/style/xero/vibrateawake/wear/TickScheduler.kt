package style.xero.vibrateawake.wear

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock

// Schedules the next buzz (and its faint pre-warn) with exact, doze-proof alarms delivered
// to WatchVibrateService. Exact alarms fire even in Wear Doze; firing one also grants a
// brief allowlist to (re)start the foreground service if the system had killed it.
object TickScheduler {

    const val ACTION_TICK = "style.xero.vibrateawake.wear.TICK"
    const val ACTION_PREWARN = "style.xero.vibrateawake.wear.PREWARN"

    private const val RC_TICK = 1
    private const val RC_PREWARN = 2

    fun scheduleImmediateTick(context: Context) = scheduleTickIn(context, 0L)

    fun scheduleTickIn(context: Context, delayMs: Long) =
        scheduleAt(context, ACTION_TICK, RC_TICK, delayMs)

    fun schedulePreWarnIn(context: Context, delayMs: Long) =
        scheduleAt(context, ACTION_PREWARN, RC_PREWARN, delayMs)

    fun cancelAll(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(pending(context, ACTION_TICK, RC_TICK))
        am.cancel(pending(context, ACTION_PREWARN, RC_PREWARN))
    }

    private fun scheduleAt(context: Context, action: String, requestCode: Int, delayMs: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val triggerAt = SystemClock.elapsedRealtime() + delayMs.coerceAtLeast(0L)
        val pi = pending(context, action, requestCode)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
        }
    }

    private fun pending(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, WatchVibrateService::class.java).setAction(action)
        return PendingIntent.getForegroundService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
