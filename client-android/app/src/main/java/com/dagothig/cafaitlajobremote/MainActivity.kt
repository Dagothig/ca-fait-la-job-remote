package com.dagothig.cafaitlajobremote

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doOnTextChanged
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.illposed.osc.OSCMessage
import com.illposed.osc.transport.udp.OSCPortOut
import java.net.InetAddress
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

interface IAction

data class KeyDef(val s: String, val v: String)
data class CursorAction(val v: Int, val x: Float, val y: Float): IAction
data class KeyboardAction(val keyCode: String, val pressed: Int): IAction

class MainActivity: AppCompatActivity() {
    private var serverAddress: String = ""
    private var serverPort: Int? = null
    private var cursorHMult: Float = 1f
    private var cursorVMult: Float = 1f
    private var wheelMult: Float = 0.1f
    private var applyAccel: Boolean = true

    private var prefs: SharedPreferences? = null
    private var started: Boolean = true

    private var layout: ConstraintLayout? = null

    private var cursorZone: View? = null
    private var leftClick: Button? = null
    private var rightClick: Button? = null

    private var v: Int = 0
    private var x: Float = 0.5f
    private var y: Float = 0.5f
    private var time: Long = 0L
    private var sx: Float = 0f
    private var sy: Float = 0f
    private var stime: Long = 0L

    private var keyboardZone: Flow? = null
    private var keys: Dictionary<String, Button>? = null
    private var toggleInput: Button? = null
    private var textInput: EditText? = null

    private val actions: BlockingQueue<IAction>

    init {
        actions = ArrayBlockingQueue( 69)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        layout = findViewById(R.id.layout)

        cursorZone = findViewById(R.id.zone_cursor)
        cursorZone?.setOnTouchListener { _, ev ->
            val newX = ev.x * cursorHMult
            val newY = ev.y * cursorVMult
            val now = Date().time
            var dx = newX - x
            var dy = newY - y
            val dt = now - time
            if (applyAccel) {
                val speed = min(max(0.25f, sqrt(dx * dx + dy * dy) / dt), 3f)
                dx *= speed
                dy *= speed
            }
            when (ev.action) {
                MotionEvent.ACTION_MOVE -> {
                    if (ev.pointerCount > 1)
                        actions.add(CursorAction(v.or(4), dx * wheelMult, dy * wheelMult))
                    else
                        actions.add(CursorAction(v, dx, dy))
                }
                MotionEvent.ACTION_DOWN -> {
                    sx = newX
                    sy = newY
                    stime = now
                }
                MotionEvent.ACTION_UP -> {
                    if (now - stime < 1000 &&
                        (newX - sx).pow(2) + (newY - sy).pow(2) < 64) {
                        actions.add(CursorAction(v.or(1), dx, dy))
                        actions.add(CursorAction(v.and(1.inv()), dx, dy))
                    }
                }
            }
            x = newX
            y = newY
            time = now
            true
        }

        leftClick = findViewById(R.id.left_click)
        leftClick?.setOnTouchListener { _, ev ->
            if (when (ev.action ) {
                MotionEvent.ACTION_DOWN -> { v = v.or(1); true }
                MotionEvent.ACTION_UP -> { v = v.and(1.inv()); true }
                    else -> false
            }) {
                actions.add(CursorAction(v, 0f, 0f))
            }
            false
        }

        rightClick = findViewById(R.id.right_click)
        rightClick?.setOnTouchListener { _, ev ->
            if (when (ev.action ) {
                MotionEvent.ACTION_DOWN -> { v = v.or(2); true }
                MotionEvent.ACTION_UP -> { v = v.and(2.inv()); true }
                else -> false
            }) {
                actions.add(CursorAction(v, 0f, 0f))
            }
            false
        }

        keyboardZone = findViewById(R.id.keyboard_zone)
        keys = Hashtable()
        for (def in listOf(
            KeyDef("esc", "escape"),
            KeyDef("↑", "up"),
            KeyDef("↓", "down"),
            KeyDef("←", "left"),
            KeyDef("→", "right"),
            KeyDef("sh", "shift"),
            KeyDef("ctl", "ctrl"),
            KeyDef("win", "win"),
            KeyDef("alt", "alt"))) {

            val btn = layoutInflater.inflate(R.layout.button, layout, false) as Button
            btn.id = def.s.hashCode()
            val layoutParams = ConstraintLayout.LayoutParams(
                0,
                ConstraintLayout.LayoutParams.WRAP_CONTENT)
            btn.layoutParams = layoutParams
            btn.text = def.s
            btn.setOnTouchListener { _, ev ->
                when (ev.action) {
                    MotionEvent.ACTION_DOWN -> actions.add(KeyboardAction(def.v, 1))
                    MotionEvent.ACTION_UP -> actions.add(KeyboardAction(def.v, 2))
                }
                false
            }
            layout?.addView(btn)
            keyboardZone?.addView(btn)
            keys?.put(def.s, btn)
        }

        toggleInput = findViewById(R.id.toggle_input)
        toggleInput?.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            textInput?.requestFocus()
            imm.showSoftInput(textInput, InputMethodManager.SHOW_IMPLICIT)
        }
        keyboardZone?.removeView(toggleInput)
        keyboardZone?.addView(toggleInput)

        textInput = findViewById(R.id.text_input)
        textInput?.setOnKeyListener { _, keycode, ev ->
            val v = when (keycode) {
                KeyEvent.KEYCODE_DEL -> "backspace"
                else -> null
            }
            if (v != null) {
                when (ev?.action) {
                    KeyEvent.ACTION_DOWN -> actions.add(KeyboardAction(v, 1))
                    KeyEvent.ACTION_UP -> actions.add(KeyboardAction(v, 2))
                }
            }
            true
        }
        textInput?.setOnEditorActionListener { _, _, _ ->
            actions.add(KeyboardAction("enter", 3))
            true
        }
        textInput?.doOnTextChanged { text, _, _, _ ->
            if (text?.isNotEmpty() == true)
                actions.add(KeyboardAction(text.toString(), 4))
            if (!text?.toString().equals("")) {
                textInput?.setText("")
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        menu?.findItem(R.id.edit_address)?.setOnMenuItemClickListener {
            startActivity(Intent(this, PreferencesActivity::class.java))
            true
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onResume() {
        super.onResume()

        val prefs = getDefaultSharedPreferences(this)
        this.prefs = prefs

        if (prefs != null) {
            val savedFullAddr = prefs.getString("pref_addr", "") ?: "address:port"
            val (savedAddr, savePort) = ("$savedFullAddr:8000").split(":")
            serverAddress = savedAddr
            serverPort = savePort.toIntOrNull()

            cursorHMult = prefs.getString("pref_cursor_h_mult", "1")?.toFloatOrNull() ?: 1f
            cursorVMult = prefs.getString("pref_cursor_v_mult", "1")?.toFloatOrNull() ?: 1f

            wheelMult = prefs.getString("pref_wheel_mult", "0.1")?.toFloatOrNull() ?: 0.1f

            applyAccel = prefs.getBoolean("pref_accel_modifier", true)
        }

        started = true
        connect()
    }

    override fun onPause() {
        super.onPause()

        started = false
    }

    private fun alert(given: String?) {
        runOnUiThread {
            val s: String = given ?: "Error"
            Toast.makeText(this, s, 30 + s.length * 2).show()
        }
    }

    private fun connect() {
        val addr = serverAddress
        val port = serverPort
        Thread {
            var out: OSCPortOut? = null
            try {
                out = OSCPortOut(InetAddress.getByName(addr), port!!)
                val toSend = ArrayList<IAction>()
                while (started) {
                    actions.drainTo(toSend)
                    for (a in toSend) when(a) {
                        is CursorAction -> out.send(OSCMessage("/mouse", listOf(a.v, a.x, a.y)))
                        is KeyboardAction -> out.send(OSCMessage("/keyboard", listOf(a.keyCode, a.pressed)))
                    }
                    toSend.clear()

                    if (addr != serverAddress || port != serverPort)
                        break
                }
            } catch(e: Exception) {
                actions.clear()
                alert(e.message)
            } finally {
                out?.close()
            }
        }.start()
    }
}