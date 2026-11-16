package com.sequence.watchorder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sequence.watchorder.data.ThemeMode
import com.sequence.watchorder.ui.DetailScreen
import com.sequence.watchorder.ui.Haptics
import com.sequence.watchorder.ui.HomeScreen
import com.sequence.watchorder.ui.LibraryScreen
import com.sequence.watchorder.ui.Motion
import com.sequence.watchorder.ui.SectionAxis
import com.sequence.watchorder.ui.SectionSwitcher
import com.sequence.watchorder.ui.SeqIcons
import com.sequence.watchorder.ui.SeqSnackbar
import com.sequence.watchorder.ui.SequenceTheme
import com.sequence.watchorder.ui.SettingsScreen
import com.sequence.watchorder.ui.TagsScreen
import com.sequence.watchorder.ui.UpcomingScreen

@Composable
fun SequenceApp(vm: MainViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    // Kept above the screen switcher so a tab's scroll position, search state and remembered list
    // work survive both tab changes and trips into a detail screen.
    val tabStates = rememberSaveableStateHolder()

    LaunchedEffect(vm.snack?.id) {
        val snack = vm.snack
        if (snack == null) {
            snackbarHostState.currentSnackbarData?.dismiss()
            return@LaunchedEffect
        }
        snackbarHostState.currentSnackbarData?.dismiss()
        val result = snackbarHostState.showSnackbar(
            message = snack.text,
            actionLabel = if (snack.undo != null) "Undo" else null,
            withDismissAction = true,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) vm.runUndo(snack) else vm.dismissSnack()
    }

    BackHandler(
        enabled = vm.sheet == null &&
            (vm.detailId != null || vm.settingsOpen || vm.searchOpen || vm.tab != AppTab.HOME),
    ) {
        vm.onBack()
    }

    val darkTheme = when (vm.progress.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // These two switches live in Settings; every timed animation and haptic tick reads them.
    Motion.enabled = !vm.progress.reduceMotion
    Haptics.enabled = vm.progress.haptics

    SequenceTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            SectionSwitcher(
                target = vm.detailId to vm.settingsOpen,
                modifier = Modifier.fillMaxSize(),
                axis = SectionAxis.HORIZONTAL,
                direction = { from, to ->
                    when {
                        // Going deeper: the new screen owns the forward move.
                        to.first != null && from.first == null -> 1
                        from.first != null && to.first == null -> -1
                        to.second && !from.second -> 1
                        from.second && !to.second -> -1
                        else -> 1
                    }
                },
            ) { (detailId, settingsOpen) ->
                when {
                    detailId != null -> {
                        val franchise = vm.catalog.franchise(detailId)
                        if (franchise != null) {
                            Box(Modifier.fillMaxSize()) {
                                DetailScreen(vm, franchise)
                                SnackbarHost(
                                    hostState = snackbarHostState,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                ) { SeqSnackbar(it) }
                            }
                        }
                    }

                    settingsOpen -> Box(Modifier.fillMaxSize()) {
                        SettingsScreen(vm)
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                        ) { SeqSnackbar(it) }
                    }

                    else -> MainTabs(vm, snackbarHostState, tabStates)
                }
            }
        }

        SheetHost(vm)
    }
}

@Composable
private fun MainTabs(
    vm: MainViewModel,
    snackbarHostState: SnackbarHostState,
    stateHolder: SaveableStateHolder,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = { AppNavigationBar(vm) },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            ) { SeqSnackbar(it) }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            // Only the page moves. The navigation bar lives outside the switcher, so it never
            // re-creates, flashes or loses the selected state mid-transition.
            SectionSwitcher(
                target = vm.tab,
                modifier = Modifier.fillMaxSize(),
                axis = SectionAxis.HORIZONTAL,
                direction = { from, to -> if (to.ordinal >= from.ordinal) 1 else -1 },
            ) { tab ->
                stateHolder.SaveableStateProvider(tab.name) {
                    when (tab) {
                        AppTab.HOME -> HomeScreen(vm)
                        AppTab.LIBRARY -> LibraryScreen(vm)
                        AppTab.UPCOMING -> UpcomingScreen(vm)
                        AppTab.TAGS -> TagsScreen(vm)
                    }
                }
            }
        }
    }
}

@Composable
internal fun AppNavigationBar(vm: MainViewModel) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        // mpvEx rounds the top of its bar; the content behind it stays visible in the corners.
        modifier = Modifier.clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
    ) {
        for (tab in AppTab.entries) {
            NavigationBarItem(
                selected = vm.tab == tab,
                onClick = { vm.selectTab(tab) },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            AppTab.HOME -> SeqIcons.Home
                            AppTab.LIBRARY -> SeqIcons.Library
                            AppTab.UPCOMING -> SeqIcons.Event
                            AppTab.TAGS -> SeqIcons.Tags
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = { Text(tab.label) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
