package com.topit.ecotrace.presentation.map

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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

private val moscowCenter = Point(55.751244, 37.618423)

// Status colors for pin markers
private const val COLOR_OPEN        = 0xFFEF4444.toInt()  // red
private const val COLOR_IN_PROGRESS = 0xFFD97706.toInt()  // amber
private const val COLOR_RESOLVED    = 0xFF059669.toInt()  // green
private const val COLOR_CLUSTER     = 0xFF0C7D69.toInt()  // brand teal

// Tag used to identify the user-location placemark
private const val USER_LOCATION_TAG = "user_location"

@Composable
fun YandexMapComposable(
    reports: List<Report>,
    cameraTarget: Point?,
    /** Current user GPS position — shown as a green circle, not clustered */
    userLocation: Point?,
    onMapLongTap: (Point) -> Unit,
    onReportClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Pin markers per status — created once and reused
    val markerOpen        = remember(context) { pinMarkerBitmap(context, COLOR_OPEN) }
    val markerInProgress  = remember(context) { pinMarkerBitmap(context, COLOR_IN_PROGRESS) }
    val markerResolved    = remember(context) { pinMarkerBitmap(context, COLOR_RESOLVED) }
    val markerCluster     = remember(context) { clusterBitmap(context, COLOR_CLUSTER) }
    val markerUserLocation = remember(context) { userLocationBitmap(context) }

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

    val currentOnReportClick by rememberUpdatedState(onReportClick)
    val currentOnMapLongTap by rememberUpdatedState(onMapLongTap)

    val tapListener = remember {
        MapObjectTapListener { mapObject, _ ->
            val id = mapObject.userData as? String
            // Ignore taps on the user-location marker
            if (id != null && id != USER_LOCATION_TAG) currentOnReportClick(id)
            true
        }
    }

    val inputListener = remember {
        object : InputListener {
            override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) = Unit
            override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {
                currentOnMapLongTap(point)
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
            // Move camera if requested
            if (cameraTarget != null) {
                view.mapWindow.map.move(
                    CameraPosition(cameraTarget, 15f, 0f, 0f),
                    Animation(Animation.Type.SMOOTH, 0.7f),
                    null,
                )
            }

            // Clear all previous objects
            view.mapWindow.map.mapObjects.clear()

            // ── User location — non-clustered green circle ────────────────
            userLocation?.let { loc ->
                val userMark = view.mapWindow.map.mapObjects.addPlacemark(
                    loc,
                    markerUserLocation,
                )
                userMark.userData = USER_LOCATION_TAG
            }

            // ── Report pin markers — clustered ────────────────────────────
            val clusterCollection = view.mapWindow.map.mapObjects
                .addClusterizedPlacemarkCollection(clusterListener)

            reports.forEach { report ->
                val icon = when (report.status) {
                    ReportStatus.OPEN        -> markerOpen
                    ReportStatus.IN_PROGRESS -> markerInProgress
                    ReportStatus.RESOLVED    -> markerResolved
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
