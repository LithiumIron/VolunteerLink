package com.example.volunteerlink.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.R
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary

data class VolunteerBottomNavigationItem(
    val navigationRoute: String,
    val navigationLabel: String,
    @param:DrawableRes
    val navigationIconResourceId: Int,
    val navigationIconDisplaySize: Dp
)

@Composable
fun VolunteerBottomNavigationBar(
    currentVolunteerNavigationRoute: String?,
    onVolunteerNavigationItemSelected: (
        navigationRoute: String
    ) -> Unit
) {
    val volunteerBottomNavigationItems =
        listOf(
            VolunteerBottomNavigationItem(
                navigationRoute =
                    VolunteerOpportunityNavigationRoutes
                        .VOLUNTEER_HOME_ROUTE,
                navigationLabel = "Home",
                navigationIconResourceId =
                    R.drawable.ic_volunteer_home,
                navigationIconDisplaySize = 25.dp
            ),

            VolunteerBottomNavigationItem(
                navigationRoute =
                    VolunteerOpportunityNavigationRoutes
                        .VOLUNTEER_MAP_ROUTE,
                navigationLabel = "Map",
                navigationIconResourceId =
                    R.drawable.ic_volunteer_map,
                navigationIconDisplaySize = 25.dp
            ),

            VolunteerBottomNavigationItem(
                navigationRoute =
                    VolunteerOpportunityNavigationRoutes
                        .VOLUNTEER_SKILL_PATH_ROUTE,
                navigationLabel = "Skill Path",
                navigationIconResourceId =
                    R.drawable.ic_volunteer_skill_path,
                navigationIconDisplaySize = 27.dp
            ),

            VolunteerBottomNavigationItem(
                navigationRoute =
                    VolunteerOpportunityNavigationRoutes
                        .VOLUNTEER_CHAT_ROUTE,
                navigationLabel = "Chats",
                navigationIconResourceId =
                    R.drawable.ic_volunteer_chat,
                navigationIconDisplaySize = 27.dp
            ),

            VolunteerBottomNavigationItem(
                navigationRoute =
                    VolunteerOpportunityNavigationRoutes
                        .VOLUNTEER_PROFILE_ROUTE,
                navigationLabel = "Profile",
                navigationIconResourceId =
                    R.drawable.ic_volunteer_profile,
                navigationIconDisplaySize = 27.dp
            )
        )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = 10.dp,
                end = 10.dp,
                bottom = 8.dp
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(22.dp),
            color =
                VolunteerLinkSoftGreenSurface.copy(
                    alpha = 0.94f
                ),
            border = BorderStroke(
                width = 1.dp,
                color =
                    VolunteerLinkPrimaryGreen.copy(
                        alpha = 0.55f
                    )
            ),
            shadowElevation = 10.dp,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 4.dp,
                        vertical = 5.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                volunteerBottomNavigationItems
                    .forEach {
                            volunteerBottomNavigationItem ->

                        val navigationItemIsSelected =
                            currentVolunteerNavigationRoute ==
                                    volunteerBottomNavigationItem
                                        .navigationRoute

                        VolunteerFloatingNavigationItem(
                            volunteerBottomNavigationItem =
                                volunteerBottomNavigationItem,
                            navigationItemIsSelected =
                                navigationItemIsSelected,
                            onNavigationItemSelected = {
                                onVolunteerNavigationItemSelected(
                                    volunteerBottomNavigationItem
                                        .navigationRoute
                                )
                            }
                        )
                    }
            }
        }
    }
}

@Composable
private fun RowScope.VolunteerFloatingNavigationItem(
    volunteerBottomNavigationItem:
    VolunteerBottomNavigationItem,
    navigationItemIsSelected: Boolean,
    onNavigationItemSelected: () -> Unit
) {
    val navigationContentColour =
        if (navigationItemIsSelected) {
            VolunteerLinkPrimaryGreen
        } else {
            VolunteerLinkTextSecondary.copy(
                alpha = 0.78f
            )
        }

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .clip(
                RoundedCornerShape(17.dp)
            )
            .clickable(
                onClick =
                    onNavigationItemSelected
            )
            .padding(
                horizontal = 2.dp,
                vertical = 2.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(37.dp)
                .background(
                    color =
                        if (navigationItemIsSelected) {
                            VolunteerLinkPrimaryGreen
                                .copy(alpha = 0.14f)
                        } else {
                            Color.Transparent
                        },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    id =
                        volunteerBottomNavigationItem
                            .navigationIconResourceId
                ),
                contentDescription =
                    volunteerBottomNavigationItem
                        .navigationLabel,
                modifier = Modifier.size(
                    volunteerBottomNavigationItem
                        .navigationIconDisplaySize
                ),
                tint = navigationContentColour
            )
        }

        Text(
            text =
                volunteerBottomNavigationItem
                    .navigationLabel
                    .uppercase(),
            modifier = Modifier.fillMaxWidth(),
            fontSize = 8.sp,
            lineHeight = 9.sp,
            fontWeight =
                if (navigationItemIsSelected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                },
            color = navigationContentColour,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}