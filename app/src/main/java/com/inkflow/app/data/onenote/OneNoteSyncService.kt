package com.inkflow.app.data.onenote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.inkflow.app.data.model.ChecklistItem
import com.inkflow.app.data.model.InkPoint
import com.inkflow.app.data.model.InkStroke
import com.inkflow.app.data.model.InkToolType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Represents a parsed OneNote page ready to be imported into InkFlow's Room database,
 * including its parent notebook name, title, body text, checklist items, and any InkML vector strokes.
 */
data class ImportedOneNotePage(
    val notebookTitle: String,
    val pageTitle: String,
    val bodyText: String,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val inkStrokes: List<InkStroke> = emptyList(),
    val tags: List<String> = listOf("onenote")
)

/**
 * Production-grade Microsoft OneNote Integration & Import/Sync Service.
 * Supports:
 * 1. Live Microsoft Graph API (`https://graph.microsoft.com/v1.0/me/onenote/pages`) sync using an OAuth Bearer token.
 * 2. Local File Import via Android Storage Access Framework (`.html`, `.htm`, `.mht`, `.xml` InkML, `.txt`, `.md`).
 * 3. Instant Microsoft Account Cloud Notebook Sync for seamless testing and OneNote page/ink migration.
 */
object OneNoteSyncService {

    /**
     * Fetches the user's OneNote pages directly from Microsoft Graph API `/v1.0/me/onenote/pages`
     * when a Microsoft Graph OAuth access token is provided.
     */
    suspend fun syncFromMicrosoftGraph(accessToken: String): Result<List<ImportedOneNotePage>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanToken = accessToken.removePrefix("Bearer ").trim()
                val endpoint = URL("https://graph.microsoft.com/v1.0/me/onenote/pages?\$top=20&\$expand=parentNotebook,parentSection")
                val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $cleanToken")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 12_000
                    readTimeout = 12_000
                }

                val code = conn.responseCode
                if (code !in 200..299) {
                    val errorMsg = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                    throw IllegalStateException("Microsoft Graph returned HTTP $code: $errorMsg")
                }

                val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonString)
                val values = root.optJSONArray("value") ?: return@runCatching emptyList()

                val importedPages = mutableListOf<ImportedOneNotePage>()
                for (i in 0 until values.length()) {
                    val item = values.getJSONObject(i)
                    val title = item.optString("title", "OneNote Page ${i + 1}").ifBlank { "OneNote Page ${i + 1}" }
                    val parentNb = item.optJSONObject("parentNotebook")?.optString("displayName") ?: "OneNote Cloud"
                    val contentUrl = item.optString("contentUrl", "")

                    // Fetch HTML content of the page if available
                    val htmlBody = if (contentUrl.isNotBlank()) {
                        runCatching {
                            val pageConn = (URL(contentUrl).openConnection() as HttpURLConnection).apply {
                                requestMethod = "GET"
                                setRequestProperty("Authorization", "Bearer $cleanToken")
                                connectTimeout = 10_000
                                readTimeout = 10_000
                            }
                            if (pageConn.responseCode in 200..299) {
                                pageConn.inputStream.bufferedReader().use { it.readText() }
                            } else ""
                        }.getOrDefault("")
                    } else ""

                    val parsed = if (htmlBody.isNotBlank()) {
                        parseOneNoteHtmlOrInkMl(
                            rawContent = htmlBody,
                            fallbackTitle = title,
                            notebookTitle = parentNb
                        )
                    } else {
                        ImportedOneNotePage(
                            notebookTitle = parentNb,
                            pageTitle = title,
                            bodyText = "Synced from Microsoft OneNote ($parentNb).",
                            tags = listOf("onenote", "cloud-sync")
                        )
                    }
                    importedPages.add(parsed)
                }
                importedPages
            }
        }

    /**
     * Parses a OneNote file selected by the user from Android storage (`.html`, `.htm`, `.mht`, `.xml` InkML, `.md`, `.txt`).
     */
    suspend fun importOneNoteUri(context: Context, uri: Uri): Result<ImportedOneNotePage> =
        withContext(Dispatchers.IO) {
            runCatching {
                val fileName = queryDisplayName(context, uri) ?: "OneNote_Import.html"
                val cleanTitle = fileName.substringBeforeLast(".").replace("_", " ").ifBlank { "Imported OneNote Page" }

                val rawText = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                } ?: throw IllegalArgumentException("Unable to read selected OneNote file.")

                parseOneNoteHtmlOrInkMl(
                    rawContent = rawText,
                    fallbackTitle = cleanTitle,
                    notebookTitle = "OneNote Imports"
                )
            }
        }

    /**
     * Parses OneNote HTML, MHT, or InkML XML content into structured text, checklists, and InkStroke vectors.
     */
    fun parseOneNoteHtmlOrInkMl(
        rawContent: String,
        fallbackTitle: String,
        notebookTitle: String = "OneNote Imports"
    ): ImportedOneNotePage {
        // Extract <title> if present in OneNote HTML export
        val titleRegex = Regex("<title>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val extractedTitle = titleRegex.find(rawContent)?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.isNotBlank() && !it.equals("OneNote", ignoreCase = true) }
            ?: fallbackTitle

        // Extract OneNote data-tag="to-do" checklist items
        val checklist = mutableListOf<ChecklistItem>()
        val todoRegex = Regex(
            """data-tag\s*=\s*"to-do(:completed)?"[^>]*>(.*?)</p>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        todoRegex.findAll(rawContent).forEach { match ->
            val isCompleted = match.groupValues[1].isNotEmpty()
            val itemHtml = match.groupValues[2]
            val plainItem = stripHtmlTags(itemHtml).trim()
            if (plainItem.isNotBlank()) {
                checklist.add(
                    ChecklistItem(
                        id = "chk-${UUID.randomUUID()}",
                        text = plainItem,
                        isChecked = isCompleted
                    )
                )
            }
        }

        // Extract InkML <ink:trace> or <trace> vector handwriting coordinates if present
        val inkStrokes = parseInkMlTraces(rawContent)

        // Convert HTML paragraphs/lists into clean readable body text
        val bodyText = if (rawContent.contains("<html", ignoreCase = true) || rawContent.contains("<body", ignoreCase = true) || rawContent.contains("<p", ignoreCase = true)) {
            val bodyRegion = Regex("<body[^>]*>(.*?)</body>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(rawContent)?.groupValues?.getOrNull(1) ?: rawContent
            stripHtmlTags(bodyRegion)
        } else {
            rawContent.trim()
        }

        return ImportedOneNotePage(
            notebookTitle = notebookTitle,
            pageTitle = extractedTitle,
            bodyText = bodyText.ifBlank { "Imported from Microsoft OneNote." },
            checklistItems = checklist,
            inkStrokes = inkStrokes,
            tags = listOf("onenote", "imported")
        )
    }

    /**
     * Parses W3C / Microsoft OneNote InkML `<trace>` coordinate lists (e.g., `120 180, 125 184, 132 190`)
     * into native InkFlow `InkStroke` objects so OneNote handwriting remains editable!
     */
    private fun parseInkMlTraces(content: String): List<InkStroke> {
        val strokes = mutableListOf<InkStroke>()
        val traceRegex = Regex("<(?:ink:)?trace[^>]*>(.*?)</(?:ink:)?trace>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        traceRegex.findAll(content).forEach { match ->
            val coordsRaw = match.groupValues[1].trim()
            if (coordsRaw.isNotBlank()) {
                val points = coordsRaw.split(",")
                    .mapNotNull { pair ->
                        val nums = pair.trim().split(Regex("\\s+")).mapNotNull { it.toFloatOrNull() }
                        if (nums.size >= 2) {
                            InkPoint(
                                x = nums[0],
                                y = nums[1],
                                pressure = nums.getOrNull(2)?.coerceIn(0.2f, 1.2f) ?: 0.65f
                            )
                        } else null
                    }
                if (points.size >= 2) {
                    strokes.add(
                        InkStroke(
                            id = "stroke-${UUID.randomUUID()}",
                            tool = InkToolType.PEN,
                            colorArgb = 0xFF1C1917,
                            strokeWidth = 4.5f,
                            opacity = 1.0f,
                            points = points
                        )
                    )
                }
            }
        }
        return strokes
    }

    /** Result of starting a Microsoft device-code sign-in. */
    data class DeviceCodeInfo(
        val deviceCode: String,
        val userCode: String,
        val verificationUri: String,
        val expiresInSeconds: Int,
        val intervalSeconds: Int
    )

    private const val TENANT = "consumers"
    private const val SCOPE = "Notes.Read offline_access User.Read"

    /**
     * Step 1 of real Microsoft sign-in (OAuth 2.0 device-code flow).
     * Requires an Azure app registration Client ID with "Allow public client flows" enabled.
     * The user signs in on microsoft.com itself, so the app never sees their password.
     */
    suspend fun startDeviceCodeSignIn(clientId: String): Result<DeviceCodeInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL("https://login.microsoftonline.com/$TENANT/oauth2/v2.0/devicecode")
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    connectTimeout = 12_000
                    readTimeout = 12_000
                }
                val body = "client_id=${java.net.URLEncoder.encode(clientId.trim(), "UTF-8")}" +
                    "&scope=${java.net.URLEncoder.encode(SCOPE, "UTF-8")}"
                conn.outputStream.use { it.write(body.toByteArray()) }
                val ok = conn.responseCode in 200..299
                val text = (if (ok) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = JSONObject(text)
                if (!ok) {
                    throw IllegalStateException(
                        json.optString("error_description", json.optString("error", "Sign-in failed"))
                            .lineSequence().first()
                    )
                }
                DeviceCodeInfo(
                    deviceCode = json.getString("device_code"),
                    userCode = json.getString("user_code"),
                    verificationUri = json.optString("verification_uri", "https://microsoft.com/devicelogin"),
                    expiresInSeconds = json.optInt("expires_in", 900),
                    intervalSeconds = json.optInt("interval", 5)
                )
            }
        }

    /**
     * Step 2: poll until the user finishes signing in; returns a Graph access token.
     */
    suspend fun pollDeviceCodeToken(clientId: String, info: DeviceCodeInfo): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val deadline = System.currentTimeMillis() + info.expiresInSeconds * 1000L
                var interval = info.intervalSeconds.coerceAtLeast(2)
                while (System.currentTimeMillis() < deadline) {
                    kotlinx.coroutines.delay(interval * 1000L)
                    val conn = (URL("https://login.microsoftonline.com/$TENANT/oauth2/v2.0/token")
                        .openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                        connectTimeout = 12_000
                        readTimeout = 12_000
                    }
                    val body = "grant_type=${java.net.URLEncoder.encode("urn:ietf:params:oauth:grant-type:device_code", "UTF-8")}" +
                        "&client_id=${java.net.URLEncoder.encode(clientId.trim(), "UTF-8")}" +
                        "&device_code=${java.net.URLEncoder.encode(info.deviceCode, "UTF-8")}"
                    conn.outputStream.use { it.write(body.toByteArray()) }
                    val ok = conn.responseCode in 200..299
                    val text = (if (ok) conn.inputStream else conn.errorStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                    val json = JSONObject(text)
                    if (ok) return@runCatching json.getString("access_token")
                    when (json.optString("error")) {
                        "authorization_pending" -> Unit
                        "slow_down" -> interval += 5
                        else -> throw IllegalStateException(
                            json.optString("error_description", "Sign-in failed").lineSequence().first()
                        )
                    }
                }
                throw IllegalStateException("Sign-in code expired. Please try again.")
            }
        }


    private fun stripHtmlTags(html: String): String {
        return html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("</li>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<li[^>]*>", RegexOption.IGNORE_CASE), "â€¢ ")
            .replace(Regex("<h[1-6][^>]*>", RegexOption.IGNORE_CASE), "\n# ")
            .replace(Regex("</h[1-6]>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull()
    }
}
