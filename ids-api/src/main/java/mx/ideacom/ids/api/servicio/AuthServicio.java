package mx.ideacom.ids.api.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import mx.ideacom.ids.api.dto.LoginRequest;
import mx.ideacom.ids.api.dto.LoginResponse;
import mx.ideacom.ids.api.dto.UsuarioDTO;
import mx.ideacom.ids.api.entidad.Sesion;
import mx.ideacom.ids.api.entidad.Usuario;
import mx.ideacom.ids.api.excepcion.ApiException;
import mx.ideacom.ids.api.repositorio.DispositivoRepositorio;
import mx.ideacom.ids.api.repositorio.SesionRepositorio;
import mx.ideacom.ids.api.repositorio.UsuarioRepositorio;
import mx.ideacom.ids.api.seguridad.PasswordHasher;
import mx.ideacom.ids.api.seguridad.Sujeto;
import mx.ideacom.ids.api.seguridad.Tokens;
import mx.ideacom.ids.common.modelo.EstadoRegistro;
import mx.ideacom.ids.common.util.Fechas;

/** Inicio de sesion del panel y validacion de tokens (sesion y agente). */
@ApplicationScoped
@Transactional
public class AuthServicio {

    /** Hash de relleno para que un correo inexistente tarde lo mismo que uno real. */
    private static final String HASH_RELLENO = PasswordHasher.hash("relleno-no-valido-1");

    @Inject
    private UsuarioRepositorio usuarios;

    @Inject
    private SesionRepositorio sesiones;

    @Inject
    private DispositivoRepositorio dispositivos;

    @Inject
    private ConfiguracionServicio configuracion;

    public LoginResponse login(LoginRequest req) {
        if (req == null || vacio(req.email()) || vacio(req.password())) {
            throw ApiException.peticionInvalida("Correo y contraseña son obligatorios");
        }
        Optional<Usuario> u = usuarios.buscarPorEmail(req.email().trim());
        boolean valido = PasswordHasher.verificar(req.password(),
                u.map(Usuario::getPasswordHash).orElse(HASH_RELLENO));
        if (u.isEmpty() || !valido || !u.get().isActivo()) {
            throw ApiException.noAutenticado("Correo o contraseña incorrectos");
        }
        Usuario usuario = u.get();
        String token = Tokens.nuevoToken();
        LocalDateTime ahora = Fechas.ahoraUtc();
        Sesion s = new Sesion();
        s.setUsuario(usuario);
        s.setTokenHash(Tokens.sha256(token));
        s.setFechaCreacion(ahora);
        s.setFechaExpira(ahora.plusHours(configuracion.sesionHoras()));
        sesiones.guardar(s);
        return new LoginResponse(token, usuario.getId(), usuario.getNombre(), usuario.getEmail(),
                usuario.getRol().getNombre(), Fechas.iso(s.getFechaExpira()));
    }

    public void logout(String token) {
        if (token != null) {
            sesiones.eliminarPorHash(Tokens.sha256(token));
        }
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<Sujeto> autenticarSesion(String token) {
        return sesiones.buscarVigente(Tokens.sha256(token), Fechas.ahoraUtc())
                .map(Sesion::getUsuario)
                .filter(Usuario::isActivo)
                .map(u -> new Sujeto(Sujeto.Tipo.USUARIO, u.getId(), u.getNombre(), u.esAdmin()));
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<Sujeto> autenticarAgente(String token) {
        return dispositivos.buscarPorTokenHash(Tokens.sha256(token))
                .filter(d -> d.getEstadoRegistro() != EstadoRegistro.RECHAZADO)
                .map(d -> new Sujeto(Sujeto.Tipo.AGENTE, d.getId(), d.getNombre(), false));
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public UsuarioDTO yo(Sujeto s) {
        return usuarios.buscarPorId(s.id()).map(UsuarioDTO::de)
                .orElseThrow(() -> ApiException.noAutenticado("Usuario no encontrado"));
    }

    public int limpiarSesionesExpiradas() {
        return sesiones.eliminarExpiradas(Fechas.ahoraUtc());
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
