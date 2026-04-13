package com.topit.ecotrace.presentation.map

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.topit.ecotrace.R
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

@Composable
fun YandexMapComposable(
    reports: List<Report>,
    cameraTarget: Point?,
    onMapLongTap: (Point) -> Unit,
    onReportClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
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
    val clusterListener = remember {
        ClusterListener { cluster: Cluster ->
            cluster.appearance.setIcon(ImageProvider.fromResource(context, R.drawable.marker_in_progress))
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
        update = { currentMapView ->
            if (cameraTarget != null) {
                currentMapView.mapWindow.map.move(
                    CameraPosition(cameraTarget, 15f, 0f, 0f),
                    Animation(Animation.Type.SMOOTH, 0.7f),
                    null,
                )
            }
            currentMapView.mapWindow.map.mapObjects.clear()
            val clusterCollection = currentMapView.mapWindow.map.mapObjects
                .addClusterizedPlacemarkCollection(clusterListener)
            clusterCollection.clear()

            reports.forEach { report ->
                val placemark = clusterCollection.addPlacemark(
                    Point(report.latitude, report.longitude),
                    iconForStatus(context, report.status),
                )
                placemark.userData = report.id
                placemark.addTapListener(tapListener)
            }

            clusterCollection.clusterPlacemarks(60.0, 14)
        },
    )
}

private fun iconForStatus(
    context: android.content.Context,
    status: ReportStatus,
): ImageProvider {
    val drawableId = when (status) {
        ReportStatus.OPEN -> R.drawable.marker_open
        ReportStatus.IN_PROGRESS -> R.drawable.marker_in_progress
        ReportStatus.RESOLVED -> R.drawable.marker_resolved
    }
    return ImageProvider.fromResource(context, drawableId)
}
