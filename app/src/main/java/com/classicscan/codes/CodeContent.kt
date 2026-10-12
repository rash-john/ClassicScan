package com.classicscan.codes

sealed interface CodeContent {
    data class Url(val url: String) : CodeContent
    data class Wifi(val ssid: String, val password: String?, val security: String?, val hidden: Boolean) : CodeContent
    data class Phone(val number: String) : CodeContent
    data class Email(val address: String, val subject: String?, val body: String?) : CodeContent
    data class Sms(val number: String, val message: String?) : CodeContent
    data class Geo(val lat: Double, val lng: Double) : CodeContent
    data class Contact(val name: String?, val phones: List<String>, val emails: List<String>, val org: String?) : CodeContent
    data class Event(val summary: String?) : CodeContent
    data class Product(val code: String) : CodeContent
    data class Text(val text: String) : CodeContent
}

private val PRODUCT_FORMATS = setOf("EAN_13", "EAN_8", "UPC_A", "UPC_E")

private fun splitEscaped(s: String, sep: Char): List<String> {
    val out = mutableListOf<String>()
    val cur = StringBuilder()
    var esc = false
    for (c in s) {
        when {
            esc -> { cur.append(c); esc = false }
            c == '\\' -> esc = true
            c == sep -> { out += cur.toString(); cur.clear() }
            else -> cur.append(c)
        }
    }
    out += cur.toString()
    return out
}

private fun fields(body: String): Map<String, List<String>> =
    splitEscaped(body, ';').filter { ':' in it }.groupBy({ it.substringBefore(':').uppercase() }, { it.substringAfter(':') })

fun parseCode(raw: String, format: String): CodeContent {
    val t = raw.trim()
    val lower = t.lowercase()
    return when {
        format in PRODUCT_FORMATS && t.all { it.isDigit() } -> CodeContent.Product(t)
        lower.startsWith("http://") || lower.startsWith("https://") -> CodeContent.Url(t)
        lower.startsWith("www.") -> CodeContent.Url("https://$t")
        lower.startsWith("wifi:") -> {
            val f = fields(t.substring(5))
            CodeContent.Wifi(
                f["S"]?.firstOrNull().orEmpty(), f["P"]?.firstOrNull()?.takeIf { it.isNotEmpty() },
                f["T"]?.firstOrNull()?.takeIf { it.isNotEmpty() }, f["H"]?.firstOrNull().equals("true", true),
            )
        }
        lower.startsWith("tel:") -> CodeContent.Phone(t.substring(4))
        lower.startsWith("mailto:") -> {
            val addr = t.substring(7).substringBefore('?')
            val q = t.substringAfter('?', "").split('&').associate { it.substringBefore('=').lowercase() to java.net.URLDecoder.decode(it.substringAfter('=', ""), "UTF-8") }
            CodeContent.Email(addr, q["subject"], q["body"])
        }
        lower.startsWith("matmsg:") -> {
            val f = fields(t.substring(7))
            CodeContent.Email(f["TO"]?.firstOrNull().orEmpty(), f["SUB"]?.firstOrNull(), f["BODY"]?.firstOrNull())
        }
        lower.startsWith("smsto:") || lower.startsWith("sms:") -> {
            val rest = t.substringAfter(':')
            CodeContent.Sms(rest.substringBefore(':'), rest.substringAfter(':', "").takeIf { it.isNotEmpty() })
        }
        lower.startsWith("geo:") -> {
            val parts = t.substring(4).substringBefore('?').split(',')
            val lat = parts.getOrNull(0)?.toDoubleOrNull(); val lng = parts.getOrNull(1)?.toDoubleOrNull()
            if (lat != null && lng != null) CodeContent.Geo(lat, lng) else CodeContent.Text(t)
        }
        lower.startsWith("mecard:") -> {
            val f = fields(t.substring(7))
            CodeContent.Contact(f["N"]?.firstOrNull()?.split(',')?.reversed()?.joinToString(" ")?.trim(), f["TEL"].orEmpty(), f["EMAIL"].orEmpty(), f["ORG"]?.firstOrNull())
        }
        lower.startsWith("begin:vcard") -> {
            val lines = t.lines().map { it.trim() }
            fun values(key: String) = lines.filter { it.uppercase().startsWith(key) && ':' in it }.map { it.substringAfter(':').trim() }
            CodeContent.Contact(
                values("FN").firstOrNull() ?: values("N").firstOrNull()?.split(';')?.filter { it.isNotBlank() }?.reversed()?.joinToString(" "),
                values("TEL"), values("EMAIL"), values("ORG").firstOrNull()?.replace(';', ' ')?.trim(),
            )
        }
        lower.startsWith("begin:vevent") || lower.startsWith("begin:vcalendar") ->
            CodeContent.Event(t.lines().firstOrNull { it.uppercase().startsWith("SUMMARY") }?.substringAfter(':')?.trim())
        else -> CodeContent.Text(t)
    }
}
