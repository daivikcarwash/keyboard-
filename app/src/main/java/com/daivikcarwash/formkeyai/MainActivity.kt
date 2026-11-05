package com.daivikcarwash.formkeyai

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.mlkit.documentscanner.GmsDocumentScannerOptions
import com.google.android.gms.mlkit.documentscanner.GmsDocumentScanning
import com.google.android.gms.mlkit.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions

class MainActivity : Activity() {
    private lateinit var content: LinearLayout
    private val prefs by lazy { getSharedPreferences("formkey_scan", Context.MODE_PRIVATE) }
    private val scannerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val scan = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pages = scan?.pages.orEmpty()
            if (pages.isEmpty()) showMessage("No scanned page found. Please try again.")
            else readPages(pages.map { it.imageUri })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(0xFFF5F7FB.toInt())
        }
        root.addView(TextView(this).apply {
            text = "FormKey AI"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF172554.toInt())
        })
        root.addView(TextView(this).apply {
            text = "Scan documents, review extracted fields and fill forms one field at a time."
            textSize = 15f
            setPadding(0, 8, 0, 16)
        })
        root.addView(Button(this).apply {
            text = "Scan any document"
            textSize = 17f
            setOnClickListener { startScanner() }
        })
        root.addView(Button(this).apply {
            text = "Enable FormKey AI keyboard"
            setOnClickListener { startActivity(Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })
        root.addView(Button(this).apply {
            text = "Clear saved scan details"
            setOnClickListener {
                prefs.edit().clear().apply()
                showMessage("Saved scan details cleared from this app.")
            }
        })
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        showSavedFields()
    }

    private fun startScanner() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options).getStartScanIntent(this)
            .addOnSuccessListener { sender ->
                scannerLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(sender).build())
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Scanner could not start: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun readPages(uris: List<Uri>) {
        content.removeAllViews()
        showMessage("Reading document text…")
        val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val devanagari = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
        val output = StringBuilder()
        fun process(index: Int) {
            if (index >= uris.size) {
                latin.close()
                devanagari.close()
                showExtracted(output.toString().trim())
                return
            }
            try {
                val image = InputImage.fromFilePath(this, uris[index])
                latin.process(image).addOnSuccessListener { latinText ->
                    devanagari.process(image).addOnSuccessListener { hindiText ->
                        val lines = (latinText.text + "\n" + hindiText.text)
                            .lines().map { it.trim() }.filter { it.isNotBlank() }.distinct()
                        output.append(lines.joinToString("\n")).append("\n")
                        process(index + 1)
                    }.addOnFailureListener {
                        output.append(latinText.text).append("\n")
                        process(index + 1)
                    }
                }.addOnFailureListener { process(index + 1) }
            } catch (_: Exception) { process(index + 1) }
        }
        process(0)
    }

    private fun extractFields(raw: String): LinkedHashMap<String, String> {
        val text = raw.replace("\r", "\n")
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val result = linkedMapOf<String, String>()
        fun capture(label: String, regex: Regex, key: String = label) {
            val match = regex.find(text)
            if (match != null) result[key] = match.groupValues.last().trim().trim(':', '-', ' ')
        }
        capture("Aadhaar Number", Regex("(?<!\\d)(\\d{4}\\s?\\d{4}\\s?\\d{4})(?!\\d)"))
        capture("PAN Number", Regex("\\b([A-Z]{5}\\d{4}[A-Z])\\b", RegexOption.IGNORE_CASE))
        capture("Mobile Number", Regex("(?<!\\d)([6-9]\\d{9})(?!\\d)"))
        capture("Email", Regex("([A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,})", RegexOption.IGNORE_CASE))
        capture("PIN Code", Regex("(?<!\\d)(\\d{6})(?!\\d)"))
        capture("Date of Birth", Regex("(?:DOB|Date of Birth|Birth Date|जन्म तिथि)\\s*[:/-]?\\s*([0-3]?\\d[./-][01]?\\d[./-](?:19|20)?\\d{2})", RegexOption.IGNORE_CASE))
        capture("Driving Licence", Regex("(?:DL|Driving Licence|License No\\.?)[\\s:#-]*([A-Z]{2}\\d{2}\\s?\\d{4}\\s?\\d{7,})", RegexOption.IGNORE_CASE))
        capture("Passport Number", Regex("(?:Passport No\\.?|Passport Number)[\\s:#-]*([A-Z][0-9]{7})", RegexOption.IGNORE_CASE))
        val labelRules = listOf(
            "Name" to Regex("(?:Name|नाम)\\s*[:：-]\\s*([^\\n]{2,60})", RegexOption.IGNORE_CASE),
            "Father Name" to Regex("(?:Father'?s Name|Father Name|पिता का नाम)\\s*[:：-]\\s*([^\\n]{2,60})", RegexOption.IGNORE_CASE),
            "Address" to Regex("(?:Address|पता)\\s*[:：-]\\s*([^\\n]{4,160})", RegexOption.IGNORE_CASE),
            "Gender" to Regex("(?:Gender|Sex|लिंग)\\s*[:：-]\\s*(Male|Female|Other|पुरुष|महिला)", RegexOption.IGNORE_CASE)
        )
        labelRules.forEach { (key, regex) ->
            regex.find(text)?.groupValues?.getOrNull(1)?.trim()?.let { result[key] = it }
        }
        if (!result.containsKey("Name")) {
            val likely = lines.firstOrNull { line ->
                line.length in 4..45 && line.matches(Regex("[A-Za-z][A-Za-z .'-]+")) &&
                    !line.contains(Regex("government|india|unique|authority|income|tax", RegexOption.IGNORE_CASE))
            }
            if (likely != null) result["Possible Name (verify)"] = likely
        }
        return result
    }

    private fun showExtracted(raw: String) {
        content.removeAllViews()
        if (raw.isBlank()) {
            showMessage("Text was not detected. Try a clearer, well-lit scan.")
            return
        }
        val fields = extractFields(raw)
        val safeFields = fields.filterValues { it.isNotBlank() }
        prefs.edit().clear().putString("raw_text", raw).apply {
            safeFields.forEach { (key, value) -> putString(key, value) }
        }
        val title = TextView(this).apply {
            text = "Review extracted details"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 8, 0, 8)
        }
        content.addView(title)
        content.addView(TextView(this).apply {
            text = "OCR may misread characters. Verify every value. Tap Fill on the keyboard only after focusing the matching form field."
            textSize = 14f
            setPadding(0, 0, 0, 12)
        })
        if (safeFields.isEmpty()) {
            content.addView(TextView(this).apply { text = "No common labelled fields were confidently detected. You can copy the full OCR text below." })
        }
        safeFields.forEach { (key, value) -> addFieldRow(key, value) }
        content.addView(Button(this).apply {
            text = "Copy all scanned text"
            setOnClickListener { copyText(raw) }
        })
        content.addView(TextView(this).apply {
            text = "\nFull OCR text\n\n$raw"
            textSize = 13f
            setTextColor(0xFF475569.toInt())
        })
    }

    private fun showSavedFields() {
        val keys = prefs.all.keys.filter { it != "raw_text" }.sorted()
        if (keys.isEmpty()) {
            showMessage("No scan yet. Tap Scan any document to begin.")
            return
        }
        content.removeAllViews()
        content.addView(TextView(this).apply { text = "Saved scan details"; textSize = 20f; setTypeface(null, Typeface.BOLD) })
        keys.forEach { key -> prefs.getString(key, null)?.let { addFieldRow(key, it) } }
    }

    private fun addFieldRow(key: String, value: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 8, 10, 8)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        row.addView(TextView(this).apply { text = key; textSize = 12f; setTextColor(0xFF64748B.toInt()) })
        row.addView(TextView(this).apply { text = value; textSize = 16f; setTextColor(0xFF172554.toInt()) })
        row.addView(Button(this).apply {
            text = "Copy value"
            setOnClickListener { copyText(value) }
        })
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.bottomMargin = 6
        content.addView(row, params)
    }

    private fun copyText(value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("FormKey scanned detail", value))
        Toast.makeText(this, "Copied. Focus the matching form field and tap Fill on FormKey AI.", Toast.LENGTH_SHORT).show()
    }

    private fun showMessage(message: String) {
        content.removeAllViews()
        content.addView(TextView(this).apply { text = message; textSize = 16f; setPadding(8, 16, 8, 16) })
    }
}
