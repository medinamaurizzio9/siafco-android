package bo.org.siafco.app.core.crypto

interface TokenCipher {
    fun encrypt(plainText: String): String
    fun decrypt(cipherText: String): String
}
