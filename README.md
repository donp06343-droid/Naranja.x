# REDMI BOOST X

Asistente de rendimiento y panel de estadísticas para Android, pensado para el Redmi A5 y sesiones de juego como Free Fire.

## Funciones incluidas

- Monitor en vivo de tráfico total de descarga y subida del teléfono.
- Monitor de RAM disponible y total.
- Información de almacenamiento libre y total.
- Nivel y temperatura de batería (temperatura reportada por el sensor de batería).
- Panel flotante movible con tráfico de red y RAM disponible.
- Lista de aplicaciones usadas recientemente, mediante el permiso opcional «Acceso a uso».
- Accesos a ajustes de pantalla y batería.
- Acceso directo a los ajustes de notificaciones de WhatsApp para facilitar respuestas desde sus propias notificaciones, cuando Android y WhatsApp lo permitan.
- El panel flotante se inicia por acción del usuario y se mantiene mediante una notificación persistente.

## Privacidad y permisos

La lista de apps recientes requiere que el usuario habilite Acceso a uso. El panel superpuesto requiere el permiso «Mostrar sobre otras apps». Android puede solicitar permiso de notificaciones para mostrar avisos. La app no lee conversaciones de WhatsApp, no recopila mensajes ni envía métricas a un servidor.

## Límites importantes

- La velocidad mostrada es el tráfico total del teléfono, no una medición exclusiva de Free Fire ni un test de velocidad.
- No se garantiza un ping real hasta los servidores de Free Fire.
- «Apps usadas recientemente» no equivale a una lista exacta de procesos activos en segundo plano.
- Android no permite a una app común cerrar arbitrariamente procesos de otras aplicaciones.
- No fuerza CPU/GPU, no hace overclock, no desbloquea FPS, no aumenta físicamente la RAM y no limpia la caché privada de otras apps.
- No cambia, desconecta ni interfiere con la VPN del usuario.
- No usa root ni necesita ADB.

## Compilar

Abrí el proyecto con Android Studio y ejecutá la configuración **app**. El workflow de GitHub Actions también genera el APK debug.

- Package: `app.redmiboostx`
- Min SDK: 23
- Target SDK: 35
- Lenguaje: Kotlin + Jetpack Compose
- JDK: 17
