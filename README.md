# DentalCare

**Sistema de gestión para consultorio dental**

DentalCare es una aplicación de escritorio orientada a la gestión operativa de un consultorio dental. Centraliza pacientes, agenda, tratamientos, historial, finanzas, usuarios y auditoría en una interfaz JavaFX, con persistencia local y mecanismos de protección de credenciales y datos.

**Versión de aplicación:** 1.0.0  
**Autoría:** Juan Manuel Mendoza Monroy & Scarleth  
**Java:** 17  
**Spring Boot:** 3.5.5  
**JavaFX:** 21.0.8  
**Build:** Maven

## Contenido

- [Características](#características)
- [Roles y permisos](#roles-y-permisos)
- [Seguridad y recuperación de acceso](#seguridad-y-recuperación-de-acceso)
- [Persistencia y ubicación de datos](#persistencia-y-ubicación-de-datos)
- [Requisitos](#requisitos)
- [Ejecución desde código fuente](#ejecución-desde-código-fuente)
- [Instalación en Windows](#instalación-en-windows)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Pruebas](#pruebas)
- [Documentación adicional](#documentación-adicional)

## Características

DentalCare incluye los siguientes módulos principales:

- **Inicio:** resumen operativo del consultorio.
- **Pacientes:** alta, consulta y gestión de información de pacientes.
- **Citas:** agenda y gestión de citas, tratamientos asociados y estados.
- **Tratamientos:** catálogo y administración de tratamientos.
- **Historial:** consulta del historial clínico relacionado con la atención registrada.
- **Finanzas:** cargos, pagos, anticipos y estado financiero de las atenciones.
- **Usuarios:** administración de cuentas cuando el usuario dispone del permiso correspondiente.
- **Roles y permisos:** configuración de permisos de acceso.
- **Auditoría:** registro histórico de operaciones relevantes, incluyendo usuario, acción, entidad, fecha, cambios y resultado.
- **Configuración:** información y parámetros generales del consultorio.

## Roles y permisos

El acceso a los módulos se controla mediante permisos asociados al usuario.

### Administrador

El administrador puede acceder a las funciones de administración y operación del sistema, incluyendo pacientes, citas, tratamientos, historial, finanzas, configuración, usuarios, roles/permisos y auditoría.

### Usuario

El rol Usuario está orientado a la operación cotidiana y dispone de acceso a Inicio, Pacientes, Citas y Finanzas, de acuerdo con la matriz de permisos configurada en DentalCare.

La autorización se valida dentro de la aplicación, no únicamente mediante la visibilidad de botones o menús.

## Seguridad y recuperación de acceso

DentalCare utiliza una clave maestra interna para proteger la información de seguridad y mantener separada la protección criptográfica de los datos de la contraseña del usuario.

Las contraseñas deben tener **al menos 8 caracteres**.

Durante la configuración inicial se genera una **clave de recuperación** para la cuenta administrativa. Las cuentas creadas posteriormente pueden recibir su propia clave de recuperación durante su primer inicio de sesión.

La clave de recuperación:

- permite restablecer la contraseña de la cuenta correspondiente;
- debe conservarse fuera de DentalCare, en un lugar seguro o gestor de contraseñas;
- no debe compartirse;
- se reemplaza después de una recuperación exitosa;
- no debe almacenarse dentro de la aplicación como texto que el usuario pueda consultar posteriormente.

El restablecimiento de contraseña no está diseñado para eliminar los datos clínicos o financieros del consultorio.

### Importante

La pérdida simultánea de la contraseña y de la clave de recuperación impide utilizar este mecanismo de recuperación. DentalCare no incorpora una puerta trasera para recuperar una cuenta sin las credenciales o la clave de recuperación válidas.

## Persistencia y ubicación de datos

Los datos persistentes se almacenan fuera de la carpeta de instalación en Windows. Esto permite actualizar o desinstalar la aplicación sin depender de permisos de escritura sobre `Program Files` y evita que la instalación sea el lugar donde reside la información del consultorio.

En Windows, la ubicación predeterminada es:

```text
%LOCALAPPDATA%\DentalCare\data
```

En otros sistemas, si no se especifica una ubicación personalizada, DentalCare utiliza una carpeta `data` relativa al directorio desde el que se ejecuta la aplicación.

También puede establecerse una ubicación mediante la propiedad del sistema:

```text
-dentalcare.data.dir=/ruta/de/datos
```

Los archivos de persistencia y seguridad son responsabilidad directa de la instalación y del equipo donde se ejecuta DentalCare. Antes de realizar operaciones de mantenimiento del sistema operativo, conviene conservar una copia externa de la carpeta de datos.

> DentalCare no incluye actualmente un módulo interno de copia y restauración de respaldos. La protección de la información frente a fallas del equipo debe complementarse con las políticas de respaldo del entorno donde se utilice.

## Requisitos

### Para ejecutar desde código fuente

- JDK 17 o compatible.
- Maven.
- Un sistema operativo compatible con JavaFX 21.0.8.

### Para generar el instalador de Windows

El proceso de empaquetado utiliza `jpackage` y WiX Toolset. El script de construcción comprueba la disponibilidad de Maven y `jpackage` antes de generar el MSI.

## Ejecución desde código fuente

Clona el repositorio y ejecuta:

```bash
mvn clean test
```

Para iniciar la aplicación con el plugin de JavaFX:

```bash
mvn javafx:run
```

El comando de pruebas debe completarse sin fallos antes de considerar una compilación lista para empaquetado.

## Instalación en Windows

El proyecto incluye un proceso de empaquetado MSI en:

```text
packaging/windows/build-installer.ps1
```

El instalador está preparado para:

- instalar DentalCare por usuario;
- crear acceso desde el menú Inicio;
- crear acceso directo;
- utilizar el icono de DentalCare;
- mantener los datos fuera de la carpeta de instalación;
- generar un MSI identificado como versión 1.0.0.

Desde PowerShell, en la raíz del repositorio:

```powershell
.\packaging\windows\build-installer.ps1
```

El MSI resultante se coloca en:

```text
target\installer
```

## Estructura del proyecto

La aplicación está organizada por responsabilidades:

```text
src/
├── main/
│   ├── java/mx/dentalcare/
│   │   ├── config/          # Configuración y ubicación de datos
│   │   ├── domain/          # Entidades y reglas de dominio
│   │   ├── event/           # Eventos de dominio
│   │   ├── infrastructure/  # Persistencia e infraestructura
│   │   ├── repository/      # Acceso a datos
│   │   ├── security/        # Autenticación, permisos y criptografía
│   │   ├── service/         # Lógica de aplicación
│   │   └── ui/              # JavaFX, controladores y presentación
│   └── resources/
│       └── ui/              # FXML, CSS, iconos y recursos visuales
└── test/
    └── java/                # Pruebas automatizadas
```

## Pruebas

El proyecto cuenta con pruebas unitarias para reglas de dominio, servicios, citas, tratamientos, finanzas, configuración, usuarios y permisos.

Ejecuta toda la suite con:

```bash
mvn test
```

La versión actualmente validada del proyecto alcanzó **78 pruebas ejecutadas, 0 fallos y 0 errores**. Este dato corresponde a la última ejecución validada durante el cierre de la versión y debe volver a comprobarse después de cambios posteriores.

## Documentación adicional

- [Manual de usuario](docs/MANUAL-USUARIO.md)
- [Seguridad y recuperación](docs/SEGURIDAD.md)

## Licencia de uso

DentalCare presenta al usuario un acuerdo de licencia durante la configuración inicial, antes de crear la configuración de seguridad y el administrador. La aceptación se solicita únicamente en ese proceso de primera configuración.

La aplicación incorpora componentes de terceros sujetos a sus respectivas licencias. El acuerdo de licencia de DentalCare no sustituye ni modifica las licencias aplicables a esas dependencias.

**© 2026 Juan Manuel Mendoza Monroy & Scarleth. Todos los derechos reservados.**
