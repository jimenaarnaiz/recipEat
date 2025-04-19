package com.example.recipeat.ui.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.recipeat.data.repository.PlanRepository

class PlanViewModelFactory(
    private val application: Application,
    private val planRepository: PlanRepository
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlanViewModel::class.java)) {
            return PlanViewModel(application, planRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

