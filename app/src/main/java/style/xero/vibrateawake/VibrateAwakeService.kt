package style.xero.vibrateawake

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

// Foreground service that houses the vibration engine and a partial wake lock, so
// the alerts keep firing while the phone is locked. Stopped in-app or from the
// notification's Stop action.
class VibrateAwakeService : Service() {

    private lateinit var engine: VibrationEngine
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        engine = VibrationEngine(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val config = intent?.let(::readConfig) ?: VibrationConfig()

        startAsForeground()
        acquireWakeLock()
        engine.start(config)
        ServiceState.setRunning(true)

        // Redeliver the config-bearing intent if the process is killed and restarted.
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        engine.stop()
        releaseWakeLock()
        ServiceState.setRunning(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val notification = buildNotification()
        // Android 14+ requires the foreground-service type to be declared at start.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            stopIntent(this),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_stat_alert)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.notification_stop), stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            // LOW keeps the ongoing notification quiet (no sound/heads-up).
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_desc)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @SuppressLint("WakelockTimeout") // Held for the service lifetime, released in onDestroy.
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG).apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun readConfig(intent: Intent): VibrationConfig {
        val interval = intent.getDoubleExtra(EXTRA_INTERVAL, 3.0)
        val pattern = PatternStyle.entries[
            intent.getIntExtra(EXTRA_PATTERN, PatternStyle.STACCATO.ordinal),
        ]
        val roadNoise = RoadNoise.entries[
            intent.getIntExtra(EXTRA_ROAD_NOISE, RoadNoise.ADAPTIVE.ordinal),
        ]
        val durationScale = intent.getDoubleExtra(EXTRA_DURATION_SCALE, 2.0)
        return VibrationConfig(interval, pattern, roadNoise, durationScale)
    }

    companion object {
        const val ACTION_STOP = "style.xero.vibrateawake.action.STOP"

        private const val EXTRA_INTERVAL = "extra_interval_minutes"
        private const val EXTRA_PATTERN = "extra_pattern"
        private const val EXTRA_ROAD_NOISE = "extra_road_noise"
        private const val EXTRA_DURATION_SCALE = "extra_duration_scale"

        private const val CHANNEL_ID = "vibrate_awake_service"
        private const val NOTIFICATION_ID = 101
        private const val WAKELOCK_TAG = "VibrateAwake::AlertWakeLock"

        // Start intent carrying the active config as extras.
        fun startIntent(context: Context, config: VibrationConfig): Intent =
            Intent(context, VibrateAwakeService::class.java).apply {
                putExtra(EXTRA_INTERVAL, config.intervalMinutes)
                putExtra(EXTRA_PATTERN, config.pattern.ordinal)
                putExtra(EXTRA_ROAD_NOISE, config.roadNoise.ordinal)
                putExtra(EXTRA_DURATION_SCALE, config.durationScale)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, VibrateAwakeService::class.java).setAction(ACTION_STOP)
    }
}
