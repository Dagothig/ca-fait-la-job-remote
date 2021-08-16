package com.dagothig.cafaitlajobremote

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.illposed.osc.OSCMessage
import com.illposed.osc.transport.udp.OSCPortOut
import java.net.InetAddress
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

interface IAction {}

data class CursorAction(val v: Int, val x: Float, val y: Float): IAction
data class KeyboardAction(val keyCode: Int, val pressed: Int): IAction

class MainActivity() : AppCompatActivity() {
    private var serverAddress: String = ""
    private var serverPort: Int? = null
    private var prefs: SharedPreferences? = null
    private var started: Boolean = true

    private var cursorZone: View? = null
    private var cursorIndicator: View? = null
    private var leftClick: Button? = null
    private var rightClick: Button? = null

    //private var keyboardInput: EditText? = null

    private var x: Float = 0.5f
    private var y: Float = 0.5f
    private var v: Int = 0

    private val actions: BlockingQueue<IAction>

    init {
        actions = ArrayBlockingQueue(10)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cursorIndicator = findViewById(R.id.cursor_indicator)

        cursorZone = findViewById(R.id.zone_cursor)
        cursorZone?.setOnTouchListener { view, ev ->
            x = ev.x / view.width
            y = ev.y / view.height
            actions.add(CursorAction(v, x, y))
            cursorIndicator?.x = ev.x - ((cursorIndicator?.width ?: 0) / 2)
            cursorIndicator?.y = ev.y - ((cursorIndicator?.height ?: 0) / 2)
            true
        }

        leftClick = findViewById(R.id.left_click)
        leftClick?.setOnTouchListener { _, ev ->
            if (when (ev.action ) {
                MotionEvent.ACTION_DOWN -> { v = v.or(1); true }
                MotionEvent.ACTION_UP -> { v = v.and(1.inv()); true }
                    else -> false
            }) {
                actions.add(CursorAction(v, x, y))
            }
            true
        }

        rightClick = findViewById(R.id.right_click)
        rightClick?.setOnTouchListener { _, ev ->
            if (when (ev.action ) {
                MotionEvent.ACTION_DOWN -> { v = v.or(2); true }
                MotionEvent.ACTION_UP -> { v = v.and(2.inv()); true }
                else -> false
            }) {
                actions.add(CursorAction(v, x, y))
            }
            true
        }

        /*keyboardInput = findViewById((R.id.keyboard_input))
        keyboardInput?.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(str: Editable?) {}

            override fun onTextChanged(str: CharSequence?, start: Int, before: Int, count: Int) {
                println(str)
                if (str != "")
                    keyboardInput?.setText("")
            }

        })
        keyboardInput?.setOnEditorActionListener { v, keyCode, event ->
            when (event.action) {
                KeyEvent.ACTION_DOWN -> actions.add(KeyboardAction(keyCode, 1))
                KeyEvent.ACTION_UP -> actions.add(KeyboardAction(keyCode, 0))
            }
            true
        }*/
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        menu?.findItem(R.id.edit_address)?.setOnMenuItemClickListener {
            askServer()
            true
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onStart() {
        super.onStart()

        started = true
        prefs = getSharedPreferences("pref", MODE_PRIVATE)
        val savedFullAddr = prefs?.getString("addr", "")
        val (savedAddr, savePort) = savedFullAddr?.split(":") ?: listOf("", "")
        serverAddress = savedAddr
        serverPort = savePort.toIntOrNull()

        connect()
    }

    override fun onStop() {
        super.onStop()

        started = false
    }

    private fun alert(given: String?) {
        val s: String = given ?: "Error"
        Toast.makeText(this, s, 30 + s.length * 2).show()
    }

    @SuppressLint("SetTextI18n")
    private fun askServer() {
        val input = EditText(this)
        input.hint = "address:port"
        input.setText("$serverAddress:$serverPort")
        AlertDialog.Builder(this)
            .setTitle("Enter address")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                var success = true
                try {
                    val (givenAddress, givenPort) = input.text.toString().split(":")
                    serverAddress = givenAddress
                    serverPort = givenPort.toIntOrNull()
                    prefs?.edit()?.putString("addr", "$serverAddress:$serverPort")?.apply()
                } catch(e: Exception) {
                    alert(e.message)
                    success = false
                }
                if (success)
                    connect()
                else
                    askServer()
            }
            .setOnCancelListener {
                finish()
            }
            .show()
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
                    }
                    toSend.clear()

                    if (addr != serverAddress || port != serverPort)
                        break
                }
            } catch(e: Exception) {
                alert(e.message)
                askServer()
            } finally {
                out?.close()
            }
        }.start()
    }
}