package com.daivikcarwash.formkeyai

import android.content.ClipDescription\nimport android.content.ClipboardManager\nimport android.content.Context\nimport android.content.Intent\nimport android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo\nimport android.view.inputmethod.InputConnection
import android.widget.Button

class KeyboardService : InputMethodService() {
    private var hindiMode = false
    private val typed = StringBuilder()

    private val englishKeys = mapOf(
        R.id.k_q to "q", R.id.k_w to "w", R.id.k_e to "e", R.id.k_r to "r",
        R.id.k_t to "t", R.id.k_y to "y", R.id.k_u to "u", R.id.k_i to "i",
        R.id.k_o to "o", R.id.k_p to "p", R.id.k_a to "a", R.id.k_s to "s",
        R.id.k_d to "d", R.id.k_f to "f", R.id.k_g to "g", R.id.k_h to "h",
        R.id.k_j to "j", R.id.k_k to "k", R.id.k_l to "l", R.id.k_z to "z",
        R.id.k_x to "x", R.id.k_c to "c", R.id.k_v to "v", R.id.k_b to "b",
        R.id.k_n to "n", R.id.k_m to "m"
    )

    // Basic Hindi character layout. This is direct Devanagari input, not transliteration.
    private val hindiKeys = mapOf(
        R.id.k_q to "ौ", R.id.k_w to "ै", R.id.k_e to "ा", R.id.k_r to "ी",
        R.id.k_t to "ू", R.id.k_y to "ब", R.id.k_u to "ह", R.id.k_i to "ग",
        R.id.k_o to "द", R.id.k_p to "ज", R.id.k_a to "ो", R.id.k_s to "े",
        R.id.k_d to "्", R.id.k_f to "ि", R.id.k_g to "ु", R.id.k_h to "प",
        R.id.k_j to "र", R.id.k_k to "क", R.id.k_l to "त", R.id.k_z to "ं",
        R.id.k_x to "म", R.id.k_c to "न", R.id.k_v to "व", R.id.k_b to "ल",
        R.id.k_n to "स", R.id.k_m to "य"
    )

    private val suggestions = listOf(
        "hello", "help", "good", "thanks", "thank", "please", "today", "tomorrow",
        "नमस्ते", "धन्यवाद", "भारत", "मेरा", "आप", "है", "हैं", "करना", "कैसे"
    )

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        val keyButtons = (englishKeys.keys).associateWith { view.findViewById<Button>(it) }

        fun refreshSuggestions() {
            val row = typed.toString()
            val matches = if (row.isBlank()) listOf("FormKey", "नमस्ते", "Hello")
                else suggestions.filter { it.startsWith(row, ignoreCase = true) && it != row }.take(3)
            val ids = listOf(R.id.suggestion_one, R.id.suggestion_two, R.id.suggestion_three)
            ids.forEachIndexed { index, id ->
                val button = view.findViewById<Button>(id)
                val value = matches.getOrNull(index).orEmpty()
                button.text = value
                button.visibility = if (value.isEmpty()) View.INVISIBLE else View.VISIBLE
                button.setOnClickListener {
                    if (value.isNotEmpty()) {
                        currentInputConnection?.commitText(value, 1)
                        typed.clear()
                        refreshSuggestions()
                    }
                }
            }
        }

        fun updateLanguage() {
            view.findViewById<Button>(R.id.key_space).text = if (hindiMode) "हिंदी" else "English"
            keyButtons.forEach { (id, button) ->
                button.text = if (hindiMode) hindiKeys[id] ?: "" else englishKeys[id] ?: ""
            }
        }

        keyButtons.forEach { (id, button) ->
            button.setOnClickListener {
                val value = if (hindiMode) hindiKeys[id] else englishKeys[id]
                if (value != null) {
                    currentInputConnection?.commitText(value, 1)
                    if (!hindiMode) typed.append(value) else typed.clear()
                    refreshSuggestions()
                }
            }
        }

        view.findViewById<Button>(R.id.key_space).setOnClickListener {
            hindiMode = !hindiMode
            typed.clear()
            updateLanguage()
            refreshSuggestions()
        }
        view.findViewById<Button>(R.id.key_spacebar).setOnClickListener {
            currentInputConnection?.commitText(" ", 1)
            typed.clear()
            refreshSuggestions()
        }
        view.findViewById<Button>(R.id.key_scan).setOnClickListener {\n            val launch = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)\n            startActivity(launch)\n        }\n        view.findViewById<Button>(R.id.key_fill).setOnClickListener {\n            fillMatchingField()\n        }\n        view.findViewById<Button>(R.id.key_paste).setOnClickListener {\n            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager\n            val clip = clipboard.primaryClip\n            if (clip != null && clip.itemCount > 0) {\n                val value = clip.getItemAt(0).coerceToText(this)\n                if (!value.isNullOrEmpty()) currentInputConnection?.commitText(value, 1)\n            }\n        }\n        view.findViewById<Button>(R.id.key_comma).setOnClickListener {
            currentInputConnection?.commitText(",", 1)
            typed.clear()
            refreshSuggestions()
        }
        view.findViewById<Button>(R.id.key_period).setOnClickListener {
            currentInputConnection?.commitText(".", 1)
            typed.clear()
            refreshSuggestions()
        }
        view.findViewById<Button>(R.id.key_delete).setOnClickListener {
            currentInputConnection?.deleteSurroundingText(1, 0)
            if (typed.isNotEmpty()) typed.deleteCharAt(typed.length - 1)
            refreshSuggestions()
        }
        view.findViewById<Button>(R.id.key_enter).setOnClickListener {
            val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
            if (action != null && action != EditorInfo.IME_ACTION_NONE) {
                currentInputConnection?.performEditorAction(action)
            } else {
                currentInputConnection?.commitText("\n", 1)
            }
            typed.clear()
            refreshSuggestions()
        }

        updateLanguage()
        refreshSuggestions()
        return view
    }
}    private fun fillMatchingField() {
        val info = currentInputEditorInfo
        val hint = (info?.hintText?.toString() ?: "").lowercase()
        val prefs = getSharedPreferences("formkey_scan", Context.MODE_PRIVATE)
        val fields = prefs.all.filterKeys { it != "raw_text" }.toSortedMap()
        if (fields.isEmpty()) {
            currentInputConnection?.commitText("", 1)
            return
        }
        val aliases = listOf(
            listOf("name", "नाम") to listOf("Name", "Possible Name (verify)"),
            listOf("father", "पिता") to listOf("Father Name"),
            listOf("address", "पता") to listOf("Address"),
            listOf("mobile", "phone", "मोबाइल") to listOf("Mobile Number"),
            listOf("email", "ईमेल") to listOf("Email"),
            listOf("dob", "birth", "जन्म") to listOf("Date of Birth"),
            listOf("gender", "sex", "लिंग") to listOf("Gender"),
            listOf("pin", "postal", "zip") to listOf("PIN Code"),
            listOf("pan") to listOf("PAN Number"),
            listOf("aadhaar", "aadhar", "uid") to listOf("Aadhaar Number"),
            listOf("passport") to listOf("Passport Number"),
            listOf("licence", "license", "dl") to listOf("Driving Licence")
        )
        val matched = aliases.firstOrNull { (words, _) -> words.any { hint.contains(it) } }
            ?.second?.firstNotNullOfOrNull { fields[it] as? String }
        val selected = matched ?: run {
            val keys = fields.keys.toList()
            val index = prefs.getInt("fill_index", 0).mod(keys.size)
            prefs.edit().putInt("fill_index", (index + 1) % keys.size).apply()
            fields[keys[index]] as? String
        }
        if (!selected.isNullOrBlank()) {
            currentInputConnection?.commitText(selected, 1)
        }
    }
}\n