-- =====================================================================
-- MODULO CONTABLE: ESQUEMA DE BASE DE DATOS SQLITE (schema.sql)
-- Motor: SQLite (distribución portable sin servidor)
-- =====================================================================

PRAGMA foreign_keys = ON;

-- Eliminar tablas en orden correcto (dependencias primero)
DROP TABLE IF EXISTS detalle_asiento;
DROP TABLE IF EXISTS kardex;
DROP TABLE IF EXISTS asientos;
DROP TABLE IF EXISTS productos;
DROP TABLE IF EXISTS cuentas;
DROP TABLE IF EXISTS configuracion;

-- =========================================================
-- 1. Tabla del Catálogo de Cuentas
-- =========================================================
CREATE TABLE cuentas (
    codigo             TEXT    PRIMARY KEY,
    nombre             TEXT    NOT NULL,
    tipo               TEXT    NOT NULL,
    subtipo            TEXT,
    nivel              INTEGER NOT NULL DEFAULT 3,
    naturaleza         TEXT    NOT NULL CHECK(naturaleza IN ('DEUDORA', 'ACREEDORA')),
    cuenta_padre       TEXT,
    permite_movimiento INTEGER NOT NULL DEFAULT 1 CHECK(permite_movimiento IN (0, 1))
);

-- =========================================================
-- 2. Tabla de Productos (Parámetros y Costos Dinámicos)
-- =========================================================
CREATE TABLE productos (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre       TEXT    NOT NULL,
    costo_compra REAL    NOT NULL DEFAULT 0.00,
    precio_venta REAL    NOT NULL DEFAULT 0.00,
    stock_actual INTEGER NOT NULL DEFAULT 0
);

-- =========================================================
-- 3. Tabla del Libro Diario (Encabezado del Asiento)
-- =========================================================
CREATE TABLE asientos (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    numero      INTEGER NOT NULL UNIQUE,
    fecha       TEXT    NOT NULL,
    concepto    TEXT    NOT NULL,
    total_debe  REAL    NOT NULL,
    total_haber REAL    NOT NULL,
    created_at  TEXT    DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- 4. Tabla de Detalle del Asiento (Partidas Contables)
-- =========================================================
CREATE TABLE detalle_asiento (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    asiento_id     INTEGER NOT NULL,
    renglon        INTEGER NOT NULL,
    cuenta_codigo  TEXT    NOT NULL,
    concepto_linea TEXT,
    debe           REAL    NOT NULL DEFAULT 0.0 CHECK(debe >= 0),
    haber          REAL    NOT NULL DEFAULT 0.0 CHECK(haber >= 0),
    FOREIGN KEY (asiento_id)    REFERENCES asientos(id) ON DELETE CASCADE,
    FOREIGN KEY (cuenta_codigo) REFERENCES cuentas(codigo)
);

-- =========================================================
-- 5. Tabla del Kárdex (Control de Inventario Físico)
-- =========================================================
CREATE TABLE kardex (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    producto_id      INTEGER NOT NULL,
    fecha            TEXT    NOT NULL,
    tipo_movimiento  TEXT    NOT NULL CHECK(tipo_movimiento IN ('ENTRADA', 'SALIDA')),
    cantidad         INTEGER NOT NULL CHECK(cantidad > 0),
    costo_unitario   REAL    NOT NULL,
    costo_total      REAL    NOT NULL,
    saldo_cantidad   INTEGER NOT NULL,
    saldo_valor      REAL    NOT NULL,
    asiento_id       INTEGER,
    created_at       TEXT    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    FOREIGN KEY (asiento_id)  REFERENCES asientos(id) ON DELETE SET NULL
);

-- =========================================================
-- 6. Tabla de Configuración del Sistema
-- =========================================================
CREATE TABLE configuracion (
    clave TEXT PRIMARY KEY,
    valor TEXT NOT NULL
);

-- =========================================================
-- Índices
-- =========================================================
CREATE INDEX idx_detalle_cuenta   ON detalle_asiento(cuenta_codigo);
CREATE INDEX idx_detalle_asiento  ON detalle_asiento(asiento_id);
CREATE INDEX idx_asiento_fecha    ON asientos(fecha);
CREATE INDEX idx_kardex_producto  ON kardex(producto_id);
