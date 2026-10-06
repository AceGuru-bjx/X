package com.unknown.security.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.unknown.security.core.model.ThreatSeverity
import com.unknown.security.core.model.VerdictLevel

/** Color language for severity & verdicts — used by badges, charts, alerts. */
object SeverityStyle {
    fun severityColor(severity: ThreatSeverity): Color =
        when (severity) {
            ThreatSeverity.LOW -> Color(0xFF8FD6B4)
            ThreatSeverity.MEDIUM -> Color(0xFFF5C46B)
            ThreatSeverity.HIGH -> Color(0xFFFB8C5A)
            ThreatSeverity.CRITICAL -> Color(0xFFFF6B7A)
        }

    fun verdictColor(level: VerdictLevel): Color =
        when (level) {
            VerdictLevel.CLEAN -> Color(0xFF2DD4A7)
            VerdictLevel.SUSPICIOUS -> Color(0xFFF5C46B)
            VerdictLevel.DANGEROUS -> Color(0xFFFF6B7A)
        }

    fun verdictEmoji(level: VerdictLevel): String =
        when (level) {
            VerdictLevel.CLEAN -> "✓"
            VerdictLevel.SUSPICIOUS -> "!"
            VerdictLevel.DANGEROUS -> "✕"
        }
}

/** Glass badge showing a verdict level. */
@Composable
fun VerdictBadge(
    level: VerdictLevel,
    modifier: Modifier = Modifier,
) {
    GlassChip(
        text = "${SeverityStyle.verdictEmoji(level)} ${level.label}",
        modifier = modifier,
        tint = SeverityStyle.verdictColor(level),
    )
}

/** Glass badge showing a severity. */
@Composable
fun SeverityBadge(
    severity: ThreatSeverity,
    modifier: Modifier = Modifier,
) {
    GlassChip(
        text = severity.label,
        modifier = modifier,
        tint = SeverityStyle.severityColor(severity),
    )
}

@Composable
fun onSurfaceText(): Color = MaterialTheme.colorScheme.onSurface
