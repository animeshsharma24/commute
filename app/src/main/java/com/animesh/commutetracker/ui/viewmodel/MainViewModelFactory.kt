package com.animesh.commutetracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.animesh.commutetracker.data.repository.CommuteRepository
import com.animesh.commutetracker.data.repository.PreferenceManager

class MainViewModelFactory(
    private val repository: CommuteRepository,
    private val preferenceManager: PreferenceManager,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, preferenceManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
