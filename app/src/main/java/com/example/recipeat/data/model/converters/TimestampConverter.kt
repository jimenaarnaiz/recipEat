package com.example.recipeat.data.model.converters

import androidx.room.TypeConverter
import java.sql.Timestamp

class TimestampConverter {
    // Convierte Timestamp a Long (tiempo en milisegundos)
    @TypeConverter
    fun fromTimestamp(timestamp: Timestamp?): Long? {
        return timestamp?.time
    }

    // Convierte Long (milisegundos) a Timestamp
    @TypeConverter
    fun toTimestamp(time: Long?): Timestamp? {
        return time?.let { Timestamp(it) }
    }
}