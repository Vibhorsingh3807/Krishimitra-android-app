package com.krishimitra.app.domain.twin

import com.krishimitra.app.domain.model.FarmerField
import com.krishimitra.app.domain.model.FieldZone
import com.krishimitra.app.domain.model.GeoPoint
import kotlin.math.*

/**
 * FieldZoningEngine
 *
 * Implements mathematical zoning and geodesic area calculation for farmer fields:
 * 1. Geodesic area calculation (Spherical projection converted to Acres).
 * 2. Regular Grid-based Management Zone Generation (3x3 grid resulting in 9 digital zones).
 * 3. Future-ready architecture allowing dynamic clustering based on remote-sensing NDVI variance.
 */
object FieldZoningEngine {

    private const val EARTH_RADIUS_METERS = 6378137.0
    private const val SQ_METERS_PER_ACRE = 4046.8564224

    /**
     * Calculates the approximate geodesic area of a polygon in Acres.
     * Uses the spherical polygon projection / Shoelace formula on equirectangular projection.
     */
    fun calculatePolygonAreaAcres(points: List<GeoPoint>): Double {
        if (points.size < 3) return 0.0

        val centerLat = points.map { it.latitude }.average()
        val latRad = Math.toRadians(centerLat)
        val metersPerDegreeLat = 111132.92 - 559.82 * cos(2 * latRad) + 1.175 * cos(4 * latRad)
        val metersPerDegreeLon = (Math.PI / 180.0) * EARTH_RADIUS_METERS * cos(latRad)

        // Project lat/lon to planar metric coordinates (X = Lon in meters, Y = Lat in meters)
        val metricPoints = points.map { p ->
            val x = (p.longitude - points[0].longitude) * metersPerDegreeLon
            val y = (p.latitude - points[0].latitude) * metersPerDegreeLat
            Pair(x, y)
        }

        // Shoelace formula
        var areaSqMeters = 0.0
        val n = metricPoints.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            areaSqMeters += metricPoints[i].first * metricPoints[j].second
            areaSqMeters -= metricPoints[j].first * metricPoints[i].second
        }
        areaSqMeters = abs(areaSqMeters) / 2.0

        val acres = areaSqMeters / SQ_METERS_PER_ACRE
        // Return rounded to 2 decimal places, minimum 0.5 acres for display if non-zero
        return max(0.1, round(acres * 10.0) / 10.0)
    }

    /**
     * Generates a 3x3 grid of 9 management zones from the field boundary polygon.
     */
    fun generateZones(field: FarmerField): List<FieldZone> {
        val boundary = field.boundary
        if (boundary.size < 3) {
            // Fallback default single zone
            return listOf(
                FieldZone(
                    id = "Z-01",
                    fieldId = field.id,
                    zoneNumber = 1,
                    zoneLabel = "Zone 1",
                    zoneLabelHi = "ज़ोन 1",
                    boundary = boundary,
                    centerLat = field.centerLat,
                    centerLon = field.centerLon,
                    areaAcres = field.areaAcres,
                    crop = field.cropName,
                    cropHi = field.cropNameHi
                )
            )
        }

        val minLat = boundary.minOf { it.latitude }
        val maxLat = boundary.maxOf { it.latitude }
        val minLon = boundary.minOf { it.longitude }
        val maxLon = boundary.maxOf { it.longitude }

        val rows = 3
        val cols = 3
        val latStep = (maxLat - minLat) / rows
        val lonStep = (maxLon - minLon) / cols

        val zoneArea = round((field.areaAcres / (rows * cols)) * 100.0) / 100.0
        val zones = mutableListOf<FieldZone>()
        var count = 1

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val zMinLat = maxLat - (r + 1) * latStep
                val zMaxLat = maxLat - r * latStep
                val zMinLon = minLon + c * lonStep
                val zMaxLon = minLon + (c + 1) * lonStep

                val zCenterLat = (zMinLat + zMaxLat) / 2.0
                val zCenterLon = (zMinLon + zMaxLon) / 2.0

                // 4 corners of the rectangular grid cell
                val cellPolygon = listOf(
                    GeoPoint(zMaxLat, zMinLon),
                    GeoPoint(zMaxLat, zMaxLon),
                    GeoPoint(zMinLat, zMaxLon),
                    GeoPoint(zMinLat, zMinLon)
                )

                val id = "Z-%02d".format(count)
                zones.add(
                    FieldZone(
                        id = id,
                        fieldId = field.id,
                        zoneNumber = count,
                        zoneLabel = "Zone $count",
                        zoneLabelHi = "ज़ोन $count",
                        boundary = cellPolygon,
                        centerLat = zCenterLat,
                        centerLon = zCenterLon,
                        areaAcres = max(0.1, zoneArea),
                        crop = field.cropName,
                        cropHi = field.cropNameHi
                    )
                )
                count++
            }
        }

        return zones
    }
}
