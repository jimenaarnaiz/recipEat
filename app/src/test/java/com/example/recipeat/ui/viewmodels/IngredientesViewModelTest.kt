package com.example.recipeat.ui.viewmodels

import android.util.Log
import com.example.recipeat.data.model.IngredienteSimple
import com.example.recipeat.data.repository.IngredienteRepository
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class IngredientesViewModelTest {

    private lateinit var firestoreMock: FirebaseFirestore
    private lateinit var mockIngredientesViewModel: IngredientesViewModel
    private lateinit var mockIngredientesRepository: IngredienteRepository

    private val testDispatcher = StandardTestDispatcher()


    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        firestoreMock = mockk()
        mockIngredientesRepository = mockk()
        mockIngredientesViewModel = IngredientesViewModel(mockIngredientesRepository)
    }

    @Test
    fun `addIngredient añade un ingrediente a la lista`() = runTest {
        val ingrediente = IngredienteSimple("Tomate", "tomato.png")

        mockIngredientesViewModel.addIngredient(ingrediente)

        assertTrue(mockIngredientesViewModel.ingredientes.value.contains(ingrediente))
    }

    @Test
    fun `removeIngredient elimina un ingrediente de la lista`() = runTest {
        val ingrediente = IngredienteSimple("Cebolla", "onion.png")

        mockIngredientesViewModel.addIngredient(ingrediente)
        mockIngredientesViewModel.removeIngredient(ingrediente)

        assertFalse(mockIngredientesViewModel.ingredientes.value.contains(ingrediente))
    }

    @Test
    fun `clearIngredientes vacía la lista de ingredientes`() = runTest {
        mockIngredientesViewModel.addIngredient(IngredienteSimple("Ajo", "garlic.png"))

        mockIngredientesViewModel.clearIngredientes()

        assertTrue(mockIngredientesViewModel.ingredientes.value.isEmpty())
    }

    @Test
    fun `clearIngredientesSugeridos vacía la lista de sugerencias`() = runTest {
        mockIngredientesViewModel.ingredientesSugeridos.value = listOf(
            IngredienteSimple("Zanahoria", "carrot.png")
        )

        mockIngredientesViewModel.clearIngredientesSugeridos()

        assertTrue(mockIngredientesViewModel.ingredientesSugeridos.value.isEmpty())
    }



    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `buscarIngredientes should update _ingredientesSugeridos with the result`() = runTest {
        // Datos simulados de ingredientes
        val ingredientesSimulados = listOf(
            IngredienteSimple("tomato", "tomato_image"),
            IngredienteSimple("toast", "toast_image")
        )

        // Simular el comportamiento del repositorio
        coEvery { mockIngredientesRepository.buscarIngredientes("to") } returns ingredientesSimulados

        // Llamar al métdo
        mockIngredientesViewModel.buscarIngredientes("to")

        advanceUntilIdle() // Esto avanza todas las tareas pendientes

        // Verificar que el estado de ingredientesSugeridos se haya actualizado correctamente
        assertEquals(ingredientesSimulados, mockIngredientesViewModel.ingredientesSugeridos.value)
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `buscarIngredientes should handle exception and set empty list`() = runTest {
        // Mockear Log para evitar errores por llamadas estáticas
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0

        // Simular excepción en el repositorio
        coEvery { mockIngredientesRepository.buscarIngredientes("fallo") } throws Exception("Error de búsqueda")

        // Ejecutar el método
        mockIngredientesViewModel.buscarIngredientes("fallo")

        advanceUntilIdle() // Esperar a que se complete la corrutina

        // Verificar que se estableció una lista vacía en caso de error
        assertTrue(mockIngredientesViewModel.ingredientesSugeridos.value.isEmpty())
    }


    @Test
    fun `loadIngredientsFromFirebase should handle exception and not crash`() = runTest {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0

        // Lanzar excepción
        coEvery { mockIngredientesRepository.loadIngredientsFromFirebase() } throws Exception("Error de red")

        mockIngredientesViewModel.loadIngredientsFromFirebase()

        // Solo confirmar que no explota (no hay aserción clara posible si no se modifica el catch)
        // Podrías comprobar el valor actual si decides ponerlo como lista vacía en el ViewModel
        assertNotNull(mockIngredientesViewModel.ingredientesValidos.value)
    }


}