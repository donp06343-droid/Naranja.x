package app.redmiboostx.device

import android.os.Build
import app.redmiboostx.root.RootManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DevfreqNode(
    val name: String,
    val currentGovernor: String,
    val availableGovernors: List<String>,
    val isGpuCandidate: Boolean
)

data class DeviceCapabilities(
    val manufacturer: String,
    val model: String,
    val device: String,
    val hardware: String,
    val cpuCores: Int,
    val pageSizeKb: Int,
    val rootAvailable: Boolean,
    val selinuxMode: String,
    val devfreqNodes: List<DevfreqNode>,
    val detectionErrors: List<String>
)

object CapabilityDetector {
    private const val DEFAULT_PAGE_SIZE_BYTES = 4096L

    suspend fun detect(): DeviceCapabilities = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()

        fun safeExec(label: String, command: String): String? {
            return runCatching {
                RootManager.execute(command)
            }.onFailure { error ->
                errors += "$label: ${error.message ?: error.javaClass.simpleName}"
            }.getOrNull()?.let { result ->
                if (!result.success) {
                    errors += "$label: ${result.stderr.ifBlank { "código ${result.exitCode}" }}"
                    null
                } else {
                    result.stdout
                }
            }
        }

        val pageSizeBytes = safeExec("getconf PAGESIZE", "getconf PAGESIZE")
            ?.trim()
            ?.toLongOrNull()
            ?.takeIf { it > 0L }
            ?: DEFAULT_PAGE_SIZE_BYTES

        val rootAvailable = runCatching {
            RootManager.isRootAvailable()
        }.onFailure { error ->
            errors += "Detección de root: ${error.message ?: error.javaClass.simpleName}"
        }.getOrDefault(false)

        val selinuxMode = safeExec(
            "SELinux",
            "getenforce 2>/dev/null || cat /sys/fs/selinux/enforce 2>/dev/null || echo unknown"
        )?.lineSequence()?.firstOrNull()?.trim()?.ifBlank { "unknown" } ?: "unknown"

        // Enumerates every devfreq entry; this phase never writes to these nodes.
        val devfreqOutput = safeExec(
            "Enumeración devfreq",
            """
                for d in /sys/class/devfreq/*; do
                    [ -e "${'$'}d" ] || continue
                    n=${'$'}{d##*/}
                    current=${'$'}(cat "${'$'}d/governor" 2>/dev/null || true)
                    available=${'$'}(cat "${'$'}d/available_governors" 2>/dev/null || true)
                    printf '%s\t%s\t%s\n' "${'$'}n" "${'$'}current" "${'$'}available"
                done
            """.trimIndent()
        ).orEmpty()

        val devfreqNodes = devfreqOutput.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split('\t', limit = 3)
                if (parts.isEmpty() || parts[0].isBlank()) {
                    errors += "Entrada devfreq inválida: $line"
                    null
                } else {
                    val name = parts[0].trim()
                    val currentGovernor = parts.getOrNull(1).orEmpty().trim()
                    val governors = parts.getOrNull(2)
                        .orEmpty()
                        .trim()
                        .split(Regex("\\s+"))
                        .filter { it.isNotBlank() }
                    val lowerName = name.lowercase()
                    val lowerGovernors = governors.map { it.lowercase() }

                    val gpuName = listOf("gpu", "adreno", "mali", "kgsl").any(lowerName::contains)
                    val msmAdrenoGovernor = "msm-adreno" in lowerGovernors
                    val genericGpuGovernor = gpuName &&
                        lowerGovernors.any { it == "simple_ondemand" || it == "coarse_demand" }

                    DevfreqNode(
                        name = name,
                        currentGovernor = currentGovernor,
                        availableGovernors = governors,
                        isGpuCandidate = gpuName || msmAdrenoGovernor || genericGpuGovernor
                    )
                }
            }
            .toList()

        DeviceCapabilities(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
            device = Build.DEVICE.orEmpty(),
            hardware = Build.HARDWARE.orEmpty(),
            cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
            pageSizeKb = (pageSizeBytes / 1024L).toInt().coerceAtLeast(1),
            rootAvailable = rootAvailable,
            selinuxMode = selinuxMode,
            devfreqNodes = devfreqNodes,
            detectionErrors = errors.toList()
        )
    }
}
