package com.example.module4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream

@Serializable
data class Post(
    val id: Int,
    val userId: Int,
    val title: String,
    val body: String,
    val avatarUrl: String
)

@Serializable
data class Comment(
    val postId: Int,
    val id: Int,
    val name: String,
    val body: String
)

sealed class LoadState {
    object Loading : LoadState()
    object Ready : LoadState()
    data class Error(val message: String) : LoadState()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SocialFeed() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialFeed() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val posts = remember { mutableStateListOf<Post>() }
    val avatarStates = remember { mutableStateMapOf<Int, LoadState>() }
    val commentStates = remember { mutableStateMapOf<Int, LoadState>() }
    val comments = remember { mutableStateMapOf<Int, List<Comment>>() }
    var job by remember { mutableStateOf<Job?>(null) }

    fun loadPosts() {
        job?.cancel()
        job = scope.launch {
            try {
                val loadedPosts = withContext(Dispatchers.IO) {
                    val input: InputStream = context.assets.open("social_posts.json")
                    Json.decodeFromStream<List<Post>>(input)
                }
                posts.clear()
                posts.addAll(loadedPosts)
                loadedPosts.forEach { post ->
                    avatarStates[post.id] = LoadState.Loading
                    commentStates[post.id] = LoadState.Loading
                    launch {
                        supervisorScope {
                            val avatarDeferred = async {
                                try {
                                    delay(1000L + (0..500L).random())
                                    LoadState.Ready
                                } catch (e: Exception) {
                                    LoadState.Error("Ошибка аватара")
                                }
                            }
                            val commentsDeferred = async {
                                try {
                                    delay(1500L + (0..500L).random())
                                    val allComments = withContext(Dispatchers.IO) {
                                        val input: InputStream = context.assets.open("comments.json")
                                        Json.decodeFromStream<List<Comment>>(input)
                                    }
                                    val postComments = allComments.filter { it.postId == post.id }
                                    comments[post.id] = postComments
                                    LoadState.Ready
                                } catch (e: Exception) {
                                    LoadState.Error("Ошибка комментариев")
                                }
                            }
                            avatarStates[post.id] = avatarDeferred.await()
                            commentStates[post.id] = commentsDeferred.await()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    LaunchedEffect(Unit) { loadPosts() }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Социальная лента") },
                    actions = {
                        Button(
                            onClick = { loadPosts() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Обновить")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFFE0E0E0),
                        titleContentColor = Color.Black
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(posts) { post ->
                    PostCard(
                        post = post,
                        avatarState = avatarStates[post.id] ?: LoadState.Loading,
                        commentState = commentStates[post.id] ?: LoadState.Loading,
                        comments = comments[post.id] ?: emptyList()
                    )
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    avatarState: LoadState,
    commentState: LoadState,
    comments: List<Comment>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            when (avatarState) {
                                LoadState.Loading -> Color.LightGray
                                is LoadState.Error -> Color.Red
                                LoadState.Ready -> Color.Gray
                            }
                        )
                ) {
                    when (avatarState) {
                        LoadState.Loading -> CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.Center)
                        )
                        is LoadState.Error -> Text("Error", modifier = Modifier.align(Alignment.Center))
                        LoadState.Ready -> AsyncImage(
                            model = post.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.Black
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = post.body,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(modifier = Modifier.padding(start = 8.dp)) {
                when (commentState) {
                    LoadState.Loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Загрузка комментариев...")
                        }
                    }
                    is LoadState.Error -> Text("Ошибка загрузки комментариев")
                    LoadState.Ready -> {
                        Text(
                            "Комментарии (${comments.size}):",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.DarkGray
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        comments.forEach { comment ->
                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    text = comment.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = comment.body,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Divider(modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}