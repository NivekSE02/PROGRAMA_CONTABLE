# UNIVERSIDAD CATÓLICA DE EL SALVADOR (UNICAES)
## FACULTAD DE CIENCIAS EMPRESARIALES / INGENIERÍA Y ARQUITECTURA
### Actividad III del III Período: Módulo de Contabilidad Automatizado (25% + 5% Parcial)

---

## 📌 Descripción del Proyecto

El **Sistema Contable Automatizado** es una solución integral desarrollada en **Java 21 LTS** con interfaz moderna en **JavaFX**, diseñada para automatizar de extremo a extremo el ciclo contable empresarial, dando estricto cumplimiento a los requerimientos normativos y académicos de la Universidad Católica de El Salvador:

1. **Libro Diario (Registro de Asientos con Validación Obligatoria de Partida Doble)**:
   - Registro de transacciones con Fecha, N° correlativo, Cuenta contable, Concepto/Glosa, Debe y Haber.
   - **Validación Obligatoria en Tiempo Real**: El sistema bloquea el guardado de forma estricta si el asiento no cumple con el principio universal de la **Partida Doble** ($\sum \text{Debe} = \sum \text{Haber} \neq 0$). Muestra contadores de sumas, diferencia numérica y un badge visual dinámico (verde cuando cuadra / rojo cuando descuadra).
2. **Mayorización Automática en Tiempo Real (Libro Mayor)**:
   - Consolidación instantánea de débitos y créditos para actualizar el saldo (**Deudor / Acreedor**) de cada cuenta del catálogo al registrar cualquier asiento, sin intervención ni cálculos manuales.
   - Historial detallado de movimientos cronológicos por cuenta y visualizador interactivo de **Esquemas de Mayor (Cuentas "T")**.
3. **Estados Financieros Dinámicos (Clasificación Automática por Dígito)**:
   - **Balance General**: Clasificación por dígitos:
     $$\text{Código 1 (Activo)} = \text{Código 2 (Pasivo)} + \text{Código 3 (Capital Contable / Patrimonio)}$$
     Comprueba y certifica matemáticamente el cuadre de la Ecuación Patrimonial Fundamental.
   - **Estado de Resultados**: Clasificación por dígitos:
     $$\text{Código 5 (Ingresos)} - \text{Código 4 (Costos y Gastos)} = \text{Utilidad / Pérdida Neta del Ejercicio}$$
     Incorpora automáticamente la Utilidad Neta generada dentro del Capital Contable del Balance General.
4. **Elementos Adicionales y Complementarios**:
   - **Balanza de Comprobación de 4 Columnas**: Movimientos (Debe/Haber) y Saldos (Deudor/Acreedor) con validación de sumas iguales.
   - **Dashboard Ejecutivo**: Tarjetas KPI con Activo Total, Pasivo Total, Patrimonio, Utilidad Neta, Ratio de Liquidez Corriente (`Activo Cte / Pasivo Cte`), Margen Neto y Gráficos comparativos de rendimiento y composición de activos.
   - **Mantenimiento del Catálogo de Cuentas**: Catálogo estándar salvadoreño (NIIF para PYMES) con árbol jerárquico, buscador en vivo y asignación automática por primer dígito.
   - **Exportación e Impresión Profesional**: Generación de reportes formales en formato HTML imprimible para auditoría con membrete institucional y firmas autorizadas (Contador General, Auditor Externo, Representante Legal), además de exportación a CSV para Excel.
   - **Persistencia SQL y Portabilidad**: Base de datos SQLite embebida de inicialización automática con scripts estandarizados `schema.sql` (DDL) y `data.sql` (DML).
   - **Control de Acceso y Tabla de Roles**: Pantalla de Login con perfiles de Administrador, Contador y Auditor.

---

## 💻 Requisitos del Sistema

- **Sistema Operativo**: Windows 10/11, macOS o Linux.
- **Java Development Kit (JDK)**: Java 21 LTS instalado (o superior).
- **Entorno de Desarrollo (IDE)**: Apache NetBeans 19, 20, 21, 28 o cualquier IDE compatible con Maven (IntelliJ IDEA, Eclipse, VS Code).
- **Gestor de Construcción**: Apache Maven 3.8+ (incluido por defecto en NetBeans).

---

## 🚀 Manual de Instalación y Ejecución

### Opción 1: Ejecución desde Apache NetBeans (Recomendada para la Defensa)
1. Abra **Apache NetBeans**.
2. Vaya al menú superior: **File** $\rightarrow$ **Open Project...** (`Ctrl + Shift + O`).
3. Navegue hasta la carpeta del proyecto:
   ```
   c:\Users\nivek\Documents\NetBeansProjects\PROGRAMA_CONTABLE
   ```
   y haga clic en **Open Project**.
4. Espere unos segundos a que NetBeans cargue las dependencias del `pom.xml`.
5. Haga clic derecho sobre el proyecto en el explorador y seleccione **Run** (o presione la tecla rápida **`F6`**).
6. El sistema inicializará automáticamente la base de datos `contabilidad.db` cargando el esquema `schema.sql` y el catálogo `data.sql`, abriendo la ventana principal.

### Opción 2: Ejecución desde la Consola / Terminal (PowerShell o CMD)
1. Abra una terminal en la raíz del proyecto:
   ```powershell
   cd c:\Users\nivek\Documents\NetBeansProjects\PROGRAMA_CONTABLE
   ```
2. Ejecute la aplicación con el plugin de JavaFX:
   ```powershell
   & "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" javafx:run
   ```
   *(o simplemente `mvn javafx:run` si tiene Maven en su variable de entorno PATH)*.

---

## 👥 Tabla de Usuarios, Accesos y Roles

El sistema cuenta con autenticación segura y restricción por roles de acuerdo con las mejores prácticas de control interno:

| Usuario | Contraseña | Nombre Completo | Rol | Nivel de Acceso y Permisos |
| :--- | :--- | :--- | :--- | :--- |
| **`admin`** | `admin123` | Lic. Kevin Administrador | **ADMINISTRADOR** | Acceso total: Configuración del catálogo de cuentas, registro de asientos, eliminación/anulación, generación de todos los reportes y reinicio de datos de demostración. |
| **`contador`** | `conta123` | Licda. María Contadora | **CONTADOR** | Operativo contable: Registro de asientos en el Libro Diario, consulta de la mayorización, balanza de comprobación y estados financieros. |
| **`auditor`** | `audit123` | Lic. Carlos Auditor | **AUDITOR** | Auditoría y control: Solo lectura de todos los libros, revisión analítica de cuentas T, verificación de estados financieros y exportación formal con firmas. |

> **Nota para la presentación ante el profesor**: La pantalla de inicio de sesión cuenta con **botones de acceso rápido** (`👤 Administrador`, `📘 Contador`, `🔍 Auditor`) para alternar perfiles en un solo clic durante la defensa sin perder tiempo tecleando credenciales.

---

## 📖 Manual de Usuario Paso a Paso (Ciclo Contable)

### 1. Inicio de Sesión
- Ingrese con las credenciales deseadas o presione un botón de acceso rápido.
- Al acceder, se muestra la barra superior con el usuario activo, su rol y la barra de navegación lateral.

### 2. Dashboard General (Métricas Ejecutivas)
- Visualice los 6 indicadores clave en tarjetas dinámicas: Activo Total, Pasivo Total, Capital Contable, Utilidad Neta, Ratio de Solvencia/Liquidez y Margen de Utilidad.
- Gráfico de barras comparativo: Ingresos (5) vs Costos y Gastos (4) vs Utilidad Neta.
- Gráfico de pastel: Composición de Activo Corriente vs Activo No Corriente.
- Tabla con los últimos asientos registrados en el sistema.

### 3. Libro Diario (Registro de Asientos y Validación de Partida Doble)
- En la pestaña **📝 Registrar Nuevo Asiento**:
  1. La fecha y el número correlativo se asignan automáticamente.
  2. Ingrese el concepto o glosa de la transacción (ej. *Pago de sueldos con cheque*).
  3. Seleccione una cuenta del catálogo desplegable (solo muestra cuentas que permiten movimiento).
  4. Ingrese el monto en el campo **Debe** o en el campo **Haber** y presione **➕ Agregar Renglón**.
  5. **Comprobación de Partida Doble**:
     - Observe el panel inferior: el sistema suma en tiempo real los débitos y créditos y calcula la diferencia.
     - Si hay descuadre: se muestra el badge rojo `⚠ DESCUADRADO (Diferencia: $X.XX)` y el botón **💾 Guardar Asiento** se encuentra **estrictamente bloqueado y deshabilitado**.
     - Cuando la suma del Debe es exactamente igual a la del Haber: el badge cambia a verde `✔ PARTIDA DOBLE CUADRADA - LISTO PARA GUARDAR` y el botón de guardado se habilita.
  6. Presione **Guardar Asiento**. El sistema ejecuta una transacción ACID en SQLite y actualiza en milisegundos todo el Libro Mayor y los Estados Financieros.
- En la pestaña **📚 Historial del Libro Diario**:
  - Consulte todos los asientos asentados cronológicamente.
  - Al hacer clic en un asiento, se despliega abajo el desglose exacto de sus renglones.
  - Botón **📊 Exportar Libro Diario a CSV** para abrirlo en Microsoft Excel.

### 4. Libro Mayor (Mayorización Automática & Cuentas T)
- **Pestaña Consolidación**: Muestra en tiempo real cada cuenta con su movimiento al Debe, Haber, y su saldo final clasificado automáticamente en **Saldo Deudor** (verde) o **Saldo Acreedor** (azul) según su naturaleza contable.
- Al hacer clic en una cuenta, se visualiza su **Kardex / Movimientos individuales** con el saldo parcial acumulado tras cada asiento.
- **Pestaña Esquemas de Mayor (Cuentas "T")**: Presenta visualmente las tarjetas en forma de cuenta T con sus débitos a la izquierda, créditos a la derecha, líneas de suma y el saldo final doblemente subrayado.

### 5. Balanza de Comprobación de Sumas y Saldos
- Tabla de 4 columnas contables:
  - Movimientos Debe y Movimientos Haber.
  - Saldo Deudor y Saldo Acreedor.
- Fila inferior con las sumas totales y la certificación `✔ BALANZA CUADRADA EXACTAMENTE`.
- Botón para exportar a archivo CSV.

### 6. Balance General Automático
- Generación dinámica aplicando la clasificación:
  $$\text{Código 1 (Activo)} = \text{Código 2 (Pasivo)} + \text{Código 3 (Capital Contable)}$$
- Separa claramente:
  - **Activo Corriente y No Corriente**.
  - **Pasivo Corriente y No Corriente**.
  - **Capital Social y Reservas**.
  - **Utilidad Neta del Ejercicio** (vinculada automáticamente desde el Estado de Resultados).
- Certificación visual del balance cuadrado y botón **📄 Exportar Reporte Formal e Imprimir** que genera un documento HTML profesional con formato de auditoría y espacios para firmas.

### 7. Estado de Resultados Automático
- Generación dinámica aplicando la clasificación:
  $$\text{Código 5 (Ingresos)} - \text{Código 4 (Costos y Gastos)} = \text{Utilidad Neta}$$
- Presenta en cascada:
  - 5. Ingresos de Operación (Ventas netas)
  - (-) 41. Costo de Ventas
  - (=) Utilidad Bruta
  - (-) 42. Gastos de Administración
  - (-) 43. Gastos de Venta
  - (=) Utilidad de Operación
  - (=) Utilidad Neta del Ejercicio con indicador de rentabilidad.
- Botón de exportación formal para impresión o guardado como PDF.

### 8. Catálogo de Cuentas
- Buscador reactivo por código o nombre.
- Formulario para registrar nuevas cuentas con cálculo automático de su tipo y naturaleza al ingresar el primer dígito del código contable.

---

## 📂 Estructura del Código Fuente

```text
PROGRAMA_CONTABLE/
├── pom.xml                               # Dependencias (JavaFX 21, SQLite JDBC, JUnit 5)
├── schema.sql                            # Script DDL de Base de Datos (Tablas, llaves y restricciones)
├── data.sql                              # Script DML de Semillas (Catálogo salvadoreño y asientos de ejemplo)
├── contabilidad.db                       # Base de datos SQLite generada automáticamente
├── README.md                             # Documentación completa y manual de usuario
└── src/
    ├── main/
    │   ├── java/com/mycompany/programa_contable/
    │   │   ├── MainApp.java              # Clase principal JavaFX
    │   │   ├── PROGRAMA_CONTABLE.java    # Punto de entrada compatible con NetBeans F6
    │   │   ├── db/
    │   │   │   └── DatabaseManager.java  # Gestor de persistencia SQLite y cargador de SQL
    │   │   ├── model/
    │   │   │   ├── Rol.java              # Roles: Administrador, Contador, Auditor
    │   │   │   ├── Usuario.java          # Entidad de usuario
    │   │   │   ├── TipoCuenta.java       # Clasificación por dígito (1, 2, 3, 4, 5)
    │   │   │   ├── NaturalezaCuenta.java # Deudora / Acreedora
    │   │   │   ├── Cuenta.java           # Catálogo contable
    │   │   │   ├── Asiento.java          # Encabezado con validación de partida doble
    │   │   │   ├── DetalleAsiento.java   # Partidas contables individuales
    │   │   │   ├── MayorCuenta.java      # Consolidación y saldos del mayor
    │   │   │   ├── MovimientoMayor.java  # Renglón cronológico del kardex
    │   │   │   ├── BalanzaComprobacionDTO.java # DTO de sumas y saldos
    │   │   │   ├── EstadoResultadosDTO.java    # DTO de código 5 - 4 = Utilidad
    │   │   │   └── BalanceGeneralDTO.java      # DTO de código 1 = 2 + 3
    │   │   ├── dao/
    │   │   │   ├── UsuarioDAO.java       # Autenticación y consulta de usuarios
    │   │   │   ├── CuentaDAO.java        # Búsqueda y mantenimiento del catálogo
    │   │   │   └── LibroDiarioDAO.java   # Transacciones ACID y persistencia de asientos
    │   │   ├── service/
    │   │   │   ├── MayorizacionService.java    # Mayorización automática en tiempo real
    │   │   │   ├── ReportesFinancierosService.java # Lógica de estados financieros
    │   │   │   ├── ExportacionService.java     # Generación de HTML imprimible y CSV
    │   │   │   └── SessionManager.java         # Control de sesión activa y permisos
    │   │   └── ui/views/
    │   │       ├── LoginView.java        # Ventana de acceso y accesos rápidos
    │   │       ├── MainLayoutView.java   # Contenedor con Topbar y Sidebar
    │   │       ├── DashboardView.java    # Métricas ejecutivas y gráficos
    │   │       ├── LibroDiarioView.java  # Formulario con validación y bloqueo
    │   │       ├── LibroMayorView.java   # Mayorización y Cuentas T interactivas
    │   │       ├── BalanzaComprobacionView.java # Balanza de 4 columnas
    │   │       ├── BalanceGeneralView.java      # Balance General dinámico
    │   │       ├── EstadoResultadosView.java    # Estado de Resultados dinámico
    │   │       └── CatalogoCuentasView.java     # Gestión del catálogo
    │   └── resources/
    │       └── com/mycompany/programa_contable/css/
    │           └── styles.css            # Estilos CSS corporativos premium
    └── test/
        └── java/com/mycompany/programa_contable/
            ├── PartidaDobleTest.java     # Pruebas unitarias de cuadre de partida doble
            └── EstadosFinancierosTest.java # Pruebas unitarias de mayorización y estados
```

---

## 🎯 Guía para la Presentación de 5 Minutos (Evaluación en Clases)

Para optimizar al máximo los 5 minutos de defensa ante el docente:

1. **Minuto 1: Inicio y Dashboard General (30 seg)**
   - Inicie la aplicación en NetBeans (`F6`).
   - Use el botón rápido `👤 Administrador (admin)`.
   - Muestre el **Dashboard**: destaque los KPIs en tiempo real (Activo, Pasivo, Capital y Utilidad) y los gráficos de barra y pie.
2. **Minuto 2: Libro Diario y Demostración del Bloqueo por Partida Doble (1 min 30 seg)**
   - Vaya a la pestaña **Libro Diario**.
   - Agregue una línea con Debe: `$100.00` y otra con Haber: `$80.00` (descuadrado por `$20.00`).
   - **Muestre al profesor cómo el sistema marca el badge rojo `DESCUADRADO` y mantiene estrictamente BLOQUEADO el botón de Guardar**.
   - Corrija el Haber a `$100.00`: muestre cómo el badge cambia a verde `PARTIDA DOBLE CUADRADA` y el botón se habilita.
   - Presione Guardar y confirme que el asiento fue registrado exitosamente.
3. **Minuto 3: Mayorización en Tiempo Real y Esquemas de Mayor (Cuentas T) (1 min)**
   - Vaya a la pestaña **Libro Mayor**.
   - Muestre cómo el saldo de las cuentas se actualizó inmediatamente sin necesidad de recalcular.
   - Cambie a la pestaña **Esquemas de Mayor (Cuentas T)** para mostrar la representación gráfica que a los docentes les encanta revisar.
4. **Minuto 4: Estados Financieros Automáticos (1 min)**
   - Muestre el **Estado de Resultados**: verifique la fórmula:
     $$\text{Ingresos (5)} - \text{Costos/Gastos (4)} = \text{Utilidad}$$
   - Muestre el **Balance General**: verifique el cuadre automático:
     $$\text{Activo (1)} = \text{Pasivo (2)} + \text{Capital (3)}$$
   - Haga clic en **Exportar Reporte Formal e Imprimir** para mostrar la hoja con formato formal de auditoría y firmas.
5. **Minuto 5: Aspectos Complementarios y Cierre (30 seg)**
   - Muestre la **Balanza de Comprobación** cuadrada a 4 columnas.
   - Muestre el botón **🔄 Datos de Demostración** en el menú superior para restablecer los datos en cualquier momento.
   - Concluya demostrando que el código fuente compila con cero advertencias y cuenta con pruebas unitarias en verde.

---
*Desarrollado para la Universidad Católica de El Salvador (UNICAES) - 2026.*
