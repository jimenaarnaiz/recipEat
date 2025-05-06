package com.example.recipeat.model.repositories

import android.util.Log
import com.example.recipeat.data.repository.IngredienteRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.*
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import com.google.firebase.firestore.DocumentSnapshot
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.Assert.assertTrue


@ExperimentalCoroutinesApi
class IngredienteRepositoryTest {

    private lateinit var ingredienteRepository: IngredienteRepository
    private lateinit var firestore: FirebaseFirestore
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {

        // Crear mocks para Firestore
        firestore = mockk()

        // Configurar el repositorio con el mock de Firestore
        ingredienteRepository = IngredienteRepository(firestore)

        // Mockear Log() para que no lance excepciones en las pruebas
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        // Configurar el entorno de pruebas para coroutines
        Dispatchers.setMain(testDispatcher)
    }


//    @Test
//    fun `buscarIngredientes devuelve ingredientes que contienen el término`() = runTest {
//        // Crear mocks
//        val mockSnapshot = mockk<QuerySnapshot>()
//        val taskSnapshot = mockk<Task<QuerySnapshot>>()
//
//        // Mock de firestore
//        every { firestore.collection("bulkIngredients").get() } returns taskSnapshot
//        coEvery { taskSnapshot.await() } returns mockSnapshot
//
//        // Ingredientes que se devolverán
//        val ingredients = listOf(
//            mapOf("name" to "Tomato", "image" to "tomato_image"),
//            mapOf("name" to "Onion", "image" to "onion_image")
//        )
//
//        // Crear documentos mockeados
//        val mockDocs = ingredients.map { ingredient ->
//            mockk<DocumentSnapshot>().apply {
//                every { getString("name") } returns ingredient["name"] as String
//                every { getString("image") } returns ingredient["image"] as String
//            }
//        }
//
//        // Mock de documentos en el snapshot
//        every { mockSnapshot.documents } returns mockDocs
//
//        // Ejecutar la función a probar
//        val result = ingredienteRepository.buscarIngredientes("Tom")
//
//        // Avanzar hasta que todas las corutinas terminen su ejecución
//        advanceUntilIdle()
//
//        // Verificar el resultado
//        assertEquals(1, result.size)
//        assertEquals("Tomato", result[0].name)
//    }



    @Test
    fun `buscarIngredientes devuelve lista vacía si ocurre un error en Firebase`() = runTest {

        val query = mockk<Query> {
            coEvery { get() } throws Exception("Firebase error")
        }

        val collectionReference = mockk<CollectionReference> {
            every { whereGreaterThanOrEqualTo(any<String>(), any()) } returns query
        }

        every { query.whereLessThanOrEqualTo(any<String>(), any()) } returns query
        every { query.limit(any()) } returns query

        val db = mockk<FirebaseFirestore> {
            every { collection("bulkIngredients") } returns collectionReference
        }

        val repository = IngredienteRepository(db)

        val ingredientes = repository.buscarIngredientes("to")

        // Verificar que devuelve lista vacía si hay error
        assertTrue(ingredientes.isEmpty())
    }


    @Test
    fun `buscarIngredientes devuelve lista vacía si Firebase lanza excepción`() = runTest(testDispatcher) {
        // Mock de la CollectionReference para lanzar una excepción al hacer get()
        val collectionReference = mockk<CollectionReference> {
            every { get() } throws Exception("Firebase error")
        }

        // Configurar el firestore mock para devolver la colección
        every { firestore.collection("bulkIngredients") } returns collectionReference

        // Ejecutar la función del repositorio
        val result = ingredienteRepository.buscarIngredientes("Tomato")

        // Comprobar que la lista está vacía
        assertTrue(result.isEmpty())
    }




}

