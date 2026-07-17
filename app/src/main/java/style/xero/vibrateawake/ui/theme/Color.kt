package style.xero.vibrateawake.ui.theme

import androidx.compose.ui.graphics.Color

// Greyscale palette. Pure tones for headers/buttons, off-tones for body text.
val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)
val OffBlack = Color(0xFF222222)
val OffWhite = Color(0xFFEFEFEF)

// Two button greys so the pure black/white label always has strong contrast:
// a lighter grey behind black text in light mode, a darker grey behind white in dark.
val ButtonGrey = Color(0xFF6E6E6E)       // dark mode button + selection accents
val ButtonGreyLight = Color(0xFF9E9E9E)  // light mode button
