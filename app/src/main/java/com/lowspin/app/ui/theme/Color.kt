package com.lowspin.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Lowspin palette. Dark only, calm and quiet.
 * Accent is reserved for player controls and selected/active states.
 */
val LowspinBackground = Color(0xFF0E0B14)
val LowspinSurface = Color(0xFF1A1625)
val LowspinSurfaceHigh = Color(0xFF262033)
val LowspinAccent = Color(0xFFA78BFA)
val LowspinAccentPressed = Color(0xFF7C5CD6)
val LowspinOnAccent = Color(0xFF0E0B14)
val LowspinTextPrimary = Color(0xFFFFFFFF)
val LowspinTextSecondary = Color(0xFFA8A8B3)
val LowspinTextMuted = Color(0xFF7E7A8C)
val LowspinError = Color(0xFFFF6B6B)
val LowspinDivider = LowspinSurfaceHigh

// Backwards-compatible semantic aliases used by the existing screens.
val LowspinSurfaceElevated get() = LowspinSurfaceHigh
val LowspinSurfacePressed get() = LowspinSurfaceHigh
