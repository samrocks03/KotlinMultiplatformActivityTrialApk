package com.example.kmp_basic_app.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.kmp_basic_app.ui.screens.CameraScreen
import com.example.kmp_basic_app.ui.screens.CharacterDetailScreen
import com.example.kmp_basic_app.ui.screens.CharactersScreen
import com.example.kmp_basic_app.ui.screens.CreateEditPostScreen
import com.example.kmp_basic_app.ui.screens.FavoritesScreen
import com.example.kmp_basic_app.ui.screens.LocationScreen
import com.example.kmp_basic_app.ui.screens.PostsScreen
import kotlinx.serialization.Serializable

@Serializable object CharactersRoute
@Serializable data class CharacterDetailRoute(val id: String)
@Serializable object PostsRoute
@Serializable data class CreateEditPostRoute(
    val postId: String? = null,
    val title: String? = null,
    val body: String? = null
)
@Serializable object CameraRoute
@Serializable object LocationRoute
@Serializable object FavoritesRoute

enum class TopLevelDestination(
    val label: String,
    val icon: ImageVector,
    val route: Any
) {
    Characters("Characters", Icons.Filled.People, CharactersRoute),
    Posts("Posts", Icons.AutoMirrored.Filled.Article, PostsRoute),
    Camera("Camera", Icons.Filled.CameraAlt, CameraRoute),
    Gps("GPS", Icons.Filled.LocationOn, LocationRoute),
    Favorites("Favorites", Icons.Filled.Favorite, FavoritesRoute)
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = TopLevelDestination.entries.any { dest ->
        currentDestination?.hierarchy?.any { it.hasRoute(dest.route::class) } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                val borderColor = MaterialTheme.colorScheme.outline
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp,
                    modifier = Modifier.drawBehind {
                        drawLine(
                            color = borderColor,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                ) {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(destination.route::class)
                        } == true

                        // Spring scale on selection
                        val iconScale by animateFloatAsState(
                            targetValue = if (selected) 1.15f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "navIconScale"
                        )

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    destination.icon,
                                    contentDescription = destination.label,
                                    modifier = Modifier.scale(iconScale)
                                )
                            },
                            label = { Text(destination.label, style = MaterialTheme.typography.labelSmall) },
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = CharactersRoute,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(250)) + slideInHorizontally(
                    initialOffsetX = { it / 4 },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(250)) + slideInHorizontally(
                    initialOffsetX = { -it / 4 },
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                    targetOffsetX = { it / 4 },
                    animationSpec = tween(300)
                )
            }
        ) {
            composable<CharactersRoute> {
                CharactersScreen(
                    onCharacterClick = { id ->
                        navController.navigate(CharacterDetailRoute(id))
                    }
                )
            }
            composable<CharacterDetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<CharacterDetailRoute>()
                CharacterDetailScreen(
                    characterId = route.id,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable<PostsRoute> {
                PostsScreen(
                    onCreatePost = {
                        navController.navigate(CreateEditPostRoute())
                    },
                    onEditPost = { postId, title, body ->
                        navController.navigate(CreateEditPostRoute(postId, title, body))
                    }
                )
            }
            composable<CreateEditPostRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<CreateEditPostRoute>()
                CreateEditPostScreen(
                    postId = route.postId,
                    initialTitle = route.title ?: "",
                    initialBody = route.body ?: "",
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable<CameraRoute> {
                CameraScreen()
            }
            composable<LocationRoute> {
                LocationScreen()
            }
            composable<FavoritesRoute> {
                FavoritesScreen(
                    onCharacterClick = { id ->
                        navController.navigate(CharacterDetailRoute(id))
                    }
                )
            }
        }
    }
}
