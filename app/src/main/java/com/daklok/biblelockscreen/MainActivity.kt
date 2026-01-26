package com.daklok.biblelockscreen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.*
import coil.compose.rememberAsyncImagePainter
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppScreen()
            }
        }
    }
}

@Composable
fun AppScreen() {
    val context = LocalContext.current
    // Stav pre vybraný obrázok
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Načítanie uloženého obrázka pri štarte
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE)
        val savedUri = prefs.getString("bg_uri", null)
        if (savedUri != null) imageUri = Uri.parse(savedUri)
    }

    // Launcher pre galériu
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = it
            // Uložíme trvalé právo na čítanie súboru (aby sme ho mohli čítať aj zajtra na pozadí)
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            // Uložíme do preferences
            context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE)
                .edit().putString("bg_uri", it.toString()).apply()
        }
    }

    // UI Rozloženie
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Denný Verš Tapeta",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Karta s náhľadom
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.height(400.dp).fillMaxWidth(),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Len vizuálna ukážka, ako to bude vyzerať
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                    Text(
                        "Tu bude text\nz YouVersion",
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    Text("Vyber si fotku z galérie")
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { launcher.launch("image/*") }) {
                Text("Vybrať fotku")
            }

            Button(
                onClick = {
                    if (imageUri == null) return@Button

                    // Naplánovanie úlohy
                    val workRequest = PeriodicWorkRequestBuilder<DailyVerseWorker>(24, TimeUnit.HOURS)
                        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                        .build()

                    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                        "DailyBibleWallpaper",
                        ExistingPeriodicWorkPolicy.UPDATE, // Ak už existuje, aktualizuj ju
                        workRequest
                    )

                    // Pre testovanie: Spustiť okamžite (jednorazovo)
                    val oneTime = OneTimeWorkRequestBuilder<DailyVerseWorker>().build()
                    WorkManager.getInstance(context).enqueue(oneTime)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("Spustiť teraz")
            }
        }
    }
}