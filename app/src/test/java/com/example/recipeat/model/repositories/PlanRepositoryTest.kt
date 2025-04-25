package com.example.recipeat.model.repositories

import com.example.recipeat.data.repository.PlanRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import com.google.firebase.firestore.FirebaseFirestore

class PlanRepositoryTest {

    private lateinit var planRepository: PlanRepository
    private val firestoreMock = mockk<FirebaseFirestore>(relaxed = true) // Mock de FirebaseFirestore

    @Before
    fun setUp() {
        // Inicializamos el PlanRepository con el mock de FirebaseFirestore
        planRepository = PlanRepository(firestoreMock)
    }


    @Test
    fun `obtenerSemanaActualId should return correct week id for current week`() = runTest {

        // Ejecutamos el métdo con la fecha manualmente definida
        val resultado = planRepository.obtenerSemanaActualId()

        // Verificamos que el formato devuelto sea el esperado: "día/mes"
        assertEquals("14-4", resultado)
    }

    @Test
    fun `obtenerSemanaAnteriorId should return correct week id for last week`() = runTest {
        // Establecemos la fecha manualmente para este test
        //val fechaMock = LocalDate.of(2025, 4, 14) // Suponemos que la fecha es el 14 de abril de 2025

        // Ejecutamos el métdo con la fecha manualmente definida
        val resultado = planRepository.obtenerSemanaAnteriorId()

        // La semana anterior sería el lunes 7 de abril 2025
        assertEquals("-4", resultado)
    }


}
