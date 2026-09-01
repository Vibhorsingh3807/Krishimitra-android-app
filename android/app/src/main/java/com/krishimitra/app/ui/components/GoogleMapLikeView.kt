package com.krishimitra.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.Gson
import com.krishimitra.app.domain.model.CropHealthStatus
import com.krishimitra.app.domain.model.FieldZone
import com.krishimitra.app.domain.model.GeoPoint
import com.krishimitra.app.domain.model.ZoneObservation
import com.krishimitra.app.domain.twin.FieldZoningEngine
import com.krishimitra.app.ui.theme.GreenDark
import com.krishimitra.app.ui.theme.GreenPrimary

/**
 * GoogleMapLikeView
 * High-performance interactive map facility providing real Satellite (Esri World Imagery)
 * and Street (OpenStreetMap) tiles, GPS location marker, boundary drawing, and 9-zone digital twin overlays.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleMapLikeView(
    modifier: Modifier = Modifier,
    initialCenterLat: Double = 28.7040,
    initialCenterLon: Double = 77.1025,
    initialZoom: Int = 16,
    isEditMode: Boolean = false,
    boundaryPoints: List<GeoPoint> = emptyList(),
    zones: List<FieldZone> = emptyList(),
    observations: Map<String, ZoneObservation> = emptyMap(),
    selectedZoneId: String? = null,
    userLocation: GeoPoint? = null,
    onBoundaryPointsChanged: (List<GeoPoint>) -> Unit = {},
    onZoneClick: (FieldZone) -> Unit = {}
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isSatelliteLayer by remember { mutableStateOf(true) }
    var isMapReady by remember { mutableStateOf(false) }

    // JavaScript Bridge
    class MapJsBridge {
        private val handler = Handler(Looper.getMainLooper())

        @JavascriptInterface
        fun onPointAdded(lat: Double, lon: Double) {
            handler.post {
                if (isEditMode) {
                    val updated = boundaryPoints + GeoPoint(lat, lon)
                    onBoundaryPointsChanged(updated)
                }
            }
        }

        @JavascriptInterface
        fun onZoneSelected(zoneId: String) {
            handler.post {
                val found = zones.find { it.id == zoneId }
                if (found != null) {
                    onZoneClick(found)
                }
            }
        }

        @JavascriptInterface
        fun onMapLoaded() {
            handler.post {
                isMapReady = true
                updateMapState(
                    webView = webViewRef,
                    isEditMode = isEditMode,
                    boundaryPoints = boundaryPoints,
                    zones = zones,
                    observations = observations,
                    userLocation = userLocation,
                    selectedZoneId = selectedZoneId
                )
            }
        }
    }

    // Effect: Synchronize boundary points or zones with WebView when state updates
    LaunchedEffect(isMapReady, boundaryPoints, zones, observations, userLocation, selectedZoneId) {
        if (isMapReady && webViewRef != null) {
            updateMapState(
                webView = webViewRef,
                isEditMode = isEditMode,
                boundaryPoints = boundaryPoints,
                zones = zones,
                observations = observations,
                userLocation = userLocation,
                selectedZoneId = selectedZoneId
            )
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(true)
                        builtInZoomControls = false
                    }
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Initial center
                            view?.evaluateJavascript("panTo($initialCenterLat, $initialCenterLon, $initialZoom);", null)
                        }
                    }

                    addJavascriptInterface(MapJsBridge(), "AndroidMapBridge")
                    loadDataWithBaseURL("https://krishimitra.local", buildMapHtml(initialCenterLat, initialCenterLon, initialZoom, isEditMode), "text/html", "utf-8", null)
                    webViewRef = this
                }
            }
        )

        // Floating Map Controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Satellite / Map Layer Toggle Button
            SmallFloatingActionButton(
                onClick = {
                    isSatelliteLayer = !isSatelliteLayer
                    webViewRef?.evaluateJavascript("toggleLayer(${if (isSatelliteLayer) "'satellite'" else "'street'"});", null)
                },
                containerColor = Color.White,
                contentColor = GreenDark,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isSatelliteLayer) Icons.Default.Layers else Icons.Default.Map,
                    contentDescription = "Toggle Layer",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Zoom In
            SmallFloatingActionButton(
                onClick = { webViewRef?.evaluateJavascript("zoomIn();", null) },
                containerColor = Color.White,
                contentColor = Color.DarkGray,
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
            }

            // Zoom Out
            SmallFloatingActionButton(
                onClick = { webViewRef?.evaluateJavascript("zoomOut();", null) },
                containerColor = Color.White,
                contentColor = Color.DarkGray,
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }

            // Recenter on Field or User
            SmallFloatingActionButton(
                onClick = {
                    if (userLocation != null) {
                        webViewRef?.evaluateJavascript("panTo(${userLocation.latitude}, ${userLocation.longitude}, 17);", null)
                    } else if (boundaryPoints.isNotEmpty()) {
                        val cLat = boundaryPoints.map { it.latitude }.average()
                        val cLon = boundaryPoints.map { it.longitude }.average()
                        webViewRef?.evaluateJavascript("panTo($cLat, $cLon, 16);", null)
                    } else {
                        webViewRef?.evaluateJavascript("panTo($initialCenterLat, $initialCenterLon, $initialZoom);", null)
                    }
                },
                containerColor = Color.White,
                contentColor = GreenPrimary,
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location", modifier = Modifier.size(20.dp))
            }
        }

        // Layer Badge
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.65f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isSatelliteLayer) Color(0xFF4CAF50) else Color(0xFF2196F3))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSatelliteLayer) "🛰️ Esri उपग्रह (Satellite)" else "🗺️ OpenStreetMap",
                    color = Color.White,
                    fontSize = 10.5.sp
                )
            }
        }
    }
}

/**
 * Sends updated GeoJSON/state to Leaflet JavaScript
 */
private fun updateMapState(
    webView: WebView?,
    isEditMode: Boolean,
    boundaryPoints: List<GeoPoint>,
    zones: List<FieldZone>,
    observations: Map<String, ZoneObservation>,
    userLocation: GeoPoint?,
    selectedZoneId: String?
) {
    if (webView == null) return
    val gson = Gson()

    // 1. Boundary / User Marker
    val pointsJson = gson.toJson(boundaryPoints.map { listOf(it.latitude, it.longitude) })
    val userLocJson = if (userLocation != null) "[${userLocation.latitude}, ${userLocation.longitude}]" else "null"

    // 2. Zones GeoJSON with color codes
    val zoneDataList = zones.map { z ->
        val obs = observations[z.id]
        val fillColor = when (obs?.cropHealth) {
            CropHealthStatus.EXCELLENT -> "#2E7D32" // Dark Green
            CropHealthStatus.GOOD -> "#4CAF50"      // Light Green
            CropHealthStatus.MODERATE -> "#FBC02D"  // Amber
            CropHealthStatus.POOR -> "#D32F2F"      // Red
            null -> "#81C784"
        }
        val isSelected = z.id == selectedZoneId
        mapOf(
            "id" to z.id,
            "label" to z.zoneLabel,
            "labelHi" to z.zoneLabelHi,
            "boundary" to z.boundary.map { listOf(it.latitude, it.longitude) },
            "center" to listOf(z.centerLat, z.centerLon),
            "fillColor" to fillColor,
            "isSelected" to isSelected,
            "moisture" to (obs?.moisture ?: 25),
            "health" to (obs?.cropHealth?.name ?: "GOOD")
        )
    }
    val zonesJson = gson.toJson(zoneDataList)

    val js = "renderMapData($isEditMode, $pointsJson, $zonesJson, $userLocJson, '$selectedZoneId');"
    webView.evaluateJavascript(js, null)
}

/**
 * Builds the standalone Leaflet HTML page containing Satellite & Street tile layers,
 * high-performance Canvas vector renderers, tap listeners, and zone styling.
 */
private fun buildMapHtml(lat: Double, lon: Double, zoom: Int, isEditMode: Boolean): String {
    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <style>
            html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #1a1a1a; }
            .leaflet-control-attribution, .leaflet-control-zoom { display: none !important; }
            .zone-badge {
                background: rgba(0,0,0,0.75);
                color: #ffffff;
                font-weight: bold;
                font-size: 11px;
                padding: 2px 6px;
                border-radius: 10px;
                border: 1px solid rgba(255,255,255,0.8);
                text-align: center;
                white-space: nowrap;
            }
            .user-pulse {
                width: 16px;
                height: 16px;
                background: #1976D2;
                border: 3px solid #ffffff;
                border-radius: 50%;
                box-shadow: 0 0 10px rgba(25,118,210,0.9);
            }
        </style>
    </head>
    <body>
        <div id="map"></div>
        <script>
            var map = L.map('map', {
                zoomControl: false,
                attributionControl: false
            }).setView([$lat, $lon], $zoom);

            // Tile Layers
            var satelliteLayer = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
                maxZoom: 19
            }).addTo(map);

            var streetLayer = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19
            });

            var boundaryLayer = L.layerGroup().addTo(map);
            var zonesLayer = L.layerGroup().addTo(map);
            var userLayer = L.layerGroup().addTo(map);

            var isEditMode = $isEditMode;

            // Tap listener for Add Field boundary plotting
            map.on('click', function(e) {
                if (isEditMode && window.AndroidMapBridge) {
                    window.AndroidMapBridge.onPointAdded(e.latlng.lat, e.latlng.lng);
                }
            });

            function toggleLayer(type) {
                if (type === 'satellite') {
                    map.removeLayer(streetLayer);
                    map.addLayer(satelliteLayer);
                } else {
                    map.removeLayer(satelliteLayer);
                    map.addLayer(streetLayer);
                }
            }

            function zoomIn() { map.zoomIn(); }
            function zoomOut() { map.zoomOut(); }
            function panTo(lat, lon, z) {
                map.setView([lat, lon], z || map.getZoom(), { animate: true });
            }

            function renderMapData(editMode, points, zones, userLoc, selectedId) {
                isEditMode = editMode;
                boundaryLayer.clearLayers();
                zonesLayer.clearLayers();
                userLayer.clearLayers();

                // 1. Render User GPS Beacon
                if (userLoc && userLoc.length === 2) {
                    var userIcon = L.divIcon({ className: 'user-pulse', iconSize: [16, 16], iconAnchor: [8, 8] });
                    L.marker(userLoc, { icon: userIcon }).addTo(userLayer);
                    L.circle(userLoc, { radius: 25, color: '#1976D2', fillColor: '#2196F3', fillOpacity: 0.15, weight: 1 }).addTo(userLayer);
                }

                // 2. Render Boundary Polygons & Markers
                if (points && points.length > 0) {
                    points.forEach(function(pt, idx) {
                        L.circleMarker(pt, {
                            radius: 6,
                            color: '#ffffff',
                            fillColor: '#2E7D32',
                            fillOpacity: 1,
                            weight: 2
                        }).addTo(boundaryLayer);
                    });

                    if (points.length >= 3) {
                        L.polygon(points, {
                            color: '#4CAF50',
                            fillColor: '#81C784',
                            fillOpacity: editMode ? 0.25 : 0.05,
                            weight: 2.5,
                            dashArray: editMode ? '4, 4' : null
                        }).addTo(boundaryLayer);
                    } else if (points.length === 2) {
                        L.polyline(points, { color: '#4CAF50', weight: 2.5, dashArray: '4, 4' }).addTo(boundaryLayer);
                    }
                }

                // 3. Render 9 Digital Twin Zones
                if (zones && zones.length > 0 && !editMode) {
                    zones.forEach(function(z) {
                        if (z.boundary && z.boundary.length >= 3) {
                            var poly = L.polygon(z.boundary, {
                                color: z.isSelected ? '#FFFFFF' : z.fillColor,
                                fillColor: z.fillColor,
                                fillOpacity: z.isSelected ? 0.65 : 0.42,
                                weight: z.isSelected ? 3.5 : 1.5
                            }).addTo(zonesLayer);

                            poly.on('click', function() {
                                if (window.AndroidMapBridge) {
                                    window.AndroidMapBridge.onZoneSelected(z.id);
                                }
                            });

                            // Zone Label Badge
                            if (z.center && z.center.length === 2) {
                                var badgeHtml = '<div class="zone-badge" style="border-color:' + z.fillColor + '">' + z.id + '</div>';
                                var badgeIcon = L.divIcon({ html: badgeHtml, className: '', iconSize: [40, 20], iconAnchor: [20, 10] });
                                var badgeMarker = L.marker(z.center, { icon: badgeIcon }).addTo(zonesLayer);
                                badgeMarker.on('click', function() {
                                    if (window.AndroidMapBridge) {
                                        window.AndroidMapBridge.onZoneSelected(z.id);
                                    }
                                });
                            }
                        }
                    });
                }
            }

            // Signal ready
            if (window.AndroidMapBridge && window.AndroidMapBridge.onMapLoaded) {
                window.AndroidMapBridge.onMapLoaded();
            }
        </script>
    </body>
    </html>
    """.trimIndent()
}
