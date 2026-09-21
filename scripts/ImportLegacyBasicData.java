import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;

/** Offline, selected local test database only. No application endpoint or production seed. */
class ImportLegacyBasicData {
    private static final Map<String,String> COLUMNS = new LinkedHashMap<>();
    static {
        COLUMNS.put("param_basic", "id,name,system_name,sort,created_by,created_at,updated_by,updated_at");
        COLUMNS.put("param_basic_type", "id,basic_id,param_key,param_value,desp,sort,created_by,created_at,updated_by,updated_at");
        COLUMNS.put("param_region", "id,parent_id,city_code,name,short_name,zip_code,longitude,latitude,level,sort,created_by,created_at,updated_by,updated_at");
        COLUMNS.put("sys_configure", "id,name,`value`,type,desp,sort,created_by,created_at,updated_by,updated_at");
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 3 || !Set.of("apply","verify").contains(args[0])) throw new IllegalArgumentException("apply|verify data.tsv sha256");
        byte[] bytes = Files.readAllBytes(Path.of(args[1]));
        if (!hash(bytes).equals(args[2])) throw new IllegalStateException("Data checksum mismatch");
        var data = new LinkedHashMap<String,List<List<String>>>();
        COLUMNS.keySet().forEach(t -> data.put(t,new ArrayList<>()));
        for (String line : new String(bytes,StandardCharsets.UTF_8).lines().toList()) {
            String[] cells = line.split("\t",-1);
            if (!COLUMNS.containsKey(cells[0]) || cells.length != COLUMNS.get(cells[0]).split(",").length+1) throw new IllegalStateException("Unapproved data layout");
            var row = new ArrayList<String>();
            for (int i=1;i<cells.length;i++) row.add(cells[i].equals("~")?null:new String(Base64.getDecoder().decode(cells[i]),StandardCharsets.UTF_8));
            data.get(cells[0]).add(row);
        }
        String url = required("CYYS_TEST_DB_URL");
        if (!url.matches("jdbc:mysql://(localhost|127\\.0\\.0\\.1):3306/cyys_frame\\?.*")) throw new IllegalStateException("Only the selected local test database is allowed");
        try (var c = DriverManager.getConnection(url,required("CYYS_TEST_DB_USERNAME"),required("CYYS_TEST_DB_PASSWORD"))) {
            c.setAutoCommit(false);
            try {
                try(var s=c.createStatement();var r=s.executeQuery("select get_lock('cyys-legacy-basic-import',0)")) {
                    r.next(); if(r.getInt(1)!=1) throw new IllegalStateException("Another import is running");
                }
                try(var s=c.createStatement();var r=s.executeQuery("select count(*) from flyway_schema_history where version='18' and success=1")) {
                    r.next(); if(r.getInt(1)!=1) throw new IllegalStateException("Expected existing V18 schema");
                }
                try(var s=c.createStatement();var r=s.executeQuery("select count(*) from information_schema.columns where table_schema=database() and table_name='param_basic' and column_name='system_builtin'")) {
                    r.next();
                    if(r.getInt(1)>0) throw new IllegalStateException("V20 dictionary protection requires a reviewed import mapping; this historical V18 import must not run on the upgraded schema");
                }
                Map<String,String> before = untouched(c);
                boolean empty=true;
                for(String table:COLUMNS.keySet()) {
                    try(var s=c.createStatement();var r=s.executeQuery("select id from "+table+" for update")) { if(r.next()) empty=false; }
                }
                boolean inserted = empty && args[0].equals("apply");
                if(inserted) for(var entry:data.entrySet()) {
                    String columns=COLUMNS.get(entry.getKey());
                    String marks=String.join(",",Collections.nCopies(columns.split(",").length,"?"));
                    try(var p=c.prepareStatement("insert into "+entry.getKey()+" ("+columns+") values ("+marks+")")) {
                        for(var row:entry.getValue()) {
                            for(int i=0;i<row.size();i++) p.setString(i+1,row.get(i));
                            p.addBatch();
                        }
                        p.executeBatch();
                    }
                }
                // Existing data is never overwritten. Repeating an import only verifies identical rows.
                for(var entry:data.entrySet()) verify(c,entry.getKey(),entry.getValue());
                if(!before.equals(untouched(c))) throw new IllegalStateException("Unselected table changed; rollback");
                c.commit();
                System.out.println("result="+(inserted?"IMPORTED":"VERIFIED_EXISTING")+" data_sha256="+args[2]);
                data.forEach((t,rows)->System.out.println(t+" rows="+rows.size()+" fields=VERIFIED"));
                System.out.println("unselected_tables="+before.size()+" fingerprints=UNCHANGED");
            } catch(Exception failure) {
                c.rollback();
                // Do not expose row values, credentials, driver SQL or batch contents.
                throw new IllegalStateException("Import refused/rolled back: "+(failure instanceof SQLException sql?"SQLState="+sql.getSQLState():failure.getMessage()));
            }
        }
    }
    private static void verify(Connection c,String table,List<List<String>> expected) throws Exception {
        var rows=new HashMap<String,List<String>>(); for(var row:expected) if(rows.put(row.getFirst(),row)!=null) throw new IllegalStateException("Duplicate source ID");
        String[] columns=COLUMNS.get(table).split(","); int count=0;
        try(var s=c.createStatement();var r=s.executeQuery("select "+COLUMNS.get(table)+",version,deleted from "+table)) {
            while(r.next()) {
                count++;var wanted=rows.get(r.getString(1));
                if(wanted==null || r.getInt(columns.length+1)!=0 || r.getInt(columns.length+2)!=0) throw new IllegalStateException("Existing target rows differ: "+table);
                for(int i=0;i<columns.length;i++) {
                    String actual=r.getString(i+1), value=wanted.get(i);
                    boolean same=Objects.equals(actual,value);
                    if(!same && actual!=null && value!=null && Set.of("longitude","latitude").contains(columns[i])) same=new BigDecimal(actual).compareTo(new BigDecimal(value))==0;
                    if(!same) throw new IllegalStateException("Field mismatch: "+table+"."+columns[i]);
                }
            }
        }
        if(count!=expected.size()) throw new IllegalStateException("Count mismatch: "+table);
    }
    private static Map<String,String> untouched(Connection c) throws Exception {
        var tables=new ArrayList<String>();
        try(var s=c.createStatement();var r=s.executeQuery("select table_name from information_schema.tables where table_schema=database()")) {
            while(r.next()) {String t=r.getString(1);if(!t.matches("[a-z_]+")) throw new IllegalStateException("Unexpected table");if(!COLUMNS.containsKey(t)) tables.add(t);}
        }
        if(tables.size()!=22) throw new IllegalStateException("Unexpected schema tables");
        var result=new TreeMap<String,String>();
        for(String t:tables) {
            var rows=new ArrayList<String>();
            try(var s=c.createStatement();var r=s.executeQuery("select * from "+t)) {
                while(r.next()) {var row=new StringBuilder();for(int i=1;i<=r.getMetaData().getColumnCount();i++) {byte[] v=r.getBytes(i);row.append(v==null?"~":Base64.getEncoder().encodeToString(v)).append('\t');}rows.add(row.toString());}
            }
            Collections.sort(rows);result.put(t,hash(String.join("\n",rows).getBytes(StandardCharsets.UTF_8)));
        }
        return result;
    }
    private static String hash(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static String required(String key) { return Objects.requireNonNull(System.getenv(key),"Missing "+key); }
}
