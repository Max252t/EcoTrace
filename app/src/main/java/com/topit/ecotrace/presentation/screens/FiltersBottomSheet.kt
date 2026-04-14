package com.topit.ecotrace.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.domain.model.ReportStatus
import com.topit.ecotrace.ui.LocalAppStrings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FiltersBottomSheet(
    contentPadding: PaddingValues,
    onBack: () -> Unit = {},
    onApply: (Set<ProblemType>, Set<ReportStatus>) -> Unit = { _, _ -> },
    initialTypes: Set<ProblemType> = emptySet(),
    initialStatuses: Set<ReportStatus> = emptySet(),
) {
    val s = LocalAppStrings.current
    var selectedTypes by remember { mutableStateOf(initialTypes) }
    var selectedStatuses by remember { mutableStateOf(initialStatuses) }

    AdaptiveContent {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenHeader(icon = Icons.Default.FilterList, title = s.filtersTitle, subtitle = s.filtersSubtitle, onBack = onBack)

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                EcoSection(title = s.problemTypes) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProblemType.values().forEach { type ->
                            val selected = type in selectedTypes
                            FilterChip(
                                selected = selected,
                                onClick = { selectedTypes = if (selected) selectedTypes - type else selectedTypes + type },
                                label = { Text(typeLabel(type), style = MaterialTheme.typography.labelLarge) },
                                leadingIcon = { Icon(typeIcon(type), contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                }

                EcoSection(title = s.statuses) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportStatus.values().forEach { status ->
                            val selected = status in selectedStatuses
                            val (bg, fg) = statusColors(status)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) bg else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .toggleable(value = selected, onValueChange = { selectedStatuses = if (selected) selectedStatuses - status else selectedStatuses + status })
                                    .border(if (selected) 1.5.dp else 0.dp, if (selected) fg else Color.Transparent, RoundedCornerShape(10.dp)),
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(10.dp).background(fg, CircleShape))
                                    Text(statusLabel(status), style = MaterialTheme.typography.labelMedium, color = if (selected) fg else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }

                val activeCount = selectedTypes.size + selectedStatuses.size
                Button(
                    onClick = { onApply(selectedTypes, selectedStatuses); onBack() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (activeCount > 0) {
                        Badge(containerColor = MaterialTheme.colorScheme.onPrimary) {
                            Text("$activeCount", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(s.applyFilters, style = MaterialTheme.typography.labelLarge, fontSize = 15.sp)
                }
                if (activeCount > 0) {
                    TextButton(
                        onClick = { selectedTypes = emptySet(); selectedStatuses = emptySet(); onApply(emptySet(), emptySet()) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(s.resetAll, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
