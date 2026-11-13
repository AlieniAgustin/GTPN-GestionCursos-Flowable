package com.backend.proxy.security;

/**
 * Los dos tipos de usuario que entran al sitio (enunciado 4.1).
 *
 * <p>El personal de la institucion se valida contra el IDM de Flowable con su usuario y
 * contrasena del motor (enunciado 3.3). Las empresas usan la cuenta que les crea
 * "Habilitar acceso de la empresa" y que vive en la BD de la institucion (enunciado 4.1.2).
 */
public final class Roles {

    public static final String INSTITUCION = "INSTITUCION";
    public static final String EMPRESA = "EMPRESA";

    public static final String ROLE_INSTITUCION = "ROLE_" + INSTITUCION;
    public static final String ROLE_EMPRESA = "ROLE_" + EMPRESA;

    private Roles() {
    }
}
