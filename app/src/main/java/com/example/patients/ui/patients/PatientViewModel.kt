package com.example.patients.ui.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.patients.PatientsApplication
import com.example.patients.data.model.Patient
import com.example.patients.data.model.PatientRequest
import com.example.patients.data.repository.PatientRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PatientViewModel(
    private val repository: PatientRepository,
) : ViewModel() {

    // Screen state: can be changed here, read-only for the UI
    private val _uiState = MutableStateFlow<PatientsUiState>(PatientsUiState.Loading)
    val uiState: StateFlow<PatientsUiState> = _uiState.asStateFlow()

    // One-time messages to show once (Snackbar)
    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    // Exercise 2: selected filter. null = "Tous", otherwise "Actif" or "Inactif"
    private val _filter = MutableStateFlow<String?>(null)
    val filter: StateFlow<String?> = _filter.asStateFlow()

    // The loading request in progress (so a new one cancels the old one)
    private var loadJob: Job? = null

    init {
        loadPatients()
    }

    fun setFilter(statut: String?) {
        _filter.value = statut
        loadPatients()
    }

    fun loadPatients() {
        loadJob?.cancel()
        _uiState.value = PatientsUiState.Loading
        loadJob = viewModelScope.launch {
            repository.getPatients(_filter.value)
                .onSuccess { patients -> _uiState.value = PatientsUiState.Success(patients) }
                .onFailure { e -> _uiState.value = PatientsUiState.Error(e.message ?: "Erreur inconnue") }
        }
    }

    fun addPatient(request: PatientRequest) {
        viewModelScope.launch {
            repository.createPatient(request)
                .onSuccess { patient ->
                    updateList { it + patient }
                    _messages.emit("${patient.prenom} ${patient.nom} ajouté")
                }
                .onFailure { e -> _messages.emit(e.message ?: "Ajout impossible") }
        }
    }

    // Exercise 1
    fun updatePatient(patient: Patient, request: PatientRequest) {
        viewModelScope.launch {
            repository.updatePatient(patient.id, request)
                .onSuccess { updated ->
                    updateList { list -> list.map { if (it.id == updated.id) updated else it } }
                    _messages.emit("${updated.prenom} ${updated.nom} modifié")
                }
                .onFailure { e -> _messages.emit(e.message ?: "Modification impossible") }
        }
    }

    fun deletePatient(patient: Patient) {
        viewModelScope.launch {
            repository.deletePatient(patient.id)
                .onSuccess {
                    updateList { list -> list.filterNot { (id) -> id == patient.id } }
                    _messages.emit("${patient.prenom} ${patient.nom} supprimé")
                }
                .onFailure { e -> _messages.emit(e.message ?: "Suppression impossible") }
        }
    }

    // Changes the displayed list without reloading from the server
    private fun updateList(transform: (List<Patient>) -> List<Patient>) {
        _uiState.update { state ->
            (state as? PatientsUiState.Success)?.copy(patients = transform(state.patients)) ?: state
        }
    }

    // Tells Android how to build this ViewModel (it needs the repository)
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PatientsApplication
                PatientViewModel(app.container.patientRepository)
            }
        }
    }
}
