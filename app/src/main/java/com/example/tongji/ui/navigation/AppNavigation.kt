package com.example.tongji.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tongji.ui.screens.schedule.CourseScreen
import com.example.tongji.ui.screens.campus.CampusHomeScreen
import com.example.tongji.ui.screens.settings.SettingsScreen
import com.example.tongji.ui.screens.activities.ActivityListScreen
import com.example.tongji.ui.screens.notices.TeachingNoticeScreen
import com.example.tongji.ui.screens.card.CampusCardScreen
import com.example.tongji.ui.screens.exams.ExamScheduleScreen
import com.example.tongji.ui.screens.grades.GradeReportScreen
import com.example.tongji.ui.screens.library.LibraryScreen
import com.example.tongji.ui.screens.login.LoginScreen

sealed class Screen(val route: String, val title: String) {
    data object Schedule : Screen("schedule", "日程")
    data object Campus : Screen("campus", "校园")
    data object Settings : Screen("settings", "设置")
    data object Activities : Screen("activities", "卓越星活动")
    data object Notices : Screen("notices", "通知公告")
    data object CampusCard : Screen("campus_card", "校园卡")
    data object Exams : Screen("exams", "考试安排")
    data object Grades : Screen("grades", "课程成绩")
    data object Library : Screen("library", "图书馆座位")
    data object Login : Screen("login", "登录")
}

data class BottomNavItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Schedule, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomNavItem(Screen.Campus, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Settings, Icons.Filled.Settings, Icons.Outlined.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in listOf(
        Screen.Schedule.route,
        Screen.Campus.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.screen.title
                                )
                            },
                            label = { Text(item.screen.title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Schedule.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Schedule.route) {
                CourseScreen(onNavigateToExams = { navController.navigate(Screen.Exams.route) })
            }
            composable(Screen.Campus.route) {
                CampusHomeScreen(
                    onNavigateToActivities = { navController.navigate(Screen.Activities.route) },
                    onNavigateToNotices = { navController.navigate(Screen.Notices.route) },
                    onNavigateToCampusCard = { navController.navigate(Screen.CampusCard.route) },
                    onNavigateToExams = { navController.navigate(Screen.Exams.route) },
                    onNavigateToGrades = { navController.navigate(Screen.Grades.route) },
                    onNavigateToLibrary = { navController.navigate(Screen.Library.route) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onNavigateToLogin = { navController.navigate(Screen.Login.route) })
            }
            composable(Screen.Login.route) {
                LoginScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Activities.route) {
                ActivityListScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Notices.route) {
                TeachingNoticeScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.CampusCard.route) {
                CampusCardScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Exams.route) {
                ExamScheduleScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Grades.route) {
                GradeReportScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Library.route) {
                LibraryScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
