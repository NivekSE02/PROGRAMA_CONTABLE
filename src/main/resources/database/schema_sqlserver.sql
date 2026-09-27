-- =====================================================================
-- MODULO CONTABLE: ESQUEMA DDL PARA MICROSOFT SQL SERVER (schema_sqlserver.sql)
-- Base de Datos: Sistema_Contable
-- =====================================================================

IF OBJECT_ID('detalle_asiento', 'U') IS NOT NULL DROP TABLE detalle_asiento;
IF OBJECT_ID('kardex', 'U') IS NOT NULL DROP TABLE kardex;
IF OBJECT_ID('asientos', 'U') IS NOT NULL DROP TABLE asientos;
IF OBJECT_ID('productos', 'U') IS NOT NULL DROP TABLE productos;
IF OBJECT_ID('cuentas', 'U') IS NOT NULL DROP TABLE cuentas;
IF OBJECT_ID('configuracion', 'U') IS NOT NULL DROP TABLE configuracion;

-- 1. Tabla del Catálogo de Cuentas
CREATE TABLE cuentas (
    codigo             NVARCHAR(50)  PRIMARY KEY,
    nombre             NVARCHAR(255) NOT NULL,
    tipo               NVARCHAR(50)  NOT NULL,
    subtipo            NVARCHAR(50),
    nivel              INT           NOT NULL DEFAULT 3,
    naturaleza         NVARCHAR(20)  NOT NULL CHECK(naturaleza IN ('DEUDORA', 'ACREEDORA')),
    cuenta_padre       NVARCHAR(50),
    permite_movimiento INT           NOT NULL DEFAULT 1 CHECK(permite_movimiento IN (0, 1))
);

-- 2. Tabla de Productos (Parámetros y Costos Dinámicos)
CREATE TABLE productos (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    nombre       NVARCHAR(150) NOT NULL,
    costo_compra DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    precio_venta DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    stock_actual INT           NOT NULL DEFAULT 0
);

-- 3. Tabla del Libro Diario (Encabezados de Asientos)
CREATE TABLE asientos (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    numero      INT          NOT NULL UNIQUE,
    fecha       NVARCHAR(20) NOT NULL,
    concepto    NVARCHAR(500) NOT NULL,
    total_debe  DECIMAL(18,2) NOT NULL,
    total_haber DECIMAL(18,2) NOT NULL,
    created_at  DATETIME2    DEFAULT GETDATE()
);

-- 4. Tabla de Detalle del Asiento (Partidas Contables)
CREATE TABLE detalle_asiento (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    asiento_id     INT           NOT NULL FOREIGN KEY REFERENCES asientos(id) ON DELETE CASCADE,
    renglon        INT           NOT NULL,
    cuenta_codigo  NVARCHAR(50)  NOT NULL FOREIGN KEY REFERENCES cuentas(codigo),
    concepto_linea NVARCHAR(500),
    debe           DECIMAL(18,2) NOT NULL DEFAULT 0.0 CHECK(debe >= 0),
    haber          DECIMAL(18,2) NOT NULL DEFAULT 0.0 CHECK(haber >= 0)
);

-- 5. Tabla del Kárdex (Control de Inventario Físico)
CREATE TABLE kardex (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    producto_id     INT          NOT NULL FOREIGN KEY REFERENCES productos(id),
    fecha           NVARCHAR(20) NOT NULL,
    tipo_movimiento NVARCHAR(20) NOT NULL CHECK(tipo_movimiento IN ('ENTRADA', 'SALIDA')),
    cantidad        INT          NOT NULL CHECK(cantidad > 0),
    costo_unitario  DECIMAL(18,2) NOT NULL,
    costo_total     DECIMAL(18,2) NOT NULL,
    saldo_cantidad  INT          NOT NULL,
    saldo_valor     DECIMAL(18,2) NOT NULL,
    asiento_id      INT          FOREIGN KEY REFERENCES asientos(id) ON DELETE SET NULL,
    created_at      DATETIME2    DEFAULT GETDATE()
);

-- 6. Tabla de Configuración del Sistema
CREATE TABLE configuracion (
    clave NVARCHAR(100) PRIMARY KEY,
    valor NVARCHAR(500) NOT NULL
);

-- Índices
CREATE INDEX idx_detalle_cuenta   ON detalle_asiento(cuenta_codigo);
CREATE INDEX idx_detalle_asiento  ON detalle_asiento(asiento_id);
CREATE INDEX idx_asiento_fecha    ON asientos(fecha);
CREATE INDEX idx_kardex_producto  ON kardex(producto_id);