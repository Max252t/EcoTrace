package com.topit.ecotrace.presentation.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.presentation.map.YandexMapComposable
import com.topit.ecotrace.presentation.viewmodel.AddReportViewModel
import com.topit.ecotrace.presentation.viewmodel.MapViewModel
import com.topit.ecotrace.presentation.viewmodel.MyReportsViewModel
import com.topit.ecotrace.presentation.viewmodel.daggerViewModel
import com.yandex.mapkit.geometry.Point

@Composable
fun MapScreen(
    contentPadding: PaddingValues,
    onAddClick: (Double?, Double?) -> Unit,
    onFiltersClick: () -> Unit,
    onReportClick: (String) -> Unit,
) {
    val viewModel: MapViewModel = daggerViewModel()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var cameraTarget by remember { mutableStateOf<Point?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            centerOnUser(
                fusedLocationClient = fusedLocationClient,
                onLocation = { lat, lon -> cameraTarget = Point(lat, lon) },
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        YandexMapComposable(
            reports = reports,
            cameraTarget = cameraTarget,
            onMapLongTap = { point -> onAddClick(point.latitude, point.longitude) },
            onReportClick = onReportClick,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("EcoTrace Map", style = MaterialTheme.typography.headlineSmall)
            Text("Точек на карте: ${reports.size}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onFiltersClick) { Text("Фильтры") }
                Button(
                    onClick = {
                        reports.firstOrNull()?.id?.let(onReportClick)
                    },
                ) { Text("Превью точки") }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End,
        ) {
            FloatingActionButton(
                onClick = {
                    val hasLocationPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasLocationPermission) {
                        centerOnUser(
                            fusedLocationClient = fusedLocationClient,
                            onLocation = { lat, lon -> cameraTarget = Point(lat, lon) },
                        )
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
            ) {
                Text("GPS")
            }
            FloatingActionButton(onClick = { onAddClick(null, null) }) {
                Text("+")
            }
        }
    }
}

@Composable
fun AddReportScreen(
    contentPadding: PaddingValues,
    onPickLocation: () -> Unit,
    initialLat: Double?,
    initialLon: Double?,
) {
    val viewModel: AddReportViewModel = daggerViewModel()
    val lat = initialLat ?: 55.751244
    val lon = initialLon ?: 37.618423
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Добавление новой проблемы", style = MaterialTheme.typography.headlineSmall)
        Text("Координаты: %.6f, %.6f".format(lat, lon))
        Text("CameraX/галерея и выбор типа проблемы подключаются на этом экране.")
        Button(onClick = onPickLocation) { Text("Выбрать точку на карте") }
        Button(
            onClick = {
                viewModel.createDraftReport(
                    title = "Тестовая проблема",
                    description = "Нужно устранить нарушение",
                    type = ProblemType.DUMP,
                    latitude = lat,
                    longitude = lon,
                    imageUri = null,
                )
            },
        ) {
            Text("Сохранить тестовый отчет")
        }
    }
}

@Composable
fun MyReportsScreen(contentPadding: PaddingValues) {
    val viewModel: MyReportsViewModel = daggerViewModel()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Мои отчеты", style = MaterialTheme.typography.headlineSmall)
        if (reports.isEmpty()) {
            Text("Пока нет отчетов")
        } else {
            reports.forEach { report ->
                Text("• ${report.title} (${report.status})")
            }
        }
    }
}

@Composable
fun ProfileScreen(contentPadding: PaddingValues) {
    PlaceholderScreen(contentPadding, "Профиль", "Здесь будет профиль пользователя и геймификация")
}

@Composable
fun ReportDetailsScreen(contentPadding: PaddingValues, reportId: String) {
    PlaceholderScreen(contentPadding, "Детали", "Открыт отчет: $reportId")
}

@Composable
fun FiltersBottomSheet(contentPadding: PaddingValues) {
    PlaceholderScreen(contentPadding, "Фильтры", "Фильтрация по типам и статусам проблем")
}

@SuppressLint("MissingPermission")
private fun centerOnUser(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onLocation: (Double, Double) -> Unit,
) {
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            onLocation(location.latitude, location.longitude)
        }
    }
}

@Composable
fun LocationPickerScreen(contentPadding: PaddingValues) {
    PlaceholderScreen(contentPadding, "Выбор места", "Тап по карте и подтверждение местоположения")
}

@Composable
private fun PlaceholderScreen(contentPadding: PaddingValues, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(subtitle)
    }
}
