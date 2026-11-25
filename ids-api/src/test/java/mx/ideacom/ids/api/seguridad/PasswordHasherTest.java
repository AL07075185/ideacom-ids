package mx.ideacom.ids.api.seguridad;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void hashYVerificacion() {
        String h = PasswordHasher.hash("Clave2026x");
        assertTrue(h.startsWith("pbkdf2_sha256$210000$"));
        assertTrue(PasswordHasher.verificar("Clave2026x", h));
        assertFalse(PasswordHasher.verificar("Clave2026y", h));
    }

    @Test
    void mismaClaveDistintaSal() {
        assertFalse(PasswordHasher.hash("Clave2026x").equals(PasswordHasher.hash("Clave2026x")));
    }

    @Test
    void verificaHashDelAdminInicial() {
        // Debe coincidir con db/datos-iniciales.sql
        String admin = "pbkdf2_sha256$210000$Wzw074IG5qLGOzmb1/8Ggw==$728V9drXGqXQfcelhtm7rKxVndi6ANxiz/+MQ2w2gxE=";
        assertTrue(PasswordHasher.verificar("Admin123!", admin));
    }

    @Test
    void formatoInvalidoNoVerifica() {
        assertFalse(PasswordHasher.verificar("x", "md5$abc"));
        assertFalse(PasswordHasher.verificar(null, "x"));
    }

    @Test
    void reglasMinimas() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.validar("corta1"));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.validar("sinnumeros"));
        PasswordHasher.validar("conNumero1");
    }
}
