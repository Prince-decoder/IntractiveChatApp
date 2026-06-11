package com.my.forintern

sealed class Screens(val route: String) {
    object OnboardingScreen: Screens("OnboardingPage")
    object HomeScreen: Screens("HomePage/{username}")
}