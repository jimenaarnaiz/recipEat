package com.example.recipeat.data.repository

import android.util.Log
import com.example.recipeat.data.model.IngredienteSimple
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class IngredienteRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun extraerIngredientesYGuardar(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ingredientesSnapshot = db.collection("ingredientes").get().await()

            if (!ingredientesSnapshot.isEmpty) {
                Log.d("Firestore", "La colección 'ingredientes' ya tiene datos. No se ejecutará.")
                return@withContext Result.success(Unit)
            }

            val recetasSnapshot = db.collection("recetas").get().await()

            val ingredientesSet = mutableSetOf<IngredienteSimple>()

            for (document in recetasSnapshot.documents) {
                val ingredientsList = document.get("ingredients") as? List<Map<String, Any>> ?: continue

                for (ingredient in ingredientsList) {
                    val name = ingredient["name"] as? String ?: continue
                    val image = ingredient["image"] as? String ?: ""

                    if (name.isNotEmpty() && !name.startsWith("add ", true) && !name.startsWith("all ", true)) {
                        ingredientesSet.add(IngredienteSimple(name, image))
                    }
                }
            }

            if (ingredientesSet.isEmpty()) {
                Log.d("Firestore", "No se encontraron ingredientes.")
                return@withContext Result.success(Unit)
            }

            val startingIndex = ingredientesSnapshot.size() + 1
            val sortedIngredientes = ingredientesSet.sortedBy { it.name }

            sortedIngredientes.forEachIndexed { index, ingrediente ->
                val docId = (startingIndex + index).toString()
                val data = mapOf("name" to ingrediente.name, "image" to ingrediente.image)
                db.collection("ingredientes").document(docId).set(data).await()
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("Firestore", "Error extrayendo o guardando ingredientes", e)
            Result.failure(e)
        }
    }


    suspend fun buscarIngredientes(termino: String): List<IngredienteSimple> = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = db.collection("bulkIngredients")
                .whereGreaterThanOrEqualTo("name", termino)
                .whereLessThanOrEqualTo("name", termino + "\uF8FF")
                .limit(9)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                val image = doc.getString("image") ?: ""
                if (name.contains(termino, ignoreCase = true)) IngredienteSimple(name, image) else null
            }

        } catch (e: Exception) {
            Log.e("Firestore", "Error buscando ingredientes", e)
            emptyList()
        }
    }


    suspend fun loadIngredientsFromFirebase(): List<IngredienteSimple> = withContext(Dispatchers.IO) {
        return@withContext try {
            val result = db.collection("bulkIngredients").get().await()
            result.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                val image = doc.getString("image") ?: ""
                IngredienteSimple(name, image)
            }
        } catch (e: Exception) {
            Log.e("Firebase", "Error cargando ingredientes", e)
            emptyList()
        }
    }
}
