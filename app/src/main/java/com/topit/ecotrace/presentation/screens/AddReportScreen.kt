package com.topit.ecotrace.presentation.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.presentation.viewmodel.AddReportViewModel
import com.topit.ecotrace.presentation.viewmodel.daggerViewModel
import com.topit.ecotrace.ui.LocalAppStrings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddReportScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    /** Navigates to LocationPicker with (currentLat, currentLon) */
    onPickLocation: (Double, Double) -> Unit,
    /** Live coordinates — updated from LocationPicker result via SavedStateHandle */
    currentLat: Double,
    currentLon: Double,
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val viewModel: AddReportViewModel = daggerViewModel()

    var selectedType by remember { mutableStateOf(ProblemType.DUMP) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Temp URI for TakePicture — created before launching camera
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Mutable local copy of coordinates; syncs when parent provides new values
    var reportLat by rememberSaveable { mutableStateOf(currentLat) }
    var reportLon by rememberSaveable { mutableStateOf(currentLon) }
    LaunchedEffect(currentLat, currentLon) {
        reportLat = currentLat
        reportLon = currentLon
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Camera launcher — TakePicture returns true if photo was taken
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) selectedImageUri = cameraImageUri
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = createCameraImageUri(context)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) fetchMyLocation(fusedLocationClient) { lat, lon ->
            reportLat = lat; reportLon = lon
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) selectedImageUri = uri }

    AdaptiveContent {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenHeader(icon = Icons.Default.Add, title = s.addReportTitle, subtitle = s.addReportSubtitle, onBack = onBack)

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ── Type picker ───────────────────────────────────────────────
                EcoSection(title = s.problemType) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ProblemType.values().forEach { type ->
                            FilterChip(
                                selected = type == selectedType,
                                onClick = { selectedType = type },
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

                // ── Photo ─────────────────────────────────────────────────────
                EcoSection(title = s.photo) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = s.photo,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(8.dp)),
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
                                Text(s.noPhotoSelected, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = {
                                val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (hasCam) {
                                    val uri = createCameraImageUri(context)
                                    cameraImageUri = uri
                                    cameraLauncher.launch(uri)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s.camera)
                        }
                        OutlinedButton(
                            onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s.gallery)
                        }
                    }
                    if (selectedImageUri != null) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text("Фото выбрано", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // ── Description ───────────────────────────────────────────────
                EcoSection(title = s.descriptionSection) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(s.titleLabel) },
                        placeholder = { Text(s.titlePlaceholder) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(s.descriptionLabel) },
                        placeholder = { Text(s.descriptionPlaceholder) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                        minLines = 3,
                        maxLines = 6,
                    )
                }

                // ── Location ──────────────────────────────────────────────────
                EcoSection(title = s.locationTitle) {
                    // Coordinate display row
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    s.coordinates,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "%.5f, %.5f".format(reportLat, reportLon),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Two action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Use my GPS location directly
                        FilledTonalButton(
                            onClick = {
                                val ok = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_FINE_LOCATION,
                                ) == PackageManager.PERMISSION_GRANTED
                                if (ok) fetchMyLocation(fusedLocationClient) { lat, lon ->
                                    reportLat = lat; reportLon = lon
                                } else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s.myLocation, style = MaterialTheme.typography.labelLarge)
                        }
                        // Open LocationPicker map
                        OutlinedButton(
                            onClick = { onPickLocation(reportLat, reportLon) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.EditLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s.change, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                // ── Submit ────────────────────────────────────────────────────
                Button(
                    onClick = {
                        viewModel.createDraftReport(
                            title = title.ifBlank { s.addReportTitle },
                            description = description.ifBlank { "—" },
                            type = selectedType,
                            latitude = reportLat,
                            longitude = reportLon,
                            imageUri = selectedImageUri?.toString(),
                        )
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Default.Task, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(s.submitReport, style = MaterialTheme.typography.labelLarge, fontSize = 15.sp)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private fun createCameraImageUri(context: android.content.Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File.createTempFile("photo_", ".jpg", imagesDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
}

@SuppressLint("MissingPermission")
private fun fetchMyLocation(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onLocation: (Double, Double) -> Unit,
) {
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) onLocation(location.latitude, location.longitude)
    }
}
