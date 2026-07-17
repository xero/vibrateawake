package style.xero.vibrateawake

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// One DataStore for the whole process; the delegate lazily creates it per Context.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Persists the three knobs so the last-used config is restored on the next launch.
class SettingsRepository(private val context: Context) {

    val config: Flow<VibrationConfig> = context.dataStore.data.map { prefs ->
        VibrationConfig(
            intervalMinutes = prefs[KEY_INTERVAL] ?: 3.0,
            pattern = PatternStyle.entries[prefs[KEY_PATTERN] ?: PatternStyle.STACCATO.ordinal],
            roadNoise = RoadNoise.entries[prefs[KEY_ROAD_NOISE] ?: RoadNoise.ADAPTIVE.ordinal],
        )
    }

    suspend fun update(config: VibrationConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_INTERVAL] = config.intervalMinutes
            prefs[KEY_PATTERN] = config.pattern.ordinal
            prefs[KEY_ROAD_NOISE] = config.roadNoise.ordinal
        }
    }

    private companion object {
        val KEY_INTERVAL = doublePreferencesKey("interval_minutes")
        val KEY_PATTERN = intPreferencesKey("pattern")
        val KEY_ROAD_NOISE = intPreferencesKey("road_noise")
    }
}
