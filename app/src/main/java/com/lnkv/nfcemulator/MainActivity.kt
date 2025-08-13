package com.lnkv.nfcemulator

import android.content.ComponentName
import android.content.SharedPreferences
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lnkv.nfcemulator.cardservice.TypeAEmulatorService
import com.lnkv.nfcemulator.ui.theme.NFCEmulatorTheme

class MainActivity : ComponentActivity() {
    private lateinit var cardEmulation: CardEmulation
    private lateinit var componentName: ComponentName
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        cardEmulation = CardEmulation.getInstance(nfcAdapter)
        componentName = ComponentName(this, TypeAEmulatorService::class.java)
        prefs = getSharedPreferences("nfc_aids", MODE_PRIVATE)

        val storedAids = prefs.getStringSet("aids", setOf("F0010203040506"))!!.toList()
        registerAids(storedAids)

        setContent {
            NFCEmulatorTheme {
                var aid1 by rememberSaveable { mutableStateOf(storedAids.getOrNull(0) ?: "") }
                var aid2 by rememberSaveable { mutableStateOf(storedAids.getOrNull(1) ?: "") }
                var showAid1 by rememberSaveable { mutableStateOf(true) }
                var showAid2 by rememberSaveable { mutableStateOf(true) }
                val scrollState1 = rememberScrollState()
                val scrollState2 = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = showAid1,
                            onCheckedChange = { checked ->
                                if (!checked && !showAid2) return@Checkbox
                                showAid1 = checked
                            }
                        )
                        Text("Show AID 1")
                    }
                    if (showAid1) {
                        ScrollableTextField(aid1, { aid1 = it }, scrollState1, "AID 1")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = showAid2,
                            onCheckedChange = { checked ->
                                if (!checked && !showAid1) return@Checkbox
                                showAid2 = checked
                            }
                        )
                        Text("Show AID 2")
                    }
                    if (showAid2) {
                        ScrollableTextField(aid2, { aid2 = it }, scrollState2, "AID 2")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        val aids = mutableListOf<String>()
                        if (showAid1 && aid1.isNotBlank()) aids.add(aid1)
                        if (showAid2 && aid2.isNotBlank()) aids.add(aid2)
                        registerAids(aids)
                        prefs.edit().putStringSet("aids", aids.toSet()).apply()
                    }) {
                        Text("Save AIDs")
                    }
                }
            }
        }
    }

    private fun registerAids(aids: List<String>) {
        cardEmulation.registerAidsForService(componentName, CardEmulation.CATEGORY_OTHER, aids)
    }
}

@Composable
private fun ScrollableTextField(
    value: String,
    onValueChange: (String) -> Unit,
    scrollState: ScrollState,
    label: String
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .verticalScroll(scrollState),
        label = { Text(label) }
    )
}
