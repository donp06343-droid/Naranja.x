package app.redmiboostx

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class FloatingPanelService : Service() {
    private lateinit var windowManager: WindowManager
    private var panel: LinearLayout? = null
    private var details: TextView? = null
    private var params: WindowManager.LayoutParams? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastRx = TrafficStats.getTotalRxBytes()
    private var lastTx = TrafficStats.getTotalTxBytes()

    private val updater = object : Runnable {
        override fun run() {
            updateMetrics()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        if (panel == null) showPanel()
        handler.removeCallbacks(updater)
        handler.post(updater)
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Panel flotante", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Mantiene activo el panel de estadísticas mientras jugás."
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, FloatingPanelService::class.java).setAction(ACTION_STOP)
        val pending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("REDMI BOOST X está activo")
            .setContentText("Panel de estadísticas flotante. Tocá Detener para cerrarlo.")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Detener", pending).build())
            .build()
    }

    private fun showPanel() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 14)
            background = GradientDrawable().apply {
                setColor(0xEE17202B.toInt())
                cornerRadius = 22f
                setStroke(2, 0xFF40C4FF.toInt())
            }
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "BOOST X  •  EN VIVO"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }
        val collapse = Button(this).apply {
            text = "−"
            setOnClickListener {
                details?.visibility = if (details?.visibility == View.GONE) View.VISIBLE else View.GONE
                text = if (details?.visibility == View.GONE) "+" else "−"
            }
        }
        val close = Button(this).apply {
            text = "×"
            setOnClickListener { stopSelf() }
        }
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(collapse)
        header.addView(close)
        root.addView(header)

        details = TextView(this).apply {
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 13f
            setPadding(0, 8, 0, 0)
            text = "Midiendo conexión…"
        }
        root.addView(details)

        // Quick actions available above games without leaving the overlay.
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }
        fun actionButton(label: String, onClick: () -> Unit) {
            val button = Button(this).apply {
                text = label
                isAllCaps = false
                textSize = 11f
                setOnClickListener { onClick() }
            }
            actions.addView(button, LinearLayout.LayoutParams(0, 44, 1f).apply {
                setMargins(3, 0, 3, 0)
            })
        }
        actionButton("WhatsApp") { launchApp("com.whatsapp") }
        actionButton("Free Fire") { launchGame() }
        actionButton("Ajustes") { openSystemSettings() }
        root.addView(actions)

        @Suppress("DEPRECATION")
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 180
        }
        params = layoutParams

        // Drag only from the title bar so the collapse and close buttons remain tappable.
        header.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchX = 0f
            private var touchY = 0f
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = layoutParams.x
                        startY = layoutParams.y
                        touchX = event.rawX
                        touchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = (startX + touchX - event.rawX).toInt()
                        layoutParams.y = (startY + event.rawY - touchY).toInt()
                        runCatching { windowManager.updateViewLayout(root, layoutParams) }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> return true
                }
                return false
            }
        })

        panel = root
        runCatching { windowManager.addView(root, layoutParams) }
            .onFailure {
                panel = null
                stopSelf()
            }
    }

    private fun launchApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { startActivity(launchIntent) }
        } else {
            openSystemSettings()
        }
    }

    private fun launchGame() {
        val packages = listOf("com.dts.freefireth", "com.dts.freefiremax")
        val launchIntent = packages.firstNotNullOfOrNull { packageManager.getLaunchIntentForPackage(it) }
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { startActivity(launchIntent) }
        } else {
            openSystemSettings()
        }
    }

    private fun openSystemSettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun updateMetrics() {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val down = if (rx >= 0 && lastRx >= 0) (rx - lastRx).coerceAtLeast(0) else -1
        val up = if (tx >= 0 && lastTx >= 0) (tx - lastTx).coerceAtLeast(0) else -1
        lastRx = rx
        lastTx = tx

        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(memory)
        details?.text = "↓ ${speed(down)}    ↑ ${speed(up)}\nRAM libre: ${formatBytes(memory.availMem)}"
    }

    private fun speed(bytes: Long): String {
        if (bytes < 0) return "N/D"
        val kb = bytes / 1024.0
        return if (kb >= 1024) String.format(Locale.getDefault(), "%.1f MB/s", kb / 1024.0)
        else String.format(Locale.getDefault(), "%.0f KB/s", kb)
    }

    private fun formatBytes(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1) String.format(Locale.getDefault(), "%.1f GB", gb)
        else String.format(Locale.getDefault(), "%.0f MB", bytes / (1024.0 * 1024.0))
    }

    override fun onDestroy() {
        handler.removeCallbacks(updater)
        panel?.let { view -> runCatching { windowManager.removeView(view) } }
        panel = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "redmi_boost_overlay"
        private const val NOTIFICATION_ID = 2085
        private const val ACTION_STOP = "app.redmiboostx.STOP_FLOATING_PANEL"
    }
}
