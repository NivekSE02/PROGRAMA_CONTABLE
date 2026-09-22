-- =====================================================================
-- MODULO CONTABLE: DATOS INICIALES (data.sql)
-- Compatible con SQLite y SQL Server
-- Solo se ejecuta una vez cuando la base de datos está vacía.
-- =====================================================================


-- =========================================================
-- 1. Catálogo de Cuentas
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