package com.radiofind.auth.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TotpSecretCipherTest {

  private static final String KEY = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=";

  @Test
  void decryptsWhatItEncryptedForTheSameUser() {
    TotpSecretCipher cipher = new TotpSecretCipher(KEY);

    String decrypted = cipher.decrypt(cipher.encrypt("JBSWY3DPEHPK3PXP", 7L), 7L);

    assertEquals("JBSWY3DPEHPK3PXP", decrypted);
  }

  @Test
  void storesNoPlainSecret() {
    TotpSecretCipher cipher = new TotpSecretCipher(KEY);

    String encrypted = cipher.encrypt("JBSWY3DPEHPK3PXP", 7L);

    assertFalse(encrypted.contains("JBSWY3DPEHPK3PXP"));
  }

  @Test
  void secretCopiedToAnotherUserCannotBeDecrypted() {
    TotpSecretCipher cipher = new TotpSecretCipher(KEY);
    String encrypted = cipher.encrypt("JBSWY3DPEHPK3PXP", 7L);

    assertThrows(IllegalStateException.class, () -> cipher.decrypt(encrypted, 8L));
  }

  @Test
  void plainSecretIsNotAccepted() {
    TotpSecretCipher cipher = new TotpSecretCipher(KEY);

    assertThrows(IllegalStateException.class, () -> cipher.decrypt("JBSWY3DPEHPK3PXP", 7L));
  }

  @Test
  void rejectsAKeyOfTheWrongLength() {
    assertThrows(IllegalStateException.class, () -> new TotpSecretCipher("c2hvcnQta2V5"));
  }
}
