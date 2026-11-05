package com.daivikcarwash.formkeyai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(0xFFF5F7FB.toInt())
        }
        layout.addView(TextView(this).apply {
            text = "FormKey AI"
            textSize = 30f
            setTextColor(0xFF172554.toInt())
        })
        layout.addView(TextView(this).apply {
            text = "Your smart keyboard starts here."
            textSize = 16f
            setPadding(0, 12, 0, 28)
        })
        layout.addView(Button(this).apply {
            text = "1. Enable FormKey AI"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })
        layout.addView(Button(this).apply {
            text = "2. Select FormKey AI"
            setOnClickListener {
                val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                manager.showInputMethodPicker()
            }
        })
        setContentView(layout)
    }
}
