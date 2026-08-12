package com.example.volunteerlink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.volunteerlink.navigation.AppNavGraph
import com.example.volunteerlink.ui.theme.VolunteerLinkTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VolunteerLinkTheme(
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Root navigation now starts with the temporary
                    // Volunteer / Organisation selection page.
                    AppNavGraph()
                }
            }
        }
    }
}
