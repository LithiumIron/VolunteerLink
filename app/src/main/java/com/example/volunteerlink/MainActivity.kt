
package com.example.volunteerlink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.volunteerlink.data.time.AppClock
import com.example.volunteerlink.data.VolunteerDashboardDataSource
import com.example.volunteerlink.navigation.AppNavGraph
import com.example.volunteerlink.ui.theme.VolunteerLinkTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppClock.initialise(applicationContext)
        VolunteerDashboardDataSource.initialise(applicationContext)

        setContent {
            VolunteerLinkTheme(
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Volunteer screens use the phone's real clock. Supabase
                    // fixture time can no longer override the application.
                    val clockState by AppClock.state.collectAsStateWithLifecycle()

                    if (clockState.isLoaded) {
                        // Root navigation now starts with the temporary
                        // Volunteer / Organisation selection page.
                        AppNavGraph()
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
