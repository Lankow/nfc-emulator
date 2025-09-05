package com.lnkv.nfcemulator

import android.content.ComponentName
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lnkv.nfcemulator.cardservice.TypeAEmulatorService
import org.json.JSONArray

private const val AID_PREFS = "nfc_aids"
private const val AID_KEY = "aids"

internal fun isValidAid(aid: String): Boolean {
    return aid.length in 10..32 && aid.length % 2 == 0 && aid.all { it in '0'..'9' || it in 'A'..'F' }
}

private fun loadAids(context: Context): SnapshotStateList<String> {
    val prefs = context.getSharedPreferences(AID_PREFS, Context.MODE_PRIVATE)
    val set = prefs.getStringSet(AID_KEY, setOf("F0010203040506")) ?: emptySet()
    return set.filter(::isValidAid).toMutableList().toMutableStateList()
}

private fun saveAids(context: Context, aids: List<String>) {
    val validAids = aids.filter(::isValidAid)
    if (validAids.size != aids.size) {
        Toast.makeText(context, "Some AIDs were invalid and ignored", Toast.LENGTH_SHORT).show()
    }
    val prefs = context.getSharedPreferences(AID_PREFS, Context.MODE_PRIVATE)
    prefs.edit().putStringSet(AID_KEY, validAids.toSet()).apply()
    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    val cardEmulation = CardEmulation.getInstance(nfcAdapter)
    val component = ComponentName(context, TypeAEmulatorService::class.java)
    if (validAids.isEmpty()) {
        cardEmulation.removeAidsForService(component, CardEmulation.CATEGORY_OTHER)
    } else {
        cardEmulation.registerAidsForService(
            component,
            CardEmulation.CATEGORY_OTHER,
            validAids
        )
    }
}

private fun exportAids(context: Context, aids: List<String>, uri: Uri) {
    if (aids.isEmpty()) return
    context.contentResolver.openOutputStream(uri)?.use { os ->
        val arr = JSONArray()
        aids.forEach { arr.put(it) }
        os.write(arr.toString().toByteArray())
    }
    val name = uri.path?.substringAfterLast('/') ?: "aids.json"
    Toast.makeText(context, "AIDs saved to file: $name", Toast.LENGTH_SHORT).show()
}

private fun importAids(context: Context, uri: Uri): List<String> {
    val text = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
        ?: return emptyList()
    val arr = JSONArray(text)
    val list = mutableListOf<String>()
    for (i in 0 until arr.length()) {
        val aid = arr.getString(i).uppercase()
        if (isValidAid(aid)) list.add(aid)
    }
    return list
}

@Composable
fun AidScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val aids = remember { loadAids(context) }
    var newAid by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            exportAids(context, aids, uri)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val imported = importAids(context, uri)
            aids.clear()
            aids.addAll(imported)
            saveAids(context, aids)
            Toast.makeText(context, "${imported.size} item(s) have been imported.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newAid,
                onValueChange = { input ->
                    val filtered = input.uppercase()
                    if (filtered.length <= 32 && filtered.all { it in '0'..'9' || it in 'A'..'F' }) newAid = filtered
                },
                label = { Text("New AID") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (isValidAid(newAid)) {
                        if (!aids.contains(newAid)) {
                            aids.add(newAid)
                            saveAids(context, aids)
                        }
                        newAid = ""
                    } else {
                        Toast.makeText(context, "Invalid AID", Toast.LENGTH_SHORT).show()
                    }
                }
            ) { Text("Add") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { exportLauncher.launch("aids.json") }, modifier = Modifier.weight(1f)) {
                Text("Export")
            }
            Button(onClick = { importLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.weight(1f)) {
                Text("Import")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(aids) { index, aid ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(aid, modifier = Modifier.weight(1f), fontSize = 12.sp)
                        val clipboard = LocalClipboardManager.current
                        IconButton(onClick = { clipboard.setText(AnnotatedString(aid)) }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy AID")
                        }
                        IconButton(onClick = {
                            aids.removeAt(index)
                            saveAids(context, aids)
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete AID")
                        }
                    }
                    if (index < aids.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
