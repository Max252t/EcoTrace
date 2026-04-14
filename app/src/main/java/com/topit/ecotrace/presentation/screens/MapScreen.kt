package com.topit.ecotrace.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NaturePeople
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.topit.ecotrace.presentation.map.YandexMapComposable
import com.topit.ecotrace.presentation.viewmodel.MapViewModel
import com.topit.ecotrace.ui.LocalAppStrings
import com.topit.ecotrace.ui.theme.StatusInProgressFg
import com.topit.ecotrace.ui.theme.StatusOpenFg
import com.topit.ecotrace.ui.theme.StatusResolvedFg
import com.yandex.mapkit.geometry.Point

@Composable
fun MapScreen(
    contentPadding: PaddingValues,
    viewModel: MapViewModel,
    onAddClick: (Double?, Double?) -> Unit,
    onFiltersClick: () -> Unit,
    onReportClick: (String) -> Unit,
) {
    val s = LocalAppStrings.current
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var cameraTarget by remember { mutableStateOf<Point?>(null) }
    var userLocation by remember { mutableStateOf<Point?>(null) }
    var didAutoCenter by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) centerOnUser(fusedLocationClient) { lat, lon ->
            val p = Point(lat, lon)
            cameraTarget = p
            userLocation = p
        }
    }

    LaunchedEffect(didAutoCenter) {
        if (!didAutoCenter) {
            didAutoCenter = true
            val ok = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            if (ok) {
                centerOnUser(fusedLocationClient) { lat, lon ->
                    val p = Point(lat, lon)
                    cameraTarget = p
                    userLocation = p
                }
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        YandexMapComposable(
            reports = reports,
            cameraTarget = cameraTarget,
            userLocation = userLocation,
            onMapLongTap = { point -> onAddClick(point.latitude, point.longitude) },
            onReportClick = onReportClick,
            modifier = Modifier.fillMaxSize(),
        )

        MapTopBar(
            reportCount = reports.size,
            onFiltersClick = onFiltersClick,
            onPreviewClick = { reports.firstOrNull()?.id?.let(onReportClick) },
        )

        Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 90.dp)) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Text(s.longTapHint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End,
        ) {
            SmallFloatingActionButton(
                onClick = {
                    val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (ok) centerOnUser(fusedLocationClient) { lat, lon ->
                        val p = Point(lat, lon)
                        cameraTarget = p
                        userLocation = p
                    }
                    else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(6.dp),
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = s.navMap)
            }
            FloatingActionButton(
                onClick = { onAddClick(null, null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = s.addReportTitle)
            }
        }
    }
}

@Composable
private fun MapTopBar(reportCount: Int, onFiltersClick: () -> Unit, onPreviewClick: () -> Unit) {
    val s = LocalAppStrings.current
    Box(
        modifier = Modifier.fillMaxWidth().background(
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background.copy(alpha = 0.92f), Color.Transparent))
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.NaturePeople, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                    Column {
                        Text("EcoTrace", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                        Text(s.appSubtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                    }
                }
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Text(s.pointsCount(reportCount), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(StatusOpenFg, CircleShape))
                    Box(modifier = Modifier.size(10.dp).background(StatusInProgressFg, CircleShape))
                    Box(modifier = Modifier.size(10.dp).background(StatusResolvedFg, CircleShape))
                }
                Spacer(Modifier.weight(1f))
                FilledTonalButton(onClick = onFiltersClick, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(s.filters, style = MaterialTheme.typography.labelLarge)
                }
                FilledTonalButton(onClick = onPreviewClick, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                    Icon(Icons.Default.Report, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(s.preview, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
