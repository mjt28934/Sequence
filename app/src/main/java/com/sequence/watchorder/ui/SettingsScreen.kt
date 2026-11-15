package com.sequence.watchorder.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sequence.watchorder.BuildConfig
import com.sequence.watchorder.MainViewModel
import com.sequence.watchorder.Sheet
import com.sequence.watchorder.data.Logic
import com.sequence.watchorder.data.OrderMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
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
                .verticalScroll(rememberScrollState())
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 32.dp,
                ),
        ) {
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
                icon = SeqIcons.Trash,
                title = "Clear all progress",
                danger = true,
                onClick = { vm.openSheet(Sheet.ConfirmClearAll) },
            )
            Text(
                "Removes everything marked seen or read, and your saved franchises.",
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
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
private fun SettingsNote(text: String) {
    Text(
        text,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
