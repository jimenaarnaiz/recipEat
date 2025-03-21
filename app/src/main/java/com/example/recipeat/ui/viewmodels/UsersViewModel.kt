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
                    // El login fue exitoso
                    val user = auth.currentUser
                    Log.d("Login", "signInWithEmail:success, user: ${user?.uid}")
                    onResult(true)  // Retorna true si el login es exitoso
                } else {
                    // El login falló
                    Log.w("Login", "signInWithEmail:failure", task.exception)
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

    fun obtenerUsuarioCompleto(uid: String, onResult: (String?, String?, String?) -> Unit) {
        val userRef = db.collection("users").document(uid)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Obtener los valores de Firestore
                    val username = document.getString("username")
                    val profileImageUrl = document.getString("image")
                    val email = document.getString("email")

                    onResult(username, profileImageUrl, email) // Devuelve username y profileImageUrl
                } else {
                    onResult(null, null, null) // Usuario no encontrado
                }
            }
            .addOnFailureListener {
                onResult(null, null, null) // Error al obtener datos
            }
    }


    // Devuelve el objeto User dado el uid
    fun obtenerUsuarioCompleto(uid: String, onResult: (User?) -> Unit) {
        val userRef = db.collection("users").document(uid)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Obtener los valores de Firestore
                    val username = document.getString("username")
                    val profileImageUrl = document.getString("image")
                    val email = document.getString("email")

                    // Crear un objeto User con la información obtenida
                    val user = User(
                        username = username ?: "",
                        image = profileImageUrl,
                        email = email ?: ""
                    )

                    // Devolver el usuario a través del callback
                    onResult(user)
                } else {
                    // Si el documento no existe, devolver null
                    onResult(null)
                }
            }
            .addOnFailureListener {
                // En caso de error, devolver null
                onResult(null)
            }
    }


    fun updateUserProfile(
        newUsername: String?,
        newEmail: String?,
        newPassword: String?,
        newProfileImage: String?,
        onResult: (Boolean) -> Unit,
        uid: String
    ) {
        val user = auth.currentUser
        val db = FirebaseFirestore.getInstance()

        if (user != null) {
            Log.d("UpdateUser", "uid: ${uid}  y user.uid: ${user.uid}")
        }

        // Cambiar correo en Firebase Auth si es necesario
        if (newEmail != null && newEmail != user?.email) {
            user?.updateEmail(newEmail)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // El correo se actualizó correctamente en Firebase Auth
                        Log.d("UpdateUser", "Email updated successfully in Auth")

                        // Si el correo se actualiza, también lo actualizamos en Firestore
                        val userRef = db.collection("users").document(uid)
                        val updatedData = mutableMapOf<String, Any>()
                        updatedData["email"] = newEmail

                        // Actualizamos el correo en Firestore
                        userRef.update(updatedData)
                            .addOnSuccessListener {
                                // Si la actualización en Firestore es exitosa
                                Log.d("UpdateUser", "Email updated successfully in Firestore")
                            }
                            .addOnFailureListener { e ->
                                Log.e("UpdateUser", "Failed to update email in Firestore", e)
                                onResult(false)
                            }
                    } else {
                        Log.e("UpdateUser", "Failed to update email in Auth", task.exception)
                        onResult(false)
                    }
                }
        }

        // Cambiar la contraseña en Firebase Auth si es necesario
        if (newPassword != null) {
            user?.updatePassword(newPassword)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // La contraseña se actualizó correctamente en Firebase Auth
                        Log.d("UpdateUser", "Password updated successfully")
                    } else {
                        Log.e("UpdateUser", "Failed to update password", task.exception)
                        onResult(false)
                    }
                }
        }

        // Actualizar username y profile image en Firestore
        if (newUsername != null || newProfileImage != null) {
            val userRef = db.collection("users").document(uid)
            val updatedData = mutableMapOf<String, Any>()

            newUsername?.let {
                updatedData["username"] = it
            }

            newProfileImage?.let {
                updatedData["image"] = it
            }

            userRef.update(updatedData)
                .addOnSuccessListener {
                    // Datos actualizados correctamente en Firestore
                    Log.d("UpdateUser", "User data updated successfully in Firestore")
                    onResult(true)
                }
                .addOnFailureListener { e ->
                    Log.e("UpdateUser", "Failed to update user data in Firestore", e)
                    onResult(false)
                }
        } else {
            // Si no hay cambios en username ni en la foto
            onResult(true)
        }
    }




    //TODO eliminar cuando tenga todas las recetas bien
//    fun eliminarRecetasDeBebidaFirestore() {
//        val db = FirebaseFirestore.getInstance()
//        var count = 0
//
//        db.collection("recetas")
//            .get()
//            .addOnSuccessListener { result ->
//                val batch = db.batch() // Usamos batch para mejorar el rendimiento
//                for (document in result) {
//                    val dishTypes = document.get("dishTypes") as? List<String> ?: emptyList()
//
//                    if (dishTypes.contains("drink") || dishTypes.contains("beverage")) {
//                        batch.delete(db.collection("recetas").document(document.id))
//                        count++
//                    }
//                }
//
//                // Ejecutamos la operación en batch
//                batch.commit()
//                    .addOnSuccessListener {
//                        println("Total de recetas eliminadas: $count")
//                    }
//                    .addOnFailureListener { exception ->
//                        println("Error al eliminar recetas: $exception")
//                    }
//            }
//            .addOnFailureListener { exception ->
//                println("Error al obtener las recetas: $exception")
//            }
//    }








}