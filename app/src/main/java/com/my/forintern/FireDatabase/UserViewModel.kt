package com.my.forintern.FireDatabase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthState {
    LOADING,
    SUCCESS,
    ERROR
}

class UserViewModel : ViewModel() {

    private val _auth = FirebaseAuth.getInstance()
    private val firestore = Injection.instance()
    private val repository = UserRepository(_auth, firestore)

    private val _authstate = MutableStateFlow(AuthState.LOADING)
    val authstate = _authstate.asStateFlow()

    val isuploded: Boolean get() = _authstate.value == AuthState.LOADING

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData = _userData.asStateFlow()

    fun addUser(userData: UserData) {
        viewModelScope.launch {
            _authstate.value = AuthState.LOADING
            when (val result = repository.saveUserToDatabase(userData)) {
                is Results.Success -> {
                    _authstate.value = AuthState.SUCCESS
                }
                is Results.error -> {
                    _authstate.value = AuthState.ERROR
                }
                is Results.Loading -> {}
            }
        }
    }

    fun fetchUser() {
        viewModelScope.launch {
            _authstate.value = AuthState.LOADING
            when (val result = repository.getCurrentUser()) {
                is Results.Success -> {
                    _userData.value = result.data
                    _authstate.value = AuthState.SUCCESS
                }
                is Results.error -> {
                    _authstate.value = AuthState.ERROR
                }
                is Results.Loading -> {}
            }
        }
    }
}