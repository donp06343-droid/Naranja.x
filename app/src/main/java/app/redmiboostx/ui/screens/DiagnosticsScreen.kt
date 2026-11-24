package app.redmiboostx.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.redmiboostx.device.DeviceCapabilities
import app.redmiboostx.device.UnisocT7250Profile

@Composable
fun DiagnosticsScreen(
    capabilities: DeviceCapabilities?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Naranja Core", style = MaterialTheme.typography.headlineMedium)
        Text("Diagnóstico del dispositivo", style = MaterialTheme.typography.titleLarge)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onRefresh, enabled = !isLoading) {
                Text(if (isLoading) "Analizando…" else "Actualizar diagnóstico")
            }
            if (onBack != null) {
                OutlinedButton(onClick = onBack) { Text("Volver") }
            }
        }

        if (capabilities == null) {
            Text("Todavía no hay un diagnóstico disponible.")
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Dispositivo", style = MaterialTheme.typography.titleMedium)
                    Metric("Fabricante", capabilities.manufacturer)
                    Metric("Modelo", capabilities.model)
                    Metric("Device", capabilities.device)
                    Metric("Hardware", capabilities.hardware)
                    Metric("CPU (núcleos lógicos)", capabilities.cpuCores.toString())
                    Metric("Tamaño de página", "${capabilities.pageSizeKb} KB")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Acceso y seguridad", style = MaterialTheme.typography.titleMedium)
                    Metric("Root disponible", if (capabilities.rootAvailable) "Sí" else "No")
                    Metric("SELinux", capabilities.selinuxMode)
                    Text(
                        "El diagnóstico no modifica CPU, GPU, frecuencias ni gobernadores.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Controladores devfreq", style = MaterialTheme.typography.titleMedium)
                    if (capabilities.devfreqNodes.isEmpty()) {
                        Text("No se encontraron nodos devfreq accesibles.")
                    } else {
                        capabilities.devfreqNodes.forEach { node ->
                            Text(
                                if (node.isGpuCandidate) "GPU candidata: ${node.name}" else node.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Metric("Gobernador actual", node.currentGovernor.ifBlank { "No disponible" })
                            Metric(
                                "Gobernadores disponibles",
                                node.availableGovernors.joinToString().ifBlank { "No disponibles" }
                            )
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Perfiles de hardware", style = MaterialTheme.typography.titleMedium)
                    Metric("Perfil", UnisocT7250Profile.displayName)
                    Metric("Verificado", if (UnisocT7250Profile.isVerified) "Sí" else "No")
                    Text(UnisocT7250Profile.verificationNote, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (capabilities.detectionErrors.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Avisos de detección", style = MaterialTheme.typography.titleMedium)
                        capabilities.detectionErrors.forEach { error ->
                            Text("• $error", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
