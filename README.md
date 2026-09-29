# ContaNoPortable — Sistema Contable

Aplicación de escritorio para gestionar el ciclo contable de una empresa. Está
desarrollada con **Java 21 LTS** y **JavaFX 21**, y puede trabajar con una base
de datos local SQLite o con Microsoft SQL Server.

<div align="center">
  <img src="./Logo grande.png" alt="ContaNoPortable" width="420">
</div>

![Java](https://img.shields.io/badge/Java-21-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue)
![Maven](https://img.shields.io/badge/Maven-3.8%2B-C71A36)
![SQLite](https://img.shields.io/badge/SQLite-supported-003B57)
![SQL Server](https://img.shields.io/badge/SQL%20Server-supported-CC2927)
![License](https://img.shields.io/badge/license-Academic-lightgrey)

## Funcionalidades

- **Dashboard** con indicadores de activo, pasivo, patrimonio, utilidad y
  gráficos de resumen.
- **Libro Diario** para registrar, consultar, filtrar y eliminar asientos.
  El número de asiento se asigna automáticamente y el guardado se bloquea
  hasta que la suma del Debe sea igual a la suma del Haber.
- **Asientos predefinidos**: permite guardar plantillas con sus renglones y
  cargarlas al registrar nuevas operaciones.
- **Libro Mayor** con mayorización automática, filtros por fecha y detalle de
  movimientos por cuenta.
- **Kárdex** de productos con entradas, salidas y saldo acumulado.
- **Balanza de Comprobación** con movimientos y saldos Deudor/Acreedor.
- **Estados financieros**: Balance General y Estado de Resultados, con
  clasificación por código contable y cálculo automático de la utilidad.
- **Catálogo de cuentas** jerárquico, con búsqueda, creación de subcuentas y
  validación de cuentas que permiten movimiento.
- **Configuración** del producto, costo, precio de venta, nombre de la empresa
  y modalidad/tasa de IVA. La configuración de IVA se aplica a los renglones
  nuevos y no modifica asientos históricos.
- **Exportación** de libros y reportes a PDF y Excel (`.xlsx`), según la vista.
- **Respaldos** completos en archivos `.cbackup`, incluyendo catálogo,
  productos, configuración, asientos, Kárdex y plantillas.
- **Restauración** de datos desde un respaldo o de la base de demostración
  incluida en el proyecto.

## Requisitos

Para ejecutar el proyecto desde el código fuente:

- Windows 10/11, macOS o Linux.
- JDK 21 o superior.
- Maven 3.8 o superior.
- Conexión a Maven Central la primera vez que se descarguen las dependencias.

Las dependencias principales se encuentran en [`pom.xml`](./pom.xml):
JavaFX, SQLite JDBC, el controlador JDBC de Microsoft SQL Server, Apache POI y
JUnit 5.

## Ejecución desde el código fuente

Desde la raíz del proyecto:

```powershell
mvn javafx:run
```

También puede ejecutarse desde Apache NetBeans con **Run** (`F6`) o desde un
IDE compatible con Maven usando como clase principal:

```text
com.mycompany.programa_contable.Launcher
```

Al iniciar por primera vez, la aplicación crea las tablas y carga los datos de
demostración automáticamente.

## Base de datos

### SQLite (predeterminada)

SQLite es el motor activo por defecto. La base de datos se guarda fuera de la
carpeta de instalación para conservarla durante las actualizaciones:

- Windows: `%LOCALAPPDATA%\ContaNoPortable\contabilidad.db`
- Otros sistemas: `~/.contanoportable/contabilidad.db`

Si existe una base `contabilidad.db` antigua en la raíz del proyecto, la
aplicación la migra automáticamente cuando todavía no existe la base en la
carpeta de datos del usuario.

### Microsoft SQL Server

La vista **Configuración** permite probar y activar una conexión a SQL Server.
Se admiten host, puerto, base de datos, usuario y contraseña. El valor inicial
del puerto es `1433` y el nombre de base de datos de ejemplo es
`Sistema_Contable`.

Los scripts para cada motor están disponibles en los siguientes recursos:

- SQLite: [`schema.sql`](./schema.sql) y [`data.sql`](./data.sql)
- SQL Server: [`schema_sqlserver.sql`](./schema_sqlserver.sql) y
  [`data_sqlserver.sql`](./data_sqlserver.sql)

La aplicación usa el script correspondiente al motor activo cuando inicializa
una base que todavía no contiene las tablas requeridas.

## Uso básico

1. Abra **Configuración** y, si es necesario, defina el nombre de la empresa,
   el producto base, costo, precio y la modalidad/tasa de IVA.
2. En **Libro Diario**, seleccione una cuenta, indique el Debe o el Haber y
   agregue los renglones. El asiento solo se puede guardar cuando está
   cuadrado.
3. Consulte **Libro Mayor**, **Kárdex**, **Balanza**, **Balance General** y
   **Est. Resultados** para revisar los cálculos actualizados.
4. Use **Exportar** en cada vista para generar PDF o Excel.
5. Antes de una operación importante, use **Exportar backup**. Para recuperar
   información, use **Subir backup** y confirme la restauración.

### IVA

La modalidad puede ser:

- **IVA incluido en el monto total**: desglosa el IVA desde el importe
  introducido.
- **Más IVA**: calcula el IVA adicional al monto base.

La tasa se expresa como porcentaje y solo afecta los renglones que se agreguen
después de guardar la configuración.

## Instalador de Windows

El repositorio incluye [`INSTALADOR-ContaNoPortable-1.0.0.msi`](./INSTALADOR-ContaNoPortable-1.0.0.msi),
un instalador MSI para Windows x64 que incluye su propio runtime de Java.
La máquina destino no necesita tener Java, Maven ni SQL Server instalados para
usar la base SQLite local.

Para construir una nueva versión se requiere Windows x64, JDK 21 x64, Maven y
WiX Toolset. Consulte [`MSI_WINDOWS.md`](./MSI_WINDOWS.md) para los requisitos
completos y ejecute:

```powershell
.\package-windows.ps1
```

El script compila el proyecto, crea un runtime reducido con `jlink` y genera
el MSI con `jpackage`. El resultado se guarda en una carpeta con fecha dentro
de `target`.

## Pruebas

Ejecute las pruebas unitarias con Maven:

```powershell
mvn test
```

Las pruebas cubren la validación de partida doble y los cálculos de
mayorización y estados financieros:

- [`PartidaDobleTest.java`](./src/test/java/com/mycompany/programa_contable/PartidaDobleTest.java)
- [`KardexServiceTest.java`](./src/test/java/com/mycompany/programa_contable/KardexServiceTest.java)
- [`EstadosFinancierosTest.java`](./src/test/java/com/mycompany/programa_contable/EstadosFinancierosTest.java)

## Estructura del proyecto

```text
PROGRAMA_CONTABLE/
.
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── mycompany
│   │   │           └── programa_contable
│   │   │               ├── dao
│   │   │               │   ├── AsientoPredefinidoDAO.java
│   │   │               │   ├── CuentaDAO.java
│   │   │               │   └── LibroDiarioDAO.java
│   │   │               ├── db
│   │   │               │   └── DatabaseManager.java
│   │   │               ├── model
│   │   │               │   ├── Asiento.java
│   │   │               │   ├── BalanceGeneralDTO.java
│   │   │               │   ├── BalanzaComprobacionDTO.java
│   │   │               │   ├── ConfiguracionDAO.java
│   │   │               │   ├── Cuenta.java
│   │   │               │   ├── DetalleAsiento.java
│   │   │               │   ├── EstadoResultadosDTO.java
│   │   │               │   ├── KardexFilaDTO.java
│   │   │               │   ├── MayorCuenta.java
│   │   │               │   ├── MovimientoKardex.java
│   │   │               │   ├── MovimientoMayor.java
│   │   │               │   ├── NaturalezaCuenta.java
│   │   │               │   ├── Producto.java
│   │   │               │   ├── ProductoDAO.java
│   │   │               │   └── TipoCuenta.java
│   │   │               ├── service
│   │   │               │   ├── BackupService.java
│   │   │               │   ├── ExportacionService.java
│   │   │               │   ├── KardexService.java
│   │   │               │   ├── MayorizacionService.java
│   │   │               │   └── ReportesFinancierosService.java
│   │   │               ├── ui
│   │   │               │   └── views
│   │   │               │       ├── BalanceGeneralView.java
│   │   │               │       ├── BalanzaComprobacionView.java
│   │   │               │       ├── CatalogoCuentasView.java
│   │   │               │       ├── ConfiguracionView.java
│   │   │               │       ├── DashboardView.java
│   │   │               │       ├── EstadoResultadosView.java
│   │   │               │       ├── ExportMenuFactory.java
│   │   │               │       ├── KardexView.java
│   │   │               │       ├── LibroDiarioView.java
│   │   │               │       ├── LibroMayorView.java
│   │   │               │       └── MainLayoutView.java
│   │   │               ├── Launcher.java
│   │   │               └── MainApp.java
│   │   └── resources
│   │       ├── com
│   │       │   └── mycompany
│   │       │       └── programa_contable
│   │       │           └── css
│   │       │               └── styles.css
│   │       ├── database
│   │       │   ├── data_sqlserver.sql
│   │       │   ├── data.sql
│   │       │   ├── schema_sqlserver.sql
│   │       │   └── schema.sql
│   │       ├── icono.png
│   │       └── logo-grande.png
│   └── test
│       └── java
│           └── com
│               └── mycompany
│                   └── programa_contable
│                       ├── EstadosFinancierosTest.java
│                       └── PartidaDobleTest.java
├── target
│   ├── classes
│   │   └── com
│   │       └── mycompany
│   │           └── programa_contable
│   │               ├── css
│   │               │   └── styles.css
│   │               ├── dao
│   │               │   ├── CuentaDAO.class
│   │               │   ├── LibroDiarioDAO.class
│   │               │   └── ProductoDAO.class
│   │               ├── db
│   │               │   ├── DatabaseManager.class
│   │               │   └── DatabaseManager$MotorBD.class
│   │               ├── model
│   │               │   ├── Asiento.class
│   │               │   ├── BalanceGeneralDTO.class
│   │               │   ├── BalanceGeneralDTO$LineaBalance.class
│   │               │   ├── BalanzaComprobacionDTO.class
│   │               │   ├── BalanzaComprobacionDTO$Renglon.class
│   │               │   ├── ConfiguracionDAO.class
│   │               │   ├── Cuenta.class
│   │               │   ├── DetalleAsiento.class
│   │               │   ├── EstadoResultadosDTO.class
│   │               │   ├── EstadoResultadosDTO$LineaReporte.class
│   │               │   ├── KardexFilaDTO.class
│   │               │   ├── MayorCuenta.class
│   │               │   ├── MovimientoKardex.class
│   │               │   ├── MovimientoMayor.class
│   │               │   ├── NaturalezaCuenta.class
│   │               │   ├── Producto.class
│   │               │   └── TipoCuenta.class
│   │               ├── service
│   │               │   ├── ExportacionService.class
│   │               │   ├── KardexService.class
│   │               │   ├── MayorizacionService.class
│   │               │   └── ReportesFinancierosService.class
│   │               ├── ui
│   │               │   └── views
│   │               │       ├── BalanceGeneralView.class
│   │               │       ├── BalanzaComprobacionView.class
│   │               │       ├── CatalogoCuentasView.class
│   │               │       ├── ConfiguracionView.class
│   │               │       ├── DashboardView.class
│   │               │       ├── EstadoResultadosView.class
│   │               │       ├── KardexView.class
│   │               │       ├── LibroDiarioView.class
│   │               │       ├── LibroDiarioView$1.class
│   │               │       ├── LibroMayorView.class
│   │               │       └── MainLayoutView.class
│   │               ├── Launcher.class
│   │               └── MainApp.class
│   ├── generated-sources
│   │   └── annotations
│   ├── generated-test-sources
│   │   └── test-annotations
│   ├── maven-archiver
│   │   └── pom.properties
│   ├── maven-status
│   │   └── maven-compiler-plugin
│   │       ├── compile
│   │       │   └── default-compile
│   │       │       ├── createdFiles.lst
│   │       │       └── inputFiles.lst
│   │       └── testCompile
│   │           └── default-testCompile
│   │               ├── createdFiles.lst
│   │               └── inputFiles.lst
│   ├── surefire-reports
│   │   ├── com.mycompany.programa_contable.EstadosFinancierosTest.txt
│   │   ├── com.mycompany.programa_contable.PartidaDobleTest.txt
│   │   ├── TEST-com.mycompany.programa_contable.EstadosFinancierosTest.xml
│   │   └── TEST-com.mycompany.programa_contable.PartidaDobleTest.xml
│   ├── test-classes
│   │   └── com
│   │       └── mycompany
│   │           └── programa_contable
│   │               ├── EstadosFinancierosTest.class
│   │               └── PartidaDobleTest.class
│   └── PROGRAMA_CONTABLE-1.0-SNAPSHOT.jar
├── .gitignore
├── data_sqlserver.sql
├── data.sql
├── icono.ico
├── icono.png
├── INSTALADOR-ContaNoPortable-1.0.0.msi
├── Logo grande.png
├── MANUAL_ContaNoContable.docx
├── MANUAL_ContaNoContable.pdf
├── MSI_WINDOWS.md
├── nbactions.xml
├── package-windows.ps1
├── pom.xml
├── README.md
├── schema_sqlserver.sql
└── schema.sql

```

## Créditos/Equipo de desarrollo

Proyecto desarrollado por **Kevin Salazar** y **Javier Martinez**
