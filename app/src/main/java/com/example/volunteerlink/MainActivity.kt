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
import androidx.lifecycle.lifecycleScope
import com.example.volunteerlink.data.time.AppClock
import com.example.volunteerlink.navigation.AppNavGraph
import com.example.volunteerlink.ui.theme.VolunteerLinkTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()

        // Refresh the shared clock every time the app returns to the foreground.
        // This makes Supabase test-clock changes easy to preview during testing.
        lifecycleScope.launch {
            AppClock.refreshFromDatabase()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VolunteerLinkTheme(
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Wait for the first clock read before opening screens that
                    // calculate "today". If Supabase cannot be read, AppClock
                    // marks itself loaded and safely falls back to phone time.
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
