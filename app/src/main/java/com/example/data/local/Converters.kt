package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.GeoPoint
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromGeoPointList(points: List<GeoPoint>?): String {
        if (points.isNullOrEmpty()) return "[]"
        val array = JSONArray()
        for (p in points) {
            val obj = JSONObject()
            obj.put("lat", p.lat)
            obj.put("lng", p.lng)
            obj.put("time", p.timestamp)
            obj.put("speed", p.speedMps.toDouble())
            obj.put("alt", p.altitudeMeters)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toGeoPointList(data: String?): List<GeoPoint> {
        if (data.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<GeoPoint>()
        try {
            val array = JSONArray(data)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    GeoPoint(
                        lat = obj.optDouble("lat", 0.0),
                        lng = obj.optDouble("lng", 0.0),
                        timestamp = obj.optLong("time", 0L),
                        speedMps = obj.optDouble("speed", 0.0).toFloat(),
                        altitudeMeters = obj.optDouble("alt", 0.0)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
