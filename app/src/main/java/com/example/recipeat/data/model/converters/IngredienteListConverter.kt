package com.example.recipeat.data.model.converters

import androidx.room.TypeConverter
import com.example.recipeat.data.model.Ingrediente
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class IngredienteListConverter {
    @TypeConverter
    fun fromIngredienteList(value: List<Ingrediente>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toIngredienteList(value: String): List<Ingrediente> {
        val listType = object : TypeToken<List<Ingrediente>>() {}.type
        return Gson().fromJson(value, listType)
    }
}
