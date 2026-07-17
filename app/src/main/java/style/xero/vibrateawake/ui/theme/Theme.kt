package style.xero.vibrateawake.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Fixed greyscale, no Material You dynamic color. background/onBackground carry
// the page and body text; primary/onPrimary carry the grey button and its label.
// surfaceVariant/onSurfaceVariant are pinned to greys so the Slider inactive track
// and RadioButton rings never leak the default purple tint.
private val LightColors = lightColorScheme(
    background = White,
    surface = White,
    onBackground = OffBlack,
    onSurface = OffBlack,
    primary = ButtonGrey,
    onPrimary = Black,
    surfaceVariant = Color(0xFFCFCFCF),
    onSurfaceVariant = Color(0xFF444444),
    outline = Color(0xFF888888),
)

private val DarkColors = darkColorScheme(
    background = Black,
    surface = Black,
    onBackground = OffWhite,
    onSurface = OffWhite,
    primary = ButtonGrey,
    onPrimary = White,
    surfaceVariant = Color(0xFF3A3A3A),
    onSurfaceVariant = Color(0xFFBBBBBB),
    outline = Color(0xFF888888),
)

@Composable
fun VibrateAwakeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}

// The "pure" tone for headers and button text: black on a light page, white on a dark one.
@Composable
fun pureContentColor(): Color = if (isSystemInDarkTheme()) White else Black

// Button background: a lighter grey in light mode so the black label stays readable.
@Composable
fun buttonContainerColor(): Color = if (isSystemInDarkTheme()) ButtonGrey else ButtonGreyLight
