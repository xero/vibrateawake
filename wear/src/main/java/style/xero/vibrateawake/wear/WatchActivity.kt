package style.xero.vibrateawake.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The companion is driven entirely by the phone. This screen exists only so the app has a
// launcher entry and opens without crashing (Wear quality WO-P2); there are no controls.
class WatchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "VIBRATE AWAKE\n\nControlled from\nyour phone",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
    }
}
