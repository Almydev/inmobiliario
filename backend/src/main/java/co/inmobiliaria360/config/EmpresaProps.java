package co.inmobiliaria360.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Datos de la inmobiliaria que salen impresos en los documentos. Se definen por variables de entorno. */
@ConfigurationProperties("app.empresa")
public record EmpresaProps(
        String nombre,
        String direccion,
        String nit,
        String email,
        String beneficiario,
        String beneficiarioDocumento,
        String cuentaTipo,
        String cuentaNumero) {}
