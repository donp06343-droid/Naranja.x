# Naranja Core

Aplicación Android en Kotlin + Jetpack Compose para diagnóstico del dispositivo y herramientas auxiliares de juego. Esta rama inicia la migración desde REDMI BOOST X y conserva su panel de estadísticas flotante.

## FASE 0 — Diagnóstico y base técnica

- Detección de información del dispositivo y cantidad de núcleos lógicos.
- Tamaño de página consultado con `getconf PAGESIZE`; fallback seguro de 4096 bytes.
- Ejecuciones root aisladas con captura de errores.
- Lectura del estado SELinux.
- Enumeración de todos los nodos `/sys/class/devfreq`, sin aplicar cambios.
- Clasificación conservadora de candidatos GPU por nombre y gobernadores, incluyendo `msm-adreno`, `simple_ondemand` y `coarse_demand`.
- Perfil Unisoc T7250 marcado explícitamente como no verificado.
- Prueba sysfs no-op restringida a atributos conocidos: lee el valor y vuelve a escribir exactamente ese valor. No se ejecuta automáticamente.
- La pantalla de diagnóstico muestra CPU una sola vez y no usa contenedores locales innecesarios.

## Funciones heredadas conservadas

- Monitor de tráfico total de red, RAM, almacenamiento y batería.
- Panel flotante movible.
- Accesos a ajustes de pantalla y batería.
- Lista de aplicaciones usadas recientemente mediante el permiso opcional Acceso a uso.

## Seguridad y límites

- La detección no modifica CPU, GPU, frecuencias ni gobernadores.
- La comprobación no-op de escritura requiere invocación explícita y solo acepta rutas sysfs incluidas en una lista permitida.
- Root es opcional para el funcionamiento básico; sin root, las comprobaciones privilegiadas pueden quedar como no disponibles.
- No se promete aumentar FPS, reducir ping ni mejorar físicamente el hardware.
- No se modifica ni desconecta la VPN.
- No se leen conversaciones ni se envían métricas a servidores.

## Compilar

Abrí el proyecto con Android Studio o ejecutá `gradle assembleDebug`.

- Package: `app.redmiboostx` (se conserva durante la migración)
- Min SDK: 30
- Target SDK: 35
- Kotlin: 2.0.21
- Android Gradle Plugin: 8.7.2
- JDK: 17
