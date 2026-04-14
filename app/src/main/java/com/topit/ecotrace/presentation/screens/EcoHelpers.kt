package com.topit.ecotrace.presentation.screens

import android.annotation.SuppressLint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.domain.model.ReportStatus
import com.topit.ecotrace.ui.LocalAppStrings

@Composable
internal fun typeLabel(type: ProblemType): String {
    val s = LocalAppStrings.current
    return when (type) {
        ProblemType.DUMP -> s.typeDump
        ProblemType.ROAD_PIT -> s.typeRoadPit
        ProblemType.PIPE_RUPTURE -> s.typePipeRupture
        ProblemType.FALLEN_TREE -> s.typeFallenTree
    }
}

internal fun typeIcon(type: ProblemType): ImageVector = when (type) {
    ProblemType.DUMP -> Icons.Default.WarningAmber
    ProblemType.ROAD_PIT -> Icons.Default.Report
    ProblemType.PIPE_RUPTURE -> Icons.Default.WarningAmber
    ProblemType.FALLEN_TREE -> Icons.Default.Forest
}

@Composable
internal fun statusLabel(status: ReportStatus): String {
    val s = LocalAppStrings.current
    return when (status) {
        ReportStatus.OPEN -> s.statusOpen
        ReportStatus.IN_PROGRESS -> s.statusInProgress
        ReportStatus.RESOLVED -> s.statusResolved
    }
}

@SuppressLint("MissingPermission")
internal fun centerOnUser(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onLocation: (Double, Double) -> Unit,
) {
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) onLocation(location.latitude, location.longitude)
    }
}
