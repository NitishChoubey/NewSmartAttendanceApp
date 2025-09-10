package com.ebf.smartattendanceapp.Navigation


import AttendanceScreen
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


import com.ebf.smartattendanceapp.Screens.DashboardScreen
import com.ebf.smartattendanceapp.Screens.LoginScreen

@Composable
fun SmartAttendanceApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController = navController) }
        composable("dashboard") { DashboardScreen(navController = navController) }
        composable("attendance") {
            val context = LocalContext.current as AppCompatActivity
            AttendanceScreen(navController = navController, activity = context)
        }
    }
}