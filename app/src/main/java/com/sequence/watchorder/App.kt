package com.sequence.watchorder

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sequence.watchorder.data.ContinueCard
import com.sequence.watchorder.data.Logic
import com.sequence.watchorder.ui.ArtImage
import com.sequence.watchorder.ui.ArtKind
import com.sequence.watchorder.ui.ProgressStrip
import com.sequence.watchorder.ui.SeenToggle
import com.sequence.watchorder.ui.SeqIcons
import com.sequence.watchorder.ui.SeqSnackbar
import com.sequence.watchorder.ui.SequenceTheme
import com.sequence.watchorder.ui.DetailScreen
import com.sequence.watchorder.ui.HomeScreen
import com.sequence.watchorder.ui.LibraryScreen
import com.sequence.watchorder.ui.SearchScreen
import com.sequence.watchorder.ui.SettingsScreen
import com.sequence.watchorder.ui.UpcomingScreen
import com.sequence.watchorder.ui.toPalette

@Composable
fun SequenceApp(vm: MainViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }

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
            (vm.detailId != null || vm.settingsOpen || vm.tab != AppTab.HOME),
    ) {
        vm.onBack()
    }

    SequenceTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            AnimatedContent(
                targetState = Triple(vm.detailId, vm.settingsOpen, vm.tab),
                transitionSpec = {
                    val fromDetail = initialState.first != null
                    val toDetail = targetState.first != null
                    when {
                        toDetail -> fadeIn(tween(220)) togetherWith fadeOut(tween(120))
                        fromDetail -> fadeIn(tween(220)) togetherWith fadeOut(tween(120))
                        else -> {
                            val forward = targetState.third.ordinal > initialState.third.ordinal
                            val offset = if (forward) 1 else -1
                            (
                                slideInHorizontally(tween(240)) { it / 12 * offset } + fadeIn(tween(240))
                                ) togetherWith fadeOut(tween(120))
                        }
                    }
                },
                label = "destination",
            ) { (detailId, settingsOpen, _) ->
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

                    else -> MainTabs(vm, snackbarHostState)
                }
            }
        }

        SheetHost(vm)
    }
}

@Composable
private fun MainTabs(vm: MainViewModel, snackbarHostState: SnackbarHostState) {
    val stateHolder = rememberSaveableStateHolder()
    val miniCard = remember(vm.progress, vm.tab) {
        Logic.continueList(vm.catalog, vm.progress, vm.today(), 1).firstOrNull()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column {
                AnimatedVisibility(visible = miniCard != null) {
                    if (miniCard != null) {
                        MiniBar(vm, miniCard)
                    }
                }
                AppNavigationBar(vm)
            }
        },
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
            stateHolder.SaveableStateProvider(vm.tab.name) {
                when (vm.tab) {
                    AppTab.HOME -> HomeScreen(vm)
                    AppTab.LIBRARY -> LibraryScreen(vm)
                    AppTab.UPCOMING -> UpcomingScreen(vm)
                    AppTab.SEARCH -> SearchScreen(vm)
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
    ) {
        for (tab in AppTab.entries) {
            val selected = vm.tab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { vm.selectTab(tab) },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            AppTab.HOME -> SeqIcons.Home
                            AppTab.LIBRARY -> SeqIcons.Library
                            AppTab.UPCOMING -> SeqIcons.Event
                            AppTab.SEARCH -> SeqIcons.Search
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

/** The "keep going" strip that sits above the navigation bar. */
@Composable
private fun MiniBar(vm: MainViewModel, card: ContinueCard) {
    val accent = vm.catalog.accent(card.franchise.id).toPalette()
    val hasNext = card.next != null || card.nextBook != null
    val title = card.next?.title ?: card.nextBook?.title ?: "All caught up"
    val kicker = when {
        card.next != null -> "Up next"
        card.nextBook != null -> "Read next"
        else -> "Finished"
    }

    Surface(
        onClick = { vm.openFranchise(card.franchise.id) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .width(56.dp)
                        .aspectRatio(16f / 10f)
                        .clip(RoundedCornerShape(10.dp)),
                ) {
                    ArtImage(ArtKind.POSTER, card.franchise.id, Modifier.fillMaxSize())
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = card.franchise.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "$kicker · $title",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (hasNext) {
                    SeenToggle(
                        checked = false,
                        size = 40.dp,
                        onToggle = {
                            card.next?.let { vm.toggleSeen(card.franchise.id, it) }
                                ?: card.nextBook?.let { vm.toggleRead(card.franchise.id, it) }
                        },
                    )
                }
            }
            ProgressStrip(
                pct = card.seen.pct,
                color = accent.accent,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 0.dp),
            )
            Box(Modifier.padding(bottom = 8.dp))
        }
    }
}
