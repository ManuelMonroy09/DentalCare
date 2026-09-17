# Manual de usuario de DentalCare

**Versión de referencia:** 1.0.0  
**Aplicación:** DentalCare  
**Propósito:** gestión operativa de un consultorio dental

## 1. Introducción

DentalCare es una aplicación de escritorio para centralizar la operación de un consultorio dental. Su objetivo es mantener en un mismo sistema la información de pacientes, agenda, tratamientos, historial, finanzas y usuarios, junto con controles de acceso y un registro de auditoría.

Este manual describe el funcionamiento general de la aplicación y las operaciones que un usuario debe conocer para utilizarla de forma segura.

## 2. Primer inicio y configuración

En una instalación sin configuración previa, DentalCare solicita crear la cuenta administrativa.

El proceso consiste en:

1. Introducir la contraseña del administrador.
2. Confirmar la contraseña.
3. Leer el **Acuerdo de Licencia de Uso de Software**.
4. Marcar la casilla de aceptación.
5. Continuar con la configuración.
6. Guardar la clave de recuperación que muestra DentalCare.

La clave de recuperación debe conservarse fuera de la aplicación. Es un mecanismo de contingencia para recuperar el acceso de la cuenta cuando se conoce la clave correspondiente.

## 3. Inicio de sesión

En la pantalla de acceso se introducen:

- nombre de usuario;
- contraseña.

Después de una autenticación correcta se muestra la pantalla de Inicio.

Si una cuenta todavía no dispone de clave de recuperación, DentalCare la genera durante su primer inicio de sesión y muestra la clave para que pueda conservarse de forma segura.

## 4. Cierre de sesión

El cierre de sesión termina la sesión de seguridad activa sin eliminar la configuración ni los datos almacenados.

Para volver a utilizar el sistema es necesario iniciar sesión nuevamente.

## 5. Recuperación de contraseña

Si se olvida una contraseña:

1. Selecciona **¿Olvidaste tu contraseña?** en la pantalla de inicio de sesión.
2. Introduce el nombre de usuario.
3. Introduce la clave de recuperación de esa cuenta.
4. Define una nueva contraseña de al menos 8 caracteres.
5. Confirma la nueva contraseña.
6. Completa el restablecimiento.
7. Guarda la nueva clave de recuperación que DentalCare muestra después de la operación.

La clave anterior deja de ser la clave válida para futuras recuperaciones después de una recuperación exitosa.

Si se pierden tanto la contraseña como la clave de recuperación, este mecanismo no puede recuperar la cuenta.

## 6. Inicio

La pantalla de Inicio funciona como resumen operativo del consultorio y se muestra automáticamente después de un inicio de sesión correcto.

Desde la navegación principal se puede acceder únicamente a los módulos permitidos por los permisos de la cuenta actual.

## 7. Pacientes

El módulo de Pacientes permite gestionar la información de las personas atendidas por el consultorio.

Las operaciones disponibles dependen de los permisos de la cuenta. La información debe registrarse de forma precisa y únicamente para los fines propios de la operación del consultorio.

## 8. Agenda y citas

El módulo de Citas permite administrar la agenda del consultorio.

La agenda permite trabajar con horarios y citas, asociar tratamientos y actualizar el estado de las atenciones. Las citas pueden cambiar de horario respetando la duración registrada.

Los estados de una cita determinan su comportamiento dentro de la operación y del historial. Las operaciones que modifican información relevante quedan sujetas al registro de auditoría correspondiente.

## 9. Tratamientos

El catálogo de Tratamientos permite administrar los tratamientos disponibles para el consultorio, incluyendo información utilizada para calcular importes y asociar tratamientos a una cita.

Los tratamientos inactivos no deben utilizarse para nuevas asociaciones cuando la regla de negocio correspondiente impide su selección.

## 10. Historial

El módulo de Historial permite consultar la información clínica derivada de las atenciones registradas.

El historial debe tratarse como información sensible. El acceso está sujeto a permisos y a las credenciales de la cuenta utilizada.

## 11. Finanzas

El módulo de Finanzas permite consultar y gestionar información económica relacionada con las atenciones.

Entre las operaciones contempladas se encuentran:

- cargos generados por atenciones;
- pagos;
- anticipos;
- saldos pendientes;
- estado de pago;
- consultas por intervalo de fechas.

El rol Usuario dispone de acceso a Finanzas de acuerdo con la matriz de permisos de DentalCare.

## 12. Usuarios

La administración de usuarios está reservada a las cuentas con el permiso correspondiente.

La gestión de usuarios permite administrar cuentas y su estado de acceso. Las contraseñas se manejan mediante los mecanismos de seguridad de DentalCare y no deben compartirse entre personas.

## 13. Roles y permisos

DentalCare controla el acceso mediante permisos. El administrador puede gestionar los permisos disponibles cuando su cuenta tiene autorización para ello.

Las cuentas no deben recibir permisos superiores a los necesarios para su función dentro del consultorio.

## 14. Auditoría

El módulo de Auditoría funciona como registro histórico de operaciones relevantes.

Una entrada de auditoría puede identificar:

- quién realizó la operación;
- rol del usuario;
- módulo involucrado;
- acción realizada;
- entidad afectada;
- identificador de la entidad;
- fecha y hora;
- descripción;
- valor anterior;
- valor nuevo;
- resultado de la operación.

La auditoría permite revisar posteriormente qué ocurrió en el sistema y con qué resultado.

## 15. Configuración

El módulo de Configuración contiene la información general del consultorio y parámetros disponibles para la instalación.

Los cambios de configuración deben realizarse únicamente cuando sean necesarios y por una cuenta con los permisos correspondientes.

## 16. Seguridad de la información

DentalCare utiliza persistencia local y mecanismos criptográficos para proteger la configuración de seguridad y la información almacenada por los componentes que utilizan almacenamiento cifrado.

Recomendaciones para el uso diario:

- no compartir contraseñas;
- no compartir claves de recuperación;
- conservar la clave de recuperación fuera de DentalCare;
- evitar almacenar credenciales en archivos de texto sin protección;
- limitar el acceso físico al equipo donde se ejecuta DentalCare;
- utilizar las cuentas y permisos adecuados para cada persona.

## 17. Ubicación de los datos en Windows

En Windows, los datos de DentalCare se encuentran normalmente en:

```text
%LOCALAPPDATA%\DentalCare\data
```

La carpeta de datos está separada de la carpeta de instalación. Esto permite que una actualización o desinstalación de la aplicación no dependa de que los datos estén dentro de `Program Files`.

**No elimines ni modifiques manualmente los archivos de esta carpeta durante el uso normal de DentalCare.**

## 18. Respaldo de la información

La versión actual de DentalCare no incluye un módulo interno de respaldo y restauración.

Para proteger la información frente a una falla del equipo, la organización que opere el consultorio debe establecer su propio procedimiento de respaldo de la carpeta de datos, siguiendo sus requisitos de seguridad y privacidad.

Antes de copiar o trasladar información clínica, debe asegurarse que el medio de destino esté protegido y que el procedimiento cumpla las obligaciones aplicables al consultorio.

## 19. Desinstalación y actualización

La aplicación está preparada para mantener los datos fuera de la carpeta de instalación en Windows. Esto permite separar el ciclo de vida del programa del ciclo de vida de la información del consultorio.

Antes de realizar mantenimiento, actualización o desinstalación, debe comprobarse que exista una copia externa de la información necesaria para la continuidad operativa.

## 20. Buenas prácticas

1. Cada persona debe utilizar su propia cuenta.
2. Las contraseñas deben mantenerse privadas.
3. La clave de recuperación debe guardarse fuera de la aplicación.
4. Los permisos deben limitarse a las funciones necesarias.
5. Los datos clínicos deben tratarse como información sensible.
6. No deben modificarse manualmente los archivos internos de DentalCare.
7. Deben mantenerse respaldos externos según las políticas del consultorio.
8. Las operaciones relevantes deben poder revisarse mediante Auditoría.

## 21. Soporte y diagnóstico básico

Ante un comportamiento inesperado:

1. Registra qué operación se estaba realizando.
2. Anota el usuario utilizado y el momento aproximado, sin compartir contraseñas ni claves de recuperación.
3. Comprueba si el problema puede reproducirse de forma controlada.
4. Revisa el módulo de Auditoría cuando corresponda.
5. No elimines archivos de la carpeta de datos como método de diagnóstico.

Si el problema está relacionado con el acceso, conserva la clave de recuperación disponible antes de realizar cambios en la configuración.
