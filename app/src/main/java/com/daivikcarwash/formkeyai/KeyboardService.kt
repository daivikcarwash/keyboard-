package com.daivikcarwash.formkeyai

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button

class KeyboardService : InputMethodService() {
    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        val keys = mapOf(
            R.id.k_q to "q", R.id.k_w to "w", R.id.k_e to "e", R.id.k_r to "r",
            R.id.k_t to "t", R.id.k_y to "y", R.id.k_u to "u", R.id.k_i to "i",
            R.id.k_o to "o", R.id.k_p to "p", R.id.k_a to "a", R.id.k_s to "s",
            R.id.k_d to "d", R.id.k_f to "f", R.id.k_g to "g", R.id.k_h to "h",
            R.id.k_j to "j", R.id.k_k to "k", R.id.k_l to "l", R.id.k_z to "z",
            R.id.k_x to "x", R.id.k_c to "c", R.id.k_v to "v", R.id.k_b to "b",
            R.id.k_n to "n", R.id.k_m to "m"
        )
        keys.forEach { (id, value) ->
            view.findViewById<Button>(id).setOnClickListener {
                currentInputConnection?.commitText(value, 1)
            }
        }
        view.findViewById<Button>(R.id.key_spacebar).setOnClickListener {
            currentInputConnection?.commitText(" ", 1)
        }
        view.findViewById<Button>(R.id.key_space).setOnClickListener {
            currentInputConnection?.commitText(" ", 1)
        }
        view.findViewById<Button>(R.id.key_comma).setOnClickListener {
            currentInputConnection?.commitText(",", 1)
        }
        view.findViewById<Button>(R.id.key_period).setOnClickListener {
            currentInputConnection?.commitText(".", 1)
        }
        view.findViewById<Button>(R.id.key_delete).setOnClickListener {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
        view.findViewById<Button>(R.id.key_enter).setOnClickListener {
            val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
            if (action != null && action != EditorInfo.IME_ACTION_NONE) {
                currentInputConnection?.performEditorAction(action)
            } else {
                currentInputConnection?.commitText("\n", 1)
            }
        }
        return view
    }
}
