-- =====================================================================
-- UNIVERSIDAD CATÓLICA DE EL SALVADOR (UNICAES)
-- MODULO CONTABLE: ESQUEMA DE BASE DE DATOS (schema.sql)
-- =====================================================================

PRAGMA foreign_keys = ON;

-- 1. Tabla de Usuarios y Roles
DROP TABLE IF EXISTS detalle_asiento;
DROP TABLE IF EXISTS asientos;
DROP TABLE IF EXISTS cuentas;
DROP TABLE IF EXISTS usuarios;

CREATE TABLE usuarios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    nombre_completo TEXT NOT NULL,
    rol TEXT NOT NULL CHECK(rol IN ('ADMINISTRADOR', 'CONTADOR', 'AUDITOR')),
    estado TEXT NOT NULL DEFAULT 'ACTIVO'
);

-- 2. Tabla del Catálogo de Cuentas
-- Clasificación por Dígitos:
-- 1 = ACTIVO (Deudora)
-- 2 = PASIVO (Acreedora)
-- 3 = CAPITAL CONTABLE / PATRIMONIO (Acreedora)
-- 4 = COSTOS Y GASTOS (Deudora)
-- 5 = INGRESOS (Acreedora)
CREATE TABLE cuentas (
    codigo TEXT PRIMARY KEY,
    nombre TEXT NOT NULL,
    tipo TEXT NOT NULL CHECK(tipo IN ('ACTIVO', 'PASIVO', 'CAPITAL', 'COSTO_GASTO', 'INGRESO')),
    subtipo TEXT,
    nivel INTEGER NOT NULL DEFAULT 3,
    naturaleza TEXT NOT NULL CHECK(naturaleza IN ('DEUDORA', 'ACREEDORA')),
    cuenta_padre TEXT,
    permite_movimiento INTEGER NOT NULL DEFAULT 1 CHECK(permite_movimiento IN (0, 1)),
    FOREIGN KEY (cuenta_padre) REFERENCES cuentas(codigo)
);

-- 3. Tabla del Libro Diario (Encabezado del Asiento)
CREATE TABLE asientos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero INTEGER NOT NULL UNIQUE,
    fecha TEXT NOT NULL,
    concepto TEXT NOT NULL,
    total_debe REAL NOT NULL,
    total_haber REAL NOT NULL,
    usuario_id INTEGER,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- 4. Tabla de Detalle del Asiento (Partidas Contables)
CREATE TABLE detalle_asiento (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    asiento_id INTEGER NOT NULL,
    renglon INTEGER NOT NULL,
    cuenta_codigo TEXT NOT NULL,
    concepto_linea TEXT,
    debe REAL NOT NULL DEFAULT 0.0 CHECK(debe >= 0),
    haber REAL NOT NULL DEFAULT 0.0 CHECK(haber >= 0),
    FOREIGN KEY (asiento_id) REFERENCES asientos(id) ON DELETE CASCADE,
    FOREIGN KEY (cuenta_codigo) REFERENCES cuentas(codigo)
);

-- Índices para optimizar consultas de mayorización y reportes en tiempo real
CREATE INDEX idx_detalle_cuenta ON detalle_asiento(cuenta_codigo);
CREATE INDEX idx_detalle_asiento ON detalle_asiento(asiento_id);
CREATE INDEX idx_asiento_fecha ON asientos(fecha);
