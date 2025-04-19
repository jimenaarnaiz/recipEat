package com.example.recipeat.ui.viewmodels

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FiltrosViewModelTest {

    private lateinit var filtrosViewModel: FiltrosViewModel

    @Before
    fun setUp() {
        // Mockear Log() para que no lance excepciones en las pruebas
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        // Inicializamos el ViewModel antes de cada prueba
        filtrosViewModel = FiltrosViewModel()
    }

    @Test
    fun `aplicarFiltros should set correct values`() {
        // Datos de prueba
        val tiempo = 30
        val ingredientes = 5
        val faltantes = 2
        val pasos = 10
        val plato = "Vegetariano"
        val dietas = setOf("Vegano", "Sin Gluten")

        // Aplicamos los filtros
        filtrosViewModel.aplicarFiltros(tiempo, ingredientes, faltantes, pasos, plato, dietas)

        // Comprobamos que los filtros se hayan establecido correctamente
        assertEquals(tiempo, filtrosViewModel.maxTiempo.value)
        assertEquals(ingredientes, filtrosViewModel.maxIngredientes.value)
        assertEquals(faltantes, filtrosViewModel.maxFaltantes.value)
        assertEquals(pasos, filtrosViewModel.maxPasos.value)
        assertEquals(plato, filtrosViewModel.tipoPlato.value)
        assertEquals(dietas, filtrosViewModel.tipoDieta.value)
    }

    @Test
    fun `restablecerFiltros should reset all values`() {
        // Aplicamos filtros iniciales
        filtrosViewModel.aplicarFiltros(30, 5, 2, 10, "Vegetariano", setOf("Vegano"))

        // Restablecemos los filtros
        filtrosViewModel.restablecerFiltros()

        // Verificamos que los filtros se hayan restablecido a sus valores por defecto
        assertNull(filtrosViewModel.maxTiempo.value)
        assertNull(filtrosViewModel.maxIngredientes.value)
        assertNull(filtrosViewModel.maxFaltantes.value)
        assertNull(filtrosViewModel.maxPasos.value)
        assertNull(filtrosViewModel.tipoPlato.value)
        assertEquals(emptySet<String>(), filtrosViewModel.tipoDieta.value)
    }

    @Test
    fun `setOrden should update orden correctly`() {
        // Establecemos un nuevo orden
        filtrosViewModel.setOrden("Alfabético")

        // Comprobamos que el orden se haya actualizado correctamente
        assertEquals("Alfabético", filtrosViewModel.orden.value)
    }

    @Test
    fun `restablecerOrden should reset orden to Default`() {
        // Establecemos un nuevo orden
        filtrosViewModel.setOrden("Alfabético")

        // Restablecemos el orden
        filtrosViewModel.restablecerOrden()

        // Verificamos que el orden se haya restablecido a "Default"
        assertEquals("Default", filtrosViewModel.orden.value)
    }
}
