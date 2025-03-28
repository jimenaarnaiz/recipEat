package com.example.recipeat.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.recipeat.data.dao.RecetaDao
import com.example.recipeat.data.model.RecetaRoom
import com.example.recipeat.data.model.converters.IngredienteListConverter
import com.example.recipeat.data.model.converters.StepsTypeDishListConverter

@Database(entities = [RecetaRoom::class], version = 2) //cambiar de vers si se cambia algo de la db
@TypeConverters(IngredienteListConverter::class, StepsTypeDishListConverter::class)

abstract class AppDatabase : RoomDatabase() {

    abstract fun recetaDao(): RecetaDao

    companion object {
        // Volatile asegura que la variable se mantiene sincronizada entre hilos
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Métdo para obtener la instancia de la base de datos
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Si no existe la instancia, se crea
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recetas-database"
                )
                    .fallbackToDestructiveMigration()  // Esto eliminará la base de datos si modifica el esquema (cmabios en entidades..)
                    .build()

                // Asignar la instancia a INSTANCE
                INSTANCE = instance

                // Devolver la instancia
                instance
            }
        }
    }
}