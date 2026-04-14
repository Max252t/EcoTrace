package com.topit.ecotrace.presentation.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportStatus
import com.topit.ecotrace.presentation.viewmodel.ReportDetailsViewModel
import com.topit.ecotrace.presentation.viewmodel.daggerViewModel
import com.topit.ecotrace.ui.LocalAppStrings
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

@Composable
fun ReportDetailsScreen(contentPadding: PaddingValues, onBack: () -> Unit, reportId: String) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val viewModel: ReportDetailsViewModel = daggerViewModel()
    val report by viewModel.report.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    LaunchedEffect(reportId) { viewModel.load(reportId) }

    AdaptiveContent {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            val currentReport = report
            val subtitle = if (currentReport != null) statusLabel(currentReport.status)
                          else s.statusOpenLong

            ScreenHeader(icon = Icons.Default.Place, title = s.reportDetails, subtitle = subtitle, onBack = onBack)

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                return@Column
            }

            if (currentReport == null) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Отчёт не найден", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@Column
            }

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                val (statusBg, statusFg) = statusColors(currentReport.status)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!currentReport.imageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = currentReport.imageUri,
                            contentDescription = s.photoLabel,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(52.dp))
                            Text(s.photoLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                        Surface(shape = RoundedCornerShape(10.dp), color = statusBg) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(statusFg, CircleShape))
                                Text(statusLabel(currentReport.status), style = MaterialTheme.typography.labelMedium, color = statusFg, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(typeIcon(currentReport.type), contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                                Text(typeLabel(currentReport.type), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }

                EcoSection(title = s.descSection) {
                    Text(
                        currentReport.description.ifBlank { "—" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                EcoSection(title = s.infoSection) {
                    InfoRow(Icons.Default.LocationOn, s.coordsLabel, "%.5f, %.5f".format(currentReport.latitude, currentReport.longitude))
                    InfoRow(Icons.Default.WarningAmber, s.typeInfoLabel, typeLabel(currentReport.type))
                    InfoRow(Icons.Default.Pending, s.statusInfoLabel, statusLabel(currentReport.status))
                    InfoRow(Icons.Default.Person, s.authorLabel, currentReport.authorId)
                    InfoRow(
                        Icons.Default.CalendarToday,
                        "Дата",
                        currentReport.createdAt.atZone(ZoneId.systemDefault()).format(dateFormatter),
                    )
                }

                Button(
                    onClick = { openRouteToPoint(context, currentReport.latitude, currentReport.longitude) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(s.routeButton, style = MaterialTheme.typography.labelLarge, fontSize = 15.sp)
                }

                if (currentReport.status != ReportStatus.RESOLVED) {
                    OutlinedButton(
                        onClick = { viewModel.markResolved(reportId) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(s.markResolved, style = MaterialTheme.typography.labelLarge, fontSize = 15.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun openRouteToPoint(context: Context, latitude: Double, longitude: Double) {
    val lat = latitude.toString()
    val lon = longitude.toString()
    val candidates = listOf(
        "yandexnavi://build_route_on_map?lat_to=$lat&lon_to=$lon",
        "yandexmaps://maps.yandex.ru/?rtext=~$lat,$lon&rtt=pd",
        "geo:$lat,$lon?q=$lat,$lon",
        "https://maps.yandex.ru/?rtext=~$lat,$lon&rtt=pd",
    )
    for (uri in candidates) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
            return
        } catch (_: ActivityNotFoundException) {}
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(90.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}
