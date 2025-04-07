package com.example.recipeat.data.model

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.recipeat.ui.viewmodels.PlanViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit


class PlanSemanalWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {


    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        Log.d("PlanSemanalWorker", "Worker ejecutado")

        val applicationContext = applicationContext as Application
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()

        Log.d("PlanSemanalWorker", "Usuario autenticado: $userId")

        val viewModelStore = ViewModelStore()

        val planViewModel = ViewModelProvider(
            object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore
                    get() = viewModelStore
            },
            ViewModelProvider.AndroidViewModelFactory.getInstance(applicationContext)
        ).get(PlanViewModel::class.java)

        Log.d("PlanSemanalWorker", "Iniciando generación del plan semanal")
        planViewModel.iniciarGeneracionPlanSemanal(userId)

        return Result.success()
    }


    companion object {
        // Calcula el tiempo hasta el próximo lunes
        @RequiresApi(Build.VERSION_CODES.O)
        fun calcularDelayHastaProximoLunes(): Long {
            val now = java.time.ZonedDateTime.now()
            var nextMonday = now.with(java.time.DayOfWeek.MONDAY).withHour(9).withMinute(0).withSecond(0).withNano(0)

            if (now >= nextMonday) {
                nextMonday = nextMonday.plusWeeks(1)
            }

            return java.time.Duration.between(now, nextMonday).toMillis()
        }


        // Configura el WorkManager para que se ejecute cada lunes
        @RequiresApi(Build.VERSION_CODES.O)
        fun configurarWorker(context: Context) {
            val delay = calcularDelayHastaProximoLunes()

            val workRequest = PeriodicWorkRequestBuilder<PlanSemanalWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "PlanSemanalUnique",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

    }
}





