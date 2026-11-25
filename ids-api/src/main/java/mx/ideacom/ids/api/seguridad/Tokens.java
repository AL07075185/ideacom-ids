package mx.ideacom.ids.api.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Generacion de tokens aleatorios y su hash SHA-256 (lo unico que se guarda). */
public final class Tokens {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALFABETO_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private Tokens() {
    }

    /** Token de 32 bytes aleatorios en Base64 URL (43 caracteres). */
    public static String nuevoToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Hash SHA-256 en hexadecimal (64 caracteres). */
    public static String sha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    /** Codigo de alta legible, por ejemplo IDS-7K3M-Q9TX (sin 0/O ni 1/I). */
    public static String nuevoCodigoAlta() {
        StringBuilder sb = new StringBuilder("IDS-");
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                sb.append('-');
            }
            sb.append(ALFABETO_CODIGO.charAt(RANDOM.nextInt(ALFABETO_CODIGO.length())));
        }
        return sb.toString();
    }
}
