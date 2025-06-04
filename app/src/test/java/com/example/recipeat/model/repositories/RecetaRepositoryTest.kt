package com.example.recipeat.model.repositories

import android.util.Log
import com.example.recipeat.data.model.Ingrediente
import com.example.recipeat.data.model.Receta
import com.example.recipeat.data.model.RecetaSimple
import com.example.recipeat.data.repository.RecetaRepository
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecetaRepositoryTest {

    private lateinit var db: FirebaseFirestore
    private lateinit var recetaRepository: RecetaRepository
    private lateinit var documentSnapshot: DocumentSnapshot

    private val userDoc: DocumentReference = mockk()
    private val recetaDoc: DocumentReference = mockk()
    private val recetasCollection: CollectionReference = mockk()
    private val userCollection: CollectionReference = mockk()

    private val uid = "testUid"
    private val recetaId = "receta_test_001"

    val receta = Receta(
        id = recetaId,
        title = "Tortilla de patatas",
        image = "https://ejemplo.com/tortilla.jpg",
        servings = 4,
        ingredients = listOf(
            Ingrediente(
                name = "Patatas",
                amount = 500.0,
                unit = "g",
                image = "https://ejemplo.com/patatas.png",
                aisle = "Verduras"
            ),
            Ingrediente(
                name = "Huevos",
                amount = 4.0,
                unit = "unidad",
                image = "https://ejemplo.com/huevo.png",
                aisle = "Huevos"
            ),
            Ingrediente(
                name = "Aceite de oliva",
                amount = 50.0,
                unit = "ml",
                image = "https://ejemplo.com/aceite.png",
                aisle = "Aceites"
            )
        ),
        steps = listOf(
            "Pelar y cortar las patatas en rodajas finas.",
            "Freír las patatas en aceite de oliva hasta que estén blandas.",
            "Batir los huevos y mezclar con las patatas.",
            "Cocinar la mezcla en una sartén hasta que cuaje por ambos lados."
        ),
        time = 30,
        userId =  uid,
        dishTypes = listOf("Main course", "Spanish"),
        glutenFree = true,
        vegan = false,
        vegetarian = true,
        date = System.currentTimeMillis(),
        unusedIngredients = emptyList(),
        missingIngredientCount = 0,
        unusedIngredientCount = 0
    )


    @Before
    fun setUp() {
        db = mockk()
        documentSnapshot = mockk()
        recetaRepository = RecetaRepository(db)

        every { db.collection("my_recipes") } returns userCollection
        every { userCollection.document(uid) } returns userDoc
        every { userDoc.collection("recipes") } returns recetasCollection
        every { recetasCollection.document(recetaId) } returns recetaDoc

        // Mockear Log() para que no lance excepciones en las pruebas
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun limpiar() {
        clearAllMocks()
    }


    @Test
    fun `obtenerRecetaDesdeSnapshot mapea correctamente el snapshot a una receta`() {
        every { documentSnapshot.getString("id") } returns recetaId
        every { documentSnapshot.getString("title") } returns "Tortilla"
        every { documentSnapshot.getString("image") } returns "img.jpg"
        every { documentSnapshot.get("servings") } returns 2
        every { documentSnapshot.get("ingredients") } returns listOf(
            mapOf("name" to "Huevo", "amount" to 2.0, "unit" to "unidad", "image" to "egg.jpg", "aisle" to "Huevos")
        )
        every { documentSnapshot.get("steps") } returns listOf("Batir", "Freír")
        every { documentSnapshot.get("time") } returns 15
        every { documentSnapshot.get("dishTypes") } returns listOf("Desayuno")
        every { documentSnapshot.getString("userId") } returns uid
        every { documentSnapshot.getBoolean("glutenFree") } returns true
        every { documentSnapshot.getBoolean("vegan") } returns false
        every { documentSnapshot.getBoolean("vegetarian") } returns true
        every { documentSnapshot.get("date") } returns 123456789L

        val receta = recetaRepository.obtenerRecetaDesdeSnapshot(documentSnapshot)

        assertEquals(recetaId, receta.id)
        assertEquals("Tortilla", receta.title)
        assertEquals("img.jpg", receta.image)
        assertEquals(2, receta.servings)
        assertEquals(1, receta.ingredients.size)
        assertEquals("Huevo", receta.ingredients[0].name)
        assertEquals(2.0, receta.ingredients[0].amount, 0.001)
        assertEquals("unidad", receta.ingredients[0].unit)
        assertEquals("egg.jpg", receta.ingredients[0].image)
        assertEquals("Huevos", receta.ingredients[0].aisle)
        assertEquals(listOf("Batir", "Freír"), receta.steps)
        assertEquals(15, receta.time)
        assertEquals(listOf("Desayuno"), receta.dishTypes)
        assertEquals(uid, receta.userId)
        assertTrue(receta.glutenFree)
        assertTrue(!receta.vegan)
        assertTrue(receta.vegetarian)
        assertEquals(123456789L, receta.date)
    }

    @Test
    fun `addMyRecipe guarda correctamente la receta en Firestore y llama a onComplete con éxito`() {
        val uid = "user123"
        val documentRef: DocumentReference = mockk()
        val collectionRef: CollectionReference = mockk()
        val myRecipesRef: CollectionReference = mockk()

        every { db.collection("my_recipes") } returns myRecipesRef
        every { myRecipesRef.document(uid) } returns documentRef
        every { documentRef.collection("recipes") } returns collectionRef
        every { collectionRef.document(receta.id) } returns documentRef

        // Mock getPath() para evitar el error
        every { documentRef.path } returns "my_recipes/$uid/recipes/${receta.id}"

        every { documentRef.set(any()) } returns mockk {
            every { addOnSuccessListener(any()) } answers {
                val listener = firstArg<OnSuccessListener<Void>>()
                listener.onSuccess(null)
                this@mockk
            }
            every { addOnFailureListener(any()) } answers {
                this@mockk
            }
        }

        var successCalled = false
        recetaRepository.addMyRecipe(uid, receta) { success, error ->
            successCalled = success
            assertNull(error)
        }

        assertTrue(successCalled)
    }


    @Test
    fun `eliminarReceta elimina la receta exitosamente`() = runTest {
        val mockTask: Task<Void> = mockk(relaxed = true)

        every { mockTask.isComplete } returns true
        every { mockTask.isSuccessful } returns true
        every { mockTask.isCanceled } returns false
        every { mockTask.exception } returns null

        every { recetaDoc.delete() } returns mockTask

        recetaRepository.eliminarReceta(uid, recetaId)

        verify(exactly = 1) { recetaDoc.delete() }
    }


    @Test
    fun `eliminarReceta lanza excepción si falla la eliminación`() = runTest {
        coEvery { recetaDoc.delete() } throws RuntimeException("Error de eliminación")

        try {
            recetaRepository.eliminarReceta(uid, recetaId)
            assertTrue("Se esperaba una excepción pero no se lanzó", false)
        } catch (e: RuntimeException) {
            assertEquals("Error de eliminación", e.message)
        }

        coVerify(exactly = 1) { recetaDoc.delete() }
    }

    @Test
    fun `obtenerRecetasFavoritas devuelve lista vacía en caso de excepción`() = runTest {
        val favoritosCollection: CollectionReference = mockk()
        val favoritosDoc: DocumentReference = mockk()
        val favoritosSubCollection: CollectionReference = mockk()
        val query: Query = mockk()

        every { db.collection("favs_hist") } returns favoritosCollection
        every { favoritosCollection.document(uid) } returns favoritosDoc
        every { favoritosDoc.collection("favoritos") } returns favoritosSubCollection
        every { favoritosSubCollection.orderBy("date", Query.Direction.DESCENDING) } returns query

        coEvery { query.get().await() } throws Exception("Error de prueba")

        val favoritas = recetaRepository.obtenerRecetasFavoritas(uid)

        assertTrue(favoritas.isEmpty())
    }







}
