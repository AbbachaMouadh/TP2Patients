package com.example.patients.data.model

import kotlinx.serialization.Serializable

// A patient as sent back by the API (GET)
@Serializable
data class Patient(
    val id: String,
    val nom: String,
    val prenom: String,
    val age: Int? = null,
    val telephone: String? = null,
    val medecin: String? = null,
    val statut: String = "Actif",
)

// Data we send to create or edit a patient (POST / PUT): no id, MongoDB creates it
@Serializable
data class PatientRequest(
    val nom: String,
    val prenom: String,
    val age: Int? = null,
    val telephone: String? = null,
    val medecin: String? = null,
)

// Body of the error answers from the API: { "message": "...", "erreurs": [...] }
@Serializable
data class ApiError(
    val message: String,
    val erreurs: List<String> = emptyList(),
)
