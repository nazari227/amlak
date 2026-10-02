package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeyStoreManager {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "AshianMelkMasterKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_LENGTH_BYTES = 12
    }

    private var keyStore: KeyStore? = null
    private var fallbackKey: SecretKey? = null

    init {
        try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
            ks.load(null)
            keyStore = ks
            ensureMasterKey()
        } catch (e: Exception) {
            // Fallback for JVM unit test/Robolectric environment where AndroidKeyStore SPI is not available
            keyStore = null
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            fallbackKey = keyGen.generateKey()
        }
    }

    private fun ensureMasterKey() {
        val ks = keyStore ?: return
        if (!ks.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(parameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        fallbackKey?.let { return it }
        val ks = keyStore ?: throw IllegalStateException("KeyStore not initialized")
        val entry = ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey ?: throw IllegalStateException("Secret key not found in Keystore")
    }

    @Synchronized
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Prepend IV to cipher bytes
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    @Synchronized
    fun decrypt(encryptedPayload: String): String {
        if (encryptedPayload.isEmpty()) return ""
        return try {
            val combined = Base64.decode(encryptedPayload, Base64.NO_WRAP)
            if (combined.size < IV_LENGTH_BYTES) return ""

            val iv = ByteArray(IV_LENGTH_BYTES)
            val cipherTextLength = combined.size - IV_LENGTH_BYTES
            val cipherText = ByteArray(cipherTextLength)

            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES)
            System.arraycopy(combined, IV_LENGTH_BYTES, cipherText, 0, cipherTextLength)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
