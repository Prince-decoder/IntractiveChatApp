package com.my.forintern

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.my.forintern.HomeScreen.HomeScreen
import com.my.forintern.OnBoarding.OnboardingScreen
import com.my.forintern.OnBoarding.OnboardingViewModel
import com.my.forintern.UserRoomDataBase.UserViewModel
import com.my.forintern.ui.theme.ForInternTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val onboardingViewModel: OnboardingViewModel= viewModel()
            val navHostController= rememberNavController()
            ForInternTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    navigationControl(
                        navhost = navHostController,
                        modifier = Modifier.padding(innerPadding),
                        onboardingViewModel = onboardingViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun navigationControl(navhost: NavHostController,modifier: Modifier,onboardingViewModel: OnboardingViewModel)
{
    val context = LocalContext.current
    val userViewModel: UserViewModel = viewModel()
    val startdestination = Screens.OnboardingScreen.route

    NavHost(navController = navhost, startDestination = startdestination, modifier = modifier)
    {
        composable(Screens.OnboardingScreen.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    val currentName = onboardingViewModel.onboardingState.value.name.ifBlank { "User" }
                    navhost.navigate("HomePage/$currentName") {
                        popUpTo(Screens.OnboardingScreen.route) { inclusive = true }
                    }
                },
                navhost = navhost,viewModel = onboardingViewModel, userViewModel = userViewModel
            )
        }
        composable(
            route = Screens.HomeScreen.route,
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            HomeScreen(username = username)
        }
    }
}