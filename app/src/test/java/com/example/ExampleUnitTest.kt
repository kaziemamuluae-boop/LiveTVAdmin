package com.example

import com.example.data.util.KaziCrypto
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun encryption_decryption_isCorrect() {
    val originalUrl = "https://example.com/live/stream.m3u8"
    val encrypted = KaziCrypto.encrypt(originalUrl)
    
    // Ensure that the URL is actually encrypted and not readable
    assertNotEquals(originalUrl, encrypted)
    assertFalse(encrypted.contains("https://"))
    
    // Ensure that we can decrypt it back to the original URL perfectly
    val decrypted = KaziCrypto.decrypt(encrypted)
    assertEquals(originalUrl, decrypted)
  }

  @Test
  fun decryption_gracefully_handles_unencrypted_text() {
    val unencryptedUrl = "https://example.com/live/unencrypted.m3u8"
    val decrypted = KaziCrypto.decrypt(unencryptedUrl)
    
    // Ensure that trying to decrypt an unencrypted URL returns the original unencrypted text
    assertEquals(unencryptedUrl, decrypted)
  }
}
