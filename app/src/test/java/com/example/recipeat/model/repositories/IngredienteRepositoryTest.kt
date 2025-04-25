package com.example.recipeat.model.repositories

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

        // Configurar el entorno de pruebas para coroutines
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun `buscarIngredientes should return a list of IngredienteSimple`() = runTest {
        val mockSnapshot = mockk<QuerySnapshot>()
        val taskSnapshot = mockk<Task<QuerySnapshot>>()

        coEvery { taskSnapshot.await() } returns mockSnapshot
        every { firestore.collection("bulkIngredients").get() } returns taskSnapshot

        val ingredients = listOf(
            mapOf("name" to "Tomato", "image" to "tomato_image"),
            mapOf("name" to "Onion", "image" to "onion_image")
        )

        val mockDocs = ingredients.map { ingredient ->
            val doc = mockk<DocumentSnapshot>()
            every { doc.getString("name") } returns ingredient["name"] as String
            every { doc.getString("image") } returns ingredient["image"] as String
            doc
        }

        every { mockSnapshot.documents } returns mockDocs

        val result = ingredienteRepository.buscarIngredientes("Tom")

        assertEquals(1, result.size)
        assertEquals("Tomato", result[0].name)
    }
}

