package com.dmax.nexus.cloud

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

data class CloudMessage(
    val id: String,
    val authorId: String,
    val authorName: String,
    val text: String,
    val createdAtMillis: Long
)

class CommunityService(context: Context) {
    private val appContext = context.applicationContext

    val isConfigured: Boolean
        get() = runCatching { FirebaseApp.getApps(appContext).isNotEmpty() }.getOrDefault(false)

    private val auth: FirebaseAuth?
        get() = if (isConfigured) runCatching { FirebaseAuth.getInstance() }.getOrNull() else null

    private val firestore: FirebaseFirestore?
        get() = if (isConfigured) runCatching { FirebaseFirestore.getInstance() }.getOrNull() else null

    val currentUser: FirebaseUser? get() = auth?.currentUser

    suspend fun signIn(email: String, password: String) {
        val authInstance = requireNotNull(auth) { "Firebase is not configured. Add app/google-services.json first." }
        val result = authInstance.signInWithEmailAndPassword(email.trim(), password).await()
        val user = requireNotNull(result.user) { "Sign-in did not return a user." }
        ensureUserProfile(user.uid, user.email ?: email)
    }

    suspend fun createAccount(email: String, password: String) {
        val authInstance = requireNotNull(auth) { "Firebase is not configured. Add app/google-services.json first." }
        val result = authInstance.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = requireNotNull(result.user) { "Account creation did not return a user." }
        val username = email.substringBefore('@').take(32).ifBlank { "Nexus member" }
        firestoreOrThrow().collection("users").document(user.uid).set(
            mapOf("uid" to user.uid, "username" to username, "createdAt" to FieldValue.serverTimestamp())
        ).await()
    }

    private suspend fun ensureUserProfile(uid: String, email: String) {
        val username = email.substringBefore('@').take(32).ifBlank { "Nexus member" }
        firestoreOrThrow().collection("users").document(uid).set(
            mapOf("uid" to uid, "username" to username, "createdAt" to FieldValue.serverTimestamp()),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    fun signOut() {
        auth?.signOut()
    }

    suspend fun prepareDirectConversation(otherUidRaw: String): String {
        val user = requireNotNull(currentUser) { "Sign in first." }
        val otherUid = otherUidRaw.trim()
        require(otherUid.isNotBlank()) { "Enter the other person's Firebase user ID." }
        require(otherUid != user.uid) { "You cannot start a direct message with yourself." }
        require(otherUid.length <= 128) { "The user ID is too long." }
        val participants = listOf(user.uid, otherUid).sorted()
        val conversationId = participants.joinToString("_")
        firestoreOrThrow().collection("directMessages").document(conversationId).set(
            mapOf("participants" to participants, "updatedAt" to FieldValue.serverTimestamp()),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
        return otherUid
    }

    fun observeMessages(
        channel: String,
        otherUid: String?,
        onMessages: (List<CloudMessage>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val user = currentUser ?: return null
        val db = firestore ?: return null
        val safeChannel = channel.lowercase().filter { it.isLetterOrDigit() || it == '-' }.take(32)
            .ifBlank { "general" }
        val messageCollection = if (otherUid.isNullOrBlank()) {
            db.collection("communities").document("public")
                .collection("channels").document(safeChannel).collection("messages")
        } else {
            val target = otherUid.trim()
            val conversationId = listOf(user.uid, target).sorted().joinToString("_")
            db.collection("directMessages").document(conversationId).collection("messages")
        }

        return messageCollection.orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(75)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Could not load messages.")
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents.orEmpty().map { document ->
                    CloudMessage(
                        id = document.id,
                        authorId = document.getString("authorId").orEmpty(),
                        authorName = document.getString("authorName").orEmpty().ifBlank { "NEXUS member" },
                        text = document.getString("text").orEmpty(),
                        createdAtMillis = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    )
                }
                onMessages(messages)
            }
    }

    suspend fun sendMessage(channel: String, otherUid: String?, text: String) {
        val user = requireNotNull(currentUser) { "Sign in first." }
        val clean = text.trim()
        require(clean.isNotBlank()) { "Message cannot be empty." }
        require(clean.length <= 2000) { "Messages must be 2,000 characters or fewer." }
        val db = firestoreOrThrow()
        val username = user.email.orEmpty().substringBefore('@').take(32).ifBlank { "NEXUS member" }
        val payload = mapOf(
            "authorId" to user.uid,
            "authorName" to username,
            "text" to clean,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (otherUid.isNullOrBlank()) {
            val safeChannel = channel.lowercase().filter { it.isLetterOrDigit() || it == '-' }.take(32)
                .ifBlank { "general" }
            db.collection("communities").document("public")
                .collection("channels").document(safeChannel)
                .collection("messages").add(payload).await()
        } else {
            val target = otherUid.trim()
            require(target != user.uid) { "You cannot DM yourself." }
            val conversationId = listOf(user.uid, target).sorted().joinToString("_")
            db.collection("directMessages").document(conversationId)
                .collection("messages").add(payload).await()
            db.collection("directMessages").document(conversationId)
                .update("updatedAt", FieldValue.serverTimestamp()).await()
        }
    }

    suspend fun reportMessage(message: CloudMessage, channel: String) {
        val user = requireNotNull(currentUser) { "Sign in first." }
        firestoreOrThrow().collection("reports").add(
            mapOf(
                "reporterId" to user.uid,
                "reportedUserId" to message.authorId,
                "messageId" to message.id,
                "channel" to channel.take(64),
                "reason" to "User-submitted report",
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    private fun firestoreOrThrow(): FirebaseFirestore =
        requireNotNull(firestore) { "Firebase is not configured. Add app/google-services.json first." }
}
