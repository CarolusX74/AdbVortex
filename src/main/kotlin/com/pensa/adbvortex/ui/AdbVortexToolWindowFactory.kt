package com.pensa.adbvortex.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.pensa.adbvortex.AutoConnectService
import com.pensa.adbvortex.HttpProxyManager
import java.awt.*
import javax.swing.*
import javax.swing.Timer

class AdbVortexToolWindowFactory : ToolWindowFactory {

    private var proxy: HttpProxyManager? = null
    private var auto: AutoConnectService? = null
    private var isRunning = false
    private lateinit var statusDot: JLabel
    private lateinit var statusLabel: JLabel
    private var blinkTimer: Timer? = null
    private var isBright = false

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val console = JTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }

        val scroll = JBScrollPane(console).apply {
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
        }

        fun log(msg: String) {
            SwingUtilities.invokeLater {
                console.append("$msg\n")
                console.caretPosition = console.document.length
            }
        }

        // 🟢 Status indicator
        statusDot = JLabel("●").apply {
            font = Font("Dialog", Font.BOLD, 20)
            foreground = Color.GRAY // initial: idle
            toolTipText = "Proxy idle"
        }

        statusLabel = JLabel("Idle").apply {
            font = Font("Dialog", Font.PLAIN, 14)
            foreground = Color.DARK_GRAY
        }

        fun startBlinking() {
            blinkTimer?.stop()
            isBright = false
            blinkTimer = Timer(600) {
                isBright = !isBright
                statusDot.foreground =
                    if (isBright) Color(0x66BB6A) // bright green
                    else Color(0x2E7D32)          // darker green
            }
            blinkTimer?.start()
        }

        fun stopBlinking() {
            blinkTimer?.stop()
            statusDot.foreground = Color(0xF44336) // red
        }

        fun updateStatus(running: Boolean) {
            SwingUtilities.invokeLater {
                if (running) {
                    statusLabel.text = "Running"
                    statusLabel.foreground = Color(0x4CAF50)
                    startBlinking()
                } else {
                    statusLabel.text = "Stopped"
                    statusLabel.foreground = Color(0xF44336)
                    stopBlinking()
                }
            }
        }

        val startBtn = JButton("Start Proxy").apply {
            addActionListener {
                if (isRunning) return@addActionListener
                try {
                    proxy = HttpProxyManager(::log)
                    auto = AutoConnectService(::log, proxy!!)
                    proxy!!.start(8080)
                    auto!!.start()
                    log("✅ Proxy started on http://127.0.0.1:8080")
                    isRunning = true
                    isEnabled = false
                    updateStatus(true)
                } catch (e: Exception) {
                    log("❌ Error: ${e.message}")
                    updateStatus(false)
                }
            }
        }

        val stopBtn = JButton("Stop Proxy").apply {
            addActionListener {
                if (!isRunning) return@addActionListener
                try {
                    auto?.stop()
                    proxy?.stop()
                    log("🛑 Proxy stopped.")
                } catch (e: Exception) {
                    log("❌ Error stopping: ${e.message}")
                } finally {
                    isRunning = false
                    startBtn.isEnabled = true
                    updateStatus(false)
                }
            }
        }

        val clearBtn = JButton("Clear Console").apply {
            addActionListener { console.text = "" }
        }

        val statusPanel = JPanel().apply {
            layout = FlowLayout(FlowLayout.LEFT, 5, 0)
            add(statusDot)
            add(statusLabel)
        }

        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(startBtn)
            add(Box.createHorizontalStrut(8))
            add(stopBtn)
            add(Box.createHorizontalStrut(8))
            add(clearBtn)
            add(Box.createHorizontalGlue())
            add(statusPanel)
        }

        val mainPanel = JPanel(BorderLayout()).apply {
            add(buttonPanel, BorderLayout.NORTH)
            add(scroll, BorderLayout.CENTER)
        }

        val content = ContentFactory.getInstance().createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)

        log("🟣 AdbVortex ready. Connect your device and press Start.")
        updateStatus(false)
    }
}
