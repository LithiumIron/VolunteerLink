package com.example.volunteerlink.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object VolunteerOnline {
    fun available(context: Context): Boolean = runCatching {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
        capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }.getOrDefault(false)

    fun requireConnection(context: Context, action: String) {
        check(available(context)) { "Internet connection is required to $action. Connect and try again." }
    }
}
