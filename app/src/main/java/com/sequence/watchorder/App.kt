package com.sequence.watchorder

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sequence.watchorder.data.ContinueCard
import com.sequence.watchorder.data.Logic
import com.sequence.watchorder.data.ThemeMode
import com.sequence.watchorder.ui.ArtImage
import com.sequence.watchorder.ui.ArtKind
import com.sequence.watchorder.ui.Haptics
import com.sequence.watchorder.ui.ProgressStrip
import com.sequence.watchorder.ui.SeenToggle
import com.sequence.watchorder.ui.SeqIcons
import com.sequence.watchorder.ui.SeqSnackbar
import com.sequence.watchorder.ui.SequenceTheme
import com.sequence.watchorder.ui.DetailScreen
import com.sequence.watchorder.ui.HomeScreen
import com.sequence.watchorder.ui.LibraryScreen
import com.sequence.watchorder.ui.Motion
import com.sequence.watchorder.ui.SectionAxis
import com.sequence.watchorder.ui.SectionSwitcher
import com.sequence.watchorder.ui.SettingsScreen
import com.sequence.watchorder.ui.TagsScreen
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
                target = Triple(vm.detailId, vm.settingsOpen, vm.tab),
                modifier = Modifier.fillMaxSize(),
                axis = SectionAxis.HORIZONTAL,
                direction = { from, to ->
                    when {
                        // Going deeper: the new screen owns the forward move.
                        to.first != null && from.first == null -> 1
                        from.first != null && to.first == null -> -1
                        to.second && !from.second -> 1
                        from.second && !to.second -> -1
                        to.third.ordinal >= from.third.ordinal -> 1
                        else -> -1
                    }
                },
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
        bottomBar = { AppBottomBar(vm, miniCard) },
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
                    AppTab.TAGS -> TagsScreen(vm)
                }
            }
        }
    }
}

/** Everything pinned under the content: the continue strip, then the navigation bar. */
@Composable
internal fun AppBottomBar(vm: MainViewModel, miniCard: ContinueCard?) {
    Column {
        if (Motion.enabled) {
            AnimatedVisibility(
                visible = miniCard != null,
                enter = fadeIn(tween(200)) + expandVertically(tween(280), expandFrom = Alignment.Bottom),
                exit = fadeOut(tween(140)) + shrinkVertically(tween(220), shrinkTowards = Alignment.Bottom),
            ) {
                if (miniCard != null) {
                    MiniBar(vm, miniCard)
                }
            }
        } else if (miniCard != null) {
            // Settled state: a snapshot has no time to run an enter transition.
            MiniBar(vm, miniCard)
        }
        AppNavigationBar(vm)
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
                    val pop by animateFloatAsState(
                        targetValue = if (Motion.enabled && selected) 1.12f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f),
                        label = "navIcon",
                    )
                    Icon(
                        imageVector = when (tab) {
                            AppTab.HOME -> SeqIcons.Home
                            AppTab.LIBRARY -> SeqIcons.Library
                            AppTab.UPCOMING -> SeqIcons.Event
                            AppTab.TAGS -> SeqIcons.Tags
                        },
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = pop
                                scaleY = pop
                            },
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
            // Sliding the strip's contents, rather than snapping them, when the next title changes.
            if (Motion.enabled) {
                AnimatedContent(
                    targetState = card.franchise.id,
                    transitionSpec = {
                        (
                            slideInVertically(tween(240)) { it / 3 } + fadeIn(tween(240))
                            ) togetherWith (
                            slideOutVertically(tween(160)) { -it / 3 } + fadeOut(tween(120))
                            )
                    },
                    label = "miniBar",
                ) { shownId ->
                    MiniBarRow(card, shownId, kicker, title, hasNext) {
                        card.next?.let { vm.toggleSeen(card.franchise.id, it) }
                            ?: card.nextBook?.let { vm.toggleRead(card.franchise.id, it) }
                    }
                }
            } else {
                // Reduce motion: the new title replaces the old one without a slide.
                MiniBarRow(card, card.franchise.id, kicker, title, hasNext) {
                    card.next?.let { vm.toggleSeen(card.franchise.id, it) }
                        ?: card.nextBook?.let { vm.toggleRead(card.franchise.id, it) }
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

/** One line of the mini bar: poster, franchise, what is next, and the tick. */
@Composable
private fun MiniBarRow(
    card: ContinueCard,
    shownId: String,
    kicker: String,
    title: String,
    hasNext: Boolean,
    onToggle: () -> Unit,
) {
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
            // Keeps the outgoing poster with the outgoing text during the slide.
            ArtImage(ArtKind.POSTER, shownId, Modifier.fillMaxSize())
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
            SeenToggle(checked = false, size = 40.dp, onToggle = onToggle)
        }
    }
}
