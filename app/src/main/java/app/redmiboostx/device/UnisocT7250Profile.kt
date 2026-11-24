package app.redmiboostx.device

/**
 * Device hints for a possible Unisoc T7250 target.
 * Hardware identification remains unverified and this profile performs no writes.
 */
object UnisocT7250Profile {
    const val displayName: String = "Unisoc T7250"
    const val isVerified: Boolean = false
    const val verificationNote: String =
        "Perfil pendiente de validar con datos reales del dispositivo y sus nodos sysfs."
}
