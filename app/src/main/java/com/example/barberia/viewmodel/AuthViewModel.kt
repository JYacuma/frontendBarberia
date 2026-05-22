package com.example.barberia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barberia.model.LoginResponse
import com.example.barberia.model.RegisterResponse
import com.example.barberia.network.AuthRepository
import com.example.barberia.network.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Estado que observa la pantalla de login/registro
data class AuthUiState(
    val isLoading: Boolean = false,
    val loginSuccess: LoginResponse? = null,
    val registerSuccess: RegisterResponse? = null,
    val errorMessage: String? = null
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Por favor completa todos los campos"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            when (val result = authRepository.login(email, password)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loginSuccess = result.data
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
                else -> {}
            }
        }
    }

    fun register(nombre: String, correo: String, password: String, confirmPassword: String, telefono: String? = null) {
        if (nombre.isBlank() || correo.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Por favor completa todos los campos"
            )
            return
        }
        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Las contraseñas no coinciden"
            )
            return
        }
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "La contraseña debe tener mínimo 6 caracteres"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            when (val result = authRepository.register(nombre, correo, password, telefono)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    registerSuccess = result.data
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
                else -> {}
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // Factory — reemplaza Hilt de forma simple
    // Le dice a Android cómo crear el ViewModel con sus dependencias
    companion object {
        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return AuthViewModel(authRepository) as T
                }
            }
        }
    }
}