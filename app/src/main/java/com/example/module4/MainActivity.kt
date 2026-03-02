package com.example.module4

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadAllData()
    }

    private fun loadAllData() {
        CoroutineScope(Dispatchers.IO).launch {
            val totalTime = measureTimeMillis {
                val usersDeferred = async {
                    try {
                        loadUsers()
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Ошибка при загрузке пользователей")
                        null
                    }
                }

                val salesDeferred = async {
                    try {
                        loadSales()
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Ошибка при загрузке продаж")
                        null
                    }
                }

                val weatherDeferred = async {
                    try {
                        loadWeather()
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Ошибка при загрузке погоды")
                        null
                    }
                }

                val users = usersDeferred.await()
                val sales = salesDeferred.await()
                val weather = weatherDeferred.await()

                println("\nРЕЗУЛЬТАТЫ:")
                println("Пользователи: ${users ?: "ошибка"}")
                println("Продажи: ${sales ?: "ошибка"}")
                println("Погода: ${weather ?: "ошибка"}")
            }
            println("Время: ${totalTime / 1000.0} сек")
        }
    }

    private fun readFile(fileName: String): String {
        return assets.open(fileName).bufferedReader().use { it.readText() }
    }

    private suspend fun loadUsers(): List<String> {
        delay(1800)
        if (Random().nextInt(100) < 30) throw Exception("Ошибка users")

        val json = JSONArray(readFile("users.json"))
        val names = mutableListOf<String>()
        for (i in 0 until json.length()) {
            names.add(json.getJSONObject(i).getString("name"))
        }
        return names
    }

    private suspend fun loadSales(): Map<String, Int> {
        delay(1200)
        if (Random().nextInt(100) < 20) throw Exception("Ошибка sales")

        val json = JSONObject(readFile("sales.json"))
        val items = json.getJSONArray("items")
        val map = mutableMapOf<String, Int>()
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            map[item.getString("product")] = item.getInt("qty")
        }
        return map
    }

    private suspend fun loadWeather(): List<String> {
        delay(2500)
        if (Random().nextInt(100) < 25) throw Exception("Ошибка weather")

        val json = JSONArray(readFile("weather.json"))
        val list = mutableListOf<String>()
        for (i in 0 until json.length()) {
            val city = json.getJSONObject(i)
            list.add("${city.getString("city")}: ${city.getInt("temp")}°C")
        }
        return list
    }
}