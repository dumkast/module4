package com.example.module4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.security.MessageDigest

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val timeoutSeconds = 10L

            val result = withTimeoutOrNull(timeoutSeconds * 1000) {
                val jsonFiles = findJsonFiles("")

                val hashes = jsonFiles.map { path ->
                    async {
                        path to sha256(path)
                    }
                }.awaitAll()

                hashes.groupBy({ it.second }, { it.first })
                    .filter { it.value.size > 1 }
            }

            if (result == null) {
                println("Поиск прерван по таймауту")
            } else if (result.isEmpty()) {
                println("Дубликаты не найдены")
            } else {
                result.values.forEach { group ->
                    println("Группа дубликатов:")
                    group.forEach { println(it) }
                }
            }
        }
    }

    private fun findJsonFiles(path: String): List<String> {
        val result = mutableListOf<String>()
        val list = assets.list(path) ?: return emptyList()

        for (name in list) {
            val fullPath = if (path.isEmpty()) name else "$path/$name"
            if ((assets.list(fullPath)?.isNotEmpty() == true)) {
                result += findJsonFiles(fullPath)
            } else if (name.endsWith(".json")) {
                result += fullPath
            }
        }
        return result
    }

    private suspend fun sha256(path: String): String = withContext(Dispatchers.IO) {
        val bytes = assets.open(path).use { it.readBytes() }
        val digest = MessageDigest.getInstance("SHA-256")
        digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }
}