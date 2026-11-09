package com.thelightphone.wikipedia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import com.thelightphone.sdk.rememberKeyboardOptions
import androidx.compose.runtime.*
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
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.lightClickable
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp

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
                            onSearchSubmitted = { query -> viewModel.submitSearch(query) },
                            onSettingsClick = viewModel::openAbout,
                            onRecentsClick = viewModel::openRecents,
                            recentTitles = state.recentTitles,
                            onOpenRecent = viewModel::openArticle,
                            showRecents = state.showRecents,
                        )
                    }

                    is WikipediaScreenMode.Recents -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            LightTopBar(
                                leftButton = LightBarButton.LightIcon(
                                    icon = LightIcons.BACK,
                                    onClick = viewModel::closeRecents,
                                    contentDescription = "Back",
                                ),
                                center = LightTopBarCenter.Text("Recents"),
                                modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
                            )
                            if (state.recentTitles.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    LightText(
                                        text = "No recents yet.",
                                        variant = LightTextVariant.Copy,
                                        lighten = true,
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(horizontal = 1f.gridUnitsAsDp())
                                        .verticalScroll(rememberScrollState()),
                                ) {
                                    state.recentTitles.forEach { title ->
                                        LightText(
                                            text = title,
                                            variant = LightTextVariant.Copy,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .lightClickable(onClick = { viewModel.openArticle(title) })
                                                .padding(vertical = 0.75f.gridUnitsAsDp()),
                                        )
                                    }
                                }
                            }
                        }
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
                                    variant = LightTextVariant.Subheading,
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

    /**
     * Home screen — opens straight into the search field (no "Tap to search"
     * intermediate screen). Settings lives in the bottom-left (native LightOS
     * convention) instead of the top-right, and the Recents section heading is
     * rendered fully uppercase "RECENTS" for tool-library consistency.
     */
    @Composable
    private fun HomeContent(
        onSearchSubmitted: (String) -> Unit,
        onSettingsClick: () -> Unit,
        onRecentsClick: () -> Unit,
        recentTitles: List<String> = emptyList(),
        onOpenRecent: (String) -> Unit = {},
        showRecents: Boolean = true,
    ) {
        var recentsExpanded by remember { mutableStateOf(false) }
        val searchState = remember { TextFieldState("") }
        val kbFlow = rememberKeyboardOptions()

        LightTheme(colors = LightThemeController.colors.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                // Search editor — title states the action ("Search"), the field is
                // the primary interaction.
                LightTextInputEditor(
                    title = "Search Wikipedia",
                    state = searchState,
                    keyboardOptionsFlow = kbFlow,
                    onSubmit = { result: CharSequence ->
                        val q = result.toString().trim()
                        if (q.isNotBlank()) {
                            onSearchSubmitted(q)
                        }
                    },
                    onBack = { /* stay on this screen */ },
                    submitIcon = LightIcons.SEARCH,
                    singleLine = true,
                    leftBottomBarItem = LightBarButton.LightIcon(
                        icon = LightIcons.SETTINGS,
                        onClick = onSettingsClick,
                        contentDescription = "Settings",
                    ),
                    modifier = Modifier.fillMaxSize(),
                )

                // Recents icon in bottom right.
                if (showRecents) {
                    LightIcon(
                        icon = LightIcons.LIST,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 2f.gridUnitsAsDp(), bottom = 1f.gridUnitsAsDp())
                            .clickable(onClick = { onRecentsClick() }),
                        contentDescription = "Recents",
                    )
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

    /** Search input screen using the SDK's LightTextInputEditor. */
    @Composable
    fun SearchInputContent(
        currentQuery: String,
        editorKey: Any,
        onSearchSubmitted: (String) -> Unit,
        onBack: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val textFieldState = remember { TextFieldState(currentQuery) }
        val kbFlow = rememberKeyboardOptions()

        LightTextInputEditor(
            title = "Search Wikipedia",
            state = textFieldState,
            onSubmit = { query -> onSearchSubmitted(query.toString()) },
            onBack = onBack,
            keyboardOptionsFlow = kbFlow,
            modifier = modifier.fillMaxSize(),
            submitLabel = "SEARCH",
            submitIcon = LightIcons.SEARCH,
            editorKey = editorKey,
        )
    }
}
