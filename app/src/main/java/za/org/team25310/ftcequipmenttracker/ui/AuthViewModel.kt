package za.org.team25310.ftcequipmenttracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _user = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user

    private val _emailVerified = MutableStateFlow(auth.currentUser?.isEmailVerified == true)
    val emailVerified: StateFlow<Boolean> = _emailVerified

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { auth.signInWithEmailAndPassword(email.trim(), password).await() }
                .onSuccess {
                    _user.value = auth.currentUser
                    _emailVerified.value = auth.currentUser?.isEmailVerified == true
                }
                .onFailure { _error.value = it.message ?: "Sign in failed" }
            _loading.value = false
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { auth.createUserWithEmailAndPassword(email.trim(), password).await() }
                .onSuccess {
                    _user.value = auth.currentUser
                    _emailVerified.value = false
                    runCatching { auth.currentUser?.sendEmailVerification()?.await() }
                        .onFailure { _error.value = it.message ?: "Could not send verification email" }
                }
                .onFailure { _error.value = it.message ?: "Sign up failed" }
            _loading.value = false
        }
    }

    fun resendVerification() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { auth.currentUser?.sendEmailVerification()?.await() }
                .onFailure { _error.value = it.message ?: "Could not send email" }
            _loading.value = false
        }
    }

    fun refreshVerification() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching {
                auth.currentUser?.reload()?.await()
                auth.currentUser?.getIdToken(true)?.await()
            }
                .onSuccess {
                    _user.value = auth.currentUser
                    _emailVerified.value = auth.currentUser?.isEmailVerified == true
                }
                .onFailure { _error.value = it.message ?: "Could not refresh" }
            _loading.value = false
        }
    }

    fun signOut() {
        auth.signOut()
        _user.value = null
        _emailVerified.value = false
    }

    fun clearError() {
        _error.value = null
    }
}
