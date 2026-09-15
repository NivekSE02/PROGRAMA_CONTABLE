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
    tipo TEXT NOT NULL CHECK(tipo IN ('ACTIVO', 'PASIVO', 'CAPITAL', 'COSTO_GASTO', 'INGRESO', 'CIERRE', 'ORDEN')),
    subtipo TEXT,
    nivel INTEGER NOT NULL DEFAULT 3,
    naturaleza TEXT NOT NULL CHECK(naturaleza IN ('DEUDORA', 'ACREEDORA')),
    cuenta_padre TEXT,
    permite_movimiento INTEGER NOT NULL DEFAULT 1 CHECK(permite_movimiento IN (0, 1))
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
-- Minimal chart of accounts (no test data)
INSERT INTO cuentas (codigo, nombre, tipo, subtipo, nivel, naturaleza, cuenta_padre, permite_movimiento) VALUES
('1', 'Activo', 'ACTIVO', NULL, 1, 'DEUDORA', NULL, 0),
('1.1', 'Caja', 'ACTIVO', NULL, 2, 'DEUDORA', '1', 1),
('1.2', 'Bancos', 'ACTIVO', NULL, 2, 'DEUDORA', '1', 1),
('1.3', 'Cuentas por Cobrar', 'ACTIVO', NULL, 2, 'DEUDORA', '1', 0),
('1.3.1', 'Clientes', 'ACTIVO', NULL, 3, 'DEUDORA', '1.3', 1),
('1.4', 'Inventario', 'ACTIVO', NULL, 2, 'DEUDORA', '1', 1),
('1.5', 'IVA Crédito Fiscal', 'ACTIVO', NULL, 2, 'DEUDORA', '1', 1),
('1.6', 'Gastos Pagados por Anticipado', 'ACTIVO', NULL, 2, 'DEUDORA', '1', 0),
('1.6.1', 'Alquiler', 'ACTIVO', NULL, 3, 'DEUDORA', '1.6', 1),
('1.6.2', 'Papelería y Útiles', 'ACTIVO', NULL, 3, 'DEUDORA', '1.6', 1),
('1.7', 'Propiedad, Planta y Equipo', 'ACTIVO', NULL, 2, 'DEUDORA', '1', 0),
('1.7.1', 'Mobiliaria y Equipo de Oficina', 'ACTIVO', NULL, 3, 'DEUDORA', '1.7', 1),
('1.7.2', 'Edificio', 'ACTIVO', NULL, 3, 'DEUDORA', '1.7', 1),
('1.7.3', 'Equipo de Transporte', 'ACTIVO', NULL, 3, 'DEUDORA', '1.7', 1),
('1.7.4', 'Equipo de Cómputo', 'ACTIVO', NULL, 3, 'DEUDORA', '1.7', 1),
('2', 'Pasivo', 'PASIVO', NULL, 1, 'ACREEDORA', NULL, 0),
('2.1', 'Cuentas por Pagar', 'PASIVO', NULL, 2, 'ACREEDORA', '2', 0),
('2.1.1', 'Proveedores', 'PASIVO', NULL, 3, 'ACREEDORA', '2.1', 1),
('2.1.2', 'Acreedores Varios', 'PASIVO', NULL, 3, 'ACREEDORA', '2.1', 1),
('2.1.3', 'Préstamo Bancario', 'PASIVO', NULL, 3, 'ACREEDORA', '2.1', 1),
('2.2', 'IVA Débito Fiscal', 'PASIVO', NULL, 2, 'ACREEDORA', '2', 1),
('3', 'Patrimonio', 'PATRIMONIO', NULL, 1, 'ACREEDORA', NULL, 0),
('3.1', 'Capital Social', 'PATRIMONIO', NULL, 2, 'ACREEDORA', '3', 1),
('4', 'Ingresos', 'INGRESOS', NULL, 1, 'ACREEDORA', NULL, 0),
('4.1', 'Ventas', 'INGRESOS', NULL, 2, 'ACREEDORA', '4', 1),
('5', 'Costos / Compras', 'GASTOS', NULL, 1, 'DEUDORA', NULL, 0),
('5.1', 'Compras', 'GASTOS', NULL, 2, 'DEUDORA', '5', 1),
('6', 'Gastos', 'GASTOS', NULL, 1, 'DEUDORA', NULL, 0),
('6.1', 'Gastos Financieros', 'GASTOS', NULL, 2, 'DEUDORA', '6', 1),
('6.2', 'Comisiones', 'GASTOS', NULL, 2, 'DEUDORA', '6', 1),
('6.3', 'Gastos Administrativos', 'GASTOS', NULL, 2, 'DEUDORA', '6', 1);