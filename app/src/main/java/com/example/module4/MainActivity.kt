package com.example.module4

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.module4.ui.theme.Module4Theme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

val Context.dataStore by preferencesDataStore("settings")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Module4Theme {
                ReminderScreen()
            }
        }
    }
}

@Composable
fun ReminderScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isEnabled by remember { mutableStateOf(false) }
    var nextReminderTime by remember { mutableStateOf("") }
    val dataStore = context.dataStore.data
    val reminderPref = dataStore.map { prefs ->
        prefs[booleanPreferencesKey("reminder_enabled")] ?: false
    }.collectAsState(initial = false)
    LaunchedEffect(reminderPref.value) {
        isEnabled = reminderPref.value
        if (isEnabled) {
            updateNextReminderTime(context) { time ->
                nextReminderTime = time
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && isEnabled) {
            scheduleReminder(context)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Напоминание о таблетке",
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isEnabled) Color.Green else Color.Gray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isEnabled) "Включено" else "Выключено",
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (isEnabled && nextReminderTime.isNotEmpty()) {
            Text(
                text = "Следующее напоминание: $nextReminderTime",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                if (!isEnabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        scope.launch {
                            context.dataStore.edit { prefs ->
                                prefs[booleanPreferencesKey("reminder_enabled")] = true
                            }
                            scheduleReminder(context)
                            updateNextReminderTime(context) { time ->
                                nextReminderTime = time
                            }
                        }
                    }
                } else {
                    scope.launch {
                        context.dataStore.edit { prefs ->
                            prefs[booleanPreferencesKey("reminder_enabled")] = false
                        }
                        cancelReminder(context)
                        nextReminderTime = ""
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(
                text = if (isEnabled) "Выключить напоминание" else "Включить напоминание",
                fontSize = 16.sp
            )
        }
    }
}

fun scheduleReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 20)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    val intent = Intent(context, ReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        1001,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        pendingIntent
    )
}

fun cancelReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        1001,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
    pendingIntent.cancel()
}

fun updateNextReminderTime(context: Context, onResult: (String) -> Unit) {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 20)
        set(Calendar.MINUTE, 0)

        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    val today = Calendar.getInstance()
    val isToday = calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = format.format(calendar.time)
    onResult(if (isToday) "сегодня в $timeStr" else "завтра в $timeStr")
}