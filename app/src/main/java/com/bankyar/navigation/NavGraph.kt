package com.bankyar.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.bankyar.data.database.AppDatabase
import com.bankyar.data.repository.UserRepository
import com.bankyar.ui.screens.*
import com.bankyar.ui.viewmodels.*
import androidx.compose.ui.platform.LocalContext
import com.bankyar.ui.theme.GradientEnd
import com.bankyar.ui.theme.GradientStart
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object BiometricLock : Screen("biometric_lock")
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
    object Budget : Screen("budget")
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
    val budgetViewModel: BudgetViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    // -2 = loading, -1 = not logged in, >0 = logged in user ID
    val loggedInUserId by authViewModel.loggedInUserId.collectAsState()
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()
    val isFingerprintEnabled by settingsViewModel.isFingerprintEnabled.collectAsState()
    val isSettingsLoaded by settingsViewModel.isLoaded.collectAsState()
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

    // Trigger auto-backup after login
    LaunchedEffect(loggedInUserId) {
        if (loggedInUserId > 0) {
            settingsViewModel.checkAndAutoBackup(loggedInUserId)
        }
    }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            // Wait for both auth state and settings to be loaded before navigating
            LaunchedEffect(loggedInUserId, isSettingsLoaded) {
                if (loggedInUserId == -2) return@LaunchedEffect  // Auth not loaded yet
                if (!isSettingsLoaded) return@LaunchedEffect      // Settings not loaded yet

                val dest = when {
                    loggedInUserId > 0 && isFingerprintEnabled -> Screen.BiometricLock.route
                    loggedInUserId > 0 -> Screen.Home.route
                    else -> Screen.Auth.route
                }
                navController.navigate(dest) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            SplashScreen()
        }

        composable(Screen.Auth.route) {
            AuthScreen(authViewModel) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            }
        }

        composable(Screen.BiometricLock.route) {
            BiometricLockScreen(
                userId = loggedInUserId,
                onUnlocked = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.BiometricLock.route) { inclusive = true }
                    }
                },
                onVerifyPin = { pin, callback ->
                    authViewModel.verifyPin(loggedInUserId, pin, callback)
                }
            )
        }

        composable(Screen.Home.route) {
            val userId = loggedInUserId
            if (userId > 0) {
                HomeScreen(
                    userId = userId,
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
                    onBudget = { navController.navigate(Screen.Budget.route) },
                    onAbout = { navController.navigate(Screen.About.route) },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            } else if (userId == -1) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
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
                settingsViewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
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

        composable(Screen.Budget.route) {
            BudgetScreen(
                userId = loggedInUserId,
                viewModel = budgetViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd, Color(0xFF0A2472)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountBalance, null, tint = Color.White, modifier = Modifier.size(50.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("بانک‌یار", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("مدیریت هوشمند مالی", fontSize = 14.sp, color = Color.White.copy(0.7f))
            Spacer(Modifier.height(40.dp))
            CircularProgressIndicator(color = Color.White.copy(0.8f), strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
        }
    }
}
