package com.example.recipeat.data.model

import androidx.room.Entity
import androidx.room.TypeConverters
import com.example.recipeat.data.model.converters.TimestampConverter
import java.sql.Timestamp

@Entity(
    tableName = "favoritos",
    primaryKeys = ["userId", "recetaId"]
)
data class Favorito(
    val id: Long = 0, // ID único para la tabla de favoritos
    val userId: String, // ID del usuario que ha marcado la receta como favorita
    val recetaId: String, // ID de la receta que es favorita
    @TypeConverters(TimestampConverter::class)val date: Timestamp
)