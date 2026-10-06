package com.unknown.security.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * The global glass scaffold — the ONLY screen scaffold of the whole app.
 *
 * Layout:
 * ```
 * Box
 * ├── LiquidBackground (recorded as glass backdrop)   ← in-content glass samples this
 * ├── content Box (recorded as content backdrop)      ← chrome glass also samples this
 * ├── topBar    (chrome glass)
 * └── bottomBar (chrome glass)
 * ```
 *
 * Chrome glass therefore refracts both the living background AND the content
 * scrolling beneath it; in-content glass cards refract the background only,
 * which avoids self-referential sampling.
 */
@Composable
fun GlassScaffold(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    background: @Composable (LayerBackdrop) -> Unit = { LiquidBackground(Modifier.fillMaxSize()) },
    content: @Composable BoxScope.() -> Unit,
) {
    val glassBackdrop = rememberLayerBackdrop()
    val contentBackdrop = rememberLayerBackdrop()
    val chromeBackdrop = rememberCombinedBackdrop(glassBackdrop, contentBackdrop)

    CompositionLocalProvider(
        LocalGlassBackdrop provides glassBackdrop,
        LocalChromeBackdrop provides chromeBackdrop,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(glassBackdrop),
            ) {
                background(glassBackdrop)
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(contentBackdrop),
            ) {
                content()
            }

            topBar?.invoke()

            bottomBar?.invoke()
        }
    }
}
