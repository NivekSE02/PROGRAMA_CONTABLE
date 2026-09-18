-- =====================================================================
-- UNIVERSIDAD CATÓLICA DE EL SALVADOR (UNICAES)
-- MODULO CONTABLE: ESQUEMA DDL PARA MICROSOFT SQL SERVER (schema_sqlserver.sql)
-- Base de Datos: Sistema_Contable
-- =====================================================================

-- CREATE DATABASE Sistema_Contable;
-- USE Sistema_Contable
IF OBJECT_ID('detalle_asiento', 'U') IS NOT NULL DROP TABLE detalle_asiento;
IF OBJECT_ID('asientos', 'U') IS NOT NULL DROP TABLE asientos;
IF OBJECT_ID('cuentas', 'U') IS NOT NULL DROP TABLE cuentas;
IF OBJECT_ID('usuarios', 'U') IS NOT NULL DROP TABLE usuarios;
-- 1. Tabla de Usuarios y Roles
CREATE TABLE usuarios (
    id INT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) NOT NULL UNIQUE,
    password NVARCHAR(100) NOT NULL,
    nombre_completo NVARCHAR(150) NOT NULL,
    rol NVARCHAR(50) NOT NULL CHECK(rol IN ('ADMINISTRADOR', 'CONTADOR', 'AUDITOR')),
    estado NVARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
);
-- 2. Tabla del Catálogo de Cuentas Comercial UNICAES
CREATE TABLE cuentas (
    codigo NVARCHAR(50) PRIMARY KEY,
    nombre NVARCHAR(255) NOT NULL,
    tipo NVARCHAR(50) NOT NULL,
    subtipo NVARCHAR(50),
    nivel INT NOT NULL DEFAULT 3,
    naturaleza NVARCHAR(20) NOT NULL CHECK(naturaleza IN ('DEUDORA', 'ACREEDORA')),
    cuenta_padre NVARCHAR(50),
    permite_movimiento INT NOT NULL DEFAULT 1 CHECK(permite_movimiento IN (0, 1))
);
-- 3. Tabla del Libro Diario (Encabezados de Asientos)
CREATE TABLE asientos (
    id INT IDENTITY(1,1) PRIMARY KEY,
    numero INT NOT NULL UNIQUE,
    fecha NVARCHAR(20) NOT NULL,
    concepto NVARCHAR(500) NOT NULL,
    total_debe DECIMAL(18,2) NOT NULL,
    total_haber DECIMAL(18,2) NOT NULL,
    usuario_id INT FOREIGN KEY REFERENCES usuarios(id),
    created_at DATETIME2 DEFAULT GETDATE()
);
-- 4. Tabla de Detalle del Asiento (Partidas Contables)
CREATE TABLE detalle_asiento (
    id INT IDENTITY(1,1) PRIMARY KEY,
    asiento_id INT NOT NULL FOREIGN KEY REFERENCES asientos(id) ON DELETE CASCADE,
    renglon INT NOT NULL,
    cuenta_codigo NVARCHAR(50) NOT NULL FOREIGN KEY REFERENCES cuentas(codigo),
    concepto_linea NVARCHAR(500),
    debe DECIMAL(18,2) NOT NULL DEFAULT 0.0 CHECK(debe >= 0),
    haber DECIMAL(18,2) NOT NULL DEFAULT 0.0 CHECK(haber >= 0)
);
-- Índices de consulta rápida
CREATE INDEX idx_detalle_cuenta ON detalle_asiento(cuenta_codigo);
CREATE INDEX idx_detalle_asiento ON detalle_asiento(asiento_id);
CREATE INDEX idx_asiento_fecha ON asientos(fecha);
INSERT INTO usuarios
    (username, password, nombre_completo, rol, estado)
VALUES
    ('admin', 'admin123', 'Lic. Kevin Administrador', 'ADMINISTRADOR', 'ACTIVO'),
    ('admin2', 'admin321', 'Lic. Javier Administrador', 'ADMINISTRADOR', 'ACTIVO');