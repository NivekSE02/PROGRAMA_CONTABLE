-- =====================================================================
-- MODULO CONTABLE: DATOS INICIALES (data.sql)
-- Compatible con SQLite y SQL Server
-- Solo se ejecuta una vez cuando la base de datos estÃ¡ vacÃ­a.
-- =====================================================================


-- =========================================================
-- 1. CatÃ¡logo de Cuentas
-- =========================================================

INSERT INTO cuentas (codigo, nombre, tipo, subtipo, nivel, naturaleza, cuenta_padre, permite_movimiento)
VALUES
-- ============================================================
-- 1. ACTIVO
-- ============================================================
('1',     'Activo',                          'ACTIVO',      NULL,                       1, 'DEUDORA',   NULL,  0),
('1.1',   'Efectivo y equivalente',           'ACTIVO',      'ACTIVO CORRIENTE',         2, 'DEUDORA',   '1',   0),
('1.1.1', 'Caja',                             'ACTIVO',      'ACTIVO CORRIENTE',         3, 'DEUDORA',   '1.1', 1),
('1.1.2', 'Bancos',                           'ACTIVO',      'ACTIVO CORRIENTE',         3, 'DEUDORA',   '1.1', 1),
('1.2',   'Inventario',                       'ACTIVO',      'ACTIVO CORRIENTE',         2, 'DEUDORA',   '1',   1),
('1.3',   'Cuentas por cobrar',               'ACTIVO',      'ACTIVO CORRIENTE',         2, 'DEUDORA',   '1',   0),
('1.3.1', 'Clientes',                         'ACTIVO',      'ACTIVO CORRIENTE',         3, 'DEUDORA',   '1.3', 1),
('1.4',   'IVA Credito Fiscal',               'ACTIVO',      'ACTIVO CORRIENTE',         2, 'DEUDORA',   '1',   1),
('1.5',   'IVA remanente a favor',            'ACTIVO',      'ACTIVO CORRIENTE',         2, 'DEUDORA',   '1',   1),
('1.6',   'Propiedad, planta y equipo',       'ACTIVO',      'ACTIVO NO CORRIENTE',      2, 'DEUDORA',   '1',   0),
('1.6.1', 'Mobiliario y equipo de oficina',   'ACTIVO',      'ACTIVO NO CORRIENTE',      3, 'DEUDORA',   '1.6', 1),
('1.6.2', 'Equipo de computo',                'ACTIVO',      'ACTIVO NO CORRIENTE',      3, 'DEUDORA',   '1.6', 1),
('1.6.3', 'Equipo de transporte',             'ACTIVO',      'ACTIVO NO CORRIENTE',      3, 'DEUDORA',   '1.6', 1),
('1.7',   'Gastos pagados por anticipado',    'ACTIVO',      'ACTIVO',                   2, 'DEUDORA',   '1',   0),
('1.7.1', 'Alquiler',                         'ACTIVO',      'ACTIVO',                   3, 'DEUDORA',   '1.7', 1),

-- ============================================================
-- 2. PASIVO
-- ============================================================
('2',     'Pasivo',                           'PASIVO',      NULL,                                   1, 'ACREEDORA', NULL,  0),
('2.1',   'Cuentas por pagar',                'PASIVO',      'PASIVO CORRIENTE',                     2, 'ACREEDORA', '2',   0),
('2.1.1', 'Proveedores',                      'PASIVO',      'PASIVO CORRIENTE',                     3, 'ACREEDORA', '2.1', 1),
('2.1.2', 'Acreedores varios',                'PASIVO',      'PASIVO CORRIENTE',                     3, 'ACREEDORA', '2.1', 1),
('2.2',   'Prestamo bancario',                'PASIVO',      'PASIVO CORRIENTE / NO CORRIENTE',      2, 'ACREEDORA', '2',   1),
('2.3',   'IVA Debito Fiscal',                'PASIVO',      'PASIVO CORRIENTE',                     2, 'ACREEDORA', '2',   1),
('2.4',   'Impuesto a pagar IVA',             'PASIVO',      'PASIVO CORRIENTE',                     2, 'ACREEDORA', '2',   1),

-- ============================================================
-- 3. PATRIMONIO
-- ============================================================
('3',     'Patrimonio',                       'CAPITAL',     NULL,                       1, 'ACREEDORA', NULL,  0),
('3.1',   'Capital social',                   'CAPITAL',     'PATRIMONIO',               2, 'ACREEDORA', '3',   1),

-- ============================================================
-- 4. INGRESOS
-- ============================================================
('4',     'Ingresos',                         'INGRESO',     'RESULTADO',                1, 'ACREEDORA', NULL,  0),
('4.1',   'Ventas',                           'INGRESO',     'INGRESOS',                 2, 'ACREEDORA', '4',   1),
('4.2',   'Devoluciones sobre ventas',        'INGRESO',     'RESTA A INGRESOS',         2, 'DEUDORA',   '4',   1),

-- ============================================================
-- 5. COSTOS
-- ============================================================
('5',     'Costos',                           'COSTO_GASTO', 'RESULTADO',                1, 'DEUDORA',   NULL,  0),
('5.1',   'Devoluciones sobre compras',       'COSTO_GASTO', 'RESTA A COSTOS',           2, 'ACREEDORA', '5',   1),
('5.2',   'Costo de ventas',                  'COSTO_GASTO', 'COSTOS',                   2, 'DEUDORA',   '5',   1),
('5.3',   'Mercancia disponible',             'COSTO_GASTO', 'CUENTA TRANSITORIA',       2, 'DEUDORA',   '5',   1),
('5.4',   'Compras',                          'COSTO_GASTO', 'COSTOS',                   2, 'DEUDORA',   '5',   1),

-- ============================================================
-- 6. GASTOS
-- ============================================================
('6',     'Gastos',                           'COSTO_GASTO', 'RESULTADO',                1, 'DEUDORA',   NULL,  0),
('6.1',   'Gastos financieros',               'COSTO_GASTO', 'GASTOS',                   2, 'DEUDORA',   '6',   0),
('6.1.1', 'Comision',                         'COSTO_GASTO', 'GASTOS FINANCIEROS',       3, 'DEUDORA',   '6.1', 1),
('6.2',   'Gasto administrativo',             'COSTO_GASTO', 'GASTOS',                   2, 'DEUDORA',   '6',   1),
('6.3',   'Gasto de venta',                   'COSTO_GASTO', 'GASTOS',                   2, 'DEUDORA',   '6',   0),
('6.3.1', 'Papeleria y utiles',               'COSTO_GASTO', 'GASTOS DE VENTA',          3, 'DEUDORA',   '6.3', 1);

-- =========================================================
-- 2. Producto base para Kardex (id=1 requerido por LibroDiarioDAO)
-- =========================================================
INSERT INTO productos (nombre, costo_compra, precio_venta, stock_actual)
VALUES ('Producto Estandar', 8.85, 17.70, 0);
-- Script de inserciÃ³n de asientos de demostraciÃ³n
-- Base de datos: SQL Server (T-SQL)

DECLARE @AsientoID INT;
DECLARE @Num INT;
-- Variables para rastrear saldo acumulado del Kardex (metodo PEPS)
DECLARE @KSaldoCant INT = 0;
DECLARE @KSaldoVal DECIMAL(18,2) = 0.00;

-- Obtener el siguiente nÃºmero de asiento disponible para evitar violaciones de clave Ãºnica
SELECT @Num = ISNULL(MAX(numero), 0) + 1 FROM asientos;

-- Asiento 1
INSERT INTO asientos (numero, fecha, concepto, total_debe, total_haber) VALUES (@Num, '2026-01-01', 'c/Inicio de Operaciones', 36000.00, 36000.00);
SET @AsientoID = SCOPE_IDENTITY();
INSERT INTO detalle_asiento (asiento_id, cuenta_codigo, concepto_linea, debe, haber, renglon) VALUES 
(@AsientoID, '1.1.1', 'Caja', 30000.00, 0, 1),
(@AsientoID, '1.2', 'Inventario', 6000.00, 0, 2),
(@AsientoID, '3.1', 'Capital social', 0, 36000.00, 3);

-- Kardex: Entrada inventario inicial (6000 / 8.85 = 678 unidades)
SET @KSaldoCant = @KSaldoCant + 678;
SET @KSaldoVal  = @KSaldoVal  + 5999.30;
INSERT INTO kardex (producto_id, fecha, tipo_movimiento, cantidad, costo_unitario, costo_total, saldo_cantidad, saldo_valor, asiento_id)
VALUES (1, '2026-01-01', 'ENTRADA', 678, 8.85, 5999.30, @KSaldoCant, @KSaldoVal, @AsientoID);

SET @Num = @Num + 1;
-- Asiento 2
INSERT INTO asientos (numero, fecha, concepto, total_debe, total_haber) VALUES (@Num, '2026-01-03', 'C/apertura de cuenta en banco', 20000.00, 20000.00);
SET @AsientoID = SCOPE_IDENTITY();
INSERT INTO detalle_asiento (asiento_id, cuenta_codigo, concepto_linea, debe, haber, renglon) VALUES 
(@AsientoID, '1.1.2', 'Bancos', 20000.00, 0, 1),
(@AsientoID, '1.1.1', 'Caja', 0, 20000.00, 2);

SET @Num = @Num + 1;
-- Asiento 3
INSERT INTO asientos (numero, fecha, concepto, total_debe, total_haber) VALUES (@Num, '2026-01-05', 'C/Compra de productos de Invent', 10000.00, 10000.00);
SET @AsientoID = SCOPE_IDENTITY();
INSERT INTO detalle_asiento (asiento_id, cuenta_codigo, concepto_linea, debe, haber, renglon) VALUES 
(@AsientoID, '5.4', 'Compras', 8849.55, 0, 1),
(@AsientoID, '1.4', 'IVA Credito Fiscal', 1150.45, 0, 2),
(@AsientoID, '2.1.1', 'Proveedores', 0, 10000.00, 3);

-- Kardex: Entrada por compra (8849.55 / 8.85 = 1000 unidades)
SET @KSaldoCant = @KSaldoCant + 1000;
SET @KSaldoVal  = @KSaldoVal  + 8850.00;
INSERT INTO kardex (producto_id, fecha, tipo_movimiento, cantidad, costo_unitario, costo_total, saldo_cantidad, saldo_valor, asiento_id)
VALUES (1, '2026-01-05', 'ENTRADA', 1000, 8.85, 8850.00, @KSaldoCant, @KSaldoVal, @AsientoID);

SET @Num = @Num + 1;
-- Asiento 4
INSERT INTO asientos (numero, fecha, concepto, total_debe, total_haber) VALUES (@Num, '2026-01-10', 'C/Venta de producto al credito', 12000.00, 12000.00);
SET @AsientoID = SCOPE_IDENTITY();
INSERT INTO detalle_asiento (asiento_id, cuenta_codigo, concepto_linea, debe, haber, renglon) VALUES 
(@AsientoID, '1.3.1', 'Clientes', 12000.00, 0, 1),
(@AsientoID, '4.1', 'Ventas', 0, 10619.46, 2),
(@AsientoID, '2.3', 'IVA Debito Fiscal', 0, 1380.54, 3);

-- Kardex: Salida por venta (10619.46 / 17.70 = 600 unidades). Costo PEPS: 600 x $8.85 = $5,310.00
SET @KSaldoCant = @KSaldoCant - 600;
SET @KSaldoVal  = @KSaldoVal  - 5310.00;
INSERT INTO kardex (producto_id, fecha, tipo_movimiento, cantidad, costo_unitario, costo_total, saldo_cantidad, saldo_valor, asiento_id)
VALUES (1, '2026-01-10', 'SALIDA', 600, 8.85, 5310.00, @KSaldoCant, @KSaldoVal, @AsientoID);

SET @Num = @Num + 1;
-- Asiento 5
INSERT INTO asientos (numero, fecha, concepto, total_debe, total_haber) VALUES (@Num, '2026-01-15', 'C/Pago de producto', 10000.00, 10000.00);
SET @AsientoID = SCOPE_IDENTITY();
INSERT INTO detalle_asiento (asiento_id, cuenta_codigo, concepto_linea, debe, haber, renglon) VALUES 
(@AsientoID, '2.1.1', 'Proveedores', 10000.00, 0, 1),
(@AsientoID, '1.1.2', 'Bancos', 0, 10000.00, 2);

SET @Num = @Num + 1;
-- Asiento 6
INSERT INTO asientos (numero, fecha, concepto, total_debe, total_haber) VALUES (@Num, '2026-01-31', 'C/Compra de Escritorios', 300.00, 300.00);
SET @AsientoID = SCOPE_IDENTITY();
INSERT INTO detalle_asiento (asiento_id, cuenta_codigo, concepto_linea, debe, haber, renglon) VALUES 
(@AsientoID, '1.6.1', 'Mobiliario y equipo de oficina', 265.48, 0, 1),
(@AsientoID, '1.4', 'IVA Credito Fiscal', 34.52, 0, 2),
(@AsientoID, '1.1.1', 'Caja', 0, 300.00, 3);

SET @Num = @Num + 1;
-- Asiento 7
INSERT INTO asientos (numero, fecha, concepto, total_debe, total_haber) VALUES (@Num, '2026-02-10', 'C/Primer Pago de Venta 10 enero', 6000.00, 6000.00);
SET @AsientoID = SCOPE_IDENTITY();
INSERT INTO detalle_asiento (asiento_id, cuenta_codigo, concepto_linea, debe, haber, renglon) VALUES 
(@AsientoID, '1.1.2', 'Bancos', 6000.00, 0, 1),
(@AsientoID, '1.3.1', 'Clientes', 0, 6000.00, 2);

SET @Num = @Num + 1;
-- Asiento 8
INSERT INTO asientos (numero, fecha, concepto, total_debe, total_haber) VALUES (@Num, '2026-02-15', 'C/Compra de Laptop', 580.00, 580.00);
SET @AsientoID = SCOPE_IDENTITY();
INSERT INTO detalle_asiento (asiento_id, cuenta_codigo, concepto_linea, debe, haber, renglon) VALUES 
(@AsientoID, '1.6.1', 'Mobiliario y equipo de oficina', 513.27, 0, 1),
(@AsientoID, '1.4', 'IVA Credito Fiscal', 66.73, 0, 2),
(@AsientoID, '1.1.2', 'Bancos', 0, 580.00, 3);

SET @Num = @Num + 1;
-- Asiento 9
INSERT INTO asientos (numero, fecha, concepto, total_debe, total_haber) VALUES (@Num, '2026-02-20', 'C/Compra de Vehiculo a Grupo Q', 15000.00, 15000.00);
SET @AsientoID = SCOPE_IDENTITY();
INSERT INTO detalle_asiento (asiento_id, cuenta_codigo, concepto_linea, debe, haber, renglon) VALUES 
(@AsientoID, '1.6.3', 'Equipo de transporte', 13274.33, 0, 1),
(@AsientoID, '1.4', 'IVA Credito Fiscal', 1725.67, 0, 2),
(@AsientoID, '2.1.2', 'Acreedores varios', 0, 13500.00, 3),
(@AsientoID, '1.1.2', 'Bancos', 0, 1500.00, 4);

SET @Num = @Num + 1;
-- Asiento 10
INSERT INTO asientos (numero, fecha, concepto, total_debe, total_haber) VALUES (@Num, '2026-02-21', 'C/Venta de producto', 5000.00, 5000.00);
SET @AsientoID = SCOPE_IDENTITY();
INSERT INTO detalle_asiento (asiento_id, cuenta_codigo, concepto_linea, debe, haber, renglon) VALUES 
(@AsientoID, '1.1.1', 'Caja', 5000.00, 0, 1),
(@AsientoID, '4.1', 'Ventas', 0, 4424.77, 2),
(@AsientoID, '2.3', 'IVA Debito Fiscal', 0, 575.23, 3);

-- Kardex: Salida por venta (4424.77 / 17.70 = 250 unidades). Costo PEPS: 250 x $8.85 = $2,212.50
SET @KSaldoCant = @KSaldoCant - 250;
SET @KSaldoVal  = @KSaldoVal  - 2212.50;
INSERT INTO kardex (producto_id, fecha, tipo_movimiento, cantidad, costo_unitario, costo_total, saldo_cantidad, saldo_valor, asiento_id)
VALUES (1, '2026-02-21', 'SALIDA', 250, 8.85, 2212.50, @KSaldoCant, @KSaldoVal, @AsientoID);

SET @Num = @Num + 1;
-- Asiento 11
INSERT INTO asientos (numero, fecha, concepto, total_debe, total_haber) VALUES (@Num, '2026-02-22', 'C/Prestamo a 6 aÃ±os', 20000.00, 20000.00);
SET @AsientoID = SCOPE_IDENTITY();
INSERT INTO detalle_asiento (asiento_id, cuenta_codigo, concepto_linea, debe, haber, renglon) VALUES 
(@AsientoID, '1.1.2', 'Bancos', 18870.00, 0, 1),
(@AsientoID, '6.1.1', 'Comision', 1000.00, 0, 2),
(@AsientoID, '1.4', 'IVA Credito Fiscal', 130.00, 0, 3),
(@AsientoID, '2.2', 'Prestamo bancario', 0, 20000.00, 4);
