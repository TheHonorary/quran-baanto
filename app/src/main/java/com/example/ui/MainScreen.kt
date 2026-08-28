package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.distribution.CreateEditDistributionScreen
import com.example.ui.distribution.DistributionDetailScreen
import com.example.ui.distribution.DistributionListScreen
import com.example.ui.home.HomeScreen
import com.example.ui.navigation.BottomNavItem
import com.example.ui.quran.QuranListScreen
import com.example.ui.quran.QuranReaderScreen
import com.example.ui.settings.SettingsSheet
import com.example.ui.theme.QuranBaantoTheme
import com.example.ui.viewmodel.DistributionViewModel
import com.example.ui.viewmodel.QuranViewModel

@Composable
fun MainScreen() {
    var isDarkTheme by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    QuranBaantoTheme(darkTheme = isDarkTheme) {
        val navController = rememberNavController()
        val distributionViewModel: DistributionViewModel = viewModel()
        val quranViewModel: QuranViewModel = viewModel()

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val bottomNavItems = listOf(
            BottomNavItem.Home,
            BottomNavItem.Quran,
            BottomNavItem.Distributions
        )

        // Show bottom navigation bar only on main tab routes
        val shouldShowBottomBar = currentRoute in listOf("home", "quran", "distributions")

        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = shouldShowBottomBar,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        bottomNavItems.forEach { item ->
                            val isSelected = currentRoute == item.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                // Home Screen
                composable(BottomNavItem.Home.route) {
                    HomeScreen(
                        distributionViewModel = distributionViewModel,
                        quranViewModel = quranViewModel,
                        onNavigateToQuran = {
                            navController.navigate(BottomNavItem.Quran.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToDistributions = {
                            navController.navigate(BottomNavItem.Distributions.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenDistributionDetail = { distId ->
                            navController.navigate("distribution_detail/$distId")
                        },
                        onOpenParaReader = { paraNum ->
                            quranViewModel.selectPara(paraNum)
                            navController.navigate("quran_reader_para/$paraNum")
                        },
                        onOpenSurahReader = { surahNum ->
                            quranViewModel.selectSurah(surahNum)
                            navController.navigate("quran_reader_surah/$surahNum")
                        },
                        onOpenSettings = { showSettingsSheet = true }
                    )
                }

                // Qur'an List Screen
                composable(BottomNavItem.Quran.route) {
                    QuranListScreen(
                        quranViewModel = quranViewModel,
                        onOpenParaReader = { paraNum ->
                            quranViewModel.selectPara(paraNum)
                            navController.navigate("quran_reader_para/$paraNum")
                        },
                        onOpenSurahReader = { surahNum ->
                            quranViewModel.selectSurah(surahNum)
                            navController.navigate("quran_reader_surah/$surahNum")
                        }
                    )
                }

                // Qur'an Reader Screens (Para and Surah)
                composable(
                    route = "quran_reader_para/{paraNum}",
                    arguments = listOf(navArgument("paraNum") { type = NavType.IntType })
                ) { backStackEntry ->
                    val paraNum = backStackEntry.arguments?.getInt("paraNum") ?: 1
                    QuranReaderScreen(
                        quranViewModel = quranViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "quran_reader_surah/{surahNum}",
                    arguments = listOf(navArgument("surahNum") { type = NavType.IntType })
                ) { backStackEntry ->
                    val surahNum = backStackEntry.arguments?.getInt("surahNum") ?: 1
                    QuranReaderScreen(
                        quranViewModel = quranViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "quran_reader/{paraNum}",
                    arguments = listOf(navArgument("paraNum") { type = NavType.IntType })
                ) {
                    QuranReaderScreen(
                        quranViewModel = quranViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Distributions List Screen
                composable(BottomNavItem.Distributions.route) {
                    DistributionListScreen(
                        distributionViewModel = distributionViewModel,
                        onCreateNewDistribution = { type ->
                            navController.navigate("create_edit_distribution?type=${type.name}")
                        },
                        onOpenDistributionDetail = { distId ->
                            navController.navigate("distribution_detail/$distId")
                        }
                    )
                }

                // Distribution Detail Screen
                composable(
                    route = "distribution_detail/{distId}",
                    arguments = listOf(navArgument("distId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val distId = backStackEntry.arguments?.getLong("distId") ?: 0L
                    DistributionDetailScreen(
                        distributionId = distId,
                        distributionViewModel = distributionViewModel,
                        onEditDistribution = { idToEdit ->
                            navController.navigate("create_edit_distribution?distId=$idToEdit")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Create or Edit Distribution Screen
                composable(
                    route = "create_edit_distribution?distId={distId}&type={type}",
                    arguments = listOf(
                        navArgument("distId") {
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                        navArgument("type") {
                            type = NavType.StringType
                            defaultValue = "KHATM"
                        }
                    )
                ) { backStackEntry ->
                    val rawDistId = backStackEntry.arguments?.getLong("distId") ?: -1L
                    val distId = if (rawDistId == -1L) null else rawDistId
                    val typeString = backStackEntry.arguments?.getString("type") ?: "KHATM"
                    val initialType = try {
                        com.example.data.db.DistributionType.valueOf(typeString)
                    } catch (e: Exception) {
                        com.example.data.db.DistributionType.KHATM
                    }

                    CreateEditDistributionScreen(
                        distributionViewModel = distributionViewModel,
                        existingDistributionId = distId,
                        initialType = initialType,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            if (showSettingsSheet) {
                SettingsSheet(
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { isDarkTheme = it },
                    onDismiss = { showSettingsSheet = false }
                )
            }
        }
    }
}
