# Crear el instalador MSI de Windows

El paquete incluye su propio runtime de Java y usa SQLite local. La PC donde se
instala no necesita Java ni SQL Server.

## Requisitos para construirlo

- Windows x64.
- JDK 21 x64, con `JAVA_HOME` apuntando a su carpeta.
- Maven 3.8 o posterior, disponible en `PATH` o mediante `MAVEN_HOME`.
- WiX Toolset disponible en `PATH` (`candle.exe` y `light.exe`).
- Acceso a Maven Central la primera vez para descargar dependencias.

## Construcción

Desde PowerShell, en la carpeta del proyecto:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
.\package-windows.ps1
```

El MSI queda en una carpeta con fecha dentro de `target`, por ejemplo
`target/installer-20260924-120000/ContaNoPortable-1.0.0.msi`.

El instalador crea accesos directos en el menú Inicio y en el escritorio. La
base de datos se guarda en `%LOCALAPPDATA%\ContaNoPortable\contabilidad.db`,
fuera de la carpeta de instalación, para que las actualizaciones no la
reemplacen.

El script omite la ejecución de las pruebas unitarias durante el empaquetado,
pero Maven sí compila sus fuentes. Para actualizar una instalación publicada,
incrementa la versión MSI y conserva el mismo `--win-upgrade-uuid` de
`package-windows.ps1`.
