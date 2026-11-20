package app.redmiboostx

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    BoostHome(
                        openDisplay = { openSystemSetting(Settings.ACTION_DISPLAY_SETTINGS) },
                        openBattery = { openSystemSetting(Settings.ACTION_BATTERY_SAVER_SETTINGS) },
                        openSettings = { openSystemSetting(Settings.ACTION_SETTINGS) }
                    )
                }
            }
        }
    }

    private fun openSystemSetting(action: String) {
        runCatching { startActivity(Intent(action)) }
            .onFailure { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }
}

@Composable
private fun BoostHome(
    openDisplay: () -> Unit,
    openBattery: () -> Unit,
    openSettings: () -> Unit
) {
    var gameMode by remember { mutableStateOf(false) }
    var tipsDone by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("REDMI BOOST X", style = MaterialTheme.typography.headlineMedium)
        Text("Asistente de rendimiento • Redmi A5", style = MaterialTheme.typography.bodyMedium)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Preparación para jugar", style = MaterialTheme.typography.titleLarge)
                Text("Configurá tu teléfono antes de abrir Free Fire. Este perfil es una guía: Android mantiene el control del hardware.")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Modo juego", modifier = Modifier.weight(1f))
                    Switch(checked = gameMode, onCheckedChange = { gameMode = it })
                }
                Text(if (gameMode) "Perfil activado en la app. Aplicá también los ajustes recomendados de abajo." else "Activá el perfil para ver la lista de preparación.")
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Checklist rápido", style = MaterialTheme.typography.titleMedium)
                Text("• Cerrá manualmente las apps que no estés usando.")
                Text("• Desactivá la grabación de pantalla si no la necesitás.")
                Text("• Usá una conexión estable y evitá descargas en segundo plano.")
                Text("• Si el teléfono se calienta mucho, pausá y dejalo enfriar.")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ya revisé estos puntos", modifier = Modifier.weight(1f))
                    Switch(checked = tipsDone, onCheckedChange = { tipsDone = it })
                }
            }
        }

        Text("Ajustes del sistema", style = MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick = openDisplay, modifier = Modifier.fillMaxWidth()) {
            Text("Abrir ajustes de pantalla")
        }
        OutlinedButton(onClick = openBattery, modifier = Modifier.fillMaxWidth()) {
            Text("Abrir ajustes de batería")
        }
        Button(onClick = openSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Abrir ajustes de Android")
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("¿RAM en la nube?", style = MaterialTheme.typography.titleMedium)
                Text("No puede funcionar como RAM física para Free Fire: los datos tardan demasiado en viajar por internet. La memoria del juego debe estar disponible en el propio teléfono.")
            }
        }
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Transparencia", style = MaterialTheme.typography.titleMedium)
                Text("REDMI BOOST X no inventa mediciones de FPS o temperatura. Tampoco fuerza CPU/GPU, modifica el sistema, usa root ni promete FPS imposibles. Los resultados dependen del juego, Android, temperatura y hardware.")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("REDMI BOOST X 1.0.0", style = MaterialTheme.typography.labelSmall)
    }
}
