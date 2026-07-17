package style.xero.vibrateawake

import android.app.Application
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    val config: StateFlow<VibrationConfig> = repo.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VibrationConfig(),
    )

    val isRunning: StateFlow<Boolean> = ServiceState.isRunning

    fun setInterval(minutes: Double) = persist { it.copy(intervalMinutes = minutes) }
    fun setPattern(pattern: PatternStyle) = persist { it.copy(pattern = pattern) }
    fun setRoadNoise(roadNoise: RoadNoise) = persist { it.copy(roadNoise = roadNoise) }

    private fun persist(transform: (VibrationConfig) -> VibrationConfig) {
        viewModelScope.launch { repo.update(transform(config.value)) }
    }

    fun start() {
        val app = getApplication<Application>()
        ContextCompat.startForegroundService(app, VibrateAwakeService.startIntent(app, config.value))
    }

    fun stop() {
        val app = getApplication<Application>()
        // The service is already running in the foreground, so a plain start delivers
        // the STOP action without the startForegroundService 5s-notification contract.
        app.startService(VibrateAwakeService.stopIntent(app))
    }
}
