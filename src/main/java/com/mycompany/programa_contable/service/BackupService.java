package com.mycompany.programa_contable.service;

import com.mycompany.programa_contable.db.DatabaseManager;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.zip.*;

/** Portably exports/restores application data on SQLite or SQL Server. */
public class BackupService {
    private static final String ENTRY = "contabilidad-backup.bin";
    private static final List<String> EXPORT_ORDER = List.of("cuentas", "productos", "configuracion", "asientos", "detalle_asiento", "kardex", "asientos_predefinidos", "detalle_asiento_predefinido");
    private static final List<String> DELETE_ORDER = List.of("detalle_asiento_predefinido", "asientos_predefinidos", "kardex", "detalle_asiento", "asientos", "productos", "cuentas", "configuracion");

    public void exportar(Path archivo) throws SQLException, IOException {
        try (Connection c = DatabaseManager.getInstance().getConnection(); ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archivo))) {
            zip.putNextEntry(new ZipEntry(ENTRY));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zip));
            out.writeInt(1);
            out.writeUTF(DatabaseManager.getInstance().getMotorActivo().name());
            List<String> tablas = new ArrayList<>();
            for (String tabla : EXPORT_ORDER) if (existeTabla(c, tabla)) tablas.add(tabla);
            out.writeInt(tablas.size());
            for (String tabla : tablas) exportarTabla(c, tabla, out);
            out.flush();
            zip.closeEntry();
        }
    }

    private void exportarTabla(Connection c, String tabla, DataOutputStream out) throws SQLException, IOException {
        List<Integer> tipos = new ArrayList<>();
        List<String> columnas = new ArrayList<>();
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM " + tabla + " WHERE 1=0")) {
            ResultSetMetaData md = r.getMetaData();
            for (int i=1;i<=md.getColumnCount();i++) { columnas.add(md.getColumnName(i)); tipos.add(md.getColumnType(i)); }
        }
        out.writeUTF(tabla); out.writeInt(columnas.size());
        for (int i=0;i<columnas.size();i++) { out.writeUTF(columnas.get(i)); out.writeInt(tipos.get(i)); }
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM " + tabla)) {
            List<String[]> filas = new ArrayList<>();
            while (r.next()) {
                String[] valores = new String[columnas.size()];
                for (int i=0;i<valores.length;i++) { Object value=r.getObject(i+1); valores[i]=value==null?null:(value instanceof byte[] bytes ? Base64.getEncoder().encodeToString(bytes) : value.toString()); }
                filas.add(valores);
            }
            out.writeInt(filas.size());
            for (String[] fila:filas) for (String v:fila) { out.writeBoolean(v!=null); if(v!=null) out.writeUTF(v); }
        }
    }

    public void importar(Path archivo) throws SQLException, IOException {
        List<TableData> tablas;
        String motor;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archivo))) {
            ZipEntry entry=zip.getNextEntry();
            if (entry==null || !ENTRY.equals(entry.getName())) throw new IOException("El archivo no contiene un respaldo contable válido.");
            DataInputStream in = new DataInputStream(new BufferedInputStream(zip));
            int version=in.readInt(); if(version!=1) throw new IOException("La versión del respaldo no es compatible.");
            motor=in.readUTF();
            int count=in.readInt(); if(count<1 || count>EXPORT_ORDER.size()) throw new IOException("El respaldo no tiene tablas reconocibles.");
            tablas=new ArrayList<>();
            for(int t=0;t<count;t++) {
                String nombre=in.readUTF(); if(!EXPORT_ORDER.contains(nombre)) throw new IOException("El respaldo contiene una tabla desconocida.");
                int cols=in.readInt(); if(cols<1 || cols>100) throw new IOException("Estructura de respaldo inválida.");
                List<String> nombres=new ArrayList<>(); int[] tipos=new int[cols];
                for(int i=0;i<cols;i++){nombres.add(in.readUTF());tipos[i]=in.readInt();}
                int rows=in.readInt(); if(rows<0 || rows>10_000_000) throw new IOException("Cantidad de filas inválida.");
                List<String[]> data=new ArrayList<>(rows);
                for(int row=0;row<rows;row++){String[] values=new String[cols];for(int col=0;col<cols;col++)if(in.readBoolean())values[col]=in.readUTF();data.add(values);}
                tablas.add(new TableData(nombre,nombres,tipos,data));
            }
        } catch (EOFException e) { throw new IOException("El archivo de respaldo está incompleto.", e); }

        DatabaseManager manager=DatabaseManager.getInstance();
        if (!motor.equals(manager.getMotorActivo().name())) throw new IOException("Este respaldo se creó con " + motor + "; cambie al mismo motor de base de datos antes de restaurarlo.");
        restaurarTransaccional(tablas);
    }

    private void restaurarTransaccional(List<TableData> tablas) throws SQLException, IOException {
        Map<String,TableData> datos=new HashMap<>(); for(TableData t:tablas) datos.put(t.nombre,t);
        try(Connection c=DatabaseManager.getInstance().getConnection()) {
            boolean sqlite=DatabaseManager.getInstance().getMotorActivo()==DatabaseManager.MotorBD.SQLITE;
            if(sqlite) try(Statement s=c.createStatement()){s.execute("PRAGMA foreign_keys=OFF");}
            c.setAutoCommit(false);
            try {
                for(String tabla:DELETE_ORDER) if(existeTabla(c,tabla)) try(Statement s=c.createStatement()){s.executeUpdate("DELETE FROM "+tabla);}
                for(String tabla:EXPORT_ORDER) {
                    TableData d=datos.get(tabla); if(d==null) continue;
                    if(!existeTabla(c,tabla)) throw new IOException("La base de datos no contiene la tabla requerida: "+tabla);
                    insertarTabla(c,d);
                }
                c.commit();
            } catch(Exception e) { c.rollback(); if(e instanceof SQLException se) throw se; if(e instanceof IOException ioe) throw ioe; throw new SQLException("No se pudo restaurar el respaldo.",e); }
            finally { c.setAutoCommit(true); if(sqlite) try(Statement s=c.createStatement()){s.execute("PRAGMA foreign_keys=ON");} }
        }
    }

    private void insertarTabla(Connection c, TableData d) throws SQLException {
        String cols=String.join(",",d.columnas);
        String marks=String.join(",",Collections.nCopies(d.columnas.size(),"?"));
        boolean identity=false;
        try(Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT * FROM "+d.nombre+" WHERE 1=0")) {
            ResultSetMetaData md=r.getMetaData(); for(int i=1;i<=md.getColumnCount();i++) if(md.isAutoIncrement(i)) identity=true;
        }
        boolean sqlServer=DatabaseManager.getInstance().getMotorActivo()==DatabaseManager.MotorBD.SQL_SERVER;
        if(sqlServer && identity) try(Statement s=c.createStatement()){s.execute("SET IDENTITY_INSERT "+d.nombre+" ON");}
        try(PreparedStatement p=c.prepareStatement("INSERT INTO "+d.nombre+" ("+cols+") VALUES ("+marks+")")) {
            for(String[] fila:d.filas) {
                for(int i=0;i<fila.length;i++) setValue(p,i+1,fila[i],d.tipos[i]);
                p.addBatch();
            }
            p.executeBatch();
        } finally { if(sqlServer && identity) try(Statement s=c.createStatement()){s.execute("SET IDENTITY_INSERT "+d.nombre+" OFF");} }
    }

    private void setValue(PreparedStatement p,int index,String value,int type) throws SQLException {
        if(value==null){p.setNull(index,type);return;}
        try {
            switch(type){
                case Types.TINYINT, Types.SMALLINT, Types.INTEGER -> p.setInt(index,Integer.parseInt(value));
                case Types.BIGINT -> p.setLong(index,Long.parseLong(value));
                case Types.FLOAT, Types.REAL, Types.DOUBLE -> p.setDouble(index,Double.parseDouble(value));
                case Types.NUMERIC, Types.DECIMAL -> p.setBigDecimal(index,new java.math.BigDecimal(value));
                case Types.BOOLEAN, Types.BIT -> p.setBoolean(index,"1".equals(value)||Boolean.parseBoolean(value));
                case Types.DATE -> p.setDate(index,java.sql.Date.valueOf(value));
                case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> p.setTimestamp(index,Timestamp.valueOf(value.replace('T',' ')));
                case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> p.setBytes(index,Base64.getDecoder().decode(value));
                default -> p.setString(index,value);
            }
        } catch(IllegalArgumentException e){throw new SQLException("Dato inválido al restaurar respaldo (tipo "+type+").",e);}
    }

    private boolean existeTabla(Connection c,String tabla) throws SQLException {
        if(DatabaseManager.getInstance().getMotorActivo()==DatabaseManager.MotorBD.SQL_SERVER) {
            try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME=?")){p.setString(1,tabla);try(ResultSet r=p.executeQuery()){return r.next();}}
        }
        try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")){p.setString(1,tabla);try(ResultSet r=p.executeQuery()){return r.next();}}
    }

    private record TableData(String nombre,List<String> columnas,int[] tipos,List<String[]> filas) {}
}
