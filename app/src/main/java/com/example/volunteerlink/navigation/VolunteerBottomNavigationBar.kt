package com.example.volunteerlink.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.R
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary


data class VolunteerBottomNavigationItem(
    val navigationRoute: String,
    val navigationLabel: String,
    @DrawableRes
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
                navigationIconDisplaySize = 24.dp
            ),

            VolunteerBottomNavigationItem(
                navigationRoute =
                    VolunteerOpportunityNavigationRoutes
                        .VOLUNTEER_CHAT_ROUTE,
                navigationLabel = "Chat",
                navigationIconResourceId =
                    R.drawable.ic_volunteer_chat,
                navigationIconDisplaySize = 28.dp
            ),

            VolunteerBottomNavigationItem(
                navigationRoute =
                    VolunteerOpportunityNavigationRoutes
                        .VOLUNTEER_SKILL_PATH_ROUTE,
                navigationLabel = "Skill Path",
                navigationIconResourceId =
                    R.drawable.ic_volunteer_skill_path,
                navigationIconDisplaySize = 28.dp
            ),

            VolunteerBottomNavigationItem(
                navigationRoute =
                    VolunteerOpportunityNavigationRoutes
                        .VOLUNTEER_MAP_ROUTE,
                navigationLabel = "Map",
                navigationIconResourceId =
                    R.drawable.ic_volunteer_map,
                navigationIconDisplaySize = 24.dp
            ),

            VolunteerBottomNavigationItem(
                navigationRoute =
                    VolunteerOpportunityNavigationRoutes
                        .VOLUNTEER_PROFILE_ROUTE,
                navigationLabel = "Profile",
                navigationIconResourceId =
                    R.drawable.ic_volunteer_profile,
                navigationIconDisplaySize = 28.dp
            )
        )


    NavigationBar(
        containerColor = VolunteerLinkSurface
    ) {

        volunteerBottomNavigationItems
            .forEach { volunteerBottomNavigationItem ->

                val navigationItemIsSelected =
                    currentVolunteerNavigationRoute ==
                            volunteerBottomNavigationItem
                                .navigationRoute

                NavigationBarItem(

                    selected =
                        navigationItemIsSelected,

                    onClick = {
                        onVolunteerNavigationItemSelected(
                            volunteerBottomNavigationItem
                                .navigationRoute
                        )
                    },

                    icon = {

                        // Same outer box for every icon.
                        // This keeps all labels vertically aligned.
                        Box(
                            modifier = Modifier.size(30.dp),
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

                                modifier =
                                    Modifier.size(
                                        volunteerBottomNavigationItem
                                            .navigationIconDisplaySize
                                    )
                            )
                        }
                    },

                    label = {

                        Text(
                            text =
                                volunteerBottomNavigationItem
                                    .navigationLabel,

                            fontSize = 10.sp,

                            lineHeight = 11.sp,

                            maxLines = 1,

                            textAlign = TextAlign.Center
                        )
                    },

                    alwaysShowLabel = true,

                    colors =
                        NavigationBarItemDefaults.colors(

                            selectedIconColor =
                                VolunteerLinkPrimaryGreen,

                            selectedTextColor =
                                VolunteerLinkPrimaryGreen,

                            indicatorColor =
                                VolunteerLinkPrimaryGreen
                                    .copy(alpha = 0.12f),

                            unselectedIconColor =
                                VolunteerLinkTextSecondary,

                            unselectedTextColor =
                                VolunteerLinkTextSecondary
                        )
                )
            }
    }
}