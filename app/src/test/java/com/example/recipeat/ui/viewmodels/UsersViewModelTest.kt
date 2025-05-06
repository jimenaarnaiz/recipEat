package com.example.recipeat.ui.viewmodels

import android.app.Application
import android.util.Log
import com.example.recipeat.data.repository.UserRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkStatic
import junit.framework.TestCase.assertEquals
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


    @Before
    fun setup() {
        MockKAnnotations.init(this)

        // Mockear Log() para que no lance excepciones en las pruebas
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0

        // Establecer Dispatchers.Main como un dispatcher controlado en las pruebas, ya q uso viewModelScope en corrutinas
        Dispatchers.setMain(Dispatchers.Unconfined)

        val context = mockk<Application>(relaxed = true)
        usersViewModel = UsersViewModel(context, userRepository)
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




}
