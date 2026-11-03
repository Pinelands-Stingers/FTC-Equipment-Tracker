package za.org.team25310.ftcequipmenttracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import za.org.team25310.ftcequipmenttracker.ui.AuthViewModel
import za.org.team25310.ftcequipmenttracker.ui.CatalogViewModel
import za.org.team25310.ftcequipmenttracker.ui.LoansViewModel
import za.org.team25310.ftcequipmenttracker.ui.UserViewModel
import za.org.team25310.ftcequipmenttracker.ui.screens.AboutScreen
import za.org.team25310.ftcequipmenttracker.ui.screens.LoanDetailScreen
import za.org.team25310.ftcequipmenttracker.ui.screens.LoanListScreen
import za.org.team25310.ftcequipmenttracker.ui.screens.LoginScreen
import za.org.team25310.ftcequipmenttracker.ui.screens.NewLoanScreen
import za.org.team25310.ftcequipmenttracker.ui.screens.PartListScreen
import za.org.team25310.ftcequipmenttracker.ui.screens.TeamListScreen
import za.org.team25310.ftcequipmenttracker.ui.screens.TeamSettingsScreen

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Teams : Screen("teams")
    data object Parts : Screen("parts")
    data object Loans : Screen("loans")
    data object NewLoan : Screen("new_loan")
    data object About : Screen("about")
    data object TeamSettings : Screen("team_settings")
    data object LoanDetail : Screen("loan_detail/{loanId}") {
        fun withId(id: String) = "loan_detail/$id"
    }
}

@Composable
fun FtcNavHost() {
    val navController = rememberNavController()
    val loansViewModel: LoansViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val catalogViewModel: CatalogViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Screen.Loans.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.Loans.route) {
            LoanListScreen(
                onNewLoan = { navController.navigate(Screen.NewLoan.route) },
                onTeams = { navController.navigate(Screen.Teams.route) },
                onParts = { navController.navigate(Screen.Parts.route) },
                onOpenLoan = { id -> navController.navigate(Screen.LoanDetail.withId(id)) },
                onAbout = { navController.navigate(Screen.About.route) },
                onTeamSettings = { navController.navigate(Screen.TeamSettings.route) },
                onSignedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                loansViewModel = loansViewModel,
                authViewModel = authViewModel,
                userViewModel = userViewModel,
                catalogViewModel = catalogViewModel,
            )
        }
        composable(Screen.Teams.route) {
            TeamListScreen(
                onBack = { navController.popBackStack() },
                viewModel = catalogViewModel,
            )
        }
        composable(Screen.Parts.route) {
            PartListScreen(
                onBack = { navController.popBackStack() },
                viewModel = catalogViewModel,
            )
        }
        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.TeamSettings.route) {
            TeamSettingsScreen(
                onBack = { navController.popBackStack() },
                userViewModel = userViewModel,
                catalogViewModel = catalogViewModel,
            )
        }
        composable(Screen.NewLoan.route) {
            NewLoanScreen(
                onDone = { navController.popBackStack() },
                catalogViewModel = catalogViewModel,
                loansViewModel = loansViewModel,
                userViewModel = userViewModel,
            )
        }
        composable(
            route = Screen.LoanDetail.route,
            arguments = listOf(navArgument("loanId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("loanId").orEmpty()
            LoanDetailScreen(
                loanId = id,
                onBack = { navController.popBackStack() },
                loansViewModel = loansViewModel,
                userViewModel = userViewModel,
            )
        }
    }
}
