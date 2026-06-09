package com.bankyar.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.bankyar.data.database.AppDatabase
import com.bankyar.data.repository.UserRepository
import com.bankyar.ui.screens.*
import com.bankyar.ui.viewmodels.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Lock : Screen("lock")
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
    object Settings : Screen("settings")
    object Budget : Screen("budget")
    object Debts : Screen("debts")
    object Recurring : Screen("recurring")
}

@Composable
fun BankYarNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val transactionViewModel: TransactionViewModel = viewModel()
    val accountsViewModel: AccountsViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val themeViewModel: ThemeViewModel = viewModel()
    val budgetViewModel: BudgetViewModel = viewModel()
    val debtViewModel: DebtViewModel = viewModel()
    val recurringViewModel: RecurringViewModel = viewModel()

    val loggedInUserId by authViewModel.loggedInUserId.collectAsState()
    val isSessionActive by authViewModel.isSessionActive.collectAsState()
    val biometricEnabled by authViewModel.biometricEnabled.collectAsState()
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

    // Process due recurring transactions when user is logged in
    LaunchedEffect(loggedInUserId) {
        if (loggedInUserId > 0) {
            recurringViewModel.processDue(loggedInUserId)
        }
    }

    // Determine start destination based on session state
    val startDest = when {
        loggedInUserId > 0 && isSessionActive -> Screen.Home.route
        loggedInUserId > 0 -> Screen.Lock.route
        else -> Screen.Auth.route
    }

    // React to auth/session state changes
    LaunchedEffect(loggedInUserId, isSessionActive) {
        val current = navController.currentBackStackEntry?.destination?.route
        when {
            loggedInUserId <= 0 -> {
                if (current != null && current != Screen.Auth.route) {
                    navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } }
                }
            }
            loggedInUserId > 0 && !isSessionActive -> {
                if (current != null && current != Screen.Lock.route && current != Screen.Auth.route) {
                    navController.navigate(Screen.Lock.route) { popUpTo(0) { inclusive = true } }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDest) {

        composable(Screen.Auth.route) {
            AuthScreen(authViewModel) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            }
        }

        composable(Screen.Lock.route) {
            LockScreen(
                userId = loggedInUserId,
                userName = userName,
                biometricEnabled = biometricEnabled,
                onUnlocked = {
                    authViewModel.activateSession()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSwitchAccount = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                    authViewModel.logout()
                }
            )
        }

        composable(Screen.Home.route) {
            if (loggedInUserId > 0 && isSessionActive) {
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
                    onSettings = { navController.navigate(Screen.Settings.route) },
                    onBudget = { navController.navigate(Screen.Budget.route) },
                    onDebts = { navController.navigate(Screen.Debts.route) },
                    onRecurring = { navController.navigate(Screen.Recurring.route) }
                )
            } else {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
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
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                    authViewModel.logout()
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

        composable(Screen.Settings.route) {
            SettingsScreen(
                userId = loggedInUserId,
                biometricEnabled = biometricEnabled,
                onBiometricToggle = { authViewModel.setBiometricEnabled(it) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Budget.route) {
            BudgetScreen(
                userId = loggedInUserId,
                viewModel = budgetViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Debts.route) {
            DebtsScreen(
                userId = loggedInUserId,
                viewModel = debtViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Recurring.route) {
            RecurringScreen(
                userId = loggedInUserId,
                viewModel = recurringViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
