# Seguridad de DentalCare

**Versión de referencia:** 1.0.0

Este documento describe, a nivel técnico y operativo, los mecanismos de seguridad implementados en DentalCare. No sustituye las políticas de seguridad del consultorio ni asesoría jurídica o especializada.

## 1. Modelo de seguridad

DentalCare separa la autenticación de usuario de la clave maestra utilizada para proteger la información de seguridad.

La arquitectura utiliza, entre otros componentes:

- `AuthenticationService` para coordinar el acceso y recuperación;
- `UserService` para las cuentas de usuario;
- `MasterKeyService` para la clave maestra y su protección;
- `SecuritySession` para mantener la clave maestra disponible únicamente durante una sesión autenticada;
- servicios criptográficos para derivación de claves y cifrado;
- persistencia local para conservar la configuración necesaria entre ejecuciones.

## 2. Contraseñas

DentalCare exige contraseñas de al menos 8 caracteres.

La contraseña no se utiliza como clave maestra directamente. Se deriva una clave de protección a partir de la contraseña y un valor aleatorio de sal. Con esa clave se protege la clave maestra mediante cifrado autenticado por la implementación criptográfica de la aplicación.

El objetivo de este diseño es evitar que un cambio de contraseña obligue a volver a cifrar todos los datos protegidos con la clave maestra.

## 3. Clave maestra

Durante la configuración inicial se genera una clave maestra aleatoria.

La clave maestra se protege con las credenciales de autenticación y se mantiene en memoria durante una sesión de seguridad autenticada. Al cerrar sesión se limpia la sesión de seguridad sin eliminar la configuración persistente.

La existencia de una clave maestra independiente permite que la recuperación de una cuenta pueda restablecer sus credenciales sin depender de conocer la contraseña anterior.

## 4. Clave de recuperación

La clave de recuperación es un secreto independiente de la contraseña.

Se genera mediante un generador criptográficamente seguro y se representa en Base64 URL-safe. La aplicación la muestra agrupada visualmente para facilitar su lectura y conservación.

DentalCare no necesita almacenar la clave de recuperación en texto plano para poder utilizarla. En su lugar, conserva los parámetros necesarios para derivar una clave de protección y un envoltorio cifrado de la clave maestra.

## 5. Recuperación de una cuenta

El proceso general es:

1. El usuario introduce su cuenta y clave de recuperación.
2. DentalCare deriva la clave de protección correspondiente.
3. Se intenta descifrar la clave maestra protegida para esa cuenta.
4. Si la comprobación es válida, se recupera la sesión de seguridad.
5. Se establece una nueva contraseña.
6. Se genera una nueva clave de recuperación.
7. La clave anterior deja de ser válida para la siguiente recuperación.

Una recuperación exitosa no tiene como objetivo borrar pacientes, citas, tratamientos, historial o información financiera.

## 6. Recuperación del administrador inicial

El administrador inicial dispone de un mecanismo de recuperación asociado a la configuración de seguridad de la aplicación.

La recuperación del administrador utiliza la clave de recuperación para volver a obtener acceso a la clave maestra y posteriormente restablecer la contraseña.

## 7. Cuentas creadas posteriormente

Las cuentas de usuario creadas después de la configuración inicial pueden tener una clave de recuperación individual.

Para evitar mostrar secretos innecesariamente durante la creación de una cuenta, DentalCare puede generar la clave individual en el primer inicio de sesión de esa cuenta cuando todavía no exista una clave configurada.

## 8. Cambio de contraseña

El cambio normal de contraseña conserva la clave maestra y conserva los datos asociados a la recuperación. La nueva contraseña obtiene su propio material de protección.

El cambio de contraseña no es una operación de migración de los datos clínicos ni financieros.

## 9. Sesión de seguridad

Cerrar sesión no elimina los datos persistentes ni restablece la instalación. Su función es limpiar la clave maestra de la sesión activa para que el siguiente acceso tenga que autenticarse nuevamente.

## 10. Persistencia de datos

La ubicación de datos se centraliza mediante `DataDirectoryService`.

En Windows, la ubicación predeterminada es:

```text
%LOCALAPPDATA%\DentalCare\data
```

La separación respecto de la carpeta de instalación permite que la aplicación no dependa de permisos de escritura sobre `Program Files` y facilita el mantenimiento del programa sin mezclarlo con la información del consultorio.

La ubicación puede personalizarse mediante:

```text
-dentalcare.data.dir=<ruta>
```

## 11. Persistencia cifrada

Los componentes de persistencia que manejan información protegida utilizan la infraestructura de almacenamiento cifrado de DentalCare. La configuración de seguridad utiliza específicamente `security.dat` para conservar la información necesaria para reconstruir la autenticación y la recuperación.

No debe interpretarse que todos los archivos existentes en el proyecto utilizan exactamente el mismo mecanismo de almacenamiento. La documentación debe distinguir entre configuración de seguridad, persistencia cifrada y componentes heredados de la aplicación.

## 12. Auditoría

Las operaciones relevantes pueden quedar registradas mediante el sistema de auditoría de DentalCare.

El registro contempla información como usuario, rol, módulo, acción, entidad, fecha y hora, cambios y resultado.

La auditoría tiene una finalidad histórica y de trazabilidad. No sustituye un sistema externo de monitoreo, SIEM o registro inmutable.

## 13. Protección de la clave de recuperación

La clave de recuperación debe considerarse equivalente a una credencial de acceso de contingencia.

Debe:

- conservarse fuera de DentalCare;
- mantenerse en un lugar controlado;
- evitarse su almacenamiento en capturas públicas, repositorios o archivos sin protección;
- no enviarse junto con la contraseña de la cuenta;
- reemplazarse cuando se complete una recuperación.

## 14. Limitaciones y responsabilidad operativa

DentalCare es una aplicación local de escritorio. Su seguridad efectiva también depende del entorno donde se ejecuta.

La protección del equipo, cuenta de Windows, antivirus, cifrado del dispositivo, controles de acceso físico, políticas de respaldo y cumplimiento normativo quedan fuera del alcance de la aplicación y deben gestionarse de acuerdo con las necesidades del consultorio.

La información clínica y financiera debe tratarse como información sensible y manejarse conforme a las obligaciones legales y administrativas aplicables.

## 15. Recomendaciones para una instalación real

Antes de utilizar DentalCare con información real:

1. Define quién administrará las cuentas y permisos.
2. Conserva las claves de recuperación mediante un procedimiento seguro.
3. Establece un procedimiento externo de respaldo de los datos.
4. Limita el acceso físico y de sistema operativo al equipo.
5. Evita compartir cuentas entre trabajadores.
6. Revisa periódicamente la auditoría cuando sea necesario.
7. Mantén controladas las versiones instaladas.
8. Documenta el procedimiento interno para recuperación de cuentas.

## 16. Alcance de este documento

Este documento describe el comportamiento y arquitectura de seguridad implementados en la versión de referencia 1.0.0. Cualquier modificación posterior a la autenticación, persistencia, criptografía, permisos o recuperación debe acompañarse de una revisión de esta documentación.
