package com.example.recipeat.model.repositories

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import com.example.recipeat.data.repository.UserRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.File


@ExperimentalCoroutinesApi
class UserRepositoryTest {

    private lateinit var mockUserRepository: UserRepository
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var context: Context
    private lateinit var contentResolver: android.content.ContentResolver


    @Before
    fun setUp() {

        // Mock de las dependencias
        sharedPreferences = mockk(relaxed = true)
        auth = mockk()
        db = mockk()


        // Mock de FirebaseAuth.getCurrentUser() para devolver un usuario simulado
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "mockUid"
        every { auth.signOut() } just Runs

        context = mockk(relaxed = true)
        contentResolver = mockk()
        every { context.contentResolver } returns contentResolver

        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } returns false // Ajusta el valor de retorno según lo que necesites

        // Mockear Log() para que no lance excepciones en las pruebas
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0


        // Inicializamos el repositorio
        mockUserRepository = UserRepository(sharedPreferences, auth, db)
    }


    //tests del login
    @Test
    fun `login with valid credentials should return success`() = runTest {
        val validEmail = "test@example.com"
        val validPassword = "password123"

        val mockAuthResult = mockk<AuthResult>()
        val mockUser = mockk<FirebaseUser>()
        val mockDocumentSnapshot = mockk<DocumentSnapshot>()
        val mockUserRef = mockk<DocumentReference>()

        // Simular que el login con FirebaseAuth funciona y devuelve un usuario
        every { mockAuthResult.user } returns mockUser
        every { mockUser.uid } returns "mockUid"
        every { auth.signInWithEmailAndPassword(validEmail, validPassword) } returns Tasks.forResult(mockAuthResult)

        // Simular referencia al documento del usuario en Firestore
        every { db.collection("users").document("mockUid") } returns mockUserRef
        every { mockUserRef.get() } returns Tasks.forResult(mockDocumentSnapshot)

        // Simular que el documento existe en Firestore
        every { mockDocumentSnapshot.exists() } returns true

        // Act
        val result = mockUserRepository.login(validEmail, validPassword)

        // Assert
        assertEquals("success", result)
        verify { auth.signInWithEmailAndPassword(validEmail, validPassword) }
        verify { db.collection("users").document("mockUid") }
    }

    @Test
    fun `login with empty password should return error message`() = runTest {

        val email = "test@example.com"
        val emptyPassword = ""
        val exception = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "Password is empty")

        every {
            auth.signInWithEmailAndPassword(email, emptyPassword)
        } returns Tasks.forException(exception)

        // Act
        val result = mockUserRepository.login(email, emptyPassword)

        // Assert
        assertEquals("Invalid credentials. Please, check your email or password.", result)
        verify { auth.signInWithEmailAndPassword(email, emptyPassword) }
    }


    @Test
    fun `login with empty email should return error message`() = runTest {

        val email = ""
        val emptyPassword = "12837hj"

        val exception = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "Email is empty")

        every {
            auth.signInWithEmailAndPassword(email, emptyPassword)
        } returns Tasks.forException(exception)

        // Act
        val result = mockUserRepository.login(email, emptyPassword)

        // Assert
        assertEquals("Invalid credentials. Please, check your email or password.", result)
        verify { auth.signInWithEmailAndPassword(email, emptyPassword) }
    }

    @Test
    fun `login with empty email and password should return error message`() = runTest {
        val emptyEmail = ""
        val emptyPassword = ""

        val exception = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "Email and password are empty")

        every {
            auth.signInWithEmailAndPassword(emptyEmail, emptyPassword)
        } returns Tasks.forException(exception)

        // Act
        val result = mockUserRepository.login(emptyEmail, emptyPassword)

        // Assert
        assertEquals("Invalid credentials. Please, check your email or password.", result)
        verify { auth.signInWithEmailAndPassword(emptyEmail, emptyPassword) }
    }



    @Test
    fun `test login firebase auth exception`() = runTest {
        // Mock de la excepción
        val email = "test@example.com"
        val password = "password123"

        every { auth.signInWithEmailAndPassword(email, password) } throws FirebaseAuthInvalidUserException("INVALID_USER","User does not exist.")

        // Ejecutamos el métdo
        val result = mockUserRepository.login(email, password)

        // Verificamos que el mensaje de error sea el esperado
        assertEquals("User does not exist. Please, check your email.", result)
    }


    @Test
    fun `test login invalid credentials exception`() = runTest {
        // Mock de la excepción FirebaseAuthInvalidCredentialsException
        val email = "test@example.com"
        val password = "password123"

        every { auth.signInWithEmailAndPassword(email, password) } throws FirebaseAuthInvalidCredentialsException("INVALID_CREDENTIALS", "Invalid credentials")

        // Ejecutamos el métdo
        val result = mockUserRepository.login(email, password)

        // Verificamos que el mensaje de error sea el esperado
        assertEquals("Invalid credentials. Please, check your email or password.", result)
    }


    @Test
    fun `test login network exception`() = runTest {
        // Mock de la excepción FirebaseNetworkException
        val email = "test@example.com"
        val password = "password123"

        every { auth.signInWithEmailAndPassword(email, password) } throws FirebaseNetworkException("NETWORK_ERROR")

        val result = mockUserRepository.login(email, password)

        // Verificamos que el mensaje de error sea el esperado
        assertEquals("Network error. Please, check your internet connection.", result)
    }


    @Test
    fun `test login general exception`() = runTest {
        // Simulamos una excepción desconocida
        val email = "test@example.com"
        val password = "password123"

        every { auth.signInWithEmailAndPassword(email, password) } throws Exception("Unknown error")

        // Ejecutamos el métdo
        val result = mockUserRepository.login(email, password)

        // Verificamos que el mensaje de error sea el esperado
        assertEquals("Login error: Unknown error", result)
    }


    /**
     * Este test asegura que el UserRepository funciona correctamente al cerrar sesión,
     * limpiando tanto la sesión en SharedPreferences como el valor de uid.
     */
    @Test
    fun `test logOut debería limpiar el flag de sesión y reiniciar el uid cuando el usuario esté logueado`() = runTest {

        // Mock del SharedPreferences.Editor
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { sharedPreferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.apply() } just Runs

        // Se ejecuta el métdo logOut() del repositorio
        mockUserRepository.logOut()

        // Verificamos que se llame a putBoolean con el valor esperado
        verify {
            editor.putBoolean("sesionIniciada_mockUid", false)
            editor.apply()
            auth.signOut()
        }

        // Comprobamos que el uid esté limpio (null)
        assertNull(mockUserRepository.uid.value)
    }



    //register
    @Test
    fun `test register user collision exception`() = runTest {
        // Datos de entrada para el registro
        val username = "testuser"
        val email = "test@example.com"
        val password = "password123"

        // Simulamos la excepción de colisión de usuario (email ya registrado)
        every { auth.createUserWithEmailAndPassword(email, password) } throws FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "This email is already in use by another account.")

        // Ejecutamos el métdo register
        val result = mockUserRepository.register(username, email, password)

        // Verificamos que el resultado sea el mensaje esperado
        assertEquals("This email is already in use by another account.", result)
    }

    @Test
    fun `test register network exception`() = runTest {
        // Datos de entrada para el registro
        val username = "testuser"
        val email = "test@example.com"
        val password = "password123"

        // Simulamos la excepción de error de red
        every { auth.createUserWithEmailAndPassword(email, password) } throws FirebaseNetworkException("Network error")

        // Ejecutamos el métdo register
        val result = mockUserRepository.register(username, email, password)

        // Verificamos que el resultado sea el mensaje esperado
        assertEquals("Network error. Please check your internet connection.", result)
    }

    @Test
    fun `test register general exception`() = runTest {
        // Datos de entrada para el registro
        val username = "testuser"
        val email = "test@example.com"
        val password = "password123"

        // Simulamos una excepción general
        every { auth.createUserWithEmailAndPassword(email, password) } throws Exception("Some other error")

        // Ejecutamos el métdo register
        val result = mockUserRepository.register(username, email, password)

        // Verificamos que el resultado sea el mensaje esperado
        assertEquals("Registration error: Some other error", result)
    }


    @Test
    fun `test saveImageLocally should handle exception gracefully`() {
        //datos de entrada
        val imageUri = mockk<Uri>()
        val recetaId = "receta123"

        every { context.filesDir } returns File("/mock/directory")
        every { contentResolver.openInputStream(imageUri) } throws Exception("File error")

        // Act
        mockUserRepository.saveImageLocally(context, imageUri, recetaId)

        // Assert
        // Verifica que no se lanzaron excepciones y que el log de error fue llamado.
        verify { Log.e(any(), "Error al guardar la imagen: File error") }
    }



    @Test
    fun `isSessionActive returns false when user is null`() = runTest {
        every { auth.currentUser } returns null

        val result = mockUserRepository.isSessionActive()
        assertFalse(result)
    }

    @Test
    fun `isSessionActive returns false when session flag is false`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "mockUid"
        every { sharedPreferences.getBoolean("sessionIniciadaKeymockUid", false) } returns false

        val result = mockUserRepository.isSessionActive()
        assertFalse(result)
    }


    @Test
    fun `sendPasswordResetEmail handles generic exception`() = runTest {
        coEvery { auth.sendPasswordResetEmail(any()) } throws Exception("Unexpected")

        val result = mockUserRepository.sendPasswordResetEmail("email@test.com")
        assertTrue(result.contains("An error occurred"))
    }





}
