package com.lnkv.nfcemulator

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun ServerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("server_prefs", Context.MODE_PRIVATE)
    var isExternal by rememberSaveable { mutableStateOf(prefs.getBoolean("isExternal", true)) }
    var ip by rememberSaveable { mutableStateOf(prefs.getString("ip", "0.0.0.0:0000")!!) }
    var pollingTime by rememberSaveable { mutableStateOf(prefs.getString("pollingTime", "0")!!) }
    var autoConnect by rememberSaveable { mutableStateOf(prefs.getBoolean("autoConnect", false)) }
    val serverState = ServerConnectionManager.state
    val isProcessing = ServerConnectionManager.isProcessing
    var port by rememberSaveable { mutableStateOf(prefs.getString("port", "0000")!!) }
    var staticPort by rememberSaveable { mutableStateOf(prefs.getBoolean("staticPort", false)) }
    var autoStart by rememberSaveable { mutableStateOf(prefs.getBoolean("autoStart", false)) }
    val internalState = InternalServerManager.state
    val isServerRunning = internalState == "Running"
    val localIp = remember { getLocalIpAddress(context) }
    val ipRegex = Regex("^(25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}:(\\d{1,5})$")

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ServerType")
        ) {
            SegmentedButton(
                selected = isExternal,
                onClick = {
                    isExternal = true
                    prefs.edit().putBoolean("isExternal", true).apply()
                },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                label = { Text("External") },
                modifier = Modifier.testTag("ExternalToggle")
            )
            SegmentedButton(
                selected = !isExternal,
                onClick = {
                    isExternal = false
                    prefs.edit().putBoolean("isExternal", false).apply()
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = { Text("Internal") },
                modifier = Modifier.testTag("InternalToggle")
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (isExternal) {
            OutlinedTextField(
                value = ip,
                onValueChange = { value ->
                    if (value.matches(Regex("[0-9.:]*"))) {
                        ip = value
                    } else {
                        Toast.makeText(
                            context,
                            "IP can contain only digits, dots and colon",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("IpField"),
                label = { Text("Server IP:Port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                enabled = serverState != "Connected" && !isProcessing,
                trailingIcon = {
                    IconButton(onClick = { ip = "" }, modifier = Modifier.testTag("IpClear")) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear IP")
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = pollingTime,
                onValueChange = { value ->
                    if (value.matches(Regex("\\d*"))) {
                        if (value.isEmpty() || value.toInt() <= 10000) {
                            pollingTime = value
                        } else {
                            Toast.makeText(
                                context,
                                "Max polling time is 10000",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Polling time can contain only digits",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                label = { Text("Polling Time [Ms]") },
                placeholder = { Text("Polling Time [Ms]") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = pollingTime.isNotEmpty() && pollingTime.toInt() < 10,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("PollingField"),
                enabled = serverState != "Connected" && !isProcessing,
                trailingIcon = {
                    IconButton(onClick = { pollingTime = "" }, modifier = Modifier.testTag("PollingClear")) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear Polling Time")
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleCheckbox(
                    checked = autoConnect,
                    onCheckedChange = {
                        autoConnect = it
                        prefs.edit().putBoolean("autoConnect", it).apply()
                    },
                    modifier = Modifier.testTag("AutoConnectCheck"),
                    enabled = serverState != "Connected" && !isProcessing
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect Automatically")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                buildAnnotatedString {
                    append("Server State: ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(serverState)
                    }
                },
                modifier = Modifier.testTag("ServerState")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        prefs.edit()
                            .putBoolean("isExternal", isExternal)
                            .putString("ip", ip)
                            .putString("pollingTime", pollingTime)
                            .putBoolean("autoConnect", autoConnect)
                            .apply()
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    },
                    enabled = serverState != "Connected" && !isProcessing,
                    modifier = Modifier.weight(1f).testTag("SaveServer")
                ) {
                    Text("Save")
                }
                Button(
                    onClick = {
                        if (ipRegex.matches(ip)) {
                            ServerConnectionManager.connect(
                                context,
                                ip.substringBefore(':'),
                                ip.substringAfter(':').toInt(),
                                pollingTime.toLongOrNull() ?: 0
                            )
                        } else {
                            Toast.makeText(context, "Invalid IP", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = serverState != "Connected" && !isProcessing,
                    modifier = Modifier.weight(1f).testTag("ConnectServer")
                ) {
                    Text("Connect")
                }
                Button(
                    onClick = { ServerConnectionManager.disconnect() },
                    enabled = serverState == "Connected" && !isProcessing,
                    modifier = Modifier.weight(1f).testTag("DisconnectServer")
                ) {
                    Text("Disconnect")
                }
            }
        } else {
            OutlinedTextField(
                value = port,
                onValueChange = { value ->
                    if (value.matches(Regex("\\d*"))) {
                        port = value
                    }
                },
                label = { Text("Port") },
                placeholder = { Text("Port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("PortField"),
                enabled = !isServerRunning,
                trailingIcon = {
                    IconButton(onClick = { port = "" }, modifier = Modifier.testTag("PortClear")) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear Port")
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleCheckbox(
                    checked = staticPort,
                    onCheckedChange = {
                        staticPort = it
                        prefs.edit().putBoolean("staticPort", it).apply()
                    },
                    modifier = Modifier.testTag("StaticPortCheck"),
                    enabled = !isServerRunning
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Static Port")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleCheckbox(
                    checked = autoStart,
                    onCheckedChange = {
                        autoStart = it
                        prefs.edit().putBoolean("autoStart", it).apply()
                    },
                    modifier = Modifier.testTag("AutoStartCheck"),
                    enabled = !isServerRunning
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Automatically")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                buildAnnotatedString {
                    append("Server State: ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(internalState)
                    }
                },
                modifier = Modifier.testTag("InternalServerState")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        prefs.edit()
                            .putString("port", port)
                            .putBoolean("staticPort", staticPort)
                            .putBoolean("autoStart", autoStart)
                            .apply()
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    },
                    enabled = !isServerRunning,
                    modifier = Modifier.weight(1f).testTag("SaveInternalServer")
                ) {
                    Text("Save")
                }
                Button(
                    onClick = {
                        val portNum = port.toIntOrNull() ?: 0
                        InternalServerManager.start(portNum)
                    },
                    enabled = !isServerRunning,
                    modifier = Modifier.weight(1f).testTag("StartInternalServer")
                ) {
                    Text("Start")
                }
                Button(
                    onClick = { InternalServerManager.stop() },
                    enabled = isServerRunning,
                    modifier = Modifier.weight(1f).testTag("StopInternalServer")
                ) {
                    Text("Stop")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Local IP: ${localIp ?: "Unknown"}")
        }
    }
}

@Composable
fun CircleCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val background = if (checked) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderColor = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(background)
            .border(2.dp, borderColor, CircleShape)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
