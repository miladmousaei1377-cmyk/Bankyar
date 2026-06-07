package com.bankyar.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.bankyar.data.database.AppDatabase
import com.bankyar.data.repository.UserRepository
import com.bankyar.ui.screens.*
import com.bankyar.ui.viewmodels.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Home : Screen("home")
    object Transactions : Screen("transactions")
    object AddTransaction : Screen("add?editId={editId}") {
        fun route(editId: Int = -1) = "add?editId=$editId"
    }
    object TransactionDetail : Screen("detail/{id}") {
        fun route(id: Int) = "detail/$id"
    }
    object Profile : Screen("profile")
    object Accounts : Screen("accounts")
    object Reports : Screen("reports")
    object About : Screen("about")
}

@Composable
fun BankYarNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val transactionViewModel: TransactionViewModel = viewModel()
    val accountsViewModel: AccountsViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val themeViewModel: ThemeViewModel = viewModel()

    val loggedInUserId by authViewModel.loggedInUserId.collectAsState()
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()
    val context = LocalContext.current
    var userName by remember { mutableStateOf("کاربر") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(loggedInUserId) {
        if (loggedInUserId > 0) {
            scope.launch {
                UserRepository(AppDatabase.getInstance(context).userDao())
                    .getUserById(loggedInUserId).collect { user -> user?.let { userName = it.name } }
            }
        }
    }

    val startDest = if (loggedInUserId > 0) Screen.Home.route else Screen.Auth.route

    NavHost(navController = navController, startDestination = startDest) {

        composable(Screen.Auth.route) {
            AuthScreen(authViewModel) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            }
        }

        composable(Screen.Home.route) {
            if (loggedInUserId > 0) {
                HomeScreen(
                    userId = loggedInUserId,
                    userName = userName,
                    isDarkMode = isDarkMode,
                    viewModel = transactionViewModel,
                    onToggleDarkMode = { themeViewModel.toggle() },
                    onAddTransaction = { navController.navigate(Screen.AddTransaction.route()) },
                    onViewAll = { navController.navigate(Screen.Transactions.route) },
                    onTransactionClick = { navController.navigate(Screen.TransactionDetail.route(it)) },
                    onProfile = { navController.navigate(Screen.Profile.route) },
                    onAccounts = { navController.navigate(Screen.Accounts.route) },
                    onReports = { navController.navigate(Screen.Reports.route) },
                    onAbout = { navController.navigate(Screen.About.route) },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Screen.Transactions.route) {
            TransactionsScreen(
                viewModel = transactionViewModel,
                onBack = { navController.popBackStack() },
                onAddTransaction = { navController.navigate(Screen.AddTransaction.route()) },
                onTransactionClick = { navController.navigate(Screen.TransactionDetail.route(it)) }
            )
        }

        composable(
            Screen.AddTransaction.route,
            arguments = listOf(navArgument("editId") { type = NavType.IntType; defaultValue = -1 })
        ) { back ->
            AddTransactionScreen(
                userId = loggedInUserId,
                editId = back.arguments?.getInt("editId") ?: -1,
                viewModel = transactionViewModel,
                accountsViewModel = accountsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Screen.TransactionDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { back ->
            TransactionDetailScreen(
                transactionId = back.arguments?.getInt("id") ?: return@composable,
                viewModel = transactionViewModel,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Screen.AddTransaction.route(it)) }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                userId = loggedInUserId,
                viewModel = profileViewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Accounts.route) {
            AccountsScreen(
                userId = loggedInUserId,
                viewModel = accountsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Reports.route) {
            ReportsScreen(
                userId = loggedInUserId,
                viewModel = transactionViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
