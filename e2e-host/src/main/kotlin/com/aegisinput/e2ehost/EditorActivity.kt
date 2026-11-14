package com.aegisinput.e2ehost

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** Test-only target. Receives real InputConnection calls; no IME internals. */
class EditorActivity : Activity() {
    companion object { @Volatile var current: EditorActivity? = null }
    override fun onResume() { super.onResume(); current = this }
    override fun onPause() { current = null; super.onPause() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val state = TextView(this).apply { id = R.id.state; text = "composition:none" }
        val action = TextView(this).apply { id = R.id.action; text = "action:none" }
        val editor = EditText(this).apply {
            id = R.id.editor
            hint = "E2E editor"
            inputType = when (intent.getStringExtra("kind")) {
                "password" -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                "search" -> InputType.TYPE_CLASS_TEXT
                else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            }
            imeOptions = if (intent.getStringExtra("kind") == "search") EditorInfo.IME_ACTION_SEARCH else EditorInfo.IME_ACTION_NONE
            setOnEditorActionListener { _, actionId, _ -> action.text = "action:$actionId"; intent.getStringExtra("kind") == "search" }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    state.text = "composition:" + if (s != null && BaseInputConnection.getComposingSpanStart(s) >= 0) "active" else "none"
                }
            })
        }
        layout.addView(editor, LinearLayout.LayoutParams(-1, 100))
        layout.addView(state); layout.addView(action)
        setContentView(layout)
    }
}
