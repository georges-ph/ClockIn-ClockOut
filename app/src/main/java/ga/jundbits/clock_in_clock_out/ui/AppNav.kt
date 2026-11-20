package ga.jundbits.clock_in_clock_out.ui

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ga.jundbits.clock_in_clock_out.data.AppDatabase
import ga.jundbits.clock_in_clock_out.data.EmployeeStore
import ga.jundbits.clock_in_clock_out.ui.theme.AppTheme

private object Routes {
    const val START = "start"
    const val WELCOME = "welcome"
    const val BADGE = "badge"
    const val GUARD = "guard"
    const val SCAN_GUARD = "scan/guard"
    const val SCAN_SETUP = "scan/setup"
    const val PROFILES = "profiles"
    const val PROFILE = "profile/{id}"
    const val ATTENDANCE = "attendance"

    fun profile(id: String) = "profile/${Uri.encode(id)}"
}

/** Goes back once, ignoring repeat taps while the screen is already leaving. */
private fun NavHostController.back() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED && previousBackStackEntry != null) {
        popBackStack()
    }
}

/** Replaces the whole back stack with one of the two home screens. */
private fun NavHostController.home(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val store = remember { EmployeeStore(context) }

    NavHost(
        navController = nav,
        startDestination = Routes.START,
        modifier = Modifier.background(AppTheme.colors.background),
        enterTransition = { fadeIn(tween(160)) },
        exitTransition = { fadeOut(tween(160)) },
    ) {
        // The phone's role follows its data: an imported list makes it the gate,
        // a saved badge makes it an employee phone.
        composable(Routes.START) {
            LaunchedEffect(Unit) {
                val route = when {
                    db.profileDao().count() > 0 -> Routes.GUARD
                    store.read() != null -> Routes.BADGE
                    else -> Routes.WELCOME
                }
                nav.home(route)
            }
            Box(Modifier.fillMaxSize())
        }
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onEmployee = { nav.navigate(Routes.SCAN_SETUP) },
                onGate = { nav.home(Routes.GUARD) },
            )
        }
        composable(Routes.BADGE) {
            BadgeScreen(
                store = store,
                onBack = if (nav.previousBackStackEntry != null) ({ nav.back() }) else null,
                onScanAnother = { nav.navigate(Routes.SCAN_SETUP) },
                onGateMode = { nav.home(Routes.GUARD) },
                onMissing = { nav.home(Routes.WELCOME) },
            )
        }
        composable(Routes.GUARD) {
            GuardHomeScreen(
                db = db,
                hasBadge = store.read() != null,
                onScan = { nav.navigate(Routes.SCAN_GUARD) },
                onProfiles = { nav.navigate(Routes.PROFILES) },
                onAttendance = { nav.navigate(Routes.ATTENDANCE) },
                onMyBadge = { nav.navigate(Routes.BADGE) },
            )
        }
        composable(Routes.SCAN_GUARD) {
            ScanScreen(mode = ScanMode.GUARD, db = db, store = store, onClose = { nav.back() }, onSetupDone = {})
        }
        composable(Routes.SCAN_SETUP) {
            ScanScreen(mode = ScanMode.SETUP, db = db, store = store, onClose = { nav.back() }, onSetupDone = { nav.home(Routes.BADGE) })
        }
        composable(Routes.PROFILES) {
            ProfilesScreen(db = db, onBack = { nav.back() }, onOpen = { nav.navigate(Routes.profile(it)) })
        }
        composable(Routes.PROFILE, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            ProfileScreen(db = db, id = entry.arguments?.getString("id").orEmpty(), onBack = { nav.back() })
        }
        composable(Routes.ATTENDANCE) {
            AttendanceScreen(db = db, onBack = { nav.back() })
        }
    }
}
