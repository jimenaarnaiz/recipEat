package com.example.recipeat.data.model

import androidx.room.Entity
import androidx.room.TypeConverters
import com.example.recipeat.data.model.converters.TimestampConverter
import java.sql.Timestamp

@Entity(
    tableName = "recientes",
    primaryKeys = ["userId", "recetaId"]
)
data class Reciente(
    val recetaId: String,
    @TypeConverters(TimestampConverter::class)val fechaVista: Timestamp,
    val userId: String     // Para diferenciar usuarios
)