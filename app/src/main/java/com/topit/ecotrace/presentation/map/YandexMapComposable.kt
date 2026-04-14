package com.topit.ecotrace.presentation.map

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportStatus
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.Cluster
import com.yandex.mapkit.map.ClusterListener
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

private val moscowCenter = Point(55.751244, 37.618423)

// Marker colors matching the app status palette
private const val COLOR_OPEN = 0xFFEF4444.toInt()        // red
private const val COLOR_IN_PROGRESS = 0xFFD97706.toInt() // amber
private const val COLOR_RESOLVED = 0xFF059669.toInt()    // green
private const val COLOR_CLUSTER = 0xFF0C7D69.toInt()     // brand primary

@Composable
fun YandexMapComposable(
    reports: List<Report>,
    cameraTarget: Point?,
    onMapLongTap: (Point) -> Unit,
    onReportClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Bitmap markers — created once, re-used across recompositions
    val markerOpen = remember(context) { markerBitmap(context, COLOR_OPEN) }
    val markerInProgress = remember(context) { markerBitmap(context, COLOR_IN_PROGRESS) }
    val markerResolved = remember(context) { markerBitmap(context, COLOR_RESOLVED) }
    val markerCluster = remember(context) { markerBitmap(context, COLOR_CLUSTER) }

    val mapView = remember {
        MapView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            mapWindow.map.move(
                CameraPosition(moscowCenter, 12f, 0f, 0f),
                Animation(Animation.Type.SMOOTH, 0.7f),
                null,
            )
        }
    }

    val clusterListener = remember(markerCluster) {
        ClusterListener { cluster: Cluster ->
            cluster.appearance.setIcon(markerCluster)
        }
    }

    val tapListener = remember {
        MapObjectTapListener { mapObject, _ ->
            val id = mapObject.userData as? String
            if (id != null) onReportClick(id)
            true
        }
    }

    val inputListener = remember {
        object : InputListener {
            override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) = Unit
            override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {
                onMapLongTap(point)
            }
        }
    }

    DisposableEffect(mapView) {
        mapView.mapWindow.map.addInputListener(inputListener)
        mapView.onStart()
        MapKitFactory.getInstance().onStart()
        onDispose {
            mapView.mapWindow.map.removeInputListener(inputListener)
            MapKitFactory.getInstance().onStop()
            mapView.onStop()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            if (cameraTarget != null) {
                view.mapWindow.map.move(
                    CameraPosition(cameraTarget, 15f, 0f, 0f),
                    Animation(Animation.Type.SMOOTH, 0.7f),
                    null,
                )
            }

            view.mapWindow.map.mapObjects.clear()
            val clusterCollection = view.mapWindow.map.mapObjects
                .addClusterizedPlacemarkCollection(clusterListener)

            reports.forEach { report ->
                val icon = when (report.status) {
                    ReportStatus.OPEN -> markerOpen
                    ReportStatus.IN_PROGRESS -> markerInProgress
                    ReportStatus.RESOLVED -> markerResolved
                }
                val placemark = clusterCollection.addPlacemark(
                    Point(report.latitude, report.longitude),
                    icon,
                )
                placemark.userData = report.id
                placemark.addTapListener(tapListener)
            }

            clusterCollection.clusterPlacemarks(60.0, 14)
        },
    )
}
