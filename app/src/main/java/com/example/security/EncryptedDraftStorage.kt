package com.example.security

import android.content.Context
import com.example.domain.model.PropertyDraft
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

class EncryptedDraftStorage(
    private val context: Context,
    private val keyStoreManager: KeyStoreManager
) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(PropertyDraft::class.java)

    private val draftDirectory: File by lazy {
        File(context.filesDir, "secure_drafts").apply {
            if (!exists()) mkdirs()
        }
    }

    fun saveDraft(draft: PropertyDraft) {
        val json = adapter.toJson(draft)
        val encrypted = keyStoreManager.encrypt(json)
        val file = File(draftDirectory, "draft_${draft.idempotencyKey}.enc")
        file.writeText(encrypted, Charsets.UTF_8)
    }

    fun getLatestDraft(): PropertyDraft? {
        val drafts = getAllDrafts()
        return drafts.maxByOrNull { it.updatedAt }
    }

    fun getDraft(idempotencyKey: String): PropertyDraft? {
        val file = File(draftDirectory, "draft_${idempotencyKey}.enc")
        if (!file.exists()) return null
        return try {
            val encrypted = file.readText(Charsets.UTF_8)
            val decrypted = keyStoreManager.decrypt(encrypted)
            if (decrypted.isEmpty()) null else adapter.fromJson(decrypted)
        } catch (e: Exception) {
            null
        }
    }

    fun getAllDrafts(): List<PropertyDraft> {
        val files = draftDirectory.listFiles { _, name -> name.endsWith(".enc") } ?: return emptyList()
        return files.mapNotNull { file ->
            try {
                val encrypted = file.readText(Charsets.UTF_8)
                val decrypted = keyStoreManager.decrypt(encrypted)
                if (decrypted.isEmpty()) null else adapter.fromJson(decrypted)
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.updatedAt }
    }

    fun deleteDraft(idempotencyKey: String) {
        val file = File(draftDirectory, "draft_${idempotencyKey}.enc")
        if (file.exists()) {
            file.delete()
        }
    }

    fun clearAllDrafts() {
        val files = draftDirectory.listFiles() ?: return
        for (f in files) {
            f.delete()
        }
    }
}
