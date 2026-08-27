
package com.example.volunteerlink.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.volunteerlink.BuildConfig
import com.example.volunteerlink.data.VolunteerOpportunitySessionStore
import com.example.volunteerlink.data.location.DeviceLocationHelper
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.ui.theme.CreamBackground
import com.example.volunteerlink.ui.theme.DeepGreen
import com.example.volunteerlink.ui.theme.TextDark
import com.example.volunteerlink.ui.theme.TextMuted
import org.osmdroid.config.Configuration
import org.osmdroid.util.MapTileIndex
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.ui.draw.clipToBounds
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable

@Composable
fun MapScreen(
    volunteerOpportunityEvents: List<VolunteerOpportunityEvent>,
    initialEventId: Int? = null,
    onEventSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val mappableEvents = remember(volunteerOpportunityEvents) {
        volunteerOpportunityEvents.filter {
            it.eventLatitude != null && it.eventLongitude != null
        }
    }
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedEventId by rememberSaveable(initialEventId) {
        mutableStateOf(initialEventId)
    }
    var markerPreviewEventId by rememberSaveable {
        mutableStateOf<Int?>(null)
    }
    var userLocation by remember {
        mutableStateOf<GeoPoint?>(null)
    }

    val mapView = remember(context) {
        Configuration.getInstance().apply {
            load(
                context,
                context.getSharedPreferences(
                    "volunteer_map_tiles",
                    Context.MODE_PRIVATE
                )
            )
            userAgentValue = context.packageName
        }

        MapView(context).apply {
            setTileSource(
                geoapifyTileSource(
                    BuildConfig.GEOAPIFY_API_KEY
                )
            )
            setMultiTouchControls(true)
            minZoomLevel = 4.0
            maxZoomLevel = 20.0
            controller.setZoom(13.0)
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    val filteredEvents = remember(searchText, mappableEvents) {
        mappableEvents.filter { event ->
            searchText.isBlank() ||
                event.eventTitle.contains(searchText, true) ||
                event.eventOrganisationName.contains(searchText, true) ||
                event.eventLocation.contains(searchText, true)
        }
    }
    val selectedEvent = filteredEvents.firstOrNull {
        it.eventId == selectedEventId
    } ?: filteredEvents.firstOrNull()

    LaunchedEffect(filteredEvents) {
        if (selectedEventId !in filteredEvents.map { it.eventId }) {
            selectedEventId = filteredEvents.firstOrNull()?.eventId
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            DeviceLocationHelper.getApproximateCurrentLocation(context) { location ->
                location?.let {
                    val currentPoint =
                        GeoPoint(it.latitude, it.longitude)

                    userLocation = currentPoint
                    VolunteerOpportunitySessionStore.updateDistancesFromDevice(
                        latitude = it.latitude,
                        longitude = it.longitude
                    )
                    mapView.post {
                        mapView.controller.setCenter(currentPoint)
                        mapView.controller.setZoom(14.5)
                    }
                }
            }
        }
    }

    LaunchedEffect(selectedEventId) {
        filteredEvents
            .firstOrNull { it.eventId == selectedEventId }
            ?.let { event ->
                val latitude = event.eventLatitude
                val longitude = event.eventLongitude

                if (latitude != null && longitude != null) {
                    mapView.controller.animateTo(
                        GeoPoint(latitude, longitude)
                    )
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
            .padding(bottom = 96.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(DeepGreen)
        ) {
            Text(
                text = "NEARBY OPPORTUNITIES",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 14.dp)
            )
        }

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("Search event, organisation or location") },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
        ) {
            if (BuildConfig.GEOAPIFY_API_KEY.isBlank()) {
                MapMessage(
                    "GEOAPIFY_API_KEY is missing from local.properties."
                )
            } else if (mappableEvents.isEmpty()) {
                MapMessage("No published physical opportunity has coordinates yet.")
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { mapView },
                    update = { updatedMap ->
                        updatedMap.overlays.clear()

                        filteredEvents.forEach { event ->
                            val latitude = event.eventLatitude
                            val longitude = event.eventLongitude

                            if (latitude != null && longitude != null) {
                                val eventMarker = Marker(updatedMap).apply {
                                    position = GeoPoint(latitude, longitude)
                                    title = event.eventTitle
                                    snippet = event.eventOrganisationName
                                    setAnchor(
                                        Marker.ANCHOR_CENTER,
                                        Marker.ANCHOR_BOTTOM
                                    )
                                    setOnMarkerClickListener { marker, _ ->
                                        selectedEventId = event.eventId
                                        markerPreviewEventId = event.eventId
                                        updatedMap.controller.animateTo(
                                            marker.position
                                        )
                                        true
                                    }
                                }

                                updatedMap.overlays.add(eventMarker)
                            }
                        }

                        userLocation?.let { currentPoint ->
                            val userMarker = Marker(updatedMap).apply {
                                position = currentPoint
                                title = "Your approximate location"
                                icon = createUserLocationDrawable(context)

                                setAnchor(
                                    Marker.ANCHOR_CENTER,
                                    Marker.ANCHOR_CENTER
                                )

                                setOnMarkerClickListener { _, _ ->
                                    true
                                }
                            }
                            updatedMap.overlays.add(userMarker)
                        }

                        if (updatedMap.tag == null) {
                            val initialEvent =
                                filteredEvents.firstOrNull {
                                    it.eventId == initialEventId
                                } ?: filteredEvents.firstOrNull()

                            initialEvent?.let { event ->
                                updatedMap.controller.setCenter(
                                    GeoPoint(
                                        event.eventLatitude!!,
                                        event.eventLongitude!!
                                    )
                                )
                                updatedMap.controller.setZoom(13.5)
                            }
                            updatedMap.tag = "initialised"
                        }

                        updatedMap.invalidate()
                    }
                )
            }

            FilledIconButton(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.White,
                    contentColor = DeepGreen
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Use my location")
            }

            markerPreviewEventId
                ?.let { previewEventId ->
                    filteredEvents
                        .firstOrNull {
                            it.eventId == previewEventId
                        }
                }
                ?.let { previewEvent ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(
                                start = 16.dp,
                                top = 14.dp,
                                end = 68.dp
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                Color.White.copy(alpha = 0.92f)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 7.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 10.dp
                            )
                        ) {
                            Text(
                                text = previewEvent.eventTitle,
                                color = TextDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text =
                                    previewEvent.eventOrganisationName,
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
        }

        selectedEvent?.let { event ->
            MapEventCard(
                event = event,
                onViewDetails = { onEventSelected(event.eventId) },
                onDirections = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                "https://www.openstreetmap.org/directions?" +
                                    "to=${event.eventLatitude},${event.eventLongitude}"
                            )
                        )
                    )
                }
            )
        }
    }
}

private fun geoapifyTileSource(
    apiKey: String
): OnlineTileSourceBase {
    return object : OnlineTileSourceBase(
        "Geoapify OSM Bright",
        1,
        20,
        256,
        ".png",
        arrayOf(
            "https://maps.geoapify.com/v1/tile/osm-bright/"
        )
    ) {
        override fun getTileURLString(
            mapTileIndex: Long
        ): String {
            return baseUrl +
                MapTileIndex.getZoom(mapTileIndex) + "/" +
                MapTileIndex.getX(mapTileIndex) + "/" +
                MapTileIndex.getY(mapTileIndex) +
                ".png?apiKey=" + Uri.encode(apiKey)
        }
    }
}

private fun createUserLocationDrawable(
    context: Context
): GradientDrawable {
    val density =
        context.resources.displayMetrics.density

    val markerSize =
        (20 * density).toInt()

    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(
            AndroidColor.rgb(
                45,
                112,
                210
            )
        )
        setStroke(
            (3 * density).toInt(),
            AndroidColor.WHITE
        )
        setSize(
            markerSize,
            markerSize
        )
    }
}

@Composable
private fun MapMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Text(message, color = TextMuted, modifier = Modifier.padding(18.dp))
        }
    }
}

@Composable
private fun MapEventCard(
    event: VolunteerOpportunityEvent,
    onViewDetails: () -> Unit,
    onDirections: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            VolunteerOpportunityThumbnail(
                storagePath = event.eventThumbnailPath,
                fallbackIconResourceId =
                    com.example.volunteerlink.R.drawable
                        .ic_volunteer_physical_event,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                contentDescription =
                    "${event.eventTitle} thumbnail",
                cornerRadius = 12.dp
            )
            Spacer(Modifier.height(9.dp))
            Text(
                event.eventTitle,
                color = TextDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(event.eventOrganisationName, color = TextMuted, fontSize = 12.sp)
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, Modifier.size(15.dp), TextMuted)
                Text(
                    event.eventLocation + event.eventDistanceKm?.let { " • $it km" }.orEmpty(),
                    modifier = Modifier.weight(1f),
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Icon(Icons.Filled.CalendarMonth, null, Modifier.size(15.dp), TextMuted)
                Text(event.eventDate, color = TextMuted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onDirections,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepGreen)
                ) {
                    Icon(Icons.Filled.Navigation, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Directions")
                }
                Button(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE7EFE2),
                        contentColor = DeepGreen
                    )
                ) {
                    Text("View details")
                }
            }
        }
    }
}

