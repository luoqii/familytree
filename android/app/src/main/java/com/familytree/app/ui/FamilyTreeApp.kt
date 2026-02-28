package com.familytree.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.familytree.app.ui.screens.AddEditMemberScreen
import com.familytree.app.ui.screens.HomeScreen
import com.familytree.app.ui.screens.MemberDetailScreen
import com.familytree.app.ui.screens.MemberListScreen
import com.familytree.app.ui.screens.SearchScreen
import com.familytree.app.ui.screens.SettingsScreen
import com.familytree.app.ui.screens.TreeScreen
import com.familytree.app.ui.viewmodel.FamilyViewModel

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : BottomNavItem("home", "首页", Icons.Filled.Home)
    data object Tree : BottomNavItem("tree", "族谱", Icons.Filled.AccountTree)
    data object Members : BottomNavItem("members", "成员", Icons.Filled.Person)
    data object Search : BottomNavItem("search", "搜索", Icons.Filled.Search)
    data object Settings : BottomNavItem("settings", "设置", Icons.Filled.Settings)
}

object Routes {
    const val MEMBER_DETAIL = "member_detail/{memberId}"
    const val ADD_MEMBER = "add_member"
    const val EDIT_MEMBER = "edit_member/{memberId}"

    fun memberDetail(memberId: String) = "member_detail/$memberId"
    fun editMember(memberId: String) = "edit_member/$memberId"
}

@Composable
fun FamilyTreeApp() {
    val navController = rememberNavController()
    val familyViewModel: FamilyViewModel = viewModel()

    val navItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Tree,
        BottomNavItem.Members,
        BottomNavItem.Search,
        BottomNavItem.Settings
    )

    val bottomBarRoutes = navItems.map { it.route }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val currentDestination = navBackStackEntry?.destination
                    navItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    viewModel = familyViewModel,
                    onNavigateToAddMember = { navController.navigate(Routes.ADD_MEMBER) },
                    onNavigateToTree = {
                        navController.navigate(BottomNavItem.Tree.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(BottomNavItem.Tree.route) {
                TreeScreen(
                    viewModel = familyViewModel,
                    onMemberClick = { memberId ->
                        navController.navigate(Routes.memberDetail(memberId))
                    }
                )
            }
            composable(BottomNavItem.Members.route) {
                MemberListScreen(
                    viewModel = familyViewModel,
                    onAddMember = { navController.navigate(Routes.ADD_MEMBER) },
                    onMemberClick = { memberId ->
                        navController.navigate(Routes.memberDetail(memberId))
                    }
                )
            }
            composable(BottomNavItem.Search.route) {
                SearchScreen(
                    viewModel = familyViewModel,
                    onMemberClick = { memberId ->
                        navController.navigate(Routes.memberDetail(memberId))
                    }
                )
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = Routes.MEMBER_DETAIL,
                arguments = listOf(navArgument("memberId") { type = NavType.StringType })
            ) { backStackEntry ->
                val memberId = backStackEntry.arguments?.getString("memberId") ?: return@composable
                MemberDetailScreen(
                    memberId = memberId,
                    viewModel = familyViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onEditMember = { navController.navigate(Routes.editMember(memberId)) },
                    onMemberClick = { id -> navController.navigate(Routes.memberDetail(id)) }
                )
            }
            composable(Routes.ADD_MEMBER) {
                AddEditMemberScreen(
                    viewModel = familyViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.EDIT_MEMBER,
                arguments = listOf(navArgument("memberId") { type = NavType.StringType })
            ) { backStackEntry ->
                val memberId = backStackEntry.arguments?.getString("memberId") ?: return@composable
                AddEditMemberScreen(
                    memberId = memberId,
                    viewModel = familyViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
