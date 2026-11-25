package mx.ideacom.ids.api.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Locale;
import mx.ideacom.ids.api.dto.DepartamentoDTO;
import mx.ideacom.ids.api.dto.SitioDTO;
import mx.ideacom.ids.api.dto.SitioGuardarRequest;
import mx.ideacom.ids.api.dto.UsuarioDTO;
import mx.ideacom.ids.api.dto.UsuarioGuardarRequest;
import mx.ideacom.ids.api.entidad.Departamento;
import mx.ideacom.ids.api.entidad.Rol;
import mx.ideacom.ids.api.entidad.Sitio;
import mx.ideacom.ids.api.entidad.Usuario;
import mx.ideacom.ids.api.excepcion.ApiException;
import mx.ideacom.ids.api.repositorio.DepartamentoRepositorio;
import mx.ideacom.ids.api.repositorio.SitioRepositorio;
import mx.ideacom.ids.api.repositorio.UsuarioRepositorio;
import mx.ideacom.ids.api.seguridad.PasswordHasher;
import mx.ideacom.ids.api.seguridad.Sujeto;
import mx.ideacom.ids.api.seguridad.Tokens;
import mx.ideacom.ids.common.util.Fechas;

/** Departamentos, sitios y usuarios. */
@ApplicationScoped
@Transactional
public class CatalogoServicio {

    @Inject
    private DepartamentoRepositorio departamentos;

    @Inject
    private SitioRepositorio sitios;

    @Inject
    private UsuarioRepositorio usuarios;

    // ------------------------------------------------------------ departamentos

    public List<DepartamentoDTO> departamentos() {
        return departamentos.listar().stream().map(d -> new DepartamentoDTO(d.getId(), d.getNombre())).toList();
    }

    public DepartamentoDTO crearDepartamento(DepartamentoDTO req) {
        String nombre = Textos.obligatorio(req == null ? null : req.nombre(), 80, "nombre");
        if (departamentos.existeNombre(nombre)) {
            throw ApiException.conflicto("Ya existe el departamento " + nombre);
        }
        Departamento d = new Departamento();
        d.setNombre(nombre);
        d = departamentos.guardar(d);
        return new DepartamentoDTO(d.getId(), d.getNombre());
    }

    public void eliminarDepartamento(Integer id) {
        Departamento d = departamentos.buscarPorId(id)
                .orElseThrow(() -> ApiException.noEncontrado("Departamento no encontrado"));
        departamentos.eliminar(d);
    }

    // ------------------------------------------------------------ sitios

    public List<SitioDTO> sitios() {
        return sitios.listar().stream().map(this::aDto).toList();
    }

    public SitioDTO crearSitio(SitioGuardarRequest req) {
        Sitio s = new Sitio();
        s.setNombre(Textos.obligatorio(req == null ? null : req.nombre(), 100, "nombre"));
        s.setDireccion(Textos.limpio(req.direccion(), 200));
        s.setActivo(req.activo() == null || req.activo());
        s.setCodigoAlta(codigoUnico());
        s.setFechaAlta(Fechas.ahoraUtc());
        return aDto(sitios.guardar(s));
    }

    public SitioDTO actualizarSitio(Integer id, SitioGuardarRequest req) {
        Sitio s = sitio(id);
        s.setNombre(Textos.obligatorio(req == null ? null : req.nombre(), 100, "nombre"));
        s.setDireccion(Textos.limpio(req.direccion(), 200));
        if (req.activo() != null) {
            s.setActivo(req.activo());
        }
        return aDto(sitios.guardar(s));
    }

    /** Genera un codigo de alta nuevo (el anterior deja de servir para registrar agentes). */
    public SitioDTO regenerarCodigo(Integer id) {
        Sitio s = sitio(id);
        s.setCodigoAlta(codigoUnico());
        return aDto(sitios.guardar(s));
    }

    private Sitio sitio(Integer id) {
        return sitios.buscarPorId(id).orElseThrow(() -> ApiException.noEncontrado("Sitio no encontrado"));
    }

    private String codigoUnico() {
        for (int i = 0; i < 10; i++) {
            String c = Tokens.nuevoCodigoAlta();
            if (!sitios.existeCodigo(c)) {
                return c;
            }
        }
        throw new IllegalStateException("No se pudo generar un código de alta único");
    }

    private SitioDTO aDto(Sitio s) {
        return new SitioDTO(s.getId(), s.getNombre(), s.getDireccion(), s.getCodigoAlta(), s.isActivo(),
                s.getId() == null ? 0 : sitios.contarDispositivos(s.getId()));
    }

    // ------------------------------------------------------------ usuarios

    public List<UsuarioDTO> usuarios() {
        return usuarios.listar().stream().map(UsuarioDTO::de).toList();
    }

    public UsuarioDTO crearUsuario(UsuarioGuardarRequest req) {
        if (req == null) {
            throw ApiException.peticionInvalida("Cuerpo vacío");
        }
        Usuario u = new Usuario();
        u.setNombre(Textos.obligatorio(req.nombre(), 100, "nombre"));
        u.setEmail(emailValido(req.email(), null));
        PasswordHasher.validar(req.password());
        u.setPasswordHash(PasswordHasher.hash(req.password()));
        u.setRol(rol(req.rol() == null ? Rol.USUARIO : req.rol()));
        u.setActivo(req.activo() == null || req.activo());
        u.setFechaAlta(Fechas.ahoraUtc());
        return UsuarioDTO.de(usuarios.guardar(u));
    }

    public UsuarioDTO actualizarUsuario(Sujeto quien, Integer id, UsuarioGuardarRequest req) {
        if (req == null) {
            throw ApiException.peticionInvalida("Cuerpo vacío");
        }
        Usuario u = usuarios.buscarPorId(id).orElseThrow(() -> ApiException.noEncontrado("Usuario no encontrado"));
        boolean esSiMismo = u.getId().equals(quien.id());
        if (req.nombre() != null) {
            u.setNombre(Textos.obligatorio(req.nombre(), 100, "nombre"));
        }
        if (req.email() != null) {
            u.setEmail(emailValido(req.email(), u.getId()));
        }
        if (req.rol() != null) {
            Rol r = rol(req.rol());
            if (esSiMismo && !r.esAdmin()) {
                throw ApiException.peticionInvalida("No puedes quitarte el rol de administrador");
            }
            u.setRol(r);
        }
        if (req.activo() != null) {
            if (esSiMismo && !req.activo()) {
                throw ApiException.peticionInvalida("No puedes desactivar tu propio usuario");
            }
            u.setActivo(req.activo());
        }
        if (!Textos.vacio(req.password())) {
            PasswordHasher.validar(req.password());
            u.setPasswordHash(PasswordHasher.hash(req.password()));
        }
        return UsuarioDTO.de(usuarios.guardar(u));
    }

    private Rol rol(String nombre) {
        return usuarios.rolPorNombre(nombre.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> ApiException.peticionInvalida("Rol invalido: " + nombre));
    }

    private String emailValido(String email, Integer idActual) {
        String e = Textos.obligatorio(email, 150, "email").toLowerCase(Locale.ROOT);
        if (!e.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw ApiException.peticionInvalida("Correo inválido");
        }
        usuarios.buscarPorEmail(e)
                .filter(otro -> idActual == null || !otro.getId().equals(idActual))
                .ifPresent(otro -> {
                    throw ApiException.conflicto("El correo ya está registrado");
                });
        return e;
    }
}
