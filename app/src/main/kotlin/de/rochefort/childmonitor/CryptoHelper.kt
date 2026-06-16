/*
 * This file is part of Open Babyphone.
 *
 * Open Babyphone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Open Babyphone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open Babyphone. If not, see <http://www.gnu.org/licenses/>.
 */
package de.rochefort.childmonitor

import org.libsodium.jni.NaCl
import org.libsodium.jni.Sodium

object CryptoHelper {

    private const val ARGON2_OPS_LIMIT = 3
    private const val ARGON2_MEM_LIMIT = 64 * 1024 * 1024
    private val ARGON2_SALT = "openbabyphone.argon2id.salt.v1".toByteArray(Charsets.UTF_8)

    init {
        NaCl.sodium()
    }

    fun deriveKey(pairingCode: String): ByteArray {
        val codeBytes = pairingCode.toByteArray(Charsets.UTF_8)
        val key = ByteArray(32)
        Sodium.crypto_pwhash(
            key,
            key.size,
            codeBytes,
            codeBytes.size,
            ARGON2_SALT,
            ARGON2_OPS_LIMIT,
            ARGON2_MEM_LIMIT,
            Sodium.crypto_pwhash_alg_argon2i13()
        )
        return key
    }

    fun encryptChunk(plaintext: ByteArray, key: ByteArray, counter: Long): ByteArray {
        val nonce = buildNonce(counter)
        val ciphertext = ByteArray(plaintext.size + 16)
        val clen = intArrayOf(ciphertext.size)
        Sodium.crypto_aead_chacha20poly1305_ietf_encrypt(
            ciphertext,
            clen,
            plaintext,
            plaintext.size,
            null,
            0,
            null,
            nonce,
            key
        )
        return ciphertext
    }

    fun decryptChunk(ciphertext: ByteArray, key: ByteArray, counter: Long): ByteArray? {
        val nonce = buildNonce(counter)
        val plaintext = ByteArray(ciphertext.size - 16)
        val mlen = intArrayOf(plaintext.size)
        val result = Sodium.crypto_aead_chacha20poly1305_ietf_decrypt(
            plaintext,
            mlen,
            null,
            ciphertext,
            ciphertext.size,
            null,
            0,
            nonce,
            key
        )
        return if (result == 0) plaintext else null
    }

    private fun buildNonce(counter: Long): ByteArray {
        val nonce = ByteArray(12)
        for (i in 0 until 8) {
            nonce[11 - i] = ((counter shr (i * 8)) and 0xFF).toByte()
        }
        return nonce
    }
}
