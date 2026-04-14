package com.topit.ecotrace.presentation.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.view.ViewGroup
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.topit.ecotrace.ui.LocalAppStrings
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.mapview.MapView

@Composable
fun LocationPickerScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit = {},
    initialLat: Double = 55.751244,
    initialLon: Double = 37.618423,
    onConfirm: (Double, Double) -> Unit = { _, _ -> },
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var centerLat by remember { mutableStateOf(initialLat) }
    var centerLon by remember { mutableStateOf(initialLon) }
    var isCameraMoving by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) moveToGpsLocation(fusedLocationClient) { lat, lon ->
            centerLat = lat; centerLon = lon
        }
    }

    val mapView = remember {
        MapView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    val cameraListener = remember {
        object : CameraListener {
            override fun onCameraPositionChanged(
                map: com.yandex.mapkit.map.Map,
                cameraPosition: CameraPosition,
                reason: CameraUpdateReason,
                finished: Boolean,
            ) {
                centerLat = cameraPosition.target.latitude
                centerLon = cameraPosition.target.longitude
                isCameraMoving = !finished
            }
        }
    }

    DisposableEffect(mapView) {
        mapView.mapWindow.map.addCameraListener(cameraListener)
        mapView.mapWindow.map.move(
            CameraPosition(Point(initialLat, initialLon), 15f, 0f, 0f),
            Animation(Animation.Type.SMOOTH, 0.5f),
            null,
        )
        mapView.onStart()
        MapKitFactory.getInstance().onStart()
        onDispose {
            mapView.mapWindow.map.removeCameraListener(cameraListener)
            MapKitFactory.getInstance().onStop()
            mapView.onStop()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(contentPadding),
    ) {
        ScreenHeader(
            icon = Icons.Default.LocationOn,
            title = s.pickLocationTitle,
            subtitle = s.pickLocationSubtitle,
            onBack = onBack,
        )

        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
            )

            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .offset(y = (-24).dp),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(6.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        RoundedCornerShape(3.dp),
                    ),
            )

            if (isCameraMoving) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-56).dp)
                        .size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            FloatingActionButton(
                onClick = {
                    val ok = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (ok) {
                        moveToGpsLocation(fusedLocationClient) { lat, lon ->
                            centerLat = lat
                            centerLon = lon
                            mapView.mapWindow.map.move(
                                CameraPosition(Point(lat, lon), 16f, 0f, 0f),
                                Animation(Animation.Type.SMOOTH, 0.6f),
                                null,
                            )
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(6.dp),
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = s.myLocation)
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Selected coordinate preview
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Column {
                        Text(
                            s.coordinates,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        )
                        Text(
                            "%.6f,  %.6f".format(centerLat, centerLon),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            Button(
                onClick = { onConfirm(centerLat, centerLon) },
                enabled = !isCameraMoving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(s.confirmLocation, style = MaterialTheme.typography.labelLarge, fontSize = 15.sp)
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun moveToGpsLocation(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onLocation: (Double, Double) -> Unit,
) {
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) onLocation(location.latitude, location.longitude)
    }
}
