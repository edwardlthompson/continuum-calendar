package org.continuumcalendar.platform

import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.SwingUtilities

/** Best-effort Windows/Linux tray balloon via AWT SystemTray. */
class DesktopTrayNotifier(
    private val tooltip: String = "Continuum Calendar",
    private val onActivate: (() -> Unit)? = null,
) : Notifier {
    private val icon: TrayIcon? by lazy { createIcon() }

    /** Ensure tray icon exists (for hide-to-tray without a notify). */
    fun ensureTray() {
        icon
    }

    override suspend fun notify(title: String, body: String) {
        val trayIcon = icon
        if (trayIcon != null) {
            SwingUtilities.invokeLater {
                trayIcon.displayMessage(title, body, TrayIcon.MessageType.INFO)
            }
        }
    }

    private fun createIcon(): TrayIcon? {
        if (!SystemTray.isSupported()) return null
        return runCatching {
            val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
            val g = image.createGraphics()
            g.color = java.awt.Color(15, 110, 140)
            g.fillOval(1, 1, 14, 14)
            g.dispose()
            val trayIcon = TrayIcon(image, tooltip)
            trayIcon.isImageAutoSize = true
            if (onActivate != null) {
                trayIcon.addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        if (e.clickCount >= 1) {
                            SwingUtilities.invokeLater { onActivate.invoke() }
                        }
                    }
                })
            }
            SystemTray.getSystemTray().add(trayIcon)
            trayIcon
        }.getOrNull()
    }
}
