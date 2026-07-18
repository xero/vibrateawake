package style.xero.vibrateawake.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlin.random.Random
import style.xero.vibrateawake.core.VibrationConfig
import style.xero.vibrateawake.core.VibrationTiming

// The watch's local vibrate loop. It is only ever invoked by the tick/pre-warn alarms from
// TickScheduler, which fire even in Doze and grant a brief allowlist to start it in the
// foreground. Each buzz reschedules the next, so the loop survives being killed between
// ticks and never depends on the Bluetooth link once started.
class WatchVibrateService : Service() {

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()

        if (!WatchSession.isRunning(this)) {
            stopSelfCleanly()
            return START_NOT_STICKY
        }
        if (WatchSession.elapsedSinceStart(this) > MAX_SESSION_MS) {
            WatchSession.end(this)
            TickScheduler.cancelAll(this)
            stopSelfCleanly()
            return START_NOT_STICKY
        }

        if (intent?.action == TickScheduler.ACTION_PREWARN) {
            WatchVibrator.firePreWarn(this)
        } else {
            // ACTION_TICK, and the initial immediate tick: buzz, then queue the next cycle.
            val config = WatchSession.config(this)
            WatchVibrator.fireMain(this, config)
            scheduleNextCycle(config)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun scheduleNextCycle(config: VibrationConfig) {
        val baseMs = (config.intervalMinutes * 60_000).toLong()
        val jitter = Random.nextLong(-VibrationTiming.RANDOM_WINDOW_MS, VibrationTiming.RANDOM_WINDOW_MS + 1)
        val nextDelay = (baseMs + jitter).coerceAtLeast(VibrationTiming.MIN_DELAY_MS)
        val preDelay = nextDelay - VibrationTiming.PRE_WARN_LEAD_MS
        if (preDelay > 0) TickScheduler.schedulePreWarnIn(this, preDelay)
        TickScheduler.scheduleTickIn(this, nextDelay)
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopSelfCleanly() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_watch_alert)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "vibrate_awake_watch"
        private const val NOTIFICATION_ID = 201

        // Safety ceiling: if the phone dies without sending STOP, the watch stops itself
        // after this long rather than buzzing indefinitely.
        private const val MAX_SESSION_MS = 8L * 60L * 60L * 1000L
    }
}
