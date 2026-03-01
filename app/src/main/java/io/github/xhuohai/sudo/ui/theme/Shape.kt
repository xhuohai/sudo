package io.github.xhuohai.sudo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    // Extra small - chips, small buttons
    extraSmall = RoundedCornerShape(4.dp),
    // Small - cards, dialogs 
    small = RoundedCornerShape(8.dp),
    // Medium - floating action buttons
    medium = RoundedCornerShape(12.dp),
    // Large - cards, sheets
    large = RoundedCornerShape(16.dp),
    // Extra large - large sheets
    extraLarge = RoundedCornerShape(24.dp)
)

// Custom corner radii for specific components
object CornerRadius {
    val Card = 16.dp
    val Button = 12.dp
    val TextField = 12.dp
    val BottomSheet = 24.dp
    val Dialog = 24.dp
    val Avatar = 50.dp // Full circle for avatars
    val Image = 12.dp
    val Chip = 8.dp
}
