package com.example.volunteerlink.organisation.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.volunteerlink.organisation.auth.OrganisationSessionStore
import com.example.volunteerlink.organisation.repository.RecentPostSummary

@Composable
fun OrganisationProfileScreen(
    onSettingsSelected: () -> Unit = {},
    onEditProfileSelected: () -> Unit = {},
    onRecentPostsSelected: () -> Unit = {},
    onPostSelected: (String) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    // Read from the shared cache instead of fetching locally — mirrors
    // VolunteerProfileScreen's use of VolunteerOpportunitySessionStore.
    val data = OrganisationSessionStore.profileData

    // null + still loading -> show the spinner.
    // null + not loading -> the fetch actually failed; show the error
    // message below instead of spinning forever.
    val isLoading = data == null && OrganisationSessionStore.isProfileLoading

    // Refetch every time this screen is visited, not just the first time —
    // same reasoning as VolunteerProfileScreen: onRefresh() is idempotent
    // and cheap to call again, and this is what keeps the profile current
    // without requiring a trip through Edit to invalidate the cache.
    LaunchedEffect(Unit) {
        onRefresh()
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 102.dp)
    ) {

        // =====================================================
        // TOP BAR
        // =====================================================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .height(70.dp)
        ) {
            Text(
                text = "PROFILE",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 14.dp)
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onSettingsSelected) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        if (data == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Couldn't load your organisation profile.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        // =====================================================
        // PROFILE HEADER
        // =====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 20.dp, end = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!data.profileImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = data.profileImageUrl,
                        contentDescription = "Organisation logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Business,
                        contentDescription = "Organisation logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
            ) {
                Text(
                    text = data.organisationName,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2
                )

                if (data.organisationType.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = data.organisationType,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = data.loginEmail,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onEditProfileSelected,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit Profile",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Description, location and member-since now sit full-width below
        // the header row, instead of squeezed into the narrow column
        // beside the logo.
        if (data.description.isNotBlank()) {
            Text(
                text = data.description,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (data.locationName.isNotBlank() ||
            data.stateRegion.isNotBlank() ||
            data.country.isNotBlank()
        ) {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = listOf(data.locationName, data.stateRegion, data.country)
                        .filter { it.isNotBlank() }
                        .joinToString(", "),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "Member since ${data.memberSince}",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // =====================================================
        // VERIFICATION STATUS
        // =====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        when (data.verificationStatus) {
                            "VERIFIED" -> Color(0xFFACD8A7)
                            "REJECTED" -> Color(0xFFE8A6A6)
                            else -> Color(0xFFE8D9A6) // PENDING
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = when (data.verificationStatus) {
                        "VERIFIED" -> "Verified organisation"
                        "REJECTED" -> "Verification rejected"
                        else -> "Verification pending"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // =====================================================
        // RECENTLY POSTED EVENTS
        // =====================================================

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recently Posted Events",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "See all",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onRecentPostsSelected() }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (data.recentPosts.isEmpty()) {
                Text(
                    text = "No posts yet",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.recentPosts.take(3).forEach { post: RecentPostSummary ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(65.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { onPostSelected(post.postId) }
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = post.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = post.status,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}