package com.example.volunteerlink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.model.VolunteerEvent
import com.example.volunteerlink.ui.theme.CardBeige
import com.example.volunteerlink.ui.theme.CreamBackground
import com.example.volunteerlink.ui.theme.DeepGreen
import com.example.volunteerlink.ui.theme.MapBackground
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
                mapY = 0.18f
            ),
            VolunteerEvent(
                id = "food_bank_distribution",
                title = "Food Bank Distribution",
                organisation = "Community Food Support",
                distanceKm = 5.1,
                date = "22 Aug 2026",
                spotsLeft = 12,
                mapX = 0.55f,
                mapY = 0.38f
            ),
            VolunteerEvent(
                id = "community_health_fair",
                title = "Community Health Fair",
                organisation = "Care Malaysia",
                distanceKm = 7.4,
                date = "29 Aug 2026",
                spotsLeft = 9,
                mapX = 0.30f,
                mapY = 0.66f
            )
        )
    }

    var mapSearchQuery by rememberSaveable {
        mutableStateOf("")
    }

    var selectedMapEventId by rememberSaveable {
        mutableStateOf(mapVolunteerEvents.first().id)
    }

    val filteredMapVolunteerEvents =
        remember(mapSearchQuery, mapVolunteerEvents) {
            if (mapSearchQuery.isBlank()) {
                mapVolunteerEvents
            } else {
                mapVolunteerEvents.filter { volunteerEvent ->
                    volunteerEvent.title.contains(
                        mapSearchQuery,
                        ignoreCase = true
                    ) ||
                            volunteerEvent.organisation.contains(
                                mapSearchQuery,
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
                .padding(
                    horizontal = 16.dp,
                    vertical = 16.dp
                )
        ) {
            Text(
                text = "Nearby Opportunities",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        OutlinedTextField(
            value = mapSearchQuery,
            onValueChange = {
                    updatedSearchQuery ->
                mapSearchQuery = updatedSearchQuery
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search location"
                )
            },
            placeholder = {
                Text("Search nearby opportunities")
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MapBackground)
        ) {
            Text(
                text = "George Town, Penang",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                fontSize = 12.sp,
                color = TextMuted
            )

            filteredMapVolunteerEvents.forEach {
                    volunteerEvent ->

                val mapPinIsSelected =
                    volunteerEvent.id ==
                            selectedMapVolunteerEvent?.id

                Column(
                    modifier = Modifier
                        .offset(
                            x = (volunteerEvent.mapX * 260f).dp,
                            y = (volunteerEvent.mapY * 380f).dp
                        )
                        .clickable {
                            selectedMapEventId =
                                volunteerEvent.id
                        },
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector =
                            Icons.Filled.LocationOn,
                        contentDescription =
                            volunteerEvent.title,
                        tint =
                            if (mapPinIsSelected) {
                                DeepGreen
                            } else {
                                Color(0xFF4A7C3F)
                            },
                        modifier = Modifier.size(
                            if (mapPinIsSelected) {
                                34.dp
                            } else {
                                28.dp
                            }
                        )
                    )

                    if (mapPinIsSelected) {
                        Text(
                            text = volunteerEvent.title,
                            modifier = Modifier
                                .background(
                                    color = Color.White,
                                    shape =
                                        RoundedCornerShape(6.dp)
                                )
                                .padding(
                                    horizontal = 6.dp,
                                    vertical = 3.dp
                                ),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark,
                            maxLines = 1
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .background(
                        color = Color.White.copy(
                            alpha = 0.92f
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(
                            DeepGreen,
                            CircleShape
                        )
                )

                Text(
                    text = "Volunteer opportunity",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        selectedMapVolunteerEvent?.let {
                volunteerEvent ->

            Card(
                onClick = {
                    onEventSelected(volunteerEvent.id)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 12.dp
                    ),
                shape = RoundedCornerShape(14.dp),
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
                                RoundedCornerShape(10.dp)
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
                            fontSize = 14.sp,
                            color = TextDark,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis
                        )

                        Text(
                            text =
                                volunteerEvent.organisation,
                            fontSize = 11.sp,
                            color = TextMuted
                        )

                        Text(
                            text =
                                "${volunteerEvent.distanceKm} km • " +
                                        volunteerEvent.date,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    Text(
                        text =
                            "${volunteerEvent.spotsLeft} left",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreen
                    )
                }
            }
        }
    }
}