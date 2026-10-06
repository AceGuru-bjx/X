package com.unknown.security.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle

/** Standard glass corner radius used across the app. */
val GlassCornerRadius = 22.dp

/** Container-level glass card — the default surface for content blocks. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = GlassCornerRadius,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    onPressScale: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val backdrop = ambientGlassBackdrop()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onPressScale) 0.97f else 1f,
        label = "glassCardScale",
    )

    val glassModifier =
        Modifier
            .liquidGlass(
                backdrop = backdrop,
                shape = { RoundedRectangle(cornerRadius) },
                tint = tint,
                surfaceColor = surfaceColor.takeIf { it.isSpecified } ?: defaultGlassSurface(),
            )

    val clickableModifier =
        if (onClick != null) {
            Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                )
        } else {
            Modifier
        }

    Column(
        modifier
            .then(glassModifier)
            .then(clickableModifier),
    ) {
        content()
    }
}

/** Pill glass button — the primary action control. */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    emphasized: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val backdrop = ambientGlassBackdrop()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        label = "glassButtonScale",
    )

    Row(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.liquidGlass(
                backdrop = backdrop,
                shape = { Capsule() },
                tint =
                    tint.takeIf { it.isSpecified } ?: if (emphasized) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Unspecified
                    },
                surfaceColor =
                    if (emphasized) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                    } else {
                        defaultGlassSurface()
                    },
            ).heightIn(min = 48.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Small glass chip for tags/capability badges. */
@Composable
fun GlassChip(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val backdrop = ambientGlassBackdrop()
    Row(
        modifier
            .liquidGlass(
                backdrop = backdrop,
                shape = { Capsule() },
                surfaceColor = defaultGlassSurface(),
                tint = tint,
            ).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** One list row rendered on glass. */
@Composable
fun GlassListItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    headline: String,
    supporting: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val backdrop = ambientGlassBackdrop()
    Row(
        modifier
            .liquidGlass(
                backdrop = backdrop,
                shape = { RoundedRectangle(18.dp) },
                surfaceColor = defaultGlassSurface(),
            ).then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.padding(horizontal = 8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
internal fun defaultGlassSurface(): Color =
    if (isSystemInDarkTheme()) {
        Color(0xB30F1A15)
    } else {
        Color(0xCCFFFFFF)
    }
