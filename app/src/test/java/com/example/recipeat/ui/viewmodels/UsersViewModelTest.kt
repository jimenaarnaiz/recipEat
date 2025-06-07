package com.example.recipeat.ui.viewmodels

import android.app.Application
import android.util.Log
import com.example.recipeat.data.model.User
import com.example.recipeat.data.repository.UserRepository
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UsersViewModelTest {

    @RelaxedMockK
    private lateinit var userRepository: UserRepository

    private lateinit var usersViewModel: UsersViewModel

    // Variables reutilizables para los tests
    private var emailValid = "prueba@gmail.com"
    private var passwordValid = "123456"
    private var emailInvalid = "yufjg@gmail.com"
    private var passwordInvalid = "57777676"
    private var emailInvalidFormat = "prueba@prueba"
    private var username = "prueba"

    private lateinit var application: Application

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        // Mockear Log() para que no lance excepciones en las pruebas
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0

        // Establecer Dispatchers.Main como un dispatcher controlado en las pruebas, ya q uso viewModelScope en corrutinas
        Dispatchers.setMain(Dispatchers.Unconfined)

        val context = mockk<Application>(relaxed = true)
        usersViewModel = UsersViewModel(context, userRepository)
        application = mockk(relaxed = true)
    }

    // login
    @Test
    fun `login correcto devuelve mensaje esperado`() = runTest {
        val expectedMessage = "Login exitoso"

        coEvery { userRepository.login(emailValid, passwordValid) } returns expectedMessage

        var actualMessage = ""

        usersViewModel.login(emailValid, passwordValid) {
            actualMessage = it
        }

        advanceUntilIdle()

        assertEquals(expectedMessage, actualMessage)
    }

    @Test
    fun `login con contraseña errónea y devuelve mensaje de error`() = runTest {
        val expectedMessage = "Invalid credentials. Please, check your email or password."

        coEvery { userRepository.login(emailValid, passwordInvalid) } returns expectedMessage

        var actualMessage = ""

        usersViewModel.login(emailValid, passwordInvalid) {
            actualMessage = it
        }

        advanceUntilIdle()

        println("Mensaje recibido: $actualMessage")

        assertTrue(actualMessage.contains("Invalid credentials"))
    }

    @Test
    fun `login con correo sin autenticar y devuelve mensaje de error`() = runTest {
        val expectedMessage = "User does not exist. Please, check your email."

        coEvery { userRepository.login(emailInvalid, passwordInvalid) } returns expectedMessage

        var actualMessage = ""

        usersViewModel.login(emailInvalid, passwordInvalid) {
            actualMessage = it
        }

        advanceUntilIdle()

        println("Mensaje recibido: $actualMessage")

        assertTrue(actualMessage.equals(expectedMessage))
    }

    @Test
    fun `login con correo sin formato correcto y devuelve mensaje de error especifico`() = runTest {
        val expectedMessage = "Login error: "

        coEvery { userRepository.login(emailInvalidFormat, passwordValid) } returns expectedMessage

        var actualMessage = ""

        usersViewModel.login(emailInvalidFormat, passwordValid) {
            actualMessage = it
        }

        advanceUntilIdle()

        println("Mensaje recibido: $actualMessage")

        assertTrue(actualMessage.contains(expectedMessage))
    }

    //registro
    @Test
    fun `registro con correo ya usado y devuelve mensaje de error especifico`() = runTest {
        val expectedMessage = "Registration error: "

        coEvery { userRepository.register(username, emailValid, passwordValid) } returns expectedMessage

        var actualMessage = ""

        usersViewModel.register(username, emailValid, passwordValid) {
            actualMessage = it
        }

        advanceUntilIdle()

        println("Mensaje recibido: $actualMessage")

        assertTrue(actualMessage.contains(expectedMessage))
    }

    @Test
    fun `registro con correo mal formateado y devuelve mensaje de error especifico`() = runTest {
        username = "prueba"
        val expectedMessage = "Registration error: "

        coEvery { userRepository.register(username, emailInvalidFormat, passwordValid) } returns expectedMessage

        var actualMessage = ""

        usersViewModel.register(username, emailInvalidFormat, passwordValid) {
            actualMessage = it
        }

        advanceUntilIdle()

        println("Mensaje recibido: $actualMessage")

        assertTrue(actualMessage.contains(expectedMessage))
    }

    @Test
    fun `registro con usuario valido y contraseña devuelve mensaje de success`() = runTest {
        val expectedMessage = "success"
        var actualMessage = ""

        coEvery { userRepository.register("", emailValid, passwordValid) } returns expectedMessage

        usersViewModel.register("", emailValid, passwordValid) {
            actualMessage = it
        }

        advanceUntilIdle()

        println("Mensaje recibido: $actualMessage")

        assertTrue(actualMessage == expectedMessage)
    }


    @Test
    fun `logOut llama al método del repositorio y no falla`() = runTest {
        coEvery { userRepository.logOut() } just Runs

        usersViewModel.logOut()

        advanceUntilIdle()
        coVerify { userRepository.logOut() }
    }

    @Test
    fun `obtenerUsername devuelve el username esperado`() = runTest {
        val expectedUsername = "testUser"
        coEvery { userRepository.obtenerUsername() } returns expectedUsername

        var result: String? = null
        usersViewModel.obtenerUsername {
            result = it
        }

        advanceUntilIdle()
        assertEquals(expectedUsername, result)
    }

    @Test
    fun `obtenerUsername  maneja excepción y devuelve null`() = runTest {
        coEvery { userRepository.obtenerUsername() } throws Exception("Failed")

        var result: String? = "notNull"
        usersViewModel.obtenerUsername {
            result = it
        }

        advanceUntilIdle()
        assertNull(result)
    }

    @Test
    fun `sendPasswordResetEmail llama al método del repositorio`() = runTest {
        coEvery { userRepository.sendPasswordResetEmail(emailValid) } returns "Email sent"

        var result: String? = null
        usersViewModel.sendPasswordResetEmail(emailValid) {
            result = it
        }

        advanceUntilIdle()
        assertEquals("Email sent", result)
    }

    @Test
    fun `actualizarUserProfile devuelve éxito`() = runTest {
        coEvery { userRepository.actualizarUserProfile("newName", "imgUrl") } returns true

        var result = false
        usersViewModel.actualizarUserProfile("newName", "imgUrl") {
            result = it
        }

        advanceUntilIdle()
        assertTrue(result)
    }


    @Test
    fun `obtenerUsuarioCompletoPorCampos devuelve valores esperados`() = runTest {
        // Simular retorno de un Triple con los datos
        coEvery { userRepository.obtenerUsuarioCompletoPorCampos("uid123") } returns Triple("name", "img", "email")

        var result: Triple<String, String?, String?>? = null
        // Ejecutar el métdo y capturar resultado
        usersViewModel.obtenerUsuarioCompletoPorCampos("uid123") { name, img, email ->
            result = Triple(name, img, email)
        }

        advanceUntilIdle()
        // Verificar que el resultado coincide con lo esperado
        assertEquals(Triple("name", "img", "email"), result)
    }

    @Test
    fun `obtenerUsuarioCompletoPorCampos maneja excepción y devuelve valores vacíos`() = runTest {
        // Simular excepción al llamar al repositorio
        coEvery { userRepository.obtenerUsuarioCompletoPorCampos("uid123") } throws Exception("Fail")

        var result: Triple<String, String?, String?>? = Triple("default", "img", "email")
        // Llamar al métdo y capturar resultado
        usersViewModel.obtenerUsuarioCompletoPorCampos("uid123") { name, img, email ->
            result = Triple(name, img, email)
        }

        advanceUntilIdle()
        // Comprobar que en caso de fallo devuelve valores vacíos o null
        assertEquals(Triple("", null, null), result)
    }

    @Test
    fun `obtenerUsuarioCompleto devuelve el usuario esperado`() = runTest {
        val user = User("name", "email")
        // Simular retorno del usuario
        coEvery { userRepository.obtenerUsuarioCompleto("uid") } returns user

        var result: User? = null
        // Llamar al métdo y guardar resultado
        usersViewModel.obtenerUsuarioCompleto("uid") {
            result = it
        }

        advanceUntilIdle()
        // Verificar que el usuario devuelto es el esperado
        assertEquals(user, result)
    }


    @Test
    fun `getUidValue devuelve el uid actual`() {
        every { userRepository.getUidValue() } returns "1234"
        val result = usersViewModel.getUidValue()
        assertEquals("1234", result)
    }

    @Test
    fun `uid se actualiza tras init si hay sesión activa`() = runTest {
        coEvery { userRepository.isSessionActive() } returns true
        every { userRepository.getUidValue() } returns "abc123"

        val vm = UsersViewModel(application, userRepository)

        assertEquals("abc123", vm.uid.value)
    }


    @Test
    fun `obtenerUsuarioCompleto con excepción devuelve null`() = runTest {
        var result: User? = User("d", "d@gmail.com")

        coEvery { userRepository.obtenerUsuarioCompleto("uid") } throws Exception("Failure")

        usersViewModel.obtenerUsuarioCompleto("uid") {
            result = it
        }

        advanceUntilIdle()
        assertNull(result)
    }

    @Test
    fun `login con excepción devuelve mensaje de error adecuado`() = runTest {
        val email = emailValid
        val password = passwordValid
        val exception = Exception("Unknown error")
        var result: String? = null

        coEvery { userRepository.login(email, password) } throws exception

        usersViewModel.login(email, password) {
            result = it
        }

        advanceUntilIdle()
        assertEquals("Unknown error during registration: Unknown error", result)
    }

    @Test
    fun `register con excepción devuelve mensaje de error adecuado`() = runTest {
        val username = "user"
        val email = emailValid
        val password = passwordValid
        val exception = Exception("Something failed")
        var result: String? = null

        coEvery { userRepository.register(username, email, password) } throws exception

        usersViewModel.register(username, email, password) {
            result = it
        }

        advanceUntilIdle()
        assertEquals("Unknown error during registration: Something failed", result)
    }
}
