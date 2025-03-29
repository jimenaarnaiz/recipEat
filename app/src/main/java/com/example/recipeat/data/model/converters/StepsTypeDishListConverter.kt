package com.example.recipeat.data.model.converters

import androidx.room.TypeConverter

/**
 * Room necesita convertidores para manejar tipos complejos como List<Ingrediente> o List<String> (usado steps y dishTypes)
 */
class StepsTypeDishListConverter { //list<string> converter
    @TypeConverter
    fun fromStepsTypeDishListToString(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStepsTypeDishList(value: String): List<String> {
        return value.split(",")
    }
}