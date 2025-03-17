package com.example.recipeat.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.recipeat.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UsersViewModel: ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener() { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d("Register", "signInWithEmail:success")
                    onResult(true)  // Retorna true si el login es exitoso
                } else {
                    // Sign in fails
                    Log.w("Login", "signInWithEmail:failure", task.exception)
                    //Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    onResult(false)  // Retorna false si el login falla
                }
            }
    }

    fun register(username: String, email: String, password: String, onResult: (Boolean) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    // Crear un objeto Usuario con datos del usuario
                    val userData = User(
                        //id = user?.uid ?: "", // El UID de Firebase
                        username = username,
                        image = "", // URL de la imagen (si la tienes)
                        email = email
                    )

                    // Almacenar la información del usuario en Firestore
                    user?.let {
                        db.collection("users")
                            .document(it.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                // Mostrar mensaje de éxito en el registro
                                //Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                onResult(true) // Registro exitoso
                            }
                            .addOnFailureListener { e ->
                                // Si falla la escritura en Firestore, manejar el error
                                //Toast.makeText(context, "Error al guardar los datos en Firestore", Toast.LENGTH_SHORT).show()
                                Log.w("Register", "Error writing document", e)
                                onResult(false) // Error al almacenar en la base de datos
                            }
                    }
                } else {
                    // Si el registro en Firebase Authentication falla
                    //Toast.makeText(context, "Error en el registro. Intenta nuevamente.", Toast.LENGTH_SHORT).show()
                    Log.w("Register", "Error en el registro", task.exception)
                    onResult(false)  // Retorna false si el registro falla
                }
            }
    }


    // Función para obtener el username de un usuario
    fun obtenerUsername(uid: String, onResult: (String?) -> Unit) {
        val userRef = db.collection("users").document(uid)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username")
                    onResult(username) // Devuelve el username si existe
                } else {
                    onResult(null) // Usuario no encontrado
                }
            }
            .addOnFailureListener {
                onResult(null) // Error al obtener el usuario
            }
    }

    fun obtenerUsuario(uid: String, onResult: (String?, String?) -> Unit) {
        val userRef = db.collection("users").document(uid)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Obtener los valores de Firestore
                    val username = document.getString("username")
                    val profileImageUrl = document.getString("image")

                    onResult(username, profileImageUrl) // Devuelve username y profileImageUrl
                } else {
                    onResult(null, null) // Usuario no encontrado
                }
            }
            .addOnFailureListener {
                onResult(null, null) // Error al obtener datos
            }
    }








}