package com.example.recipeat.utils

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.recipeat.data.repository.PlanRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit


/**
 * Worker que se ejecuta una vez por semana (los lunes a las 00:00)
 * para generar automáticamente un nuevo plan semanal para el usuario loggeado.
 */
class PlanSemanalWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val planRepository = PlanRepository() // Instancia del repositorio

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        Log.d("PlanSemanalWorker", "Worker ejecutado")

        // Verifica si el usuario está autenticado
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()

        Log.d("PlanSemanalWorker", "Usuario autenticado: $userId")

        // Llama al repositorio para generar el plan semanal
        try {
            planRepository.iniciarGeneracionPlanSemanal(userId)
            return Result.success() // El trabajo fue exitoso
        } catch (e: Exception) {
            Log.e("PlanSemanalWorker", "Error generando el plan semanal", e)
            return Result.failure() // Si algo sale mal, marca el worker como fallido
        }
    }


    companion object {
        // Calcula el tiempo (en milisegundos) que falta hasta el próximo lunes a las 00:00 AM
        @RequiresApi(Build.VERSION_CODES.O)
        fun calcularDelayHastaProximoLunes(): Long {
            val now = java.time.ZonedDateTime.now()

            // Establece el próximo lunes a las 00:00
            var nextMonday = now.with(java.time.DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0).withNano(0)

            // Si ya ha pasado este lunes, se calcula el siguiente
            if (now >= nextMonday) {
                nextMonday = nextMonday.plusWeeks(1)
            }

            // Retorna la diferencia en milisegundos
            return java.time.Duration.between(now, nextMonday).toMillis()
        }


        // Configura el WorkManager para que se ejecute cada lunes
        @RequiresApi(Build.VERSION_CODES.O)
        fun configurarWorker(context: Context) {


            val currentUser = FirebaseAuth.getInstance().currentUser
            val currentUid = currentUser?.uid ?: return

            // Nombre único por usuario para evitar conflictos entre sesiones
            val uniqueWorkName = "PlanSemanalWorker_$currentUid"


            // Calcula cuánto falta para el próximo lunes a las 00:00
            val delay = calcularDelayHastaProximoLunes()


            // Crea la solicitud periódica con un intervalo de 7 días y el delay inicial
            val workRequest = PeriodicWorkRequestBuilder<PlanSemanalWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            // Encola el worker asegurando que se actualice si ya existía uno con ese nombre
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                uniqueWorkName,
                ExistingPeriodicWorkPolicy.UPDATE, // IMPORTANTE para que reemplace si cambia el usuario
                workRequest
            )
        }

    }
}





