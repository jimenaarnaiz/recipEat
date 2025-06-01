package com.example.recipeat.model.repositories

import android.util.Log
import com.example.recipeat.data.repository.IngredienteRepository
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
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

