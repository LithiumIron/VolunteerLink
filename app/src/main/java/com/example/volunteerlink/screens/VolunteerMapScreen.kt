package com.example.volunteerlink.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.model.VolunteerEvent
import com.example.volunteerlink.ui.theme.CardBeige
import com.example.volunteerlink.ui.theme.CreamBackground
import com.example.volunteerlink.ui.theme.DeepGreen
import com.example.volunteerlink.ui.theme.MapBackground
import com.example.volunteerlink.ui.theme.RiverBlue
import com.example.volunteerlink.ui.theme.TextDark
import com.example.volunteerlink.ui.theme.TextMuted

@Composable
fun MapScreen(
    onEventSelected: (String) -> Unit = {}
) {
    val mapVolunteerEvents = remember {
        listOf(
            VolunteerEvent(
                id = "charity_fun_run",
                title = "Charity Fun Run 2026",
                organisation = "Green Earth Society",
                distanceKm = 2.3,
                date = "15 Aug 2026",
                spotsLeft = 18,
                mapX = 0.15f,
                mapY = 0.20f
            ),
            VolunteerEvent(
                id = "food_bank_distribution",
                title = "Food Bank Distribution",
                organisation = "Community Food Support",
                distanceKm = 5.1,
                date = "22 Aug 2026",
                spotsLeft = 12,
                mapX = 0.53f,
                mapY = 0.38f
            ),
            VolunteerEvent(
                id = "community_health_fair",
                title = "Community Health Fair",
                organisation = "Care Malaysia",
                distanceKm = 7.4,
                date = "29 Aug 2026",
                spotsLeft = 9,
                mapX = 0.28f,
                mapY = 0.68f
            ),
            VolunteerEvent(
                id = "beach_cleanup",
                title = "Beach Cleanup",
                organisation = "GreenEarth NGO",
                distanceKm = 8.6,
                date = "5 Sep 2026",
                spotsLeft = 24,
                mapX = 0.70f,
                mapY = 0.62f
            )
        )
    }

    var mapSearchText by rememberSaveable {
        mutableStateOf("")
    }

    var selectedMapEventId by rememberSaveable {
        mutableStateOf(mapVolunteerEvents.first().id)
    }

    val filteredMapVolunteerEvents =
        remember(mapSearchText, mapVolunteerEvents) {
            if (mapSearchText.isBlank()) {
                mapVolunteerEvents
            } else {
                mapVolunteerEvents.filter { volunteerEvent ->
                    volunteerEvent.title.contains(
                        mapSearchText,
                        ignoreCase = true
                    ) ||
                            volunteerEvent.organisation.contains(
                                mapSearchText,
                                ignoreCase = true
                            )
                }
            }
        }

    val selectedMapVolunteerEvent =
        filteredMapVolunteerEvents.firstOrNull {
                volunteerEvent ->
            volunteerEvent.id == selectedMapEventId
        } ?: filteredMapVolunteerEvents.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepGreen)
                .height(90.dp)


        ) {
            Text(
                text = "NEARBY EVENTS",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.BottomStart)
                    .padding(
                        start = 20.dp,
                        bottom = 14.dp
                    )
            )
        }

        OutlinedTextField(
            value = mapSearchText,
            onValueChange = {
                    updatedSearchText ->
                mapSearchText = updatedSearchText
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search"
                )
            },
            placeholder = {
                Text("Search event or organisation")
            },
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
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MapBackground)
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val verticalStreetSpacing =
                    size.width / 5f

                var streetX = 0f

                while (streetX < size.width) {
                    drawLine(
                        color = Color(0xFFD8D2C0),
                        start = Offset(streetX, 0f),
                        end = Offset(
                            streetX,
                            size.height
                        ),
                        strokeWidth = 2f
                    )

                    streetX += verticalStreetSpacing
                }

                val horizontalStreetSpacing =
                    size.height / 8f

                var streetY = 0f

                while (streetY < size.height) {
                    drawLine(
                        color = Color(0xFFD8D2C0),
                        start = Offset(0f, streetY),
                        end = Offset(
                            size.width,
                            streetY
                        ),
                        strokeWidth = 2f
                    )

                    streetY += horizontalStreetSpacing
                }

                // Main diagonal roads
                drawLine(
                    color = Color(0xFFC8C1AF),
                    start = Offset(
                        0f,
                        size.height * 0.25f
                    ),
                    end = Offset(
                        size.width,
                        size.height * 0.45f
                    ),
                    strokeWidth = 5f
                )

                drawLine(
                    color = Color(0xFFC8C1AF),
                    start = Offset(
                        size.width * 0.15f,
                        0f
                    ),
                    end = Offset(
                        size.width * 0.72f,
                        size.height
                    ),
                    strokeWidth = 5f
                )

                // River
                drawLine(
                    color = RiverBlue,
                    start = Offset(
                        0f,
                        size.height * 0.68f
                    ),
                    end = Offset(
                        size.width,
                        size.height * 0.82f
                    ),
                    strokeWidth = 26f
                )

                // User location
                val userLocation =
                    Offset(
                        size.width * 0.38f,
                        size.height * 0.40f
                    )

                drawCircle(
                    color = Color(0xFF3B6FD6),
                    radius = 14f,
                    center = userLocation
                )

                drawCircle(
                    color = Color.White,
                    radius = 14f,
                    center = userLocation,
                    style = Stroke(width = 3f)
                )
            }

            Text(
                text = "George Town",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            filteredMapVolunteerEvents.forEach {
                    volunteerEvent ->

                VolunteerMapPin(
                    volunteerEvent = volunteerEvent,
                    mapPinIsSelected =
                        volunteerEvent.id ==
                                selectedMapVolunteerEvent?.id,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x =
                                (volunteerEvent.mapX *
                                        270f).dp,
                            y =
                                (volunteerEvent.mapY *
                                        380f).dp
                        )
                        .clickable {
                            selectedMapEventId =
                                volunteerEvent.id
                        }
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(
                        color =
                            Color.White.copy(
                                alpha = 0.92f
                            ),
                        shape =
                            RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                VolunteerMapLegendRow(
                    legendColour =
                        Color(0xFF3B6FD6),
                    legendLabel = "Your location"
                )

                VolunteerMapLegendRow(
                    legendColour = DeepGreen,
                    legendLabel =
                        "Volunteer opportunity"
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                )
        ) {
            mapVolunteerEvents
                .filter {
                        volunteerEvent ->
                    volunteerEvent.id !=
                            selectedMapVolunteerEvent?.id
                }
                .forEach {
                        volunteerEvent ->

                    AssistChip(
                        onClick = {
                            selectedMapEventId =
                                volunteerEvent.id
                        },
                        label = {
                            Text(
                                text =
                                    "${volunteerEvent.title}  " +
                                            "${volunteerEvent.distanceKm} km",
                                maxLines = 1
                            )
                        },
                        modifier =
                            Modifier.padding(
                                end = 8.dp
                            )
                    )
                }
        }

        selectedMapVolunteerEvent?.let {
                volunteerEvent ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 12.dp
                    )
                    .clickable {
                        onEventSelected(
                            volunteerEvent.id
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(
                                RoundedCornerShape(8.dp)
                            )
                            .background(CardBeige),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = DeepGreen
                        )
                    }

                    Spacer(
                        modifier = Modifier.size(12.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = volunteerEvent.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis
                        )

                        Text(
                            text =
                                volunteerEvent.organisation,
                            fontSize = 12.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier =
                                    Modifier.size(13.dp)
                            )

                            Text(
                                text =
                                    "${volunteerEvent.distanceKm} km",
                                fontSize = 11.sp,
                                color = TextMuted
                            )

                            Spacer(
                                modifier =
                                    Modifier.size(8.dp)
                            )

                            Icon(
                                imageVector =
                                    Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier =
                                    Modifier.size(13.dp)
                            )

                            Text(
                                text = volunteerEvent.date,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Text(
                        text =
                            "${volunteerEvent.spotsLeft} left",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun VolunteerMapPin(
    volunteerEvent: VolunteerEvent,
    mapPinIsSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color =
                    if (mapPinIsSelected) {
                        DeepGreen
                    } else {
                        Color.White
                    },
                shape = RoundedCornerShape(14.dp)
            )
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription =
                volunteerEvent.title,
            tint =
                if (mapPinIsSelected) {
                    Color.White
                } else {
                    DeepGreen
                },
            modifier = Modifier.size(14.dp)
        )

        Spacer(
            modifier = Modifier.size(4.dp)
        )

        Text(
            text = volunteerEvent.title,
            fontSize = 10.sp,
            maxLines = 1,
            color =
                if (mapPinIsSelected) {
                    Color.White
                } else {
                    TextDark
                },
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun VolunteerMapLegendRow(
    legendColour: Color,
    legendLabel: String
) {
    Row(
        modifier = Modifier.padding(
            vertical = 2.dp
        ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    color = legendColour,
                    shape = CircleShape
                )
        )

        Spacer(
            modifier = Modifier.size(6.dp)
        )

        Text(
            text = legendLabel,
            fontSize = 10.sp,
            color = TextDark
        )
    }
}