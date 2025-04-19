package com.example.recipeat.data.repository

import android.util.Log
import com.example.recipeat.data.dao.FavoritoDao
import com.example.recipeat.data.dao.RecetaRoomDao
import com.example.recipeat.data.model.Favorito
import com.example.recipeat.data.model.Receta
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.sql.SQLException
import java.sql.Timestamp

@OptIn(ExperimentalCoroutinesApi::class)
class RecetaRoomRepositoryTest {

    private lateinit var recetaRoomDao: RecetaRoomDao
    private lateinit var favoritoDao: FavoritoDao
    private lateinit var repository: RecetaRoomRepository

    private val userId1 = "user1"
    private val userId2 = "user2"

    private val receta1 = Receta(
        id = "r1",
        title = "Receta 1",
        userId = userId1,
        ingredients = emptyList(),
        steps = emptyList(),
        dishTypes = emptyList(),
        time = 15,
        servings = 1,
        image = "",
        glutenFree = false,
        vegan = false,
        vegetarian = false,
        date = System.currentTimeMillis(),
        unusedIngredients = emptyList(),
        missingIngredientCount = 0,
        unusedIngredientCount = 0,
        esFavorita = false
    )
    private val receta2 = Receta(
        id = "r2",
        title = "Receta 2",
        userId = userId2,
        ingredients = emptyList(),
        steps = emptyList(),
        dishTypes = emptyList(),
        time = 20,
        servings = 3,
        image = "",
        glutenFree = false,
        vegan = false,
        vegetarian = false,
        date = System.currentTimeMillis(),
        unusedIngredients = emptyList(),
        missingIngredientCount = 0,
        unusedIngredientCount = 0,
        esFavorita = false
    )

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        recetaRoomDao = mockk()
        favoritoDao = mockk()
        repository = RecetaRoomRepository(recetaRoomDao, favoritoDao)

        // Mockear Log() para que no lance excepciones en las pruebas
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `insertReceta debe llamar al dao correctamente`() = runTest {

        coEvery { recetaRoomDao.insertReceta(receta1) } just Runs

        repository.insertReceta(receta1)

        coVerify { recetaRoomDao.insertReceta(receta1) }
    }

    @Test
    fun `getRecetaById debe devolver la receta correcta`() = runTest {
        coEvery { recetaRoomDao.getRecetaById("r1") } returns receta1

        val result = repository.getRecetaById("r1")

        assertEquals(receta1, result)
        coVerify { recetaRoomDao.getRecetaById("r1") }
    }

    @Test
    fun `getRecetasFavoritas debe devolver las recetas asociadas a favoritos`() = runTest {
        val userId = "user1"
        val favoritos = listOf(
            Favorito(userId = userId, recetaId = "r1", date = Timestamp(System.currentTimeMillis())),
            Favorito(userId = userId, recetaId = "r2", date = Timestamp(System.currentTimeMillis()))
        )

        coEvery { favoritoDao.obtenerFavoritosPorUsuario(userId) } returns favoritos
        coEvery { recetaRoomDao.getRecetaById("r1") } returns receta1
        coEvery { recetaRoomDao.getRecetaById("r2") } returns receta2

        val result = repository.getRecetasFavoritas(userId)

        assertEquals(listOf(receta1, receta2), result)
        coVerify { favoritoDao.obtenerFavoritosPorUsuario(userId) }
        coVerify { recetaRoomDao.getRecetaById("r1") }
        coVerify { recetaRoomDao.getRecetaById("r2") }
    }

    @Test
    fun `agregarFavorito debe crear el favorito correctamente si no existe previamente`() = runTest {
        val recetaId = "r1"

        // Simula que el favorito NO existe
        coEvery { favoritoDao.esFavorita(userId1, recetaId) } returns null

        // Simula la inserción sin hacer nada
        coEvery { favoritoDao.agregarFavorito(any()) } just Runs

        // Ejecuta el métdo
        repository.agregarFavorito(userId1, recetaId)

        // Verifica que se haya llamado a agregarFavorito con los datos esperados
        coVerify {
            favoritoDao.agregarFavorito(withArg {
                assertEquals(userId1, it.userId)
                assertEquals(recetaId, it.recetaId)
            })
        }

        // También verifica que se consultó primero si existía
        coVerify { favoritoDao.esFavorita(userId1, recetaId) }
    }


    @Test
    fun `eliminarFavorito debe eliminar el favorito del dao`() = runTest {
        val recetaId = "r1"

        coEvery { favoritoDao.eliminarFavorito(userId1, recetaId) } just Runs

        repository.eliminarFavorito(userId1, recetaId)

        coVerify { favoritoDao.eliminarFavorito(userId1, recetaId) }
    }

    @Test
    fun `esFavorita debe devolver true si existe el favorito`() = runTest {
        coEvery { favoritoDao.esFavorita(userId1, "r1") } returns Favorito(userId1, "r1", Timestamp(System.currentTimeMillis()))

        val result = repository.esFavorita(userId1, "r1")

        assertTrue(result)
    }

    @Test
    fun `esFavorita debe devolver false si no existe el favorito`() = runTest {
        coEvery { favoritoDao.esFavorita(userId1, "r1") } returns null

        val result = repository.esFavorita(userId1, "r1")

        assertFalse(result)
    }

    @Test
    fun `getRecetaById debe lanzar excepcion si el id no existe`() = runTest {
        // Simulamos que el DAO lanza una excepción al buscar un ID inexistente
        coEvery { recetaRoomDao.getRecetaById("inexistente") } throws Exception("Receta no encontrada")

        // Comprobamos que el repository lanza la excepción correctamente
        try {
            repository.getRecetaById("inexistente")
            // Si no se lanza la excepción, fallará el test
            assertTrue("Se esperaba una excepción", false)
        } catch (ex: Exception) {
            assertEquals("Receta no encontrada", ex.message)
        }
        coVerify { recetaRoomDao.getRecetaById("inexistente") }
    }


    @Test
    fun `getRecetasUser devuelve lista vacia si el usuario no tiene recetas`() = runTest {
        // Simulamos que el DAO devuelve lista vacía para un userId sin recetas
        coEvery { recetaRoomDao.getRecetasUser(userId2) } returns emptyList()

        val resultado = repository.getRecetasUser(userId2)

        assertTrue(resultado.isEmpty())
        coVerify { recetaRoomDao.getRecetasUser(userId2) }
    }


    @Test
    fun `agregarFavorito no debe agregar un favorito duplicado`() = runTest {
        val recetaId = "r1"

        // Simulamos que el favorito ya existe en la base de datos
        coEvery { favoritoDao.esFavorita(userId1, recetaId) } returns Favorito(userId1, recetaId, Timestamp(System.currentTimeMillis()))

        // Simulamos que la inserción de un favorito duplicado lanza una excepción (por ejemplo, SQLException)
        coEvery { favoritoDao.agregarFavorito(any()) } throws SQLException("Favorito ya existe")

        // Ejecutamos el métdo del repositorio que debería manejar la excepción
        repository.agregarFavorito(userId1, recetaId)

        // Verificamos que no se haya intentado agregar el favorito duplicado
        coVerify(exactly = 0) { favoritoDao.agregarFavorito(any()) }
    }



    //Verifica que cuando se actualiza una receta, el repositorio pasa la receta actualizada al DAO.
    @Test
    fun `updateReceta debe actualizar la receta correctamente`() = runTest {
        val updatedReceta = receta1.copy(title = "Receta Actualizada")

        coEvery { recetaRoomDao.updateReceta(updatedReceta) } just Runs

        repository.updateReceta(updatedReceta)

        coVerify { recetaRoomDao.updateReceta(updatedReceta) }
    }




}
