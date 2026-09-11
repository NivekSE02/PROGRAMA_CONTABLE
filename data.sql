-- =====================================================================
-- UNIVERSIDAD CATÓLICA DE EL SALVADOR (UNICAES)
-- MODULO CONTABLE: DATOS INICIALES Y CATÁLOGO DE CUENTAS (data.sql)
-- =====================================================================

-- 1. USUARIOS Y ROLES DEL SISTEMA
INSERT INTO usuarios (username, password, nombre_completo, rol, estado) VALUES
('admin', 'admin123', 'Lic. Kevin Administrador', 'ADMINISTRADOR', 'ACTIVO'),
('contador', 'conta123', 'Licda. María Contadora', 'CONTADOR', 'ACTIVO'),
('auditor', 'audit123', 'Lic. Carlos Auditor', 'AUDITOR', 'ACTIVO');

-- 2. CATÁLOGO DE CUENTAS ESTÁNDAR (Clasificación NIIF para PYMES / El Salvador)
-- CLASE 1: ACTIVO (Naturaleza Deudora)
INSERT INTO cuentas (codigo, nombre, tipo, subtipo, nivel, naturaleza, cuenta_padre, permite_movimiento) VALUES
('1', 'ACTIVO', 'ACTIVO', NULL, 1, 'DEUDORA', NULL, 0),
('11', 'ACTIVO CORRIENTE', 'ACTIVO', 'CORRIENTE', 2, 'DEUDORA', '1', 0),
('1101', 'Efectivo y Equivalentes de Efectivo', 'ACTIVO', 'CORRIENTE', 3, 'DEUDORA', '11', 0),
('110101', 'Caja General', 'ACTIVO', 'CORRIENTE', 4, 'DEUDORA', '1101', 1),
('110102', 'Caja Chica', 'ACTIVO', 'CORRIENTE', 4, 'DEUDORA', '1101', 1),
('110103', 'Bancos Locales (Cta. Corriente)', 'ACTIVO', 'CORRIENTE', 4, 'DEUDORA', '1101', 1),
('1102', 'Inversiones Financieras a Corto Plazo', 'ACTIVO', 'CORRIENTE', 3, 'DEUDORA', '11', 0),
('110201', 'Depósitos a Plazo Fijo', 'ACTIVO', 'CORRIENTE', 4, 'DEUDORA', '1102', 1),
('1103', 'Cuentas y Documentos por Cobrar', 'ACTIVO', 'CORRIENTE', 3, 'DEUDORA', '11', 0),
('110301', 'Clientes Locales', 'ACTIVO', 'CORRIENTE', 4, 'DEUDORA', '1103', 1),
('110302', 'Documentos por Cobrar Comerciales', 'ACTIVO', 'CORRIENTE', 4, 'DEUDORA', '1103', 1),
('1104', 'Impuestos por Recuperar / Crédito Fiscal', 'ACTIVO', 'CORRIENTE', 3, 'DEUDORA', '11', 0),
('110401', 'IVA Crédito Fiscal (13%)', 'ACTIVO', 'CORRIENTE', 4, 'DEUDORA', '1104', 1),
('1105', 'Inventarios', 'ACTIVO', 'CORRIENTE', 3, 'DEUDORA', '11', 0),
('110501', 'Inventario de Mercaderías', 'ACTIVO', 'CORRIENTE', 4, 'DEUDORA', '1105', 1),
('1106', 'Pagos Anticipados', 'ACTIVO', 'CORRIENTE', 3, 'DEUDORA', '11', 0),
('110601', 'Seguros Pagados por Anticipado', 'ACTIVO', 'CORRIENTE', 4, 'DEUDORA', '1106', 1),
('110602', 'Alquileres Pagados por Anticipado', 'ACTIVO', 'CORRIENTE', 4, 'DEUDORA', '1106', 1),
('12', 'ACTIVO NO CORRIENTE', 'ACTIVO', 'NO_CORRIENTE', 2, 'DEUDORA', '1', 0),
('1201', 'Propiedad, Planta y Equipo', 'ACTIVO', 'NO_CORRIENTE', 3, 'DEUDORA', '12', 0),
('120101', 'Terrenos', 'ACTIVO', 'NO_CORRIENTE', 4, 'DEUDORA', '1201', 1),
('120102', 'Edificios e Instalaciones', 'ACTIVO', 'NO_CORRIENTE', 4, 'DEUDORA', '1201', 1),
('120103', 'Mobiliario y Equipo de Oficina', 'ACTIVO', 'NO_CORRIENTE', 4, 'DEUDORA', '1201', 1),
('120104', 'Equipo de Computación', 'ACTIVO', 'NO_CORRIENTE', 4, 'DEUDORA', '1201', 1),
('120105', 'Equipo de Transporte', 'ACTIVO', 'NO_CORRIENTE', 4, 'DEUDORA', '1201', 1);

-- CLASE 2: PASIVO (Naturaleza Acreedora)
INSERT INTO cuentas (codigo, nombre, tipo, subtipo, nivel, naturaleza, cuenta_padre, permite_movimiento) VALUES
('2', 'PASIVO', 'PASIVO', NULL, 1, 'ACREEDORA', NULL, 0),
('21', 'PASIVO CORRIENTE', 'PASIVO', 'CORRIENTE', 2, 'ACREEDORA', '2', 0),
('2101', 'Cuentas y Documentos por Pagar Comerciales', 'PASIVO', 'CORRIENTE', 3, 'ACREEDORA', '21', 0),
('210101', 'Proveedores Locales', 'PASIVO', 'CORRIENTE', 4, 'ACREEDORA', '2101', 1),
('210102', 'Acreedores Varios', 'PASIVO', 'CORRIENTE', 4, 'ACREEDORA', '2101', 1),
('2102', 'Préstamos Bancarios a Corto Plazo', 'PASIVO', 'CORRIENTE', 3, 'ACREEDORA', '21', 0),
('210201', 'Préstamos Bancarios CP', 'PASIVO', 'CORRIENTE', 4, 'ACREEDORA', '2102', 1),
('2103', 'Impuestos y Retenciones por Pagar', 'PASIVO', 'CORRIENTE', 3, 'ACREEDORA', '21', 0),
('210301', 'IVA Débito Fiscal (13%)', 'PASIVO', 'CORRIENTE', 4, 'ACREEDORA', '2103', 1),
('210302', 'Retenciones Legales por Pagar (Renta/ISSS)', 'PASIVO', 'CORRIENTE', 4, 'ACREEDORA', '2103', 1),
('2104', 'Beneficios a Empleados por Pagar', 'PASIVO', 'CORRIENTE', 3, 'ACREEDORA', '21', 0),
('210401', 'Sueldos por Pagar', 'PASIVO', 'CORRIENTE', 4, 'ACREEDORA', '2104', 1),
('22', 'PASIVO NO CORRIENTE', 'PASIVO', 'NO_CORRIENTE', 2, 'ACREEDORA', '2', 0),
('2201', 'Obligaciones Financieras a Largo Plazo', 'PASIVO', 'NO_CORRIENTE', 3, 'ACREEDORA', '22', 0),
('220101', 'Préstamos Hipotecarios a Largo Plazo', 'PASIVO', 'NO_CORRIENTE', 4, 'ACREEDORA', '2201', 1);

-- CLASE 3: CAPITAL CONTABLE / PATRIMONIO (Naturaleza Acreedora)
INSERT INTO cuentas (codigo, nombre, tipo, subtipo, nivel, naturaleza, cuenta_padre, permite_movimiento) VALUES
('3', 'CAPITAL CONTABLE', 'CAPITAL', NULL, 1, 'ACREEDORA', NULL, 0),
('31', 'CAPITAL SOCIAL', 'CAPITAL', 'CAPITAL_SOCIAL', 2, 'ACREEDORA', '3', 0),
('3101', 'Capital Social Pagado', 'CAPITAL', 'CAPITAL_SOCIAL', 3, 'ACREEDORA', '31', 0),
('310101', 'Capital Social - Aportes de Socios', 'CAPITAL', 'CAPITAL_SOCIAL', 4, 'ACREEDORA', '3101', 1),
('32', 'RESERVAS', 'CAPITAL', 'RESERVAS', 2, 'ACREEDORA', '3', 0),
('3201', 'Reservas de Capital', 'CAPITAL', 'RESERVAS', 3, 'ACREEDORA', '32', 0),
('320101', 'Reserva Legal (7%)', 'CAPITAL', 'RESERVAS', 4, 'ACREEDORA', '3201', 1),
('33', 'RESULTADOS ACUMULADOS', 'CAPITAL', 'RESULTADOS', 2, 'ACREEDORA', '3', 0),
('3301', 'Resultados del Ejercicio y Anteriores', 'CAPITAL', 'RESULTADOS', 3, 'ACREEDORA', '33', 0),
('330101', 'Utilidades de Ejercicios Anteriores', 'CAPITAL', 'RESULTADOS', 4, 'ACREEDORA', '3301', 1),
('330102', 'Utilidad del Presente Ejercicio', 'CAPITAL', 'RESULTADOS', 4, 'ACREEDORA', '3301', 1);

-- CLASE 4: COSTOS Y GASTOS (Naturaleza Deudora)
INSERT INTO cuentas (codigo, nombre, tipo, subtipo, nivel, naturaleza, cuenta_padre, permite_movimiento) VALUES
('4', 'COSTOS Y GASTOS', 'COSTO_GASTO', NULL, 1, 'DEUDORA', NULL, 0),
('41', 'COSTOS DE OPERACIÓN', 'COSTO_GASTO', 'COSTO_VENTA', 2, 'DEUDORA', '4', 0),
('4101', 'Costo de Ventas', 'COSTO_GASTO', 'COSTO_VENTA', 3, 'DEUDORA', '41', 0),
('410101', 'Costo de Mercaderías Vendidas', 'COSTO_GASTO', 'COSTO_VENTA', 4, 'DEUDORA', '4101', 1),
('42', 'GASTOS DE ADMINISTRACIÓN', 'COSTO_GASTO', 'GASTO_ADMIN', 2, 'DEUDORA', '4', 0),
('4201', 'Gastos de Administración', 'COSTO_GASTO', 'GASTO_ADMIN', 3, 'DEUDORA', '42', 0),
('420101', 'Sueldos y Salarios Administrativos', 'COSTO_GASTO', 'GASTO_ADMIN', 4, 'DEUDORA', '4201', 1),
('420102', 'Servicios Básicos (Agua, Energía, Teléfono)', 'COSTO_GASTO', 'GASTO_ADMIN', 4, 'DEUDORA', '4201', 1),
('420103', 'Alquiler de Instalaciones Administrativas', 'COSTO_GASTO', 'GASTO_ADMIN', 4, 'DEUDORA', '4201', 1),
('420104', 'Papelería y Útiles de Oficina', 'COSTO_GASTO', 'GASTO_ADMIN', 4, 'DEUDORA', '4201', 1),
('43', 'GASTOS DE VENTA', 'COSTO_GASTO', 'GASTO_VENTA', 2, 'DEUDORA', '4', 0),
('4301', 'Gastos de Comercialización y Venta', 'COSTO_GASTO', 'GASTO_VENTA', 3, 'DEUDORA', '43', 0),
('430101', 'Comisiones a Vendedores', 'COSTO_GASTO', 'GASTO_VENTA', 4, 'DEUDORA', '4301', 1),
('430102', 'Publicidad y Mercadeo', 'COSTO_GASTO', 'GASTO_VENTA', 4, 'DEUDORA', '4301', 1),
('44', 'GASTOS FINANCIEROS', 'COSTO_GASTO', 'GASTO_FINANCIERO', 2, 'DEUDORA', '4', 0),
('4401', 'Gastos Financieros', 'COSTO_GASTO', 'GASTO_FINANCIERO', 3, 'DEUDORA', '44', 0),
('440101', 'Intereses y Comisiones Bancarias', 'COSTO_GASTO', 'GASTO_FINANCIERO', 4, 'DEUDORA', '4401', 1);

-- CLASE 5: INGRESOS (Naturaleza Acreedora)
INSERT INTO cuentas (codigo, nombre, tipo, subtipo, nivel, naturaleza, cuenta_padre, permite_movimiento) VALUES
('5', 'INGRESOS', 'INGRESO', NULL, 1, 'ACREEDORA', NULL, 0),
('51', 'INGRESOS DE OPERACIÓN', 'INGRESO', 'INGRESO_OPERACIONAL', 2, 'ACREEDORA', '5', 0),
('5101', 'Ventas de Mercaderías', 'INGRESO', 'INGRESO_OPERACIONAL', 3, 'ACREEDORA', '51', 0),
('510101', 'Ventas al Contado', 'INGRESO', 'INGRESO_OPERACIONAL', 4, 'ACREEDORA', '5101', 1),
('510102', 'Ventas al Crédito', 'INGRESO', 'INGRESO_OPERACIONAL', 4, 'ACREEDORA', '5101', 1),
('52', 'OTROS INGRESOS', 'INGRESO', 'OTRO_INGRESO', 2, 'ACREEDORA', '5', 0),
('5201', 'Ingresos No Operacionales', 'INGRESO', 'OTRO_INGRESO', 3, 'ACREEDORA', '52', 0),
('520101', 'Rendimientos e Intereses Financieros Ganados', 'INGRESO', 'OTRO_INGRESO', 4, 'ACREEDORA', '5201', 1),
('520102', 'Ingresos Extraordinarios Diversos', 'INGRESO', 'OTRO_INGRESO', 4, 'ACREEDORA', '5201', 1);

-- 3. ASIENTOS INICIALES DE EJEMPLO (Ciclo Contable Cuadrado y Completo)

-- Asiento 1: Partida de Apertura de Operaciones
INSERT INTO asientos (id, numero, fecha, concepto, total_debe, total_haber, usuario_id) VALUES
(1, 1, '2026-09-01', 'Asiento de apertura por inicio de operaciones de la empresa con aportaciones de socios', 50000.00, 50000.00, 1);

INSERT INTO detalle_asiento (asiento_id, renglon, cuenta_codigo, concepto_linea, debe, haber) VALUES
(1, 1, '110103', 'Depósito en cuenta bancaria por aportes de capital', 35000.00, 0.00),
(1, 2, '120103', 'Aporte en mobiliario y equipo de oficina', 10000.00, 0.00),
(1, 3, '120104', 'Aporte de equipo de computación', 5000.00, 0.00),
(1, 4, '310101', 'Capital Social aportado por socios fundadores', 0.00, 50000.00);

-- Asiento 2: Compra de Mercadería al Crédito y Contado con IVA Crédito Fiscal
INSERT INTO asientos (id, numero, fecha, concepto, total_debe, total_haber, usuario_id) VALUES
(2, 2, '2026-09-05', 'Compra de mercadería para la venta, 50% al contado y 50% al crédito según comprobante de crédito fiscal', 11300.00, 11300.00, 2);

INSERT INTO detalle_asiento (asiento_id, renglon, cuenta_codigo, concepto_linea, debe, haber) VALUES
(2, 1, '110501', 'Ingreso de mercadería al inventario', 10000.00, 0.00),
(2, 2, '110401', 'IVA Crédito Fiscal 13% sobre compra', 1300.00, 0.00),
(2, 3, '110103', 'Pago del 50% mediante cheque/transferencia bancaria', 0.00, 5650.00),
(2, 4, '210101', 'Saldo pendiente al crédito con proveedores locales', 0.00, 5650.00);

-- Asiento 3: Venta de Mercaderías con IVA Débito Fiscal
INSERT INTO asientos (id, numero, fecha, concepto, total_debe, total_haber, usuario_id) VALUES
(3, 3, '2026-09-10', 'Venta de mercaderías: 60% al contado y 40% al crédito comercial según factura de venta', 18080.00, 18080.00, 2);

INSERT INTO detalle_asiento (asiento_id, renglon, cuenta_codigo, concepto_linea, debe, haber) VALUES
(3, 1, '110103', 'Ingreso en bancos por cobro del 60% de la venta', 10848.00, 0.00),
(3, 2, '110301', 'Cuentas por cobrar a clientes por el 40% al crédito', 7232.00, 0.00),
(3, 3, '510101', 'Venta de mercaderías netas', 0.00, 16000.00),
(3, 4, '210301', 'IVA Débito Fiscal 13% sobre la venta efectuada', 0.00, 2080.00);

-- Asiento 4: Reconocimiento del Costo de Venta de la Mercadería Vendida
INSERT INTO asientos (id, numero, fecha, concepto, total_debe, total_haber, usuario_id) VALUES
(4, 4, '2026-09-10', 'Registro del costo de lo vendido correspondiente a la venta de mercaderías del periodo', 6000.00, 6000.00, 2);

INSERT INTO detalle_asiento (asiento_id, renglon, cuenta_codigo, concepto_linea, debe, haber) VALUES
(4, 1, '410101', 'Costo de mercaderías vendidas', 6000.00, 0.00),
(4, 2, '110501', 'Descargo de inventario de mercadería por salida de venta', 0.00, 6000.00);

-- Asiento 5: Pago de Gastos Administrativos (Servicios Básicos y Alquiler)
INSERT INTO asientos (id, numero, fecha, concepto, total_debe, total_haber, usuario_id) VALUES
(5, 5, '2026-09-15', 'Pago de servicios básicos y alquiler de oficina administrativa mediante transferencia bancaria', 1200.00, 1200.00, 2);

INSERT INTO detalle_asiento (asiento_id, renglon, cuenta_codigo, concepto_linea, debe, haber) VALUES
(5, 1, '420102', 'Pago de energía eléctrica, agua potable e internet', 400.00, 0.00),
(5, 2, '420103', 'Pago del alquiler del local comercial y administrativo', 800.00, 0.00),
(5, 3, '110103', 'Salida de fondos de banco cuenta corriente', 0.00, 1200.00);
