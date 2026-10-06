package com.unknown.security.feature.shield

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.unknown.security.core.designsystem.GlassCard
import com.unknown.security.core.designsystem.GlassChip
import com.unknown.security.core.designsystem.GlassListItem
import com.unknown.security.core.designsystem.GlassScaffold
import com.unknown.security.core.designsystem.GlassTopBar
import com.unknown.security.core.designsystem.SeverityStyle
import com.unknown.security.core.model.InterceptionEvent
import com.unknown.security.core.model.VerdictLevel
import com.unknown.security.data.repository.GuardRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Live interception feed — the audit trail of every guard decision. */
@Composable
fun ShieldScreen(
    repository: GuardRepository,
    modifier: Modifier = Modifier,
) {
    val vm: ShieldViewModel =
        viewModel(
            factory = viewModelFactory { initializer { ShieldViewModel(repository) } },
        )
    val events by vm.events.collectAsState()
    val foreground by vm.foreground.collectAsState()

    LaunchedEffect(Unit) { vm.refresh() }

    GlassScaffold(
        modifier = modifier,
        topBar = {
            GlassTopBar(
                title = "拦截日志",
                subtitle = if (foreground != null) "前台：$foreground" else "前台监控未激活",
                actions = {
                    IconButton(onClick = vm::clearLog) {
                        Icon(
                            Icons.Rounded.DeleteSweep,
                            contentDescription = "清空日志",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(bottom = 140.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (events.isEmpty()) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Rounded.GppMaybe,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            text = "暂无拦截事件",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "安装、前台告警与自动处置都会记录在这里",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(events, key = { it.id }) { event ->
                        EventRow(event)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: InterceptionEvent) {
    GlassListItem(
        modifier = Modifier.fillMaxWidth(),
        leading = {
            Text(
                text = SeverityStyle.verdictEmoji(event.level),
                color = SeverityStyle.verdictColor(event.level),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        headline = event.appLabel,
        supporting =
            "${event.kind.label} · ${event.action.label} · " +
                SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(event.timestampMillis)) +
                if (event.note.isNotBlank()) "\n${event.note}" else "",
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                GlassChip(
                    text = event.kind.label,
                    tint = SeverityStyle.verdictColor(event.level),
                )
                if (event.level != VerdictLevel.CLEAN) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "评分 ${event.score}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}
