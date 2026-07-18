package style.xero.vibrateawake.wear

import android.content.Intent
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import style.xero.vibrateawake.core.SessionSync
import style.xero.vibrateawake.core.SessionState

// Wakes (and cold-starts if needed) when the phone writes the /session data item, then
// translates the phone's START/STOP into the watch's own local vibrate service and alarms.
class SessionListenerService : WearableListenerService() {

    override fun onDataChanged(events: DataEventBuffer) {
        for (event in events) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            if (event.dataItem.uri.path != SessionSync.PATH) continue

            val map = DataMapItem.fromDataItem(event.dataItem).dataMap
            when (SessionSync.readState(map)) {
                SessionState.STARTED -> {
                    WatchSession.begin(this, SessionSync.readConfig(map))
                    // Kick the first buzz via an immediate exact alarm; the alarm's brief
                    // allowlist lets it start the foreground service from this background context.
                    TickScheduler.scheduleImmediateTick(this)
                }
                SessionState.STOPPED -> {
                    WatchSession.end(this)
                    TickScheduler.cancelAll(this)
                    stopService(Intent(this, WatchVibrateService::class.java))
                }
            }
        }
    }
}
