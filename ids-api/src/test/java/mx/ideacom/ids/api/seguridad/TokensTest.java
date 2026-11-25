package mx.ideacom.ids.api.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TokensTest {

    @Test
    void tokenTiene43CaracteresYEsUnico() {
        String a = Tokens.nuevoToken();
        assertEquals(43, a.length());
        assertFalse(a.equals(Tokens.nuevoToken()));
    }

    @Test
    void sha256Conocido() {
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", Tokens.sha256("hello"));
    }

    @Test
    void codigoAltaLegible() {
        String c = Tokens.nuevoCodigoAlta();
        assertTrue(c.matches("IDS-[A-Z2-9]{4}-[A-Z2-9]{4}"));
    }

    @Test
    void extraeTokenBearer() {
        assertEquals("abc", FiltroAutenticacion.extraerToken("Bearer abc"));
        assertEquals("abc", FiltroAutenticacion.extraerToken("bearer  abc "));
        assertNull(FiltroAutenticacion.extraerToken("Basic xyz"));
        assertNull(FiltroAutenticacion.extraerToken("Bearer "));
        assertNull(FiltroAutenticacion.extraerToken(null));
    }
}
