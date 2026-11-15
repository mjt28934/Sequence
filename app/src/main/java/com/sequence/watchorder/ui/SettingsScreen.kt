package com.sequence.watchorder.ui

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sequence.watchorder.BuildConfig
import com.sequence.watchorder.MainViewModel
import com.sequence.watchorder.Sheet
import com.sequence.watchorder.data.LibView
import com.sequence.watchorder.data.Logic
import com.sequence.watchorder.data.OrderMode
import com.sequence.watchorder.data.StartTab
import com.sequence.watchorder.data.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BACKUP_NAME = "sequence-progress.json"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: MainViewModel,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Paparazzi (static renders) has no Activity to host a file picker; the rows stay visible
    // and simply do nothing there. On a phone the registry owner is always present.
    val hasResultHost = LocalActivityResultRegistryOwner.current != null

    // The Storage Access Framework needs no permission: the user picks the exact file.
    val exportLauncher = if (!hasResultHost) {
        null
    } else {
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            if (uri != null) {
                val text = vm.exportProgress()
                if (text == null) {
                    vm.notify("Nothing to export yet")
                } else {
                    scope.launch {
                        val written = withContext(Dispatchers.IO) {
                            runCatching {
                                context.contentResolver.openOutputStream(uri)?.use { out ->
                                    out.write(text.toByteArray(Charsets.UTF_8))
                                } ?: error("no output stream")
                            }.isSuccess
                        }
                        vm.notify(if (written) "Progress exported" else "Could not write that file")
                    }
                }
            }
        }
    }

    val importLauncher = if (!hasResultHost) {
        null
    } else {
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    val text = withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver.openInputStream(uri)?.use {
                                it.reader(Charsets.UTF_8).readText()
                            }
                        }.getOrNull()
                    }
                    when {
                        text == null -> vm.notify("Could not read that file")
                        !vm.importProgress(text) -> vm.notify("That is not a Sequence backup")
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    BarIconButton(SeqIcons.Back, "Back") { vm.closeSettings() }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 32.dp,
                ),
        ) {
            SettingsDivider(first = true)
            SettingsGroup("General")
            SettingsGroupLabel("Start tab")
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                StartTab.entries.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = vm.progress.startTab == tab,
                        onClick = { vm.setStartTab(tab) },
                        shape = SegmentedButtonDefaults.itemShape(index, StartTab.entries.size),
                    ) { Text(tab.label) }
                }
            }
            Text(
                "The tab Sequence opens on.",
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SheetToggleRow(
                title = "Hide finished franchises",
                subtitle = "Leave franchises you have completed out of the Home A\u2013Z grid",
                checked = vm.progress.hideFinished,
                onCheckedChange = { vm.setHideFinished(it) },
            )

            SettingsDivider()
            SettingsGroup("Appearance")
            SettingsGroupLabel("Theme")
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = vm.progress.theme == mode,
                        onClick = { vm.setTheme(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                    ) { Text(mode.label) }
                }
            }
            Text(
                "System follows your phone's dark mode.",
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SheetToggleRow(
                title = "Vibration",
                subtitle = "A short buzz when you mark a title",
                checked = vm.progress.haptics,
                onCheckedChange = { vm.setHaptics(it) },
            )
            SheetToggleRow(
                title = "Reduce motion",
                subtitle = "Skip screen transitions and entrance animations",
                checked = vm.progress.reduceMotion,
                onCheckedChange = { vm.setReduceMotion(it) },
            )

            SettingsDivider()
            SettingsGroup("Library")
            SettingsGroupLabel("Layout")
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                val views = LibView.entries
                views.forEachIndexed { index, view ->
                    SegmentedButton(
                        selected = vm.progress.libView == view,
                        onClick = { vm.setLibView(view) },
                        shape = SegmentedButtonDefaults.itemShape(index, views.size),
                    ) { Text(if (view == LibView.GRID) "Grid" else "List") }
                }
            }
            Spacer(Modifier.height(8.dp))
            SheetActionRow(
                icon = SeqIcons.Sort,
                title = "Sort: ${vm.progress.libSort.label}",
                subtitle = "Changes the order of the Library",
                onClick = { vm.openSheet(Sheet.Sort) },
            )

            SettingsDivider()
            SettingsGroup("Watching order")
            Text(
                "Applies everywhere in the app: the order list on each franchise, the " +
                    "continue shelf and the up-next queue.",
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SheetOptionRow(
                title = "Release date",
                subtitle = "Titles in the order they premiered",
                selected = vm.progress.mode == OrderMode.RELEASE,
                onClick = { vm.setMode(OrderMode.RELEASE) },
            )
            SheetOptionRow(
                title = "Story order",
                subtitle = "Titles in the order the story happens",
                selected = vm.progress.mode == OrderMode.STORY,
                onClick = { vm.setMode(OrderMode.STORY) },
            )

            SettingsDivider()
            SettingsGroup("Your data")
            SheetActionRow(
                icon = SeqIcons.Upload,
                title = "Export progress",
                subtitle = "Save a $BACKUP_NAME backup you can keep or move",
                onClick = { exportLauncher?.launch(BACKUP_NAME) },
            )
            SheetActionRow(
                icon = SeqIcons.Download,
                title = "Import progress",
                subtitle = "Replace everything with a backup file",
                onClick = { importLauncher?.launch(arrayOf("application/json", "text/plain")) },
            )
            SheetActionRow(
                icon = SeqIcons.Trash,
                title = "Clear all progress",
                subtitle = "Removes every seen and read mark and your saved franchises",
                danger = true,
                onClick = { vm.openSheet(Sheet.ConfirmClearAll) },
            )
            SheetActionRow(
                icon = SeqIcons.Refresh,
                title = "Reset settings",
                subtitle = "Back to the defaults; your progress stays",
                onClick = { vm.resetSettings() },
            )

            SettingsDivider()
            SettingsGroup("About")
            SettingsNote(
                "Sequence ${BuildConfig.VERSION_NAME}. Dates and orders last checked " +
                    "${Logic.formatDate(vm.catalog.dataAsOf)}. Titles dated in the future are " +
                    "listed as upcoming and do not count toward progress.",
            )
            Spacer(Modifier.height(12.dp))
            SettingsNote(
                "Everything works offline. Progress is saved only on this phone. Posters and " +
                    "covers come from each title's Wikipedia article and stay bundled; the art " +
                    "belongs to its studios and publishers.",
            )
            Spacer(Modifier.height(12.dp))
            SettingsNote(
                "${vm.catalog.franchises.size} franchises · " +
                    "${vm.catalog.totalEntries()} films and series · " +
                    "no network permission",
            )
        }
    }
}

@Composable
private fun SettingsDivider(first: Boolean = false) {
    HorizontalDivider(
        modifier = Modifier.padding(top = if (first) 0.dp else 20.dp, bottom = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
private fun SettingsGroup(label: String) {
    Text(
        label,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingsGroupLabel(label: String) {
    Text(
        label,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SettingsNote(text: String) {
    Text(
        text,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
