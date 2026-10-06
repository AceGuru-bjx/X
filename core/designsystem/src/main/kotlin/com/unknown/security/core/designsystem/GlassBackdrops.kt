package com.unknown.security.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * Backdrop for in-content glass: samples the animated liquid background.
 * Provided by [GlassScaffold].
 */
val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * Backdrop for chrome (top bar / bottom nav / FAB): samples the liquid
 * background **plus** the content scrolling underneath. Provided by [GlassScaffold].
 */
val LocalChromeBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * The canonical liquid-glass modifier of the design system:
 * vibrancy + soft blur + refraction lens over the live backdrop.
 *
 * This is true liquid glass (refraction with chromatic dispersion),
 * not a frosted/translucent surface.
 */
fun Modifier.liquidGlass(
    backdrop: Backdrop?,
    shape: () -> Shape,
    refractionHeight: Dp = 14.dp,
    refractionAmount: Dp = 28.dp,
    blurRadius: Dp = 3.dp,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    chromaticAberration: Boolean = true,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
): Modifier {
    if (backdrop == null) {
        return this
            .clip(shape())
            .background(surfaceColor.takeIf { it.isSpecified } ?: Color(0xD9142119))
    }
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = shape,
        effects = {
            vibrancy()
            blur(blurRadius.toPx())
            lens(
                refractionHeight = refractionHeight.toPx(),
                refractionAmount = refractionAmount.toPx(),
                depthEffect = false,
                chromaticAberration = chromaticAberration,
            )
        },
        layerBlock = layerBlock,
        onDrawSurface = {
            if (tint.isSpecified) {
                drawRect(tint, blendMode = BlendMode.Hue)
                drawRect(tint.copy(alpha = 0.75f))
            }
            if (surfaceColor.isSpecified) {
                drawRect(surfaceColor)
            }
        },
    )
}

/** Reads the ambient backdrop for in-content glass. */
@Composable
fun ambientGlassBackdrop(): Backdrop? = LocalGlassBackdrop.current

/** Reads the ambient backdrop for chrome glass. */
@Composable
fun chromeGlassBackdrop(): Backdrop? = LocalChromeBackdrop.current

/** Recommended translucent surface tint for glass in the current theme. */
@Composable
fun glassSurfaceTint(darkTheme: Boolean = isSystemInDarkTheme()): Color =
    if (darkTheme) {
        Color(0xB3101A15)
    } else {
        Color(0xCCFFFFFF)
    }
