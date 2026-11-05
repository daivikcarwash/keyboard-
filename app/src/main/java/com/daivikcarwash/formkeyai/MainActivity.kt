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
            text = "Scan a document, review the details, then copy a field into any form."
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
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
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
                latin.process(image)
                    .addOnSuccessListener { latinText ->
                        devanagari.process(image)
                            .addOnSuccessListener { hindiText ->
                                val combined = (latinText.text + "\n" + hindiText.text)
                                    .lines().map { it.trim() }.filter { it.isNotBlank() }.distinct()
                                output.append(combined.joinToString("\n")).append("\n")
                                process(index + 1)
                            }
                            .addOnFailureListener {
                                output.append(latinText.text).append("\n")
                                process(index + 1)
                            }
                    }
                    .addOnFailureListener {
                        process(index + 1)
                    }
            } catch (e: Exception) {
                process(index + 1)
            }
        }
        process(0)
    }

    private fun showExtracted(raw: String) {
        content.removeAllViews()
        if (raw.isBlank()) {
            showMessage("Text was not detected. Try a clearer, well-lit scan.")
            return
        }
        val title = TextView(this).apply {
            text = "Review extracted details"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 8, 0, 8)
        }
        content.addView(title)
        val hint = TextView(this).apply {
            text = "Tap Copy beside a value, then tap the target field and use Paste Scan on the keyboard. Check every value before submitting."
            textSize = 14f
            setPadding(0, 0, 0, 12)
        }
        content.addView(hint)
        val lines = raw.lines().map { it.trim() }.filter { it.length >= 2 }.distinct()
        lines.take(60).forEach { line ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(8, 5, 8, 5)
                setBackgroundColor(0xFFFFFFFF.toInt())
            }
            row.addView(TextView(this).apply {
                text = line
                textSize = 15f
                setTextColor(0xFF172554.toInt())
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(Button(this).apply {
                text = "Copy"
                setOnClickListener { copyText(line) }
            })
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = 5
            content.addView(row, params)
        }
        content.addView(Button(this).apply {
            text = "Copy all scanned text"
            setOnClickListener { copyText(lines.joinToString("\n")) }
        })
        content.addView(TextView(this).apply {
            text = "\nRaw OCR text\n\n" + lines.joinToString("\n")
            textSize = 13f
            setTextColor(0xFF475569.toInt())
        })
    }

    private fun copyText(value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("FormKey scanned detail", value))
        Toast.makeText(this, "Copied. Tap a form field, then Paste Scan.", Toast.LENGTH_SHORT).show()
    }

    private fun showMessage(message: String) {
        content.removeAllViews()
        content.addView(TextView(this).apply { text = message; textSize = 16f; setPadding(8, 16, 8, 16) })
    }
}
