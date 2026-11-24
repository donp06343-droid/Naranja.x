package app.redmiboostx.root

import com.topjohnwu.superuser.Shell

/**
 * Small, read-first wrapper around libsu.
 * This phase exposes command execution and a narrowly allow-listed no-op sysfs probe only.
 */
object RootManager {
    data class CommandResult(
        val success: Boolean,
        val stdout: String,
        val stderr: String,
        val exitCode: Int
    )

    data class WriteProbeResult(
        val path: String,
        val writable: Boolean,
        val selinuxMode: String,
        val message: String
    )

    fun execute(command: String): CommandResult {
        require(command.isNotBlank()) { "El comando no puede estar vacío." }
        val result = Shell.cmd(command).exec()
        return CommandResult(
            success = result.isSuccess,
            stdout = result.out.joinToString("\n").trim(),
            stderr = result.err.joinToString("\n").trim(),
            exitCode = result.code
        )
    }

    fun isRootAvailable(): Boolean = runCatching {
        val result = execute("id -u")
        result.success && result.stdout.trim() == "0"
    }.getOrDefault(false)

    /**
     * Performs a read-then-write of the exact current value.
     * Only known governor/frequency attributes are accepted.
     */
    fun isWritable(path: String): WriteProbeResult {
        val allowedPath = path.matches(
            Regex(
                "^/sys/(class/devfreq/[A-Za-z0-9_.-]+/governor|" +
                    "devices/system/cpu/cpu[0-9]+/cpufreq/" +
                    "(scaling_governor|scaling_min_freq|scaling_max_freq))$"
            )
        )
        if (!allowedPath) {
            return WriteProbeResult(path, false, "unknown", "Ruta fuera de la lista permitida.")
        }

        val selinux = runCatching {
            val result = execute("getenforce 2>/dev/null || echo unavailable")
            if (result.success) result.stdout.ifBlank { "unknown" } else "unknown"
        }.getOrDefault("unknown")

        val script = """
            target='$path'
            [ -e "$target" ] || { echo "MISSING"; exit 20; }
            current=$(cat "$target" 2>/dev/null) || { echo "READ_DENIED"; exit 21; }
            case "$current" in
                *"
            "*) echo "MULTILINE_VALUE"; exit 22 ;;
            esac
            [ -n "$current" ] || { echo "EMPTY_VALUE"; exit 23; }
            printf '%s' "$current" > "$target" 2>/dev/null || { echo "WRITE_DENIED"; exit 24; }
            echo "NO_OP_WRITE_OK"
        """.trimIndent()

        return runCatching {
            val result = execute(script)
            val ok = result.success && result.stdout.contains("NO_OP_WRITE_OK")
            val message = when {
                ok -> "Lectura y escritura no-op verificadas."
                result.stdout.isNotBlank() -> result.stdout
                result.stderr.isNotBlank() -> result.stderr
                else -> "La prueba no pudo completarse (código \${result.exitCode})."
            }
            WriteProbeResult(path, ok, selinux, message)
        }.getOrElse { error ->
            WriteProbeResult(path, false, selinux, error.message ?: "Error al comprobar sysfs.")
        }
    }
}
