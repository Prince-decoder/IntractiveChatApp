package com.my.forintern.OnBoarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.remove
import kotlin.compareTo
import kotlin.text.contains

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserProfileRepository(application)

    // UI state representing the inputs
    private val _onboardingState = MutableStateFlow(UserProfile())
    val onboardingState = _onboardingState.asStateFlow()

    // Read the saved profile from DataStore to know if they already onboarded
    val savedUserProfile: StateFlow<UserProfile?> = repository.userProfileFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun updateName(name: String) {
        _onboardingState.value = _onboardingState.value.copy(name = name)
    }

    fun updateAge(age: String) {
        _onboardingState.value = _onboardingState.value.copy(age = age)
    }

    fun updatePhone(phone: String) {
        _onboardingState.value = _onboardingState.value.copy(phone = phone)
    }

    fun toggleTrait(trait: String) {
        val currentTraits = _onboardingState.value.traits.toMutableSet()
        if (currentTraits.contains(trait)) {
            currentTraits.remove(trait)
        } else {
            if (currentTraits.size < 3) {
                currentTraits.add(trait)
            }
        }
        _onboardingState.value = _onboardingState.value.copy(traits = currentTraits)
    }

    fun isStep2Valid(profile: UserProfile, otp: String): Boolean {
        return profile.name.isNotBlank() &&
                profile.age.isNotBlank() &&
                profile.phone.length == 10 &&
                profile.phone.all { it.isDigit() } &&
                otp.equals("1234",true)
    }

    fun isStep3Valid(profile: UserProfile): Boolean {
        return profile.traits.size == 3
    }

    fun saveProfile() {
        viewModelScope.launch {
            repository.saveUserProfile(_onboardingState.value)
        }
    }
}
