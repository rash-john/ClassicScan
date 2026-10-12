package com.classicscan.codes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.classicscan.R
import com.classicscan.ui.components.AppButton
import com.classicscan.ui.components.AppChip
import com.classicscan.ui.components.IconTile

fun codeLook(c: CodeContent): Triple<ImageVector, Color, Int> = when (c) {
    is CodeContent.Url -> Triple(Icons.Filled.Link, Color(0xFF2F6BFF), R.string.code_link)
    is CodeContent.Wifi -> Triple(Icons.Filled.Wifi, Color(0xFF00B894), R.string.code_wifi)
    is CodeContent.Phone -> Triple(Icons.Filled.Call, Color(0xFF16A085), R.string.code_phone)
    is CodeContent.Email -> Triple(Icons.Filled.Email, Color(0xFFE17055), R.string.code_email)
    is CodeContent.Sms -> Triple(Icons.Filled.Sms, Color(0xFF6C5CE7), R.string.code_sms)
    is CodeContent.Geo -> Triple(Icons.Filled.Map, Color(0xFF0984E3), R.string.code_location)
    is CodeContent.Contact -> Triple(Icons.Filled.Person, Color(0xFFE84393), R.string.code_contact)
    is CodeContent.Event -> Triple(Icons.Filled.CalendarMonth, Color(0xFFFF9F43), R.string.code_event)
    is CodeContent.Product -> Triple(Icons.Filled.ShoppingBag, Color(0xFFD4A017), R.string.code_product)
    is CodeContent.Text -> Triple(Icons.Filled.TextFields, Color(0xFF607D8B), R.string.code_text)
}

fun codeSummary(c: CodeContent, raw: String): String = when (c) {
    is CodeContent.Url -> c.url
    is CodeContent.Wifi -> c.ssid
    is CodeContent.Phone -> c.number
    is CodeContent.Email -> c.address
    is CodeContent.Sms -> c.number
    is CodeContent.Geo -> "${c.lat}, ${c.lng}"
    is CodeContent.Contact -> c.name ?: c.phones.firstOrNull() ?: c.emails.firstOrNull() ?: raw
    is CodeContent.Event -> c.summary ?: raw
    is CodeContent.Product -> c.code
    is CodeContent.Text -> c.text
}

private fun Context.launch(intent: Intent) {
    try { startActivity(intent) } catch (_: ActivityNotFoundException) { }
}

private fun webSearch(q: String) = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(q)))

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CodeSheet(raw: String, format: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val content = parseCode(raw, format)
    val (icon, color, label) = codeLook(content)
    val copy = { text: String -> clipboard.setText(AnnotatedString(text)) }
    val share = { context.launch(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, raw), null)) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(icon, color, 52)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(stringResource(label), style = MaterialTheme.typography.titleLarge)
                    Text(formatLabel(format), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh).verticalScroll(rememberScrollState()).padding(16.dp),
            ) {
                when (content) {
                    is CodeContent.Wifi -> {
                        Field(R.string.wifi_network, content.ssid)
                        content.password?.let { Field(R.string.wifi_password, it) }
                        content.security?.let { Field(R.string.wifi_security, it) }
                    }
                    is CodeContent.Contact -> {
                        content.name?.let { Field(R.string.contact_name, it) }
                        content.org?.let { Field(R.string.contact_org, it) }
                        content.phones.forEach { Field(R.string.code_phone, it) }
                        content.emails.forEach { Field(R.string.code_email, it) }
                    }
                    is CodeContent.Url -> {
                        Text(Uri.parse(content.url).host ?: content.url, style = MaterialTheme.typography.titleMedium)
                        SelectionContainer { Text(content.url, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    else -> SelectionContainer { Text(codeSummary(content, raw).takeIf { content !is CodeContent.Text } ?: raw, style = MaterialTheme.typography.bodyLarge) }
                }
            }
            if (content is CodeContent.Url) {
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Shield, null, Modifier.padding(end = 6.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.link_safety), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(20.dp))

            when (content) {
                is CodeContent.Url -> AppButton(stringResource(R.string.open_link), { context.launch(Intent(Intent.ACTION_VIEW, Uri.parse(content.url))) }, Modifier.fillMaxWidth(), icon = Icons.Filled.OpenInNew)
                is CodeContent.Wifi -> AppButton(stringResource(R.string.wifi_open_settings), { context.launch(Intent(Settings.ACTION_WIFI_SETTINGS)) }, Modifier.fillMaxWidth(), icon = Icons.Filled.Wifi)
                is CodeContent.Phone -> AppButton(stringResource(R.string.call), { context.launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + content.number))) }, Modifier.fillMaxWidth(), icon = Icons.Filled.Call)
                is CodeContent.Email -> AppButton(stringResource(R.string.send_email), {
                    context.launch(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + content.address)).putExtra(Intent.EXTRA_SUBJECT, content.subject).putExtra(Intent.EXTRA_TEXT, content.body))
                }, Modifier.fillMaxWidth(), icon = Icons.Filled.Email)
                is CodeContent.Sms -> AppButton(stringResource(R.string.send_message), {
                    context.launch(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + content.number)).putExtra("sms_body", content.message))
                }, Modifier.fillMaxWidth(), icon = Icons.Filled.Sms)
                is CodeContent.Geo -> AppButton(stringResource(R.string.open_map), { context.launch(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${content.lat},${content.lng}?q=${content.lat},${content.lng}"))) }, Modifier.fillMaxWidth(), icon = Icons.Filled.Map)
                is CodeContent.Contact -> AppButton(stringResource(R.string.add_contact), {
                    context.launch(Intent(ContactsContract.Intents.Insert.ACTION).setType(ContactsContract.RawContacts.CONTENT_TYPE).apply {
                        content.name?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
                        content.org?.let { putExtra(ContactsContract.Intents.Insert.COMPANY, it) }
                        content.phones.firstOrNull()?.let { putExtra(ContactsContract.Intents.Insert.PHONE, it) }
                        content.emails.firstOrNull()?.let { putExtra(ContactsContract.Intents.Insert.EMAIL, it) }
                    })
                }, Modifier.fillMaxWidth(), icon = Icons.Filled.PersonAdd)
                is CodeContent.Event -> AppButton(stringResource(R.string.add_to_calendar), {
                    context.launch(Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).putExtra(CalendarContract.Events.TITLE, content.summary))
                }, Modifier.fillMaxWidth(), icon = Icons.Filled.CalendarMonth)
                is CodeContent.Product -> AppButton(stringResource(R.string.look_up_product), { context.launch(webSearch(content.code)) }, Modifier.fillMaxWidth(), icon = Icons.Filled.Search)
                is CodeContent.Text -> AppButton(stringResource(R.string.copy_text), { copy(raw); onDismiss() }, Modifier.fillMaxWidth(), icon = Icons.Filled.ContentCopy)
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (content is CodeContent.Wifi && content.password != null) {
                    AppChip(false, { copy(content.password) }) { ChipLabel(Icons.Filled.ContentCopy, R.string.copy_password) }
                }
                if (content !is CodeContent.Text) AppChip(false, { copy(codeSummary(content, raw)) }) { ChipLabel(Icons.Filled.ContentCopy, R.string.copy_text) }
                AppChip(false, share) { ChipLabel(Icons.Filled.Share, R.string.share) }
                if (content is CodeContent.Text) AppChip(false, { context.launch(webSearch(raw)) }) { ChipLabel(Icons.Filled.Search, R.string.search_web) }
            }
        }
    }
}

@Composable
private fun ChipLabel(icon: ImageVector, text: Int) {
    Icon(icon, null, Modifier.padding(end = 6.dp))
    Text(stringResource(text))
}

@Composable
private fun Field(label: Int, value: String) {
    Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    SelectionContainer { Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp)) }
}

