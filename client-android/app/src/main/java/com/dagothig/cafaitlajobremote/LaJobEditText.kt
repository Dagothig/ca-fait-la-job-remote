package com.dagothig.cafaitlajobremote

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.appcompat.widget.AppCompatEditText

class LaJobEditText: AppCompatEditText {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    var keyPress: KeyboardListener? = null
    var keyRelease: KeyboardListener? = null

    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection {
        return LaJobInputConnection(
            this,
            super.onCreateInputConnection(outAttrs),
            true)
    }
}

class LaJobInputConnection(private val widget: LaJobEditText, target: InputConnection?, mutable: Boolean) :
    InputConnectionWrapper(target, mutable) {

    override fun sendKeyEvent(event: KeyEvent?): Boolean {
        when (event?.action) {
            KeyEvent.ACTION_DOWN -> widget.keyPress?.onKey(event)
            KeyEvent.ACTION_UP -> widget.keyRelease?.onKey(event)
        }
        return true
    }
}

fun interface KeyboardListener {
    fun onKey(ev: KeyEvent?)
}

