package app.redmiboostx

import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.os.Process
import android.app.usage.UsageStatsManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BoostDashboard(
                    context = this,
                    startOverlay = { startFloatingPanel() },
                    openOverlayPermission = {
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                    },
                    openUsagePermission = {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    openDisplay = { openSetting(Settings.ACTION_DISPLAY_SETTINGS) },
                    openBattery = { openSetting(Settings.ACTION_BATTERY_SAVER_SETTINGS) }
                )
            }
        }
    }

    private fun startFloatingPanel() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            return
        }
        val intent = Intent(this, FloatingPanelService::class.java)
        startService(intent)
    }

    private fun openSetting(action: String) {
        runCatching { startActivity(Intent(action)) }
            .onFailure { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }
}

private data class DeviceStats(
    val download: String,
    val upload: String,
    val ramFree: String,
    val ramTotal: String,
    val storageFree: String,
    val storageTotal: String,
    val battery: String,
    val batteryTemp: String
)

private fun bytesPerSecond(value: Long): String {
    if (value < 0) return "No disponible"
    val kb = value / 1024.0
    return if (kb >= 1024) String.format(Locale.getDefault(), "%.2f MB/s", kb / 1024.0)
    else String.format(Locale.getDefault(), "%.0f KB/s", kb)
}

private fun readDeviceStats(context: Context, down: Long, up: Long): DeviceStats {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memory = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memory)

    val stat = StatFs(Environment.getDataDirectory().absolutePath)
    val free = stat.availableBytes
    val total = stat.totalBytes

    val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val percent = if (level >= 0 && scale > 0) "${(level * 100) / scale}%" else "No disponible"
    val tenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
    val temp = if (tenths >= 0) String.format(Locale.getDefault(), "%.1f °C", tenths / 10f) else "No disponible"

    return DeviceStats(
        download = bytesPerSecond(down),
        upload = bytesPerSecond(up),
        ramFree = formatBytes(memory.availMem),
        ramTotal = formatBytes(memory.totalMem),
        storageFree = formatBytes(free),
        storageTotal = formatBytes(total),
        battery = percent,
        batteryTemp = temp
    )
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 1) String.format(Locale.getDefault(), "%.2f GB", gb)
    else String.format(Locale.getDefault(), "%.0f MB", bytes / (1024.0 * 1024.0))
}

private fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun recentApps(context: Context): List<String> {
    if (!hasUsageAccess(context)) return emptyList()
    val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val now = System.currentTimeMillis()
    val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - TimeUnit.DAYS.toMillis(1), now)
        ?: return emptyList()
    return stats.asSequence()
        .filter { it.packageName != context.packageName && it.lastTimeUsed > 0L }
        .sortedByDescending { it.lastTimeUsed }
        .distinctBy { it.packageName }
        .take(8)
        .mapNotNull {
            runCatching {
                val app = context.packageManager.getApplicationInfo(it.packageName, 0)
                context.packageManager.getApplicationLabel(app).toString()
            }.getOrNull()
        }.toList()
}

@Composable
private fun BoostDashboard(
    context: Context,
    startOverlay: () -> Unit,
    openOverlayPermission: () -> Unit,
    openUsagePermission: () -> Unit,
    openDisplay: () -> Unit,
    openBattery: () -> Unit
) {
    var stats by remember { mutableStateOf(readDeviceStats(context, 0, 0)) }
    var recent by remember { mutableStateOf(recentApps(context)) }
    var usageAllowed by remember { mutableStateOf(hasUsageAccess(context)) }
    var lastRx by remember { mutableLongStateOf(TrafficStats.getTotalRxBytes()) }
    var lastTx by remember { mutableLongStateOf(TrafficStats.getTotalTxBytes()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            val down = if (rx >= 0 && lastRx >= 0) (rx - lastRx).coerceAtLeast(0) else -1
            val up = if (tx >= 0 && lastTx >= 0) (tx - lastTx).coerceAtLeast(0) else -1
            lastRx = rx
            lastTx = tx
            stats = readDeviceStats(context, down, up)
            usageAllowed = hasUsageAccess(context)
            if (usageAllowed) recent = recentApps(context)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("REDMI BOOST X", style = MaterialTheme.typography.headlineMedium)
        Text("Panel de rendimiento • Redmi A5 • Android 15")

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Internet en tiempo real", style = MaterialTheme.typography.titleLarge)
                MetricRow("Descarga", stats.download)
                MetricRow("Subida", stats.upload)
                Text("Medición del tráfico total del teléfono; no es un test de velocidad ni mide exclusivamente Free Fire.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Estado del teléfono", style = MaterialTheme.typography.titleLarge)
                MetricRow("RAM disponible", stats.ramFree)
                MetricRow("RAM total", stats.ramTotal)
                MetricRow("Almacenamiento libre", stats.storageFree)
                MetricRow("Almacenamiento total", stats.storageTotal)
                MetricRow("Batería", stats.battery)
                MetricRow("Temperatura de batería", stats.batteryTemp)
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Panel flotante para jugar", style = MaterialTheme.typography.titleLarge)
                Text("Muestra descarga, subida y RAM encima de Free Fire. Requiere que autorices «Mostrar sobre otras apps» y mantener activa la notificación del servicio.")
                Button(onClick = startOverlay, modifier = Modifier.fillMaxWidth()) {
                    Text("Iniciar panel flotante")
                }
                OutlinedButton(onClick = openOverlayPermission, modifier = Modifier.fillMaxWidth()) {
                    Text("Permiso de ventanas flotantes")
                }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Aplicaciones usadas recientemente", style = MaterialTheme.typography.titleLarge)
                Text("Android no permite a una app común ver libremente todos los procesos activos. Esta lista muestra apps usadas recientemente, no confirma cuáles siguen ejecutándose en segundo plano.")
                if (!usageAllowed) {
                    Text("Necesitás habilitar Acceso a uso para mostrar la lista.")
                    OutlinedButton(onClick = openUsagePermission, modifier = Modifier.fillMaxWidth()) {
                        Text("Habilitar acceso a uso")
                    }
                } else if (recent.isEmpty()) {
                    Text("Todavía no hay datos de uso disponibles.")
                } else {
                    recent.forEach { Text("• $it") }
                }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Accesos rápidos", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = openDisplay, modifier = Modifier.fillMaxWidth()) { Text("Ajustes de pantalla") }
                OutlinedButton(onClick = openBattery, modifier = Modifier.fillMaxWidth()) { Text("Ajustes de batería") }
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("WhatsApp sin salir del juego", style = MaterialTheme.typography.titleMedium)
                Text("Para responder mientras jugás, activá las notificaciones emergentes de WhatsApp y usá «Responder» desde la propia notificación. La disponibilidad depende de WhatsApp y de los ajustes del Redmi. REDMI BOOST X no lee tus mensajes.")
            }
        }

        Text("REDMI BOOST X 2.0 • Sin root • Sin modificar ni desconectar tu VPN", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
