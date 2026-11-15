@file:Suppress("DEPRECATION")

package com.example.findit

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.example.findit.ui.theme.FindItTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------
// Design tokens (Foundit palette from the reference sheet)
// ---------------------------------------------------------------------------
private val Blue = Color(0xFF2F80ED)        // primary
private val BlueSoft = Color(0xFFE8F0FB)    // poster bar / soft containers
private val Navy = Color(0xFF1B1A57)        // titles, top bars
private val Slate = Color(0xFF4F5E7B)       // secondary text, icons
private val TextDark = Color(0xFF2B2B2B)
private val TextBody = Color(0xFF4B4B4B)
private val TextMuted = Color(0xFF999999)
private val FieldBg = Color(0xFFF1F1F1)
private val CardBg = Color(0xFFF7F7F7)
private val ScreenBg = Color(0xFFFDFDFD)
private val DividerColor = Color(0xFFEDEDED)
private val Yellow = Color(0xFFF9D45C)
private val LostBg = Color(0xFFFFF1C2)
private val LostText = Color(0xFF8A6A00)
private val FoundBg = Color(0xFFDCEBFF)
private val FoundText = Color(0xFF1F5FBF)
private val Danger = Color(0xFFD64545)

// Header: gradient that runs behind the status bar, right to the top edge of the phone
private val HeaderBrush = Brush.horizontalGradient(listOf(Color(0xFF1F5FBF), Blue))
private val HeaderShape = RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp)

// Open Sans (single variable font file: res/font/open_sans.ttf)
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private val AppFont = FontFamily(
    Font(R.font.open_sans, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.open_sans, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.open_sans, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.open_sans, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

private const val DB_URL = "https://findit-c9d61-default-rtdb.firebaseio.com"

// Leave empty to use the bucket from google-services.json.
// If uploads fail with "Object does not exist", paste the bucket shown at the top of
// Firebase Console > Storage here, e.g. "gs://findit-c9d61.firebasestorage.app"
private const val STORAGE_BUCKET = ""

private val ReportCategories = listOf(
    "Electronics", "Wallets", "Documents", "Bags", "Accessories", "Clothing", "Others"
)

// ---------------------------------------------------------------------------
// Built-in icons (no material-icons dependency needed)
// ---------------------------------------------------------------------------
private fun vec(name: String, path: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes(path), fill = SolidColor(Color.Black))
        .build()

/** Outline icons, like the reference's iconography. */
private fun svec(name: String, vararg paths: String): ImageVector {
    val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    paths.forEach { p ->
        b.addPath(
            addPathNodes(p),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }
    return b.build()
}

private object Ic {
    // outline
    val Home by lazy { svec("Home", "M3,9 L12,2 L21,9 L21,20 A2,2 0 0,1 19,22 L5,22 A2,2 0 0,1 3,20 Z", "M9,22 L9,12 L15,12 L15,22") }
    val Person by lazy { svec("Person", "M20,21 L20,19 A4,4 0 0,0 16,15 L8,15 A4,4 0 0,0 4,19 L4,21", "M16,7 A4,4 0 1,1 8,7 A4,4 0 1,1 16,7 Z") }
    val List by lazy { svec("Files", "M14,2 L6,2 A2,2 0 0,0 4,4 L4,20 A2,2 0 0,0 6,22 L18,22 A2,2 0 0,0 20,20 L20,8 Z", "M14,2 L14,8 L20,8", "M16,13 L8,13", "M16,17 L8,17") }
    val Search by lazy { svec("Search", "M19,11 A8,8 0 1,1 3,11 A8,8 0 1,1 19,11 Z", "M21,21 L16.65,16.65") }
    val ArrowBack by lazy { svec("ArrowBack", "M19,12 L5,12", "M12,19 L5,12 L12,5") }
    val DateRange by lazy { svec("Calendar", "M5,4 L19,4 A2,2 0 0,1 21,6 L21,20 A2,2 0 0,1 19,22 L5,22 A2,2 0 0,1 3,20 L3,6 A2,2 0 0,1 5,4 Z", "M16,2 L16,6", "M8,2 L8,6", "M3,10 L21,10") }
    val Clock by lazy { svec("Clock", "M22,12 A10,10 0 1,1 2,12 A10,10 0 1,1 22,12 Z", "M12,6 L12,12 L16,14") }
    val LocationOn by lazy { svec("Pin", "M21,10 C21,17 12,23 12,23 C12,23 3,17 3,10 A9,9 0 0,1 21,10 Z", "M15,10 A3,3 0 1,1 9,10 A3,3 0 1,1 15,10 Z") }
    val Bell by lazy { svec("Bell", "M18,8 A6,6 0 0,0 6,8 C6,15 3,17 3,17 L21,17 C21,17 18,15 18,8", "M13.73,21 A2,2 0 0,1 10.27,21") }
    val Scan by lazy { svec("Scan", "M8,3 L5,3 A2,2 0 0,0 3,5 L3,8", "M21,8 L21,5 A2,2 0 0,0 19,3 L16,3", "M3,16 L3,19 A2,2 0 0,0 5,21 L8,21", "M16,21 L19,21 A2,2 0 0,0 21,19 L21,16", "M7,12 L17,12") }
    val Upload by lazy { svec("Upload", "M22,12 A10,10 0 1,1 2,12 A10,10 0 1,1 22,12 Z", "M12,16 L12,8", "M8,12 L12,8 L16,12") }
    val Camera by lazy { svec("Camera", "M23,19 A2,2 0 0,1 21,21 L3,21 A2,2 0 0,1 1,19 L1,8 A2,2 0 0,1 3,6 L7,6 L9,3 L15,3 L17,6 L21,6 A2,2 0 0,1 23,8 Z", "M16,13 A4,4 0 1,1 8,13 A4,4 0 1,1 16,13 Z") }
    val Chat by lazy { svec("Chat", "M21,15 A2,2 0 0,1 19,17 L7,17 L3,21 L3,5 A2,2 0 0,1 5,3 L19,3 A2,2 0 0,1 21,5 Z") }
    val Attach by lazy { svec("Attach", "M21.44,11.05 L12.25,20.24 A6,6 0 0,1 3.76,11.75 L12.95,2.56 A4,4 0 0,1 18.61,8.22 L9.41,17.41 A2,2 0 0,1 6.59,14.59 L15.07,6.11") }
    // filled
    val MoreVert by lazy { vec("MoreVert", "M12,8c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2zM12,16c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z") }
    val Call by lazy { vec("Call", "M6.62,10.79c1.44,2.83 3.76,5.14 6.59,6.59l2.2,-2.2c0.27,-0.27 0.67,-0.36 1.02,-0.24 1.12,0.37 2.33,0.57 3.57,0.57 0.55,0 1,0.45 1,1V20c0,0.55 -0.45,1 -1,1 -9.39,0 -17,-7.61 -17,-17 0,-0.55 0.45,-1 1,-1h3.5c0.55,0 1,0.45 1,1 0,1.25 0.2,2.45 0.57,3.57 0.11,0.35 0.03,0.74 -0.25,1.02l-2.2,2.2z") }
    val Close by lazy { vec("Close", "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z") }
    val Delete by lazy { vec("Delete", "M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z") }
    val Email by lazy { vec("Email", "M20,4L4,4c-1.1,0 -1.99,0.9 -1.99,2L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2L22,6c0,-1.1 -0.9,-2 -2,-2zM20,8l-8,5 -8,-5L4,6l8,5 8,-5v2z") }
    val Send by lazy { vec("Send", "M2.01,21L23,12 2.01,3 2,10l15,2 -15,2z") }
    val Warning by lazy { vec("Warning", "M1,21h22L12,2 1,21zM13,18h-2v-2h2v2zM13,14h-2v-4h2v4z") }
}

// ---------------------------------------------------------------------------
// Data
// ---------------------------------------------------------------------------
data class FindItReport(
    val reportId: String = "",
    val userId: String = "",
    val itemName: String = "",
    val category: String = "",
    val description: String = "",
    val location: String = "",
    val status: String = "",
    val imageUrl: String = "",
    val date: String = "",
    val time: String = "",
    val posterName: String = "",
    val posterInfo: String = "",
    val posterPhone: String = "",
    val posterEmail: String = "",
    val timestamp: Long = 0L
)

private fun DataSnapshot.toReport(): FindItReport? {
    val id = child("reportId").getValue(String::class.java) ?: key ?: return null
    fun s(k: String) = child(k).getValue(String::class.java) ?: ""
    return FindItReport(
        reportId = id,
        userId = s("userId"),
        itemName = s("itemName"),
        category = s("category"),
        description = s("description"),
        location = s("location"),
        status = s("status"),
        imageUrl = s("imageUrl"),
        date = s("date"),
        time = s("time"),
        posterName = s("posterName"),
        posterInfo = s("posterInfo"),
        posterPhone = s("posterPhone"),
        posterEmail = s("posterEmail"),
        timestamp = (child("timestamp").value as? Number)?.toLong() ?: 0L
    )
}

class ReportsState {
    val reports = mutableStateListOf<FindItReport>()
    var isLoading by mutableStateOf(true)
    var error by mutableStateOf("")
}

/** Live list of reports. Pass a userId to only get that user's reports. */
@Composable
fun rememberReports(userId: String? = null): ReportsState {
    val state = remember(userId) { ReportsState() }

    DisposableEffect(userId) {
        if (userId != null && userId.isBlank()) {
            state.isLoading = false
            state.error = "Please log in again."
            return@DisposableEffect onDispose { }
        }

        val ref = FirebaseDatabase.getInstance(DB_URL).getReference("reports")
        val query = if (userId == null) ref.orderByChild("timestamp")
        else ref.orderByChild("userId").equalTo(userId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val loaded = snapshot.children.mapNotNull { it.toReport() }
                    .sortedByDescending { it.timestamp }
                state.reports.clear()
                state.reports.addAll(loaded)
                state.isLoading = false
                state.error = ""
            }

            override fun onCancelled(error: DatabaseError) {
                state.isLoading = false
                state.error = error.message
            }
        }
        query.addValueEventListener(listener)
        onDispose { query.removeEventListener(listener) }
    }
    return state
}

private fun timeAgo(ts: Long): String {
    if (ts <= 0L) return ""
    val mins = (System.currentTimeMillis() - ts) / 60_000
    return when {
        mins < 1 -> "Just now"
        mins < 60 -> "${mins}m ago"
        mins < 60 * 24 -> "${mins / 60}h ago"
        mins < 60 * 24 * 7 -> "${mins / (60 * 24)}d ago"
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(ts))
    }
}

private fun dateLabel(r: FindItReport): String =
    r.date.ifBlank { SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(r.timestamp)) }

private fun timeLabel(r: FindItReport): String =
    r.time.ifBlank { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(r.timestamp)) }

private fun deleteReport(context: Context, id: String, onDone: () -> Unit = {}) {
    FirebaseDatabase.getInstance(DB_URL).getReference("reports").child(id).removeValue()
        .addOnCompleteListener { t ->
            Toast.makeText(
                context,
                if (t.isSuccessful) "Post deleted" else "Could not delete post",
                Toast.LENGTH_SHORT
            ).show()
            if (t.isSuccessful) onDone()
        }
}

// ---------------------------------------------------------------------------
// Messaging data
//   userChats/{uid}/{chatId} -> summary shown in the Messages list
//   messages/{chatId}/{msgId} -> the actual messages
//   chatId = the two user ids, sorted and joined with "_"
// ---------------------------------------------------------------------------
data class ChatTarget(
    val otherId: String,
    val otherName: String,
    val draft: String = ""
)

data class ChatSummary(
    val chatId: String = "",
    val otherId: String = "",
    val otherName: String = "",
    val lastMessage: String = "",
    val lastTimestamp: Long = 0L,
    val unread: Int = 0
)

data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L
)

class ChatsState {
    val chats = mutableStateListOf<ChatSummary>()
    var isLoading by mutableStateOf(true)
    var error by mutableStateOf("")
}

private fun chatIdOf(a: String, b: String): String = if (a < b) "${a}_$b" else "${b}_$a"

/** Live list of the user's conversations, newest first. */
@Composable
fun rememberUserChats(userId: String): ChatsState {
    val state = remember(userId) { ChatsState() }

    DisposableEffect(userId) {
        if (userId.isBlank()) {
            state.isLoading = false
            return@DisposableEffect onDispose { }
        }
        val ref = FirebaseDatabase.getInstance(DB_URL).getReference("userChats").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val loaded = snapshot.children.mapNotNull { c ->
                    val otherId = c.child("otherId").getValue(String::class.java) ?: ""
                    if (otherId.isBlank()) return@mapNotNull null
                    ChatSummary(
                        chatId = c.key ?: return@mapNotNull null,
                        otherId = otherId,
                        otherName = c.child("otherName").getValue(String::class.java) ?: "",
                        lastMessage = c.child("lastMessage").getValue(String::class.java) ?: "",
                        lastTimestamp = (c.child("lastTimestamp").value as? Number)?.toLong() ?: 0L,
                        unread = (c.child("unread").value as? Number)?.toInt() ?: 0
                    )
                }.sortedByDescending { it.lastTimestamp }
                state.chats.clear()
                state.chats.addAll(loaded)
                state.isLoading = false
                state.error = ""
            }

            override fun onCancelled(error: DatabaseError) {
                state.isLoading = false
                state.error = error.message
            }
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }
    return state
}

private fun chatTime(ts: Long): String {
    if (ts <= 0L) return ""
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = ts }
    fun sameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    if (sameDay(now, then)) return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (sameDay(yesterday, then)) return "Yesterday"
    return SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(ts))
}

private fun bubbleTime(ts: Long): String =
    if (ts <= 0L) "" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

/** Sends a text or an image message and updates both users' chat summaries in one write. */
private fun sendChatMessage(
    myId: String,
    myName: String,
    target: ChatTarget,
    text: String,
    imageUri: Uri?,
    onResult: (String?) -> Unit // null = success, otherwise the error text
) {
    val root = FirebaseDatabase.getInstance(DB_URL).reference
    val chatId = chatIdOf(myId, target.otherId)
    val msgId = root.child("messages").child(chatId).push().key
    if (msgId == null) {
        onResult("Could not create a message id")
        return
    }

    fun write(imageUrl: String) {
        val now = System.currentTimeMillis()
        val preview = if (text.isNotBlank()) text else "Photo"
        val updates = hashMapOf<String, Any?>(
            "messages/$chatId/$msgId" to mapOf(
                "messageId" to msgId,
                "senderId" to myId,
                "text" to text,
                "imageUrl" to imageUrl,
                "timestamp" to now
            ),
            // my side of the conversation
            "userChats/$myId/$chatId/otherId" to target.otherId,
            "userChats/$myId/$chatId/otherName" to target.otherName,
            "userChats/$myId/$chatId/lastMessage" to preview,
            "userChats/$myId/$chatId/lastTimestamp" to now,
            // their side of the conversation
            "userChats/${target.otherId}/$chatId/otherId" to myId,
            "userChats/${target.otherId}/$chatId/otherName" to myName,
            "userChats/${target.otherId}/$chatId/lastMessage" to preview,
            "userChats/${target.otherId}/$chatId/lastTimestamp" to now,
            "userChats/${target.otherId}/$chatId/unread" to ServerValue.increment(1)
        )
        root.updateChildren(updates).addOnCompleteListener {
            onResult(if (it.isSuccessful) null else it.exception?.localizedMessage ?: "Permission denied")
        }
    }

    if (imageUri == null) {
        write("")
    } else {
        uploadImage("chat_images/$chatId/$msgId.jpg", imageUri) { url, err ->
            if (url != null) write(url) else onResult(err ?: "Image upload failed")
        }
    }
}

/** Portrait-locked QR scanner screen (the default one is landscape). */
class PortraitCaptureActivity : CaptureActivity()

private fun portraitScanOptions(prompt: String) = ScanOptions().apply {
    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
    setPrompt(prompt)
    setBeepEnabled(false)
    setOrientationLocked(false)
    setCaptureActivity(PortraitCaptureActivity::class.java)
}

/** Uploads an image and returns its download link (or an error message). */
private fun uploadImage(path: String, uri: Uri, onDone: (url: String?, error: String?) -> Unit) {
    val storage = if (STORAGE_BUCKET.isBlank()) FirebaseStorage.getInstance()
    else FirebaseStorage.getInstance(STORAGE_BUCKET)
    val ref = storage.reference.child(path)
    val meta = StorageMetadata.Builder().setContentType("image/jpeg").build()
    // Storage not available? Fall back to a small compressed copy kept in the database.
    fun fallback(error: String) {
        val dataUri = imageToDataUri(uri)
        if (dataUri != null) onDone(dataUri, null) else onDone(null, error)
    }
    ref.putFile(uri, meta)
        .addOnSuccessListener { snap ->
            snap.storage.downloadUrl
                .addOnSuccessListener { onDone(it.toString(), null) }
                .addOnFailureListener {
                    fallback("${it.localizedMessage ?: "Could not get the image link"} [bucket: ${ref.bucket}]")
                }
        }
        .addOnFailureListener {
            fallback("${it.localizedMessage ?: "Image upload failed"} [bucket: ${ref.bucket}]")
        }
}

/** Shrinks an image and returns it as a "data:image/jpeg;base64,..." string (about 30-80 KB). */
private fun imageToDataUri(uri: Uri, maxSide: Int = 640, quality: Int = 65): String? {
    return try {
        val resolver = FirebaseApp.getInstance().applicationContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return null
        val scale = minOf(1f, maxSide / maxOf(bmp.width, bmp.height).toFloat())
        val scaled = if (scale < 1f) Bitmap.createScaledBitmap(
            bmp,
            (bmp.width * scale).toInt().coerceAtLeast(1),
            (bmp.height * scale).toInt().coerceAtLeast(1),
            true
        ) else bmp
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}

/** Shows normal image links/URIs and the "data:" images saved by the fallback above. */
@Composable
private fun SmartImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    if (model is String && model.startsWith("data:")) {
        val bitmap = remember(model) {
            try {
                val bytes = Base64.decode(model.substringAfter("base64,"), Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        } else {
            Box(modifier.background(BlueSoft))
        }
    } else {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

// ---------------------------------------------------------------------------
// Activity
// ---------------------------------------------------------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge() // draw behind the status bar so headers reach the top edge
        super.onCreate(savedInstanceState)
        setContent {
            FindItTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = ScreenBg) {
                    ProvideTextStyle(TextStyle(fontFamily = AppFont)) {
                        FindItAuthScreen()
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared components
// ---------------------------------------------------------------------------

/** Light status-bar icons on the blue header, dark icons on the white login screen. */
@Composable
private fun StatusBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
    }
}

/** Gradient header that extends behind the status bar, right to the top edge of the phone. */
@Composable
private fun HeaderContainer(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(HeaderShape)
            .background(HeaderBrush)   // background BEFORE statusBarsPadding so it fills the top
            .statusBarsPadding()
            .padding(contentPadding),
        content = content
    )
}

@Composable
private fun ScreenHeader(
    title: String,
    subtitle: String = "",
    trailing: @Composable RowScope.() -> Unit = {}
) {
    HeaderContainer(
        contentPadding = PaddingValues(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
            trailing()
        }
    }
}

@Composable
private fun LogoMark(size: Dp = 34.dp) {
    // Ring + person mark, modelled on the Foundit logo.
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.2f
        val r = (s - w) / 2f
        val tl = Offset(w / 2f, w / 2f)
        val arc = Size(2f * r, 2f * r)
        drawArc(color = Color(0xFF3B2FD1), startAngle = 120f, sweepAngle = 150f, useCenter = false,
            topLeft = tl, size = arc, style = Stroke(w))
        drawArc(color = Blue, startAngle = 270f, sweepAngle = 140f, useCenter = false,
            topLeft = tl, size = arc, style = Stroke(w))
        drawArc(color = Color(0xFFB9D3F8), startAngle = 50f, sweepAngle = 70f, useCenter = false,
            topLeft = tl, size = arc, style = Stroke(w))
        drawCircle(color = Color(0xFFDCE8FA), radius = r - w / 2f)
        drawCircle(color = Color.White, radius = s * 0.11f, center = Offset(s / 2f, s * 0.42f))
        drawArc(color = Color.White, startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(s * 0.28f, s * 0.56f), size = Size(s * 0.44f, s * 0.4f))
    }
}

@Composable
private fun LogoRow(onDark: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (onDark) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) { LogoMark(30.dp) }
        } else {
            LogoMark()
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "FindIt",
            color = if (onDark) Color.White else Blue,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
    }
}

@Composable
private fun TinyIcon(icon: ImageVector, tint: Color = Slate, size: Dp = 16.dp) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size))
}

@Composable
private fun ClockIcon(tint: Color = Slate, size: Dp = 14.dp) {
    TinyIcon(Ic.Clock, tint, size)
}

@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    readOnly: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    containerColor: Color = FieldBg
) {
    val leading: (@Composable () -> Unit)? =
        if (leadingIcon == null) null else ({ TinyIcon(leadingIcon, Slate, 20.dp) })

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextMuted, fontSize = 14.sp) },
        leadingIcon = leading,
        trailingIcon = trailing,
        singleLine = singleLine,
        minLines = minLines,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = TextDark),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(if (singleLine) 50.dp else 20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = containerColor,
            focusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            focusedBorderColor = Blue,
            cursorColor = Blue,
            disabledTextColor = TextMuted
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Medium,
        color = TextBody,
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
    )
}

@Composable
private fun PickerField(
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = FieldBg,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            icon()
            Spacer(Modifier.width(12.dp))
            Text(text, color = Slate, fontSize = 14.sp)
        }
    }
}

@Composable
private fun Pill(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) Blue else FieldBg,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Text(
            text,
            color = if (selected) Color.White else TextBody,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = (if (fill) Modifier.fillMaxWidth() else Modifier)
                .padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun LinkText(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text,
        color = if (enabled) Blue else TextMuted,
        textDecoration = TextDecoration.Underline,
        fontSize = 14.sp,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp)
    )
}

@Composable
private fun StatusBadge(status: String) {
    val found = status.equals("Found", ignoreCase = true)
    Surface(color = if (found) FoundBg else LostBg, shape = RoundedCornerShape(50)) {
        Text(
            text = status.ifBlank { "Unknown" },
            color = if (found) FoundText else LostText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ReportImage(url: String, name: String, modifier: Modifier) {
    if (url.isNotBlank()) {
        SmartImage(
            model = url,
            contentDescription = "Image of $name",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(modifier.background(BlueSoft), contentAlignment = Alignment.Center) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                color = Blue,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            )
        }
    }
}

@Composable
private fun Avatar(name: String, size: Dp = 36.dp, onDark: Boolean = false) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (onDark) Color.White else Blue.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.firstOrNull()?.uppercase() ?: "?",
            color = Blue, fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MetaItem(
    icon: @Composable () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(5.dp))
        Text(
            text, color = TextBody, fontSize = 12.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

/** Date / time / location row used in cards and details. */
@Composable
private fun MetaRow(report: FindItReport, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        MetaItem({ TinyIcon(Ic.DateRange, Slate, 14.dp) }, dateLabel(report))
        MetaItem({ ClockIcon() }, timeLabel(report))
        MetaItem(
            { TinyIcon(Ic.LocationOn, Slate, 14.dp) },
            report.location.ifBlank { "No location" },
            Modifier.weight(1f, fill = false)
        )
    }
}

/** Blue-tinted poster bar shown at the bottom of a post. */
@Composable
private fun PosterBar(report: FindItReport, showContact: Boolean, onContact: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(BlueSoft)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(report.posterName)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                report.posterName.ifBlank { "FindIt user" },
                color = Blue, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (report.posterInfo.isNotBlank()) {
                Text(
                    report.posterInfo, color = Navy, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (showContact) {
            Button(
                onClick = onContact,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp)
            ) { Text("Contact", fontSize = 12.sp, fontWeight = FontWeight.Medium) }
        }
    }
}

/** The main feed card: image + text + date/time/location + footer slot. */
@Composable
private fun PostCard(
    report: FindItReport,
    onClick: () -> Unit,
    footer: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            Row(Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp)) {
                ReportImage(
                    report.imageUrl, report.itemName,
                    Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            report.itemName.ifBlank { "Unnamed item" },
                            fontWeight = FontWeight.Medium, fontSize = 16.sp, color = TextDark,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(6.dp))
                        StatusBadge(report.status)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        report.description, fontSize = 12.sp, color = TextBody, lineHeight = 16.sp,
                        maxLines = 3, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            MetaRow(
                report,
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
            footer()
        }
    }
}

@Composable
private fun CenteredLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Blue)
    }
}

@Composable
private fun MessageState(title: String, subtitle: String = "", isError: Boolean = false) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (isError) Danger.copy(alpha = 0.12f) else BlueSoft),
            contentAlignment = Alignment.Center
        ) {
            TinyIcon(
                if (isError) Ic.Warning else Ic.Search,
                if (isError) Danger else Blue, 28.dp
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            title, color = if (isError) Danger else TextDark,
            fontWeight = FontWeight.SemiBold, fontSize = 16.sp, textAlign = TextAlign.Center
        )
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AppTopBar(title: String, onBack: () -> Unit, enabled: Boolean = true) {
    HeaderContainer(
        contentPadding = PaddingValues(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = enabled) {
                Icon(Ic.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = { Text(title, color = Navy, fontWeight = FontWeight.SemiBold) },
        text = { Text(message, color = TextBody, fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = Danger, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Blue) } }
    )
}

@Composable
private fun ContactOption(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BlueSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TinyIcon(icon, Blue, 20.dp)
        Spacer(Modifier.width(12.dp))
        Text(label, color = Navy, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ContactDialog(
    report: FindItReport,
    currentUserId: String,
    onMessage: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    fun launch(intent: Intent) {
        try { context.startActivity(intent) } catch (e: Exception) {
            Toast.makeText(context, "No app available for this action", Toast.LENGTH_SHORT).show()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                "Contact ${report.posterName.ifBlank { "poster" }}",
                color = Navy, fontWeight = FontWeight.SemiBold
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = Blue) } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val hasPhone = report.posterPhone.isNotBlank()
                val hasEmail = report.posterEmail.isNotBlank()
                val canMessage = report.userId.isNotBlank() && report.userId != currentUserId

                if (canMessage) {
                    ContactOption(Ic.Chat, "Message in app", onMessage)
                }
                if (!canMessage && !hasPhone && !hasEmail) {
                    Text("This user hasn't shared any contact details.", color = TextBody, fontSize = 14.sp)
                }
                if (hasPhone) {
                    ContactOption(Ic.Call, "Call ${report.posterPhone}") {
                        launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${report.posterPhone}")))
                    }
                    ContactOption(Ic.Send, "Send a text message") {
                        launch(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${report.posterPhone}")))
                    }
                }
                if (hasEmail) {
                    ContactOption(Ic.Email, "Send an email") {
                        launch(
                            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${report.posterEmail}")).apply {
                                putExtra(Intent.EXTRA_SUBJECT, "About your post: ${report.itemName}")
                            }
                        )
                    }
                }
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Auth
// ---------------------------------------------------------------------------
@Composable
fun FindItAuthScreen() {
    val auth = remember { FirebaseAuth.getInstance() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isRegister by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var messageIsError by remember { mutableStateOf(true) }
    var fullName by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }

    // Dark status-bar icons on the white login screen, light icons on the blue headers.
    StatusBarIcons(darkIcons = !isLoggedIn)

    if (isLoggedIn) {
        FindItDashboard(
            email = auth.currentUser?.email ?: "",
            userId = auth.currentUser?.uid ?: "",
            onLogout = {
                auth.signOut()
                isLoggedIn = false
                email = ""; password = ""; message = ""
                fullName = ""; studentId = ""; phoneNumber = ""; course = ""
            }
        )
        return
    }

    val headline = buildAnnotatedString {
        append("Seamlessly Connecting ")
        withStyle(SpanStyle(color = Blue)) { append("Lost Items") }
        append(" with Their ")
        withStyle(SpanStyle(color = Blue)) { append("Owners") }
    }

    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .statusBarsPadding()
            .imePadding()
    ) {
        val screenHeight = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            LogoRow()
            Spacer(Modifier.height(28.dp))

            val showHeadline = !isRegister && !imeVisible
            if (showHeadline) {
                Text(
                    headline, fontSize = 34.sp, fontWeight = FontWeight.Bold,
                    color = TextDark, lineHeight = 42.sp
                )
                // Pushes the login form toward the bottom on tall screens, like the reference.
                Spacer(Modifier.height((screenHeight - 600.dp).coerceIn(24.dp, 240.dp)))
            } else {
                Spacer(Modifier.height(if (isRegister) 8.dp else 40.dp))
            }

            Text(
                if (isRegister) "Sign Up" else "Log In",
                fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextDark,
                modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(Modifier.height(12.dp))

            if (isRegister) {
                AppTextField(fullName, { fullName = it }, "Enter your full name", enabled = !isLoading)
                Spacer(Modifier.height(10.dp))
                AppTextField(studentId, { studentId = it }, "Enter your student ID", enabled = !isLoading)
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    phoneNumber, { phoneNumber = it }, "Enter your phone number",
                    enabled = !isLoading, keyboardType = KeyboardType.Phone
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(course, { course = it }, "Enter your course / program", enabled = !isLoading)
                Spacer(Modifier.height(10.dp))
            }
            AppTextField(
                email, { email = it }, "Enter your college email",
                enabled = !isLoading, keyboardType = KeyboardType.Email
            )
            Spacer(Modifier.height(10.dp))
            AppTextField(
                password, { password = it }, "Enter the password",
                enabled = !isLoading, keyboardType = KeyboardType.Password,
                password = !passwordVisible,
                trailing = {
                    Text(
                        if (passwordVisible) "Hide" else "Show",
                        color = Blue, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { passwordVisible = !passwordVisible }
                            .padding(8.dp)
                    )
                }
            )

            if (!isRegister) {
                Text(
                    "Forgot password?",
                    color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable(enabled = !isLoading) {
                            val clean = email.trim()
                            if (clean.isBlank()) {
                                message = "Enter your email first."
                                messageIsError = true
                            } else {
                                auth.sendPasswordResetEmail(clean).addOnCompleteListener { t ->
                                    messageIsError = !t.isSuccessful
                                    message = if (t.isSuccessful) "Password reset email sent."
                                    else t.exception?.localizedMessage ?: "Could not send reset email."
                                }
                            }
                        }
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                )
            } else {
                Spacer(Modifier.height(10.dp))
            }

            Button(
                onClick = {
                    val cleanEmail = email.trim()
                    when {
                        cleanEmail.isBlank() || password.isBlank() -> {
                            message = "Please enter your email and password."
                            messageIsError = true
                        }
                        isRegister && (fullName.isBlank() || studentId.isBlank() ||
                                phoneNumber.isBlank() || course.isBlank()) -> {
                            message = "Please fill in your name, student ID, phone number and course."
                            messageIsError = true
                        }
                        isRegister && password.length < 6 -> {
                            message = "Password must be at least 6 characters."
                            messageIsError = true
                        }
                        else -> {
                            isLoading = true
                            message = ""
                            val task = if (isRegister)
                                auth.createUserWithEmailAndPassword(cleanEmail, password)
                            else auth.signInWithEmailAndPassword(cleanEmail, password)

                            task.addOnCompleteListener { t ->
                                isLoading = false
                                if (t.isSuccessful) {
                                    if (isRegister) {
                                        val uid = auth.currentUser?.uid.orEmpty()
                                        if (uid.isNotBlank()) {
                                            FirebaseDatabase.getInstance(DB_URL)
                                                .getReference("users").child(uid)
                                                .updateChildren(
                                                    mapOf(
                                                        "userId" to uid,
                                                        "fullName" to fullName.trim(),
                                                        "email" to cleanEmail,
                                                        "studentId" to studentId.trim(),
                                                        "phoneNumber" to phoneNumber.trim(),
                                                        "course" to course.trim(),
                                                        "updatedAt" to System.currentTimeMillis()
                                                    )
                                                )
                                        }
                                    }
                                    isLoggedIn = true
                                    message = ""
                                } else {
                                    messageIsError = true
                                    message = t.exception?.localizedMessage ?: "Authentication failed."
                                }
                            }
                        }
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(if (isRegister) "Sign Up" else "Log In", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }

            if (message.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    message,
                    color = if (messageIsError) Danger else FoundText,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    if (isRegister) "Already have an account? " else "Don't have an account? ",
                    color = TextBody, fontSize = 14.sp
                )
                Text(
                    if (isRegister) "Log In" else "Sign Up",
                    color = Blue, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    modifier = Modifier.clickable(enabled = !isLoading) {
                        isRegister = !isRegister
                        message = ""
                    }
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Dashboard: Home / Messages / My Posts / Profile + Report flow + Item details + Chat
// ---------------------------------------------------------------------------
private data class DashTab(val key: String, val label: String, val icon: ImageVector)

@Composable
fun FindItDashboard(
    email: String,
    userId: String,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Home") }
    var reportType by remember { mutableStateOf("Lost") }
    var detail by remember { mutableStateOf<FindItReport?>(null) }
    var contactFor by remember { mutableStateOf<FindItReport?>(null) }
    var chat by remember { mutableStateOf<ChatTarget?>(null) }

    val chats = rememberUserChats(userId)
    val unreadTotal = chats.chats.sumOf { it.unread }
    val dashContext = LocalContext.current

    // Home "Scan": validates the other student's QR, marks the transfer verified, opens the chat.
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val raw = result.contents ?: return@rememberLauncherForActivityResult
        val parts = raw.split(":")
        if (parts.size != 4 || parts[0] != "findit" || parts[1] != "verify") {
            Toast.makeText(dashContext, "This is not a FindIt QR code", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        val otherId = parts[3]
        val chatId = parts[2]
        if (userId.isBlank() || otherId == userId || chatId != chatIdOf(userId, otherId)) {
            Toast.makeText(dashContext, "This QR code is not meant for you", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        val otherName = chats.chats.firstOrNull { it.otherId == otherId }?.otherName
            ?.ifBlank { null } ?: "FindIt user"
        val db = FirebaseDatabase.getInstance(DB_URL)
        db.getReference("verifications").child(chatId)
            .setValue(
                mapOf(
                    "verifiedBy" to userId,
                    "verifiedUser" to otherId,
                    "timestamp" to System.currentTimeMillis()
                )
            )
            .addOnCompleteListener { t ->
                if (!t.isSuccessful) {
                    Toast.makeText(
                        dashContext,
                        "Could not verify: ${t.exception?.localizedMessage ?: "Permission denied"}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnCompleteListener
                }
                db.getReference("users").child(userId).get().addOnCompleteListener { u ->
                    val fetched = if (u.isSuccessful)
                        u.result?.child("fullName")?.getValue(String::class.java).orEmpty() else ""
                    val myName = fetched.ifBlank { email.substringBefore("@") }
                    val target = ChatTarget(otherId, otherName)
                    sendChatMessage(userId, myName, target, "✅ Item transfer verified by $myName", null) { }
                    Toast.makeText(dashContext, "Item transfer verified", Toast.LENGTH_SHORT).show()
                    chat = target
                }
            }
    }

    // Registered in this order so the most specific handler wins.
    BackHandler(enabled = detail == null && chat == null && selectedTab != "Home" && selectedTab != "Report") {
        selectedTab = "Home"
    }
    BackHandler(enabled = detail != null) { detail = null }
    BackHandler(enabled = chat != null) { chat = null }

    val tabs = listOf(
        DashTab("Home", "Home", Ic.Home),
        DashTab("Messages", "Messages", Ic.Chat),
        DashTab("My Posts", "My Posts", Ic.List),
        DashTab("Profile", "Profile", Ic.Person)
    )

    Scaffold(
        containerColor = ScreenBg,
        // No top inset here: each screen's header draws behind the status bar by itself.
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            if (chat == null && detail == null && selectedTab != "Report") {
                Column {
                    HorizontalDivider(color = DividerColor)
                    NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                        tabs.forEach { t ->
                            NavigationBarItem(
                                selected = selectedTab == t.key,
                                onClick = { selectedTab = t.key },
                                icon = {
                                    if (t.key == "Messages" && unreadTotal > 0) {
                                        BadgedBox(badge = {
                                            Badge(containerColor = Blue, contentColor = Color.White) {
                                                Text(if (unreadTotal > 99) "99+" else unreadTotal.toString())
                                            }
                                        }) { Icon(t.icon, contentDescription = t.label) }
                                    } else {
                                        Icon(t.icon, contentDescription = t.label)
                                    }
                                },
                                label = { Text(t.label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Blue,
                                    selectedTextColor = Blue,
                                    indicatorColor = Color.Transparent,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val pad = Modifier.padding(innerPadding)

        val openChat = chat
        val current = detail
        if (openChat != null) {
            ChatScreen(
                userId = userId,
                email = email,
                target = openChat,
                onBack = { chat = null },
                modifier = pad.consumeWindowInsets(innerPadding)
            )
        } else if (current != null) {
            ItemDetailScreen(
                report = current,
                isOwner = current.userId == userId,
                onBack = { detail = null },
                onContact = { contactFor = current },
                onDeleted = { detail = null },
                modifier = pad
            )
        } else when (selectedTab) {
            "Home" -> HomeScreen(
                modifier = pad,
                currentUserId = userId,
                onOpen = { detail = it },
                onContact = { contactFor = it },
                onReport = { type ->
                    reportType = type
                    selectedTab = "Report"
                },
                onScan = { scanLauncher.launch(portraitScanOptions("Scan to validate item transfer")) }
            )

            "Messages" -> MessagesScreen(
                chats = chats,
                onOpen = { s -> chat = ChatTarget(s.otherId, s.otherName.ifBlank { "FindIt user" }) },
                modifier = pad
            )

            "Report" -> ReportFormScreen(
                userId = userId,
                email = email,
                initialStatus = reportType,
                onBack = { selectedTab = "Home" },
                onPosted = { selectedTab = "My Posts" },
                modifier = pad
            )

            "My Posts" -> MyPostsScreen(userId = userId, onOpen = { detail = it }, modifier = pad)

            "Profile" -> ProfileScreen(
                userId = userId, email = email, onLogout = onLogout,
                onOpen = { detail = it }, modifier = pad
            )
        }
    }

    contactFor?.let { r ->
        ContactDialog(
            report = r,
            currentUserId = userId,
            onMessage = {
                contactFor = null
                chat = ChatTarget(
                    otherId = r.userId,
                    otherName = r.posterName.ifBlank { "FindIt user" },
                    draft = "Hi! I'm messaging about your post: ${r.itemName}."
                )
            },
            onDismiss = { contactFor = null }
        )
    }
}

// ---------------------------------------------------------------------------
// Messages (list of conversations)
// ---------------------------------------------------------------------------
@Composable
fun MessagesScreen(
    chats: ChatsState,
    onOpen: (ChatSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        ScreenHeader("Messages", "Chat about lost & found items")

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            when {
                chats.isLoading -> item { CenteredLoading() }
                chats.error.isNotBlank() -> item {
                    MessageState("Could not load messages", chats.error, isError = true)
                }
                chats.chats.isEmpty() -> item {
                    MessageState("No messages yet", "Tap Contact on a post to start a conversation.")
                }
                else -> items(chats.chats, key = { it.chatId }) { c ->
                    ChatRow(c) { onOpen(c) }
                }
            }
        }
    }
}

@Composable
private fun ChatRow(chat: ChatSummary, onClick: () -> Unit) {
    val hasUnread = chat.unread > 0
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (hasUnread) BlueSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(chat.otherName, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                chat.otherName.ifBlank { "FindIt user" },
                color = Navy, fontSize = 15.sp,
                fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                chat.lastMessage,
                color = if (hasUnread) TextDark else Slate, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(chatTime(chat.lastTimestamp), color = TextMuted, fontSize = 11.sp)
            if (hasUnread) {
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                        .clip(CircleShape)
                        .background(Blue)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (chat.unread > 99) "99+" else chat.unread.toString(),
                        color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Chat screen
// ---------------------------------------------------------------------------
@Composable
private fun ChatScreen(
    userId: String,
    email: String,
    target: ChatTarget,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val chatId = remember(userId, target.otherId) { chatIdOf(userId, target.otherId) }
    val messages = remember(chatId) { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()

    var draft by remember(chatId) { mutableStateOf(target.draft) }
    var myName by remember { mutableStateOf(email.substringBefore("@")) }
    var sending by remember { mutableStateOf(false) }

    // My display name, so the other person sees who is writing.
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            FirebaseDatabase.getInstance(DB_URL).getReference("users").child(userId).get()
                .addOnSuccessListener { s ->
                    val n = s.child("fullName").getValue(String::class.java) ?: ""
                    if (n.isNotBlank()) myName = n
                }
        }
    }

    // Live messages + mark the conversation as read while it is open.
    DisposableEffect(chatId) {
        val root = FirebaseDatabase.getInstance(DB_URL).reference
        val query = root.child("messages").child(chatId).orderByChild("timestamp")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val loaded = snapshot.children.mapNotNull { m ->
                    ChatMessage(
                        messageId = m.key ?: return@mapNotNull null,
                        senderId = m.child("senderId").getValue(String::class.java) ?: "",
                        text = m.child("text").getValue(String::class.java) ?: "",
                        imageUrl = m.child("imageUrl").getValue(String::class.java) ?: "",
                        timestamp = (m.child("timestamp").value as? Number)?.toLong() ?: 0L
                    )
                }.sortedBy { it.timestamp }
                messages.clear()
                messages.addAll(loaded)
                if (loaded.isNotEmpty()) {
                    root.child("userChats").child(userId).child(chatId).child("unread").setValue(0)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
            }
        }
        query.addValueEventListener(listener)
        onDispose { query.removeEventListener(listener) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun send(text: String, imageUri: Uri?) {
        if (sending || userId.isBlank()) return
        sending = true
        sendChatMessage(userId, myName, target, text, imageUri) { error ->
            sending = false
            if (error != null) {
                if (text.isNotBlank() && draft.isBlank()) draft = text
                Toast.makeText(context, "Could not send: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) send("", uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? ->
        if (bmp != null) {
            try {
                val file = File(context.cacheDir, "chat_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 90, out) }
                send("", Uri.fromFile(file))
            } catch (e: Exception) {
                Toast.makeText(context, "Could not save the photo.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showVerify by remember { mutableStateOf(false) }
    var verified by remember(chatId) { mutableStateOf(false) }

    // Is this transfer already verified?
    DisposableEffect(chatId) {
        val ref = FirebaseDatabase.getInstance(DB_URL).getReference("verifications").child(chatId)
        val l = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { verified = snapshot.exists() }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(l)
        onDispose { ref.removeEventListener(l) }
    }

    // Scan the other student's QR. It must belong to THIS chat and THIS person.
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val raw = result.contents ?: return@rememberLauncherForActivityResult
        val parts = raw.split(":")
        val valid = parts.size == 4 && parts[0] == "findit" && parts[1] == "verify" &&
                parts[2] == chatId && parts[3] == target.otherId
        if (!valid) {
            Toast.makeText(context, "This QR code is not for ${target.otherName}", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        FirebaseDatabase.getInstance(DB_URL).getReference("verifications").child(chatId)
            .setValue(
                mapOf(
                    "verifiedBy" to userId,
                    "verifiedUser" to target.otherId,
                    "timestamp" to System.currentTimeMillis()
                )
            )
            .addOnCompleteListener { t ->
                if (t.isSuccessful) {
                    showVerify = false
                    Toast.makeText(context, "Item transfer verified", Toast.LENGTH_SHORT).show()
                    send("✅ Item transfer verified by $myName", null)
                } else {
                    Toast.makeText(
                        context,
                        "Could not verify: ${t.exception?.localizedMessage ?: "Permission denied"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    if (showVerify) {
        VerifySheet(
            otherName = target.otherName,
            qrPayload = "findit:verify:$chatId:$userId",
            verified = verified,
            onScan = {
                scanLauncher.launch(
                    portraitScanOptions("Scan to validate item transfer to ${target.otherName}")
                )
            },
            onDismiss = { showVerify = false }
        )
    }

    Column(
        modifier
            .fillMaxSize()
            .background(ScreenBg)
            .imePadding()
    ) {
        // Top bar: back, avatar, name, Verify (gradient header reaching the top edge)
        HeaderContainer(
            contentPadding = PaddingValues(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Ic.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Avatar(target.otherName, 36.dp, onDark = true)
                Spacer(Modifier.width(10.dp))
                Text(
                    target.otherName,
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { showVerify = true },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (verified) Color(0xFF2E9E5B) else Yellow,
                        contentColor = if (verified) Color.White else Navy
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        if (verified) "Verified ✓" else "Verify →",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Messages
        Box(Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                Text(
                    "Say hi to ${target.otherName}",
                    color = TextMuted, fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.messageId }) { m ->
                    MessageBubble(m, mine = m.senderId == userId, otherName = target.otherName)
                }
            }
        }

        // Input bar
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = "Write a message...",
                modifier = Modifier.weight(1f),
                trailing = {
                    IconButton(onClick = { imagePicker.launch("image/*") }, enabled = !sending) {
                        TinyIcon(Ic.Attach, Slate, 20.dp)
                    }
                }
            )
            Spacer(Modifier.width(10.dp))
            val hasText = draft.isNotBlank()
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Blue)
                    .clickable(enabled = !sending) {
                        if (hasText) {
                            val t = draft.trim()
                            draft = ""
                            send(t, null)
                        } else {
                            cameraLauncher.launch(null)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (sending) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    TinyIcon(if (hasText) Ic.Send else Ic.Camera, Color.White, 22.dp)
                }
            }
        }
    }
}

private fun qrBitmap(content: String, size: Int = 640): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val pixels = IntArray(size * size)
    val dark = 0xFF1B1A57.toInt()
    val light = 0xFFFFFFFF.toInt()
    for (y in 0 until size) {
        for (x in 0 until size) {
            pixels[y * size + x] = if (matrix[x, y]) dark else light
        }
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}

/** Bottom sheet like the mockup: show my QR, or scan theirs, to validate the item transfer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerifySheet(
    otherName: String,
    qrPayload: String,
    verified: Boolean,
    onScan: () -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf("Show my QR") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (verified) {
                Surface(color = FoundBg, shape = RoundedCornerShape(50)) {
                    Text(
                        "Item transfer verified ✓",
                        color = FoundText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Show my QR", mode == "Show my QR") { mode = "Show my QR" }
                Pill("Scan QR", mode == "Scan QR") { mode = "Scan QR" }
            }
            Spacer(Modifier.height(18.dp))

            if (mode == "Show my QR") {
                val bmp = remember(qrPayload) { qrBitmap(qrPayload) }
                Box(
                    Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(FieldBg)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Your verification QR code",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Show this to $otherName so they can validate the item transfer.",
                    color = Navy, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            } else {
                Box(
                    Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(BlueSoft),
                    contentAlignment = Alignment.Center
                ) { TinyIcon(Ic.Scan, Blue, 44.dp) }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Scan $otherName's QR code to validate the item transfer.",
                    color = Navy, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onScan,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) { Text("Open scanner", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun MessageBubble(m: ChatMessage, mine: Boolean, otherName: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (mine) Blue else FieldBg)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (!mine) {
                Text(
                    otherName, color = Blue, fontSize = 12.sp, fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
            }
            if (m.imageUrl.isNotBlank()) {
                SmartImage(
                    model = m.imageUrl,
                    contentDescription = "Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(220.dp)
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.height(4.dp))
            }
            if (m.text.isNotBlank()) {
                Text(
                    m.text,
                    color = if (mine) Color.White else TextDark,
                    fontSize = 14.sp, lineHeight = 19.sp
                )
            }
            Text(
                bubbleTime(m.timestamp),
                color = if (mine) Color.White.copy(alpha = 0.85f) else TextMuted,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 2.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Home
// ---------------------------------------------------------------------------
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    currentUserId: String,
    onOpen: (FindItReport) -> Unit,
    onContact: (FindItReport) -> Unit,
    onReport: (String) -> Unit,
    onScan: () -> Unit = {}
) {
    val context = LocalContext.current
    val state = rememberReports()
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All items") }

    val filtered = state.reports.filter { r ->
        val q = searchText.trim()
        val matchesSearch = q.isBlank() ||
                r.itemName.contains(q, true) || r.category.contains(q, true) ||
                r.description.contains(q, true) || r.location.contains(q, true)
        val matchesCategory = selectedCategory == "All items" ||
                r.category.equals(selectedCategory, true)
        matchesSearch && matchesCategory
    }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Gradient header: logo, notifications, scan, search. Reaches the top edge of the phone.
            HeaderContainer(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 18.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    LogoRow(onDark = true)
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                Toast.makeText(context, "No new notifications", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) { TinyIcon(Ic.Bell, Blue, 18.dp) }
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White,
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { onScan() }
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Scan", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(6.dp))
                            TinyIcon(Ic.Scan, Blue, 16.dp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                AppTextField(
                    searchText, { searchText = it },
                    "Search your lost item here...", leadingIcon = Ic.Search,
                    containerColor = Color.White,
                    trailing = if (searchText.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchText = "" }) {
                                TinyIcon(Ic.Close, Slate, 18.dp)
                            }
                        }
                    } else null
                )
            }

            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (listOf("All items") + ReportCategories).forEach { c ->
                    Pill(c, selectedCategory == c) { selectedCategory = c }
                }
            }

            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp)
            ) {
                when {
                    state.isLoading -> item { CenteredLoading() }
                    state.error.isNotBlank() -> item {
                        MessageState("Could not load items", state.error, isError = true)
                    }
                    filtered.isEmpty() -> item {
                        if (state.reports.isEmpty()) {
                            MessageState("No items yet", "Lost and found items will show up here.")
                        } else {
                            MessageState("No items match your search", "Try another keyword or category.")
                        }
                    }
                    else -> items(filtered, key = { it.reportId }) { r ->
                        PostCard(r, onClick = { onOpen(r) }) {
                            PosterBar(r, showContact = r.userId != currentUserId) { onContact(r) }
                        }
                    }
                }
            }
        }

        // Floating Lost / Found buttons
        Column(Modifier.align(Alignment.BottomCenter)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(ScreenBg.copy(alpha = 0.96f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("Lost", "Found").forEach { type ->
                    Button(
                        onClick = { onReport(type) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) { Text(type, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Item details
// ---------------------------------------------------------------------------
@Composable
private fun ItemDetailScreen(
    report: FindItReport,
    isOwner: Boolean,
    onBack: () -> Unit,
    onContact: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize().background(ScreenBg)) {
        AppTopBar("Item details", onBack)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            ReportImage(
                report.imageUrl, report.itemName,
                Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    report.itemName.ifBlank { "Unnamed item" },
                    fontSize = 22.sp, fontWeight = FontWeight.Medium, color = TextDark,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                StatusBadge(report.status)
            }
            Spacer(Modifier.height(4.dp))
            Text(report.description, color = Slate, fontSize = 14.sp, lineHeight = 20.sp)
            if (report.category.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(50), color = FieldBg) {
                    Text(
                        report.category, color = TextBody, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = CardBg, modifier = Modifier.fillMaxWidth()) {
                MetaRow(report, Modifier.padding(horizontal = 14.dp, vertical = 14.dp))
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val text = "${report.status} item: ${report.itemName}\n" +
                                "${report.description}\n" +
                                "Location: ${report.location} (${dateLabel(report)})\n" +
                                "Posted on FindIt"
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(send, "Share post"))
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = FieldBg, contentColor = TextBody),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Share")
                }

                if (isOwner) {
                    Button(
                        onClick = { confirmDelete = true },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = FieldBg, contentColor = Danger),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Delete")
                    }
                } else {
                    Button(
                        onClick = {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                            if (uid.isBlank()) return@Button
                            FirebaseDatabase.getInstance(DB_URL)
                                .getReference("reportedItems")
                                .child(report.reportId).child(uid)
                                .setValue(System.currentTimeMillis())
                                .addOnCompleteListener { t ->
                                    Toast.makeText(
                                        context,
                                        if (t.isSuccessful) "Thanks, we'll review this post."
                                        else "Could not send your report.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = FieldBg, contentColor = TextBody),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Report")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Keeps the poster bar clear of the gesture / navigation bar.
        Column(Modifier.background(BlueSoft).navigationBarsPadding()) {
            PosterBar(report, showContact = !isOwner, onContact = onContact)
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this post?",
            message = "This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                confirmDelete = false
                deleteReport(context, report.reportId, onDeleted)
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

// ---------------------------------------------------------------------------
// Report flow (photo -> title/description -> location/date/time -> preview)
// ---------------------------------------------------------------------------
@Composable
fun ReportFormScreen(
    userId: String,
    email: String,
    initialStatus: String = "Lost",
    onBack: () -> Unit = {},
    onPosted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val now = remember { Calendar.getInstance() }
    val itemStatus = initialStatus

    var step by remember { mutableStateOf(0) }
    var itemName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now.time)) }
    var time by remember { mutableStateOf(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now.time)) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var posterName by remember { mutableStateOf("") }
    var posterInfo by remember { mutableStateOf("") }
    var posterPhone by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    BackHandler(enabled = !isLoading) { if (step == 0) onBack() else step-- }

    // Load the poster's own profile once, so posts can show who posted them.
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            FirebaseDatabase.getInstance(DB_URL).getReference("users").child(userId).get()
                .addOnSuccessListener { s ->
                    fun v(k: String) = s.child(k).getValue(String::class.java) ?: ""
                    posterName = v("fullName")
                    posterInfo = listOf(v("course"), v("yearLevel")).filter { it.isNotBlank() }.joinToString(" ")
                    posterPhone = v("phoneNumber")
                }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) selectedImageUri = uri
    }

    // "Take the photo": camera preview -> saved to cache -> uploaded like any picked image.
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? ->
        if (bmp != null) {
            try {
                val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 92, out) }
                selectedImageUri = Uri.fromFile(file)
            } catch (e: Exception) {
                message = "Could not save the photo."
            }
        }
    }

    fun reset() {
        step = 0; itemName = ""; category = ""; description = ""; location = ""
        selectedImageUri = null; message = ""
    }

    fun submit() {
        if (userId.isBlank()) { message = "Please log in again before submitting."; return }
        isLoading = true
        message = ""

        val reportsRef = FirebaseDatabase.getInstance(DB_URL).getReference("reports")
        val reportId = reportsRef.push().key
        if (reportId == null) {
            isLoading = false
            message = "Could not create a report. Please try again."
            return
        }

        fun saveReport(imageUrl: String) {
            val report = mapOf(
                "reportId" to reportId,
                "userId" to userId,
                "itemName" to itemName.trim(),
                "category" to category,
                "description" to description.trim(),
                "location" to location.trim(),
                "status" to itemStatus,
                "imageUrl" to imageUrl,
                "date" to date,
                "time" to time,
                "posterName" to posterName,
                "posterInfo" to posterInfo,
                "posterPhone" to posterPhone,
                "posterEmail" to email,
                "timestamp" to System.currentTimeMillis()
            )
            reportsRef.child(reportId).setValue(report).addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    reset()
                    onPosted()
                } else {
                    message = task.exception?.localizedMessage ?: "Failed to submit report."
                }
            }
        }

        val uri = selectedImageUri
        if (uri == null) {
            saveReport("")
        } else {
            uploadImage("report_images/$userId/$reportId.jpg", uri) { url, err ->
                if (url != null) {
                    saveReport(url)
                } else {
                    isLoading = false
                    message = "Image upload failed: $err"
                }
            }
        }
    }

    fun next() {
        message = ""
        when (step) {
            1 -> when {
                itemName.isBlank() -> { message = "Please enter the item name."; return }
                category.isBlank() -> { message = "Please select a category."; return }
                description.isBlank() -> { message = "Please enter a description."; return }
            }
            2 -> if (location.isBlank()) { message = "Please enter the location."; return }
        }
        if (step < 3) step++ else submit()
    }

    Column(modifier.fillMaxSize().background(ScreenBg)) {
        AppTopBar(
            title = if (itemStatus == "Lost") "Lost an item" else "Found an item",
            onBack = { if (step == 0) onBack() else step-- },
            enabled = !isLoading
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            when (step) {
                0 -> {
                    Text(
                        "Add the photo of your ${itemStatus.lowercase()} item (if has any)",
                        fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextBody, lineHeight = 34.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    PickerField(
                        { TinyIcon(Ic.Upload, Slate, 20.dp) },
                        if (selectedImageUri == null) "Upload Photos" else "Change Photo"
                    ) { imagePicker.launch("image/*") }
                    Spacer(Modifier.height(12.dp))
                    PickerField({ TinyIcon(Ic.Camera, Slate, 20.dp) }, "Take the photo") {
                        cameraLauncher.launch(null)
                    }

                    selectedImageUri?.let { uri ->
                        Spacer(Modifier.height(16.dp))
                        SmartImage(
                            model = uri,
                            contentDescription = "Selected image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                        LinkText("Remove photo") { selectedImageUri = null }
                    }
                }

                1 -> {
                    Text(
                        "Add title and description",
                        fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextBody, lineHeight = 34.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    AppTextField(itemName, { itemName = it }, "Write Title")
                    Spacer(Modifier.height(12.dp))
                    AppTextField(
                        description, { description = it }, "Write description",
                        singleLine = false, minLines = 5
                    )
                    Spacer(Modifier.height(18.dp))
                    FieldLabel("Category")
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReportCategories.forEach { c -> Pill(c, category == c) { category = c } }
                    }
                }

                2 -> {
                    Text(
                        "Add location, date and time",
                        fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextBody, lineHeight = 34.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    AppTextField(location, { location = it }, "Location", leadingIcon = Ic.LocationOn)
                    Spacer(Modifier.height(12.dp))
                    PickerField({ TinyIcon(Ic.DateRange, Slate, 20.dp) }, date) {
                        val c = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> date = "%02d/%02d/%04d".format(d, m + 1, y) },
                            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    Spacer(Modifier.height(12.dp))
                    PickerField({ ClockIcon(Slate, 20.dp) }, time) {
                        val c = Calendar.getInstance()
                        TimePickerDialog(
                            context,
                            { _, h, min ->
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, min)
                                }
                                time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
                            },
                            c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false
                        ).show()
                    }
                }

                else -> {
                    Text(
                        "Your post will look like this after posting",
                        fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextBody, lineHeight = 34.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    val preview = FindItReport(
                        itemName = itemName, description = description, location = location,
                        status = itemStatus, imageUrl = selectedImageUri?.toString() ?: "",
                        date = date, time = time, category = category,
                        posterName = posterName, posterInfo = posterInfo,
                        timestamp = System.currentTimeMillis()
                    )
                    PostCard(preview, onClick = {}) {
                        PosterBar(preview, showContact = true, onContact = {})
                    }
                }
            }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(message, color = Danger, fontSize = 13.sp)
            }
            Spacer(Modifier.height(20.dp))
        }

        Surface(color = BlueSoft) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step == 0) LinkText("Skip", !isLoading) { step = 1 }
                else LinkText("Back", !isLoading) { step-- }

                Button(
                    onClick = { next() },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    modifier = Modifier
                        .width(130.dp)
                        .height(44.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Text(if (step == 3) "Post" else "Next", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// My Posts
// ---------------------------------------------------------------------------
@Composable
fun MyPostsScreen(
    userId: String,
    onOpen: (FindItReport) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state = rememberReports(userId)
    var tab by remember { mutableStateOf("All") }
    var toDelete by remember { mutableStateOf<FindItReport?>(null) }

    val shown = state.reports.filter { tab == "All" || it.status.equals(tab, true) }

    Column(modifier.fillMaxSize()) {
        ScreenHeader("My Posts", "Lost and found items you have posted")

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Lost", "Found").forEach { s -> Pill(s, tab == s) { tab = s } }
                }
                Spacer(Modifier.height(14.dp))
            }

            when {
                state.isLoading -> item { CenteredLoading() }
                state.error.isNotBlank() -> item {
                    MessageState("Could not load your posts", state.error, isError = true)
                }
                shown.isEmpty() -> item {
                    MessageState(
                        "Nothing here yet",
                        "You haven't posted any ${if (tab == "All") "" else tab.lowercase() + " "}items."
                    )
                }
                else -> items(shown, key = { it.reportId }) { r ->
                    PostCard(r, onClick = { onOpen(r) }) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(BlueSoft)
                                .padding(start = 12.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Posted ${timeAgo(r.timestamp)}",
                                color = Navy, fontSize = 12.sp, modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { toDelete = r }) {
                                TinyIcon(Ic.Delete, Danger, 16.dp)
                                Spacer(Modifier.width(4.dp))
                                Text("Delete", color = Danger, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    toDelete?.let { r ->
        ConfirmDialog(
            title = "Delete this post?",
            message = "\"${r.itemName}\" will be removed for everyone.",
            confirmLabel = "Delete",
            onConfirm = {
                toDelete = null
                deleteReport(context, r.reportId)
            },
            onDismiss = { toDelete = null }
        )
    }
}

// ---------------------------------------------------------------------------
// Profile
// ---------------------------------------------------------------------------
@Composable
fun ProfileScreen(
    userId: String,
    email: String,
    onLogout: () -> Unit,
    onOpen: (FindItReport) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var yearLevel by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var editing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf("Lost") }
    var toDelete by remember { mutableStateOf<FindItReport?>(null) }

    val myReports = rememberReports(userId)
    val lostCount = myReports.reports.count { it.status.equals("Lost", true) }
    val foundCount = myReports.reports.count { it.status.equals("Found", true) }
    val totalCount = myReports.reports.size
    val shown = myReports.reports.filter { tab == "All" || it.status.equals(tab, true) }

    val databaseRef = remember(userId) {
        FirebaseDatabase.getInstance(DB_URL).getReference("users").child(userId)
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) selectedImageUri = uri
        message = ""
    }

    DisposableEffect(userId) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                fun s(k: String) = snapshot.child(k).getValue(String::class.java) ?: ""
                fullName = s("fullName"); studentId = s("studentId"); phoneNumber = s("phoneNumber")
                course = s("course"); yearLevel = s("yearLevel"); profileImageUrl = s("profileImageUrl")
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
                message = error.message
                isSuccess = false
            }
        }
        if (userId.isNotBlank()) databaseRef.addValueEventListener(listener)
        else { isLoading = false; message = "Please log in again." }

        onDispose { if (userId.isNotBlank()) databaseRef.removeEventListener(listener) }
    }

    fun saveProfile(imageUrl: String) {
        val data = mapOf(
            "userId" to userId,
            "fullName" to fullName.trim(),
            "email" to email,
            "studentId" to studentId.trim(),
            "phoneNumber" to phoneNumber.trim(),
            "course" to course.trim(),
            "yearLevel" to yearLevel.trim(),
            "profileImageUrl" to imageUrl,
            "updatedAt" to System.currentTimeMillis()
        )
        databaseRef.updateChildren(data).addOnCompleteListener { task ->
            isSaving = false
            if (task.isSuccessful) {
                profileImageUrl = imageUrl
                selectedImageUri = null
                message = "Profile updated successfully!"
                isSuccess = true
                editing = false
            } else {
                message = task.exception?.localizedMessage ?: "Failed to save profile."
                isSuccess = false
            }
        }
    }

    BackHandler(enabled = editing && !isSaving) {
        editing = false; selectedImageUri = null; message = ""
    }

    Column(modifier.fillMaxSize()) {
        // Gradient header with the overflow menu
        ScreenHeader(
            title = if (editing) "Edit Profile" else "Profile",
            subtitle = "Manage your account and posts"
        ) {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Ic.MoreVert, contentDescription = "More options", tint = Color.White)
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit profile", color = TextDark) },
                        onClick = { menuOpen = false; editing = true; message = "" }
                    )
                    DropdownMenuItem(
                        text = { Text("Log out", color = Danger) },
                        onClick = { menuOpen = false; confirmLogout = true }
                    )
                }
            }
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)
        ) {
            // Profile details card
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = CardBg, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (selectedImageUri != null || profileImageUrl.isNotBlank()) {
                            SmartImage(
                                model = selectedImageUri ?: profileImageUrl,
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Avatar(fullName.ifBlank { email }, 60.dp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                fullName.ifBlank { email.substringBefore("@") },
                                fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Navy
                            )
                            Text("Email: $email", fontSize = 12.sp, color = Slate)
                            if (course.isNotBlank() || yearLevel.isNotBlank()) {
                                Text(
                                    "Course: ${listOf(course, yearLevel).filter { it.isNotBlank() }.joinToString(" ")}",
                                    fontSize = 12.sp, color = Slate
                                )
                            }
                            if (studentId.isNotBlank()) Text("Student ID: $studentId", fontSize = 12.sp, color = Slate)
                            if (phoneNumber.isNotBlank()) Text("Phone: $phoneNumber", fontSize = 12.sp, color = Slate)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            if (editing) {
                item {
                    if (isLoading) {
                        CenteredLoading()
                    } else {
                        LinkText("Change profile photo", !isSaving) { imagePicker.launch("image/*") }
                        Spacer(Modifier.height(4.dp))
                        AppTextField(fullName, { fullName = it }, "Full name", enabled = !isSaving)
                        Spacer(Modifier.height(10.dp))
                        AppTextField(email, {}, "Email", readOnly = true)
                        Spacer(Modifier.height(10.dp))
                        AppTextField(studentId, { studentId = it }, "Student ID", enabled = !isSaving)
                        Spacer(Modifier.height(10.dp))
                        AppTextField(
                            phoneNumber, { phoneNumber = it }, "Phone number",
                            enabled = !isSaving, keyboardType = KeyboardType.Phone
                        )
                        Spacer(Modifier.height(10.dp))
                        AppTextField(course, { course = it }, "Course / Program", enabled = !isSaving)
                        Spacer(Modifier.height(10.dp))
                        AppTextField(yearLevel, { yearLevel = it }, "Year level (e.g. 1st Year)", enabled = !isSaving)

                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = {
                                if (userId.isBlank()) {
                                    message = "Please log in again."; isSuccess = false
                                    return@Button
                                }
                                isSaving = true
                                message = ""
                                val uri = selectedImageUri
                                if (uri == null) {
                                    saveProfile(profileImageUrl)
                                } else {
                                    uploadImage("profile_photos/$userId/profile.jpg", uri) { url, err ->
                                        if (url != null) {
                                            saveProfile(url)
                                        } else {
                                            isSaving = false
                                            message = "Photo upload failed: $err"
                                            isSuccess = false
                                        }
                                    }
                                }
                            },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                            } else Text("Save Profile", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { editing = false; selectedImageUri = null; message = "" },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Blue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) { Text("Cancel", color = Blue) }
                    }
                }
            } else {
                // Lost / Found / All tabs with counts (replaces the old stat tiles)
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("Lost ($lostCount)", tab == "Lost", Modifier.weight(1f), fill = true) { tab = "Lost" }
                        Pill("Found ($foundCount)", tab == "Found", Modifier.weight(1f), fill = true) { tab = "Found" }
                        Pill("Total ($totalCount)", tab == "All", Modifier.weight(1f), fill = true) { tab = "All" }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                when {
                    myReports.isLoading -> item { CenteredLoading() }
                    myReports.error.isNotBlank() -> item {
                        MessageState("Could not load your posts", myReports.error, isError = true)
                    }
                    shown.isEmpty() -> item {
                        MessageState(
                            "Nothing here yet",
                            "You haven't posted any ${if (tab == "All") "" else tab.lowercase() + " "}items."
                        )
                    }
                    else -> items(shown, key = { it.reportId }) { r ->
                        PostCard(r, onClick = { onOpen(r) }) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(BlueSoft)
                                    .padding(start = 12.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Posted ${timeAgo(r.timestamp)}",
                                    color = Navy, fontSize = 12.sp, modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { toDelete = r }) {
                                    TinyIcon(Ic.Delete, Danger, 16.dp)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Delete", color = Danger, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (message.isNotBlank()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Text(message, color = if (isSuccess) FoundText else Danger, fontSize = 13.sp)
                }
            }
        }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = "Log out?",
            message = "You will need to log in again to see your posts.",
            confirmLabel = "Log out",
            onConfirm = { confirmLogout = false; onLogout() },
            onDismiss = { confirmLogout = false }
        )
    }

    toDelete?.let { r ->
        ConfirmDialog(
            title = "Delete this post?",
            message = "\"${r.itemName}\" will be removed for everyone.",
            confirmLabel = "Delete",
            onConfirm = {
                toDelete = null
                deleteReport(context, r.reportId)
            },
            onDismiss = { toDelete = null }
        )
    }
}