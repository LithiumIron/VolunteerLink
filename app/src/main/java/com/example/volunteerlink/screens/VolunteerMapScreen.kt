package com.example.volunteerapp.ui.screens.applicant

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerapp.data.MockRepository
import com.example.volunteerapp.data.VolunteerEvent
import com.example.volunteerapp.ui.theme.*
import com.example.volunteerlink.model.VolunteerEvent
import com.example.volunteerlink.ui.theme.CardBeige
import com.example.volunteerlink.ui.theme.CreamBackground
import com.example.volunteerlink.ui.theme.DeepGreen
import com.example.volunteerlink.ui.theme.MapBackground
import com.example.volunteerlink.ui.theme.RiverBlue
import com.example.volunteerlink.ui.theme.TextDark
import com.example.volunteerlink.ui.theme.TextMuted

@Composable
fun MapScreen(onEventSelected: (String) -> Unit) {
    var searchText by remember { mutableStateOf("GeorgeTown, Penang") }
    var selectedEvent by remember { mutableStateOf(MockRepository.events.firstOrNull { it.id == "event_poster_design" }) }
    val events = MockRepository.events

    val filteredEvents = if (searchText.isBlank()) events else events

    Column(modifier = Modifier.fillMaxSize().background(CreamBackground)) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepGreen)
                .padding(vertical = 16.dp, horizontal = 16.dp)
        ) {
            Text("NEARBY EVENTS", color = CardBeige, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        // Search bar
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            placeholder = { Text("Search location") },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )

        // Map area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MapBackground)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // simple street grid to mimic the screenshot
                val step = size.width / 5f
                var x = 0f
                while (x < size.width) {
                    drawLine(Color(0xFFD8D2C0), Offset(x, 0f), Offset(x, size.height), strokeWidth = 2f)
                    x += step
                }
                var y = 0f
                val stepY = size.height / 8f
                while (y < size.height) {
                    drawLine(Color(0xFFD8D2C0), Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
                    y += stepY
                }
                // river
                drawLine(
                    RiverBlue,
                    Offset(0f, size.height * 0.65f),
                    Offset(size.width, size.height * 0.8f),
                    strokeWidth = 26f
                )
                // "your location" dot
                drawCircle(Color(0xFF3B6FD6), radius = 14f, center = Offset(size.width * 0.38f, size.height * 0.4f))
                drawCircle(Color.White, radius = 14f, center = Offset(size.width * 0.38f, size.height * 0.4f), style = Stroke(width = 3f))
            }

            filteredEvents.forEach { event ->
                MapPin(
                    event = event,
                    selected = event.id == selectedEvent?.id,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(
                            start = (event.mapX * 260).dp,
                            top = (event.mapY * 380).dp
                        )
                        .clickable { selectedEvent = event }
                )
            }

            // Legend
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                LegendRow(color = Color(0xFF3B6FD6), label = "Your location")
                LegendRow(color = DeepGreen, label = "Volunteer event")
            }
        }

        // Quick chips row for other nearby events
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            events.filter { it.id != selectedEvent?.id }.take(2).forEach { ev ->
                AssistChip(
                    onClick = { selectedEvent = ev },
                    label = { Text("${ev.title}  ${ev.distanceKm}km") },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        // Bottom event card
        selectedEvent?.let { event ->
            Card(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEventSelected(event.id) },
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardBeige)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(event.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(event.organisation, fontSize = 12.sp, color = TextMuted)
                        Row {
                            Text("📍 ${event.distanceKm}km", fontSize = 11.sp, color = TextMuted)
                            Spacer(Modifier.width(8.dp))
                            Text("🗓 ${event.date}", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${event.spotsLeft} Left", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepGreen)
                        Icon(Icons.Filled.LocationOn, contentDescription = "Open", tint = DeepGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun MapPin(event: VolunteerEvent, selected: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(if (selected) DeepGreen else Color.White, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.LocationOn,
            contentDescription = event.title,
            tint = if (selected) Color.White else DeepGreen,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            event.title,
            fontSize = 11.sp,
            color = if (selected) Color.White else TextDark,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 10.sp)
    }
}



