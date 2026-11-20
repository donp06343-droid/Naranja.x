# REDMI BOOST X

Aplicación Android de ayuda para preparar el Redmi A5 antes de jugar Free Fire.

## Qué hace
- Panel sencillo con un perfil de juego orientativo.
- Accesos directos a ajustes reales de Android (pantalla, batería y ajustes generales).
- Consejos para reducir carga antes de jugar y evitar sobrecalentamiento.
- Explicación clara de los límites de Android y de por qué la RAM en la nube no reemplaza la RAM física.

## Qué NO promete ni hace
Una app normal no puede forzar la frecuencia de CPU/GPU, desbloquear FPS que el juego o el dispositivo no ofrecen, cerrar arbitrariamente otras apps ni convertir internet en RAM. REDMI BOOST X no usa root, no ejecuta comandos ADB, no modifica archivos del sistema y no recopila datos.

## Compilar
Abrí esta carpeta con Android Studio (versión compatible con Android Gradle Plugin 8.7.3), dejá que Gradle sincronice y ejecutá la configuración **app** en un dispositivo Android.

- Package: `app.redmiboostx`
- Min SDK: 23
- Target SDK: 35
- Lenguaje: Kotlin + Jetpack Compose

## Dispositivo objetivo
Diseñada como asistente de configuración para Redmi A5. La disponibilidad de opciones depende de la versión de Android y de la capa del fabricante.
