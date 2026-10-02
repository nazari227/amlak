package com.example.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.PersianUtils

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Properties : Screen("properties")
    data object Demands : Screen("demands")
    data object Tasks : Screen("tasks")
    data object Notifications : Screen("notifications")
    data object Profile : Screen("profile")

    data object PropertyDetail : Screen("property_detail/{propertyId}") {
        fun createRoute(propertyId: Long) = "property_detail/$propertyId"
    }

    data object PropertyCreate : Screen("property_create")
    data object DemandDetail : Screen("demand_detail/{demandId}") {
        fun createRoute(demandId: Long) = "demand_detail/$demandId"
    }

    data object Login : Screen("login")
}

data class BottomNavItem(
    val screen: Screen,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun AshianMelkBottomBar(
    currentRoute: String?,
    unreadNotificationsCount: Int,
    todayTasksCount: Int,
    onNavigate: (Screen) -> Unit
) {
    val items = listOf(
        BottomNavItem(
            screen = Screen.Home,
            title = "امروز",
            selectedIcon = Icons.Filled.Dashboard,
            unselectedIcon = Icons.Outlined.Dashboard
        ),
        BottomNavItem(
            screen = Screen.Properties,
            title = "املاک",
            selectedIcon = Icons.Filled.HomeWork,
            unselectedIcon = Icons.Outlined.HomeWork
        ),
        BottomNavItem(
            screen = Screen.Demands,
            title = "متقاضیان",
            selectedIcon = Icons.Filled.People,
            unselectedIcon = Icons.Outlined.People
        ),
        BottomNavItem(
            screen = Screen.Tasks,
            title = "وظایف",
            selectedIcon = Icons.Filled.Assignment,
            unselectedIcon = Icons.Outlined.Assignment,
            badgeCount = todayTasksCount
        ),
        BottomNavItem(
            screen = Screen.Notifications,
            title = "اعلان‌ها",
            selectedIcon = Icons.Filled.Notifications,
            unselectedIcon = Icons.Outlined.Notifications,
            badgeCount = unreadNotificationsCount
        ),
        BottomNavItem(
            screen = Screen.Profile,
            title = "بیشتر",
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person
        )
    )

    NavigationBar(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("ashian_melk_bottom_navigation"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.screen.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.screen) },
                icon = {
                    if (item.badgeCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ) {
                                    Text(PersianUtils.toPersianDigits(item.badgeCount))
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title
                        )
                    }
                },
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
