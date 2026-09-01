package com.krishimitra.app.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Locale

class DeviceLocationProvider(private val context: Context) {

    companion object {
        private const val TAG = "DeviceLocationProvider"
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        var bestLocation: Location? = null

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        for (provider in providers) {
            try {
                if (lm.isProviderEnabled(provider)) {
                    val loc = lm.getLastKnownLocation(provider)
                    if (loc != null) {
                        if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                            bestLocation = loc
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException accessing provider $provider: ${e.message}")
            } catch (e: Exception) {
                Log.w(TAG, "Error accessing provider $provider: ${e.message}")
            }
        }

        return bestLocation
    }

    fun getResolvedLocationName(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val district = addr.subAdminArea ?: addr.locality ?: addr.adminArea ?: "स्थानीय क्षेत्र"
                val state = addr.adminArea ?: "भारत"
                "$district, $state"
            } else {
                "स्थानीय कृषि क्षेत्र ($lat, $lon)"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Geocoding failed: ${e.message}")
            "स्थानीय कृषि क्षेत्र"
        }
    }
}
