package com.megs.consultation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.megs.consultation.ui.screens.AddEditScreen
import com.megs.consultation.ui.screens.AlertsScreen
import com.megs.consultation.ui.screens.CalendarScreen
import com.megs.consultation.ui.screens.HistoryScreen
import com.megs.consultation.ui.screens.HomeScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun MegsApp(vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"

    fun go(r: String) = nav.navigate(r) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    val openEdit: (Long) -> Unit = { id -> nav.navigate("edit?id=$id&date=0") }
    val openAdd: (Long) -> Unit = { day -> nav.navigate("edit?id=0&date=$day") }

    val tabs = listOf(
        Tab("home", "Home", Icons.Outlined.Home),
        Tab("calendar", "Calendar", Icons.Outlined.CalendarMonth),
        Tab("add", "Add", Icons.Filled.Add),
        Tab("alerts", "Alerts", Icons.Outlined.Notifications),
        Tab("history", "History", Icons.Outlined.History)
    )

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            if (!route.startsWith("edit")) {
                NavigationBar(containerColor = Color.White) {
                    tabs.forEach { t ->
                        if (t.route == "add") {
                            NavigationBarItem(
                                selected = false,
                                onClick = { openAdd(0L) },
                                icon = {
                                    Box(
                                        Modifier.size(48.dp).clip(CircleShape).background(Green),
                                        contentAlignment = Alignment.Center
                                    ) { Icon(t.icon, "Add appointment", tint = Color.White) }
                                },
                                label = { Text(t.label) }
                            )
                        } else {
                            NavigationBarItem(
                                selected = route == t.route,
                                onClick = { go(t.route) },
                                icon = { Icon(t.icon, t.label) },
                                label = { Text(t.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Green, selectedTextColor = Green,
                                    indicatorColor = GreenLight
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") {
                HomeScreen(vm, onAdd = { openAdd(0L) }, onEdit = openEdit,
                    onCalendar = { go("calendar") }, onSearch = { go("history") })
            }
            composable("calendar") { CalendarScreen(vm, onAddOn = openAdd, onEdit = openEdit) }
            composable("alerts") { AlertsScreen(vm, onEdit = openEdit) }
            composable("history") { HistoryScreen(vm, onEdit = openEdit) }
            composable(
                "edit?id={id}&date={date}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("date") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { e ->
                AddEditScreen(
                    vm,
                    id = e.arguments?.getLong("id") ?: 0L,
                    presetDate = e.arguments?.getLong("date") ?: 0L,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}
