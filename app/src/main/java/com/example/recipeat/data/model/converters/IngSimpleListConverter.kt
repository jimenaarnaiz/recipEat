package com.example.recipeat.data.model.converters

import androidx.room.TypeConverter
import com.example.recipeat.data.model.IngredienteSimple
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class IngSimpleListConverter {
    @TypeConverter
    fun fromIngredienteSimpleList(value: List<IngredienteSimple>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toIngredienteSimpleList(value: String): List<IngredienteSimple> {
        val listType = object : TypeToken<List<IngredienteSimple>>() {}.type
        return Gson().fromJson(value, listType)
    }
}
