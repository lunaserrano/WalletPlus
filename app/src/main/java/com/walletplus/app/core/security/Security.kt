package com.walletplus.app.core.security

import android.util.Base64
import com.walletplus.app.data.local.dao.SettingsDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Deriva un hash del PIN con PBKDF2 + salt aleatorio. El PIN nunca se guarda en texto plano.
 */
object PinHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** Comparación en tiempo constante. */
    fun matches(pin: String, salt: ByteArray, expected: ByteArray): Boolean =
        MessageDigest.isEqual(hash(pin, salt), expected)
}

/**
 * Bloqueo de la app con PIN. Preparado para añadir biometría y bloqueo automático:
 * ambos solo necesitan llamar a [unlock] tras su propia verificación.
 */
class AppLockManager(private val settingsDao: SettingsDao) {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    suspend fun isLockEnabled(): Boolean = settingsDao.get()?.let { it.lockEnabled && it.pinHash != null } == true

    suspend fun setPin(pin: String) {
        require(isValidPin(pin)) { "El PIN debe tener entre 4 y 6 dígitos" }
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash(pin, salt)
        val current = settingsDao.get() ?: return
        settingsDao.upsert(
            current.copy(
                lockEnabled = true,
                pinHash = Base64.encodeToString(hash, Base64.NO_WRAP),
                pinSalt = Base64.encodeToString(salt, Base64.NO_WRAP),
            ),
        )
        _isUnlocked.value = true
    }

    suspend fun verify(pin: String): Boolean {
        val settings = settingsDao.get() ?: return false
        val hash = settings.pinHash ?: return false
        val salt = settings.pinSalt ?: return false
        val ok = PinHasher.matches(pin, Base64.decode(salt, Base64.NO_WRAP), Base64.decode(hash, Base64.NO_WRAP))
        if (ok) _isUnlocked.value = true
        return ok
    }

    suspend fun disable() {
        val current = settingsDao.get() ?: return
        settingsDao.upsert(current.copy(lockEnabled = false, pinHash = null, pinSalt = null))
        _isUnlocked.value = true
    }

    fun unlock() {
        _isUnlocked.value = true
    }

    fun lock() {
        _isUnlocked.value = false
    }

    companion object {
        fun isValidPin(pin: String): Boolean = pin.length in 4..6 && pin.all(Char::isDigit)
    }
}
