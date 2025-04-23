package com.example.recipeat.ui.viewmodels

import com.example.recipeat.data.model.IngredienteSimple
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class IngredientesViewModelTest {

    private lateinit var firestoreMock: FirebaseFirestore
    private lateinit var mockIngredientesViewModel: IngredientesViewModel

    @Before
    fun setUp() {
        firestoreMock = mockk()
        mockIngredientesViewModel = IngredientesViewModel(db = firestoreMock)
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


}