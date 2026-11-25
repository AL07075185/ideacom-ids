package mx.ideacom.ids.api.seguridad;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de contrasenas con PBKDF2-HMAC-SHA256 (incluido en Java, sin librerias).
 * Formato guardado: {@code pbkdf2_sha256$iteraciones$salBase64$hashBase64}
 */
public final class PasswordHasher {

    private static final String PREFIJO = "pbkdf2_sha256";
    private static final int ITERACIONES = 210_000;
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String password) {
        validar(password);
        byte[] sal = new byte[BYTES_SAL];
        RANDOM.nextBytes(sal);
        byte[] h = pbkdf2(password.toCharArray(), sal, ITERACIONES);
        Base64.Encoder b64 = Base64.getEncoder();
        return PREFIJO + "$" + ITERACIONES + "$" + b64.encodeToString(sal) + "$" + b64.encodeToString(h);
    }

    public static boolean verificar(String password, String guardado) {
        if (password == null || guardado == null) {
            return false;
        }
        String[] partes = guardado.split("\\$");
        if (partes.length != 4 || !PREFIJO.equals(partes[0])) {
            return false;
        }
        try {
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] calculado = pbkdf2(password.toCharArray(), sal, iteraciones);
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Reglas minimas: 8 caracteres, al menos una letra y un número. */
    public static void validar(String password) {
        if (password == null || password.length() < 8
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos 8 caracteres, una letra y un número");
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] sal, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, sal, iteraciones, BITS_HASH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 no disponible", e);
        }
    }
}
