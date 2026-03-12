package com.example.module4

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream

@Serializable
data class Repository(
    val id: Long,
    val full_name: String,
    val description: String?,
    val stargazers_count: Int,
    val language: String?
)

class RepoLoader(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private var allRepos: List<Repository> = emptyList()

    suspend fun loadRepos(): List<Repository> {
        if (allRepos.isNotEmpty()) return allRepos

        return withContext(Dispatchers.IO) {
            val inputStream: InputStream = context.assets.open("repos.json")
            allRepos = json.decodeFromStream<List<Repository>>(inputStream)
            allRepos
        }
    }

    suspend fun search(query: String): List<Repository> {
        delay(800)
        if (query.isBlank()) return emptyList()

        return withContext(Dispatchers.Default) {
            val lowerQuery = query.lowercase()
            allRepos.filter { repo ->
                repo.full_name.lowercase().contains(lowerQuery) ||
                        (repo.description?.lowercase()?.contains(lowerQuery) == true) ||
                        (repo.language?.lowercase()?.contains(lowerQuery) == true)
            }
        }
    }
}

class SearchViewModel(private val repoLoader: RepoLoader) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<Repository>>(emptyList())
    val results: StateFlow<List<Repository>> = _results.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch { repoLoader.loadRepos() }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            delay(500)
            if (newQuery.isBlank()) {
                _results.value = emptyList()
                _loading.value = false
                return@launch
            }

            _loading.value = true
            _results.value = repoLoader.search(newQuery)
            _loading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: SearchViewModel = viewModel()) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val loading by viewModel.loading.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Поиск репозиториев") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Введите название...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        Icon(
                            Icons.Default.Clear,
                            null,
                            modifier = Modifier.clickable { viewModel.onQueryChange("") }
                        )
                    }
                },
                singleLine = true
            )

            if (loading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (results.isEmpty() && !loading && query.isNotBlank()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Ничего не найдено")
                        }
                    }
                }

                items(results) { repo ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(repo.full_name, style = MaterialTheme.typography.titleMedium)

                            if (!repo.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(repo.description!!, style = MaterialTheme.typography.bodyMedium)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (!repo.language.isNullOrBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            repo.language!!,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                Text("Stars: ${repo.stargazers_count}")
                            }
                        }
                    }
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repoLoader = RepoLoader(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: SearchViewModel = viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return SearchViewModel(repoLoader) as T
                            }
                        }
                    )
                    SearchScreen(viewModel)
                }
            }
        }
    }
}