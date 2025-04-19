package com.example.recipeat.ui.viewmodels.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.recipeat.data.repository.PlanRepository
import com.example.recipeat.ui.viewmodels.PlanViewModel

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

