package com.thelightphone.wikipedia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightFullscreenModal
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import kotlinx.coroutines.flow.MutableStateFlow
import com.thelightphone.sdk.ui.defaultKeyboardOptions
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable

@InitialScreen
class WikipediaHomeScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, WikipediaViewModel>(sealedActivity) {

    override val viewModelClass: Class<WikipediaViewModel>
        get() = WikipediaViewModel::class.java

    override fun createViewModel(): WikipediaViewModel =
        WikipediaViewModel(lightContext.dataStore)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.uiState.collectAsState()

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                when (val mode = state.mode) {
                    is WikipediaScreenMode.Home -> {
                        HomeContent(
                            onSearchClick = viewModel::openSearch,
                            onSettingsClick = viewModel::openAbout,
                            recentTitles = state.recentTitles,
                            onOpenRecent = viewModel::openArticle,
                            showRecents = state.showRecents,
                        )
                    }

                    is WikipediaScreenMode.SearchInput -> {
                        SearchInputContent(
                            currentQuery = mode.currentQuery,
                            editorKey = mode.editorKey,
                            onSearchSubmitted = { query -> viewModel.submitSearch(query) },
                            onBack = viewModel::cancelSearch,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    is WikipediaScreenMode.Search -> {
                        SearchResultsContent(
                            query = mode.query,
                            results = mode.results,
                            isLoading = mode.isLoading,
                            onSelect = viewModel::selectSearchResult,
                            onBack = viewModel::cancelSearch,
                        )
                    }

                    is WikipediaScreenMode.Loading -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            LightTopBar(
                                center = LightTopBarCenter.Text("Wikipedia"),
                                modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                LightText(
                                    text = mode.message,
                                    variant = LightTextVariant.Detail,
                                    align = TextAlign.Center,
                                    modifier = Modifier.padding(top = 2f.gridUnitsAsDp()),
                                )
                            }
                        }
                    }

                    is WikipediaScreenMode.Article -> {
                        ArticleContent(
                            title = mode.title,
                            description = mode.description,
                            extract = mode.extract,
                            thumbnailUrl = mode.thumbnailUrl,
                            links = mode.links,
                            hasTable = mode.hasTable,
                            tables = mode.tables,
                            isLoading = mode.isLoading,
                            onBack = viewModel::cancelSearch,
                            onSearch = viewModel::openSearch,
                            onOpenSettings = viewModel::openAbout,
                            onOpenLink = viewModel::openLink,
                        )
                    }

                    is WikipediaScreenMode.About -> {
                        AboutContent(
                            onBack = viewModel::closeAbout,
                            onClearRecents = viewModel::openConfirmClearRecents,
                            showRecents = state.showRecents,
                            onToggleShowRecents = viewModel::toggleShowRecents,
                        )
                    }

                    is WikipediaScreenMode.ConfirmClearRecents -> {
                        ConfirmClearRecentsContent(
                            onBack = viewModel::cancelConfirmClearRecents,
                            onConfirm = viewModel::confirmClearRecents,
                        )
                    }
                }

                state.errorModal?.let { message ->
                    LightFullscreenModal(
                        message = message,
                        onClose = viewModel::dismissError,
                    )
                }
            }
        }
    }
}

/**
 * Home screen — gear icon top-right, "Tap to search" centered (matching the
 * Dictionary app), and "Recents" in the bottom-left corner. Tapping "Recents"
 * expands or collapses the recent-articles list beneath it; the list starts
 * collapsed. When the Show recents preference is off, the Recents section is
 * hidden entirely.
 */
@Composable
private fun HomeContent(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    recentTitles: List<String> = emptyList(),
    onOpenRecent: (String) -> Unit = {},
    showRecents: Boolean = true,
) {
    var recentsExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Settings / About — pinned to the very top-right.
        LightTopBar(
            rightButton = LightBarButton.LightIcon(
                icon = LightIcons.SETTINGS,
                onClick = onSettingsClick,
                contentDescription = "About",
            ),
            modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
        )

        // "Tap to search" — centered in the middle of the screen (matches the
        // Dictionary app's home layout).
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            LightText(
                text = "Tap to search",
                variant = LightTextVariant.Subheading,
                modifier = Modifier.lightClickable { onSearchClick() },
            )
        }

        // "Recents" — bottom-left; tap to expand/collapse the recent list.
        if (showRecents) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 2f.gridUnitsAsDp(), end = 2f.gridUnitsAsDp(), bottom = 2f.gridUnitsAsDp()),
            ) {
                LightText(
                    text = "Recents",
                    variant = LightTextVariant.Subheading,
                    modifier = Modifier.lightClickable { recentsExpanded = !recentsExpanded },
                )
                if (recentsExpanded) {
                    recentTitles.forEach { title ->
                        HomeMenuItem(
                            text = title,
                            icon = LightIcons.LIST,
                            onClick = { onOpenRecent(title) },
                            variant = LightTextVariant.Detail,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMenuItem(
    text: String,
    icon: com.thelightphone.sdk.ui.LightIconConfiguration,
    onClick: () -> Unit,
    variant: LightTextVariant = LightTextVariant.Heading,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(vertical = 0.75f.gridUnitsAsDp()),
    ) {
        LightText(
            text = text,
            variant = variant,
        )
    }
}

/**
 * Search input screen using the SDK's LightTextInputEditor — integrates with LP3
 * hardware keyboard and supports D-pad navigation properly.
 * Uses editorKey to force TextFieldState reset on re-entry (same pattern as Amtrak SearchScreen).
 */
@Composable
fun SearchInputContent(
    currentQuery: String,
    editorKey: Any,
    onSearchSubmitted: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textFieldState = rememberTextFieldState(currentQuery)

    LightTextInputEditor(
        title = "Search Wikipedia",
        state = textFieldState,
        onSubmit = { query -> onSearchSubmitted(query.toString()) },
        onBack = onBack,
        keyboardOptionsFlow = MutableStateFlow(defaultKeyboardOptions()),
        modifier = modifier.fillMaxSize(),
        submitLabel = "SEARCH",
        submitIcon = LightIcons.SEARCH,
        editorKey = editorKey,
    )
}
