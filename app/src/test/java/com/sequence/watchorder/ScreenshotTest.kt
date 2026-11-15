package com.sequence.watchorder

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.sequence.watchorder.data.Catalog
import com.sequence.watchorder.data.LibView
import com.sequence.watchorder.data.OrderMode
import com.sequence.watchorder.data.ProgressState
import com.sequence.watchorder.ui.ArtKind
import com.sequence.watchorder.ui.ArtRepository
import com.sequence.watchorder.ui.DetailScreen
import com.sequence.watchorder.ui.FallbackDark
import com.sequence.watchorder.ui.FallbackLight
import com.sequence.watchorder.ui.HomeScreen
import com.sequence.watchorder.ui.LibraryScreen
import com.sequence.watchorder.ui.SearchScreen
import com.sequence.watchorder.ui.SettingsScreen
import com.sequence.watchorder.ui.UpcomingScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Renders the real screens with the real catalogue on the JVM, so the design can be reviewed
 * without a device. Run with -Ppaparazzi.test.record=true to write the PNGs.
 */
class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        showSystemUi = false,
    )

    private val assets = File("src/main/assets")
    private val catalog: Catalog = Catalog.parse(File(assets, "sequence.json").readText())

    @Before
    fun setUpArt() {
        ArtRepository.overrideLoader = { kind, id ->
            val folder = if (kind == ArtKind.POSTER) "posters" else "covers"
            val file = File(assets, "art/$folder/$id.webp")
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
        }
        catalog.franchises.forEach { ArtRepository.warm(ArtKind.POSTER, it.id) }
    }

    private fun fixture(): ProgressState {
        val starWars = listOf("sw-acolyte", "sw-1", "sw-2")
        val mcu = listOf("mcu-eow", "mcu-ca1", "mcu-cm")
        return ProgressState(
            watched = mapOf("star-wars" to starWars, "mcu" to mcu),
            read = mapOf("wizarding-world" to listOf("bk-ww-1", "bk-ww-2")),
            mode = OrderMode.RELEASE,
            saved = listOf("star-wars", "mcu", "alien", "middle-earth"),
            recent = listOf("star-wars", "mcu", "middle-earth"),
            libView = LibView.GRID,
        )
    }

    private fun vm(state: ProgressState = fixture()): MainViewModel =
        MainViewModel(catalog, state)

    private fun shot(
        name: String,
        scheme: ColorScheme = FallbackDark,
        content: @Composable () -> Unit,
    ) {
        paparazzi.snapshot(name) {
            MaterialTheme(colorScheme = scheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    content()
                }
            }
        }
    }

    @Test
    fun home() {
        shot("home") { HomeScreen(vm()) }
    }

    @Test
    fun homeLight() {
        shot("home-light", FallbackLight) { HomeScreen(vm()) }
    }

    @Test
    fun shell() {
        val model = vm()
        shot("shell") {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) { HomeScreen(model) }
                AppNavigationBar(model)
            }
        }
    }

    @Test
    fun libraryGrid() {
        val model = vm().apply { selectTab(AppTab.LIBRARY) }
        shot("library-grid") { LibraryScreen(model) }
    }

    @Test
    fun libraryList() {
        val model = vm().apply {
            selectTab(AppTab.LIBRARY)
            toggleLibView()
        }
        shot("library-list") { LibraryScreen(model) }
    }

    @Test
    fun upcoming() {
        val model = vm().apply { selectTab(AppTab.UPCOMING) }
        shot("upcoming") { UpcomingScreen(model) }
    }

    @Test
    fun searchBrowse() {
        val model = vm().apply { selectTab(AppTab.SEARCH) }
        shot("search-browse") { SearchScreen(model) }
    }

    @Test
    fun searchResults() {
        val model = vm().apply {
            selectTab(AppTab.SEARCH)
            updateQuery("star")
        }
        shot("search-results") { SearchScreen(model) }
    }

    @Test
    fun detailWatch() {
        val model = vm().apply { openFranchise("star-wars") }
        ArtRepository.warm(ArtKind.POSTER, "star-wars")
        catalog.worksFor("star-wars").forEach { ArtRepository.warm(ArtKind.COVER, it.id) }
        val franchise = catalog.franchise("star-wars")!!
        shot("detail-watch") { DetailScreen(model, franchise) }
    }

    @Test
    fun detailBooks() {
        val model = vm().apply { openFranchise("wizarding-world", books = true) }
        ArtRepository.warm(ArtKind.POSTER, "wizarding-world")
        catalog.worksFor("wizarding-world").forEach { ArtRepository.warm(ArtKind.COVER, it.id) }
        val franchise = catalog.franchise("wizarding-world")!!
        shot("detail-books") { DetailScreen(model, franchise) }
    }

    @Test
    fun settings() {
        val model = vm().apply { openSettings() }
        shot("settings") { SettingsScreen(model) }
    }
}
