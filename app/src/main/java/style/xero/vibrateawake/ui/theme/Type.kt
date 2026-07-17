package style.xero.vibrateawake.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import style.xero.vibrateawake.R

// Embedded display face, used only for the app title and the Start/Stop button.
val Thunderline = FontFamily(Font(R.font.thunderline))

// Everything else renders in Roboto, which is FontFamily.Default on Android.
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
)
