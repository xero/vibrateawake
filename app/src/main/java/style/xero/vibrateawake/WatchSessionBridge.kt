package style.xero.vibrateawake

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import style.xero.vibrateawake.core.SessionSync
import style.xero.vibrateawake.core.VibrationConfig

// Best-effort phone->watch signalling over the Data Layer. Writes a single /session
// item that a paired watch running the companion app observes: STARTED (with the active
// config) when a session begins, STOPPED when it ends. When no watch is connected this
// is harmless; the item just has nothing to sync to. setUrgent() pushes the sync now
// rather than opportunistically, so the wrist reacts promptly.
class WatchSessionBridge(context: Context) {

    private val dataClient = Wearable.getDataClient(context.applicationContext)

    fun start(config: VibrationConfig) {
        val request = PutDataMapRequest.create(SessionSync.PATH).apply {
            SessionSync.writeStarted(dataMap, config, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(request)
            .addOnFailureListener { Log.w(TAG, "watch start signal failed", it) }
    }

    fun stop() {
        val request = PutDataMapRequest.create(SessionSync.PATH).apply {
            SessionSync.writeStopped(dataMap)
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(request)
            .addOnFailureListener { Log.w(TAG, "watch stop signal failed", it) }
    }

    private companion object {
        const val TAG = "WatchSessionBridge"
    }
}
