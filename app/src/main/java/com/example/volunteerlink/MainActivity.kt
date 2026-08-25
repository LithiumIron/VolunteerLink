
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
import com.example.volunteerlink.data.VolunteerDashboardDataSource
import com.example.volunteerlink.navigation.AppNavGraph
import com.example.volunteerlink.ui.theme.VolunteerLinkTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // While the app is visible, keep checking the tiny one-row test clock table.
    // This is intentionally simple for testing/demo use, so changing the fake
    // date in Supabase is reflected on the phone without clearing/restarting it.
    private var appClockRefreshJob: Job? = null

    override fun onResume() {
        super.onResume()
        startAppClockRefresh()
    }

    override fun onPause() {
        appClockRefreshJob?.cancel()
        appClockRefreshJob = null
        super.onPause()
    }

    private fun startAppClockRefresh() {
        appClockRefreshJob?.cancel()
        appClockRefreshJob = lifecycleScope.launch {
            while (isActive) {
                // Refresh immediately, then check again every 3 seconds while
                // VolunteerLink stays in the foreground.
                AppClock.refreshFromDatabase()
                delay(APP_CLOCK_REFRESH_INTERVAL_MS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        VolunteerDashboardDataSource.initialise(applicationContext)

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

    companion object {
        // The demo clock rarely changes. A one-minute interval avoids polling
        // Supabase 1,200 times per foreground hour while remaining demo-friendly.
        private const val APP_CLOCK_REFRESH_INTERVAL_MS = 60_000L
    }
}


