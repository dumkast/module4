package com.example.module4

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.module4.ui.theme.Module4Theme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Module4Theme {
                LocationScreen()
            }
        }
    }
}

@Composable
fun LocationScreen() {
    val context = LocalContext.current
    val fusedLocationClient: FusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isLoading by remember { mutableStateOf(false) }
    var addressText by remember { mutableStateOf("") }
    var coordinatesText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else {
            if (addressText.isNotEmpty() || coordinatesText.isNotEmpty()) {
                Text(
                    text = addressText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = coordinatesText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (errorText.isNotEmpty()) {
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Нажмите кнопку",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (hasLocationPermission) {
                        isLoading = true
                        errorText = ""
                        addressText = ""
                        coordinatesText = ""
                        val cancellationTokenSource = CancellationTokenSource()
                        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100)
                            .setWaitForAccurateLocation(true)
                            .build()

                        fusedLocationClient.getCurrentLocation(
                            request.priority,
                            cancellationTokenSource.token
                        ).addOnSuccessListener { location ->
                            if (location != null) {
                                val latitude = location.latitude
                                val longitude = location.longitude
                                coordinatesText = "Lat: $latitude\n Lng: $longitude"

                                try {
                                    val geocoder = Geocoder(context, Locale("ru"))
                                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                                    if (addresses != null && addresses.isNotEmpty()) {
                                        val address = addresses[0]
                                        val fullAddress = (0..address.maxAddressLineIndex)
                                            .map { address.getAddressLine(it) }
                                            .joinToString(", ")
                                        addressText = fullAddress
                                    } else {
                                        errorText = "Адрес не найден"
                                    }
                                    isLoading = false
                                } catch (e: Exception) {
                                    errorText = "Ошибка геокодирования: ${e.message}"
                                    isLoading = false
                                }
                            } else {
                                errorText = "Местоположение не определено"
                                isLoading = false
                            }
                        }.addOnFailureListener { exception ->
                            errorText = "Ошибка получения местоположения: ${exception.message}"
                            isLoading = false
                        }
                    } else {
                        errorText = "Разрешение на местоположение не предоставлено"
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                modifier = Modifier.width(200.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "Получить мой адрес",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}