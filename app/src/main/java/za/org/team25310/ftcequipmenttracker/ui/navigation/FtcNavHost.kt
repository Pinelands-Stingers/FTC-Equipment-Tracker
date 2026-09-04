package za.org.team25310.ftcequipmenttracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import za.org.team25310.ftcequipmenttracker.ui.screens.LoanDetailScreen
import za.org.team25310.ftcequipmenttracker.ui.screens.LoanListScreen
import za.org.team25310.ftcequipmenttracker.ui.screens.LoginScreen
import za.org.team25310.ftcequipmenttracker.ui.screens.NewLoanScreen
import za.org.team25310.ftcequipmenttracker.ui.screens.PartListScreen
import za.org.team25310.ftcequipmenttracker.ui.screens.TeamListScreen

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Teams : Screen("teams")
    data object Parts : Screen("parts")
    data object Loans : Screen("loans")
    data object NewLoan : Screen("new_loan")
    data object LoanDetail : Screen("loan_detail/{loanId}") {
        fun withId(id: String) = "loan_detail/$id"
    }
}

@Composable
fun FtcNavHost() {
    val navController = rememberNavController()
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
            )
        }
        composable(Screen.Teams.route) {
            TeamListScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Parts.route) {
            PartListScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.NewLoan.route) {
            NewLoanScreen(onDone = { navController.popBackStack() })
        }
        composable(
            route = Screen.LoanDetail.route,
            arguments = listOf(navArgument("loanId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("loanId").orEmpty()
            LoanDetailScreen(loanId = id, onBack = { navController.popBackStack() })
        }
    }
}
