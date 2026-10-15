package com.thelightphone.wikipedia

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

sealed class WikipediaScreenMode {
    /** Initial home screen — search + recents. */
    data object Home : WikipediaScreenMode()

    data class SearchInput(
        val currentQuery: String = "",
        val editorKey: Any = System.nanoTime(),
    ) : WikipediaScreenMode()

    data class Search(
        val query: String = "",
        val results: List<WikiSearchResult> = emptyList(),
        val isLoading: Boolean = false,
    ) : WikipediaScreenMode()

    data class Loading(val message: String) : WikipediaScreenMode()

    data class Article(
        val title: String,
        val description: String? = null,
        val extract: String = "",
        val thumbnailUrl: String? = null,
        val links: List<String> = emptyList(),
        val hasTable: Boolean = false,
        val tables: List<WikiTable> = emptyList(),
        val isLoading: Boolean = false,
    ) : WikipediaScreenMode()

    /** About / attribution screen. */
    data object About : WikipediaScreenMode()

    /** Confirmation screen before clearing recent articles. */
    data object ConfirmClearRecents : WikipediaScreenMode()
}

data class WikipediaUiState(
    val mode: WikipediaScreenMode = WikipediaScreenMode.Home,
    val errorModal: String? = null,
    val recentTitles: List<String> = emptyList(),
    val showRecents: Boolean = true,
)

private const val NETWORK_ERROR_MESSAGE =
    "Wikipedia requires a network connection. Please insert a data sim or connect to wi-fi."

private const val MAX_RECENT = 10

private val MIN_LOADING_DISPLAY = 1.seconds

internal object WikipediaPreferences {
    val LAST_SEARCH_QUERY = stringPreferencesKey("wiki_last_search_query")
    val LAST_ARTICLE_TITLE = stringPreferencesKey("wiki_last_article_title")
    val RECENT_TITLES = stringPreferencesKey("wiki_recent_titles")
    val SHOW_RECENTS = booleanPreferencesKey("wiki_show_recents")
}

class WikipediaViewModel(
    private val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
) : LightViewModel<Unit>() {

    private val api = WikipediaApi()

    private val _uiState = MutableStateFlow(WikipediaUiState())
    val uiState: StateFlow<WikipediaUiState> = _uiState.asStateFlow()

    private val apiExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is CancellationException) return@CoroutineExceptionHandler
        val message = throwable.message ?: "Unexpected error"
        viewModelScope.launch(Dispatchers.Main) {
            updateErrorModal(message)
        }
    }

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        // Load recent article titles and preferences from DataStore on first show
        if (_uiState.value.recentTitles.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO + apiExceptionHandler) {
                val prefs = dataStore.data.first()
                val recent = prefs[WikipediaPreferences.RECENT_TITLES]
                    ?.split("|")
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()
                val showRecents = prefs[WikipediaPreferences.SHOW_RECENTS] ?: true
                _uiState.value = _uiState.value.copy(
                    recentTitles = recent,
                    showRecents = showRecents,
                )
                // One-shot launch self-test marker — confirms the Wikipedia tool
                // screen shows and prefs load on the LightOS emulator. Matts reads
                // this via `adb logcat WikiDebug:V` (he self-navigates the UI).
                Log.d(
                    "WikiDebug",
                    "LAUNCH_OK mode=Home showRecents=$showRecents recents=${recent.size}",
                )
            }
        }
    }

    // === Home screen actions ===

    fun openSearch() {
        _uiState.value = _uiState.value.copy(
            mode = WikipediaScreenMode.SearchInput(
                currentQuery = "",
                editorKey = System.nanoTime(),
            ),
            errorModal = null,
        )
    }

    // === Search ===

    fun submitSearch(query: CharSequence) {
        val q = query.toString().trim()
        if (q.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO + apiExceptionHandler) {
            val loadStart = Clock.System.now()
            setState(WikipediaScreenMode.Search(query = q, results = emptyList(), isLoading = true))
            api.search(q).fold(
                onSuccess = { results ->
                    setState(
                        WikipediaScreenMode.Search(
                            query = q,
                            results = results,
                            isLoading = false,
                        ),
                    )
                },
                onFailure = {
                    showError(NETWORK_ERROR_MESSAGE)
                },
            )
            // Enforce minimum loading display
            val remaining = MIN_LOADING_DISPLAY - (Clock.System.now() - loadStart)
            if (remaining.isPositive()) delay(remaining)

            // Persist search query to DataStore
            dataStore.edit { prefs ->
                prefs[WikipediaPreferences.LAST_SEARCH_QUERY] = q
            }
        }
    }

    fun cancelSearch() {
        _uiState.value = _uiState.value.copy(
            mode = WikipediaScreenMode.Home,
            errorModal = null,
        )
    }

    fun selectSearchResult(result: WikiSearchResult) {
        openArticle(result.title)
    }

    // === Article view ===

    fun openArticle(title: String) {
        viewModelScope.launch(Dispatchers.IO + apiExceptionHandler) {
            val loadStart = Clock.System.now()
            setState(
                WikipediaScreenMode.Article(
                    title = title,
                    isLoading = true,
                    extract = "",
                    links = emptyList(),
                ),
            )

            // Fetch summary (description + thumbnail) and extract (full text) and links
            val summary = api.fetchSummary(title).getOrNull()
            val extract = api.fetchExtract(title).getOrElse { "" }
            val links = api.fetchLinks(title).getOrElse { emptyList() }
            // Tables/lists are dropped by the plain-text extract, so parse the raw
            // wikitext and render those tables inline in the article.
            val rawWikitext = api.fetchRawExtract(title).getOrElse { "" }
            val tables = api.parseTables(rawWikitext)
            val hasTable = tables.isNotEmpty()

            setState(
                WikipediaScreenMode.Article(
                    title = title,
                    description = summary?.description,
                    extract = extract,
                    thumbnailUrl = summary?.thumbnailUrl,
                    links = links,
                    hasTable = hasTable,
                    tables = tables,
                    isLoading = false,
                ),
            )

            // Enforce minimum loading display
            val remaining = MIN_LOADING_DISPLAY - (Clock.System.now() - loadStart)
            if (remaining.isPositive()) delay(remaining)

            // Persist last article title to DataStore
            dataStore.edit { prefs ->
                prefs[WikipediaPreferences.LAST_ARTICLE_TITLE] = title
            }

            // Update recent titles (most-recent first, dedupe, cap at MAX_RECENT)
            val updated = (listOf(title) + _uiState.value.recentTitles)
                .distinct()
                .take(MAX_RECENT)
            _uiState.value = _uiState.value.copy(recentTitles = updated)
            dataStore.edit { prefs ->
                prefs[WikipediaPreferences.RECENT_TITLES] = updated.joinToString("|")
            }
        }
    }

    fun openLink(title: String) {
        openArticle(title)
    }

    // === About ===

    fun openAbout() {
        _uiState.value = _uiState.value.copy(mode = WikipediaScreenMode.About)
    }

    fun closeAbout() {
        _uiState.value = _uiState.value.copy(mode = WikipediaScreenMode.Home)
    }

    fun toggleShowRecents() {
        viewModelScope.launch(Dispatchers.IO + apiExceptionHandler) {
            val newValue = !_uiState.value.showRecents
            _uiState.value = _uiState.value.copy(showRecents = newValue)
            dataStore.edit { prefs ->
                prefs[WikipediaPreferences.SHOW_RECENTS] = newValue
            }
        }
    }

    fun openConfirmClearRecents() {
        _uiState.value = _uiState.value.copy(mode = WikipediaScreenMode.ConfirmClearRecents)
    }

    fun cancelConfirmClearRecents() {
        _uiState.value = _uiState.value.copy(mode = WikipediaScreenMode.About)
    }

    fun confirmClearRecents() {
        viewModelScope.launch(Dispatchers.IO + apiExceptionHandler) {
            _uiState.value = _uiState.value.copy(
                recentTitles = emptyList(),
                mode = WikipediaScreenMode.About,
            )
            dataStore.edit { prefs ->
                prefs.remove(WikipediaPreferences.RECENT_TITLES)
            }
        }
    }

    // === Error handling ===

    fun dismissError() {
        val currentMode = _uiState.value.mode
        _uiState.value = _uiState.value.copy(errorModal = null)
        // Return to home if we were in an error state
        if (currentMode is WikipediaScreenMode.Loading) {
            _uiState.value = _uiState.value.copy(mode = WikipediaScreenMode.Home)
        }
    }

    private suspend fun updateErrorModal(message: String?) {
        _uiState.value = _uiState.value.copy(errorModal = message)
    }

    private suspend fun setState(mode: WikipediaScreenMode) {
        _uiState.value = _uiState.value.copy(mode = mode, errorModal = null)
    }

    private suspend fun showError(message: String) {
        _uiState.value = _uiState.value.copy(errorModal = message)
    }
}
