package life.catalogue.postgres;

import life.catalogue.common.io.UTF8IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.postgresql.jdbc.PgConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;

public class PgCopyUtils {
  private static final Logger LOG = LoggerFactory.getLogger(PgCopyUtils.class);
  private static final Joiner HEADER_JOINER = Joiner.on(",");
  private static final String[] EMPTY_ARRAY = new String[0];
  
  public static long copy(PgConnection con, String table, String resourceName) throws IOException, SQLException {
    return copy(con, table, resourceName, Collections.emptyMap(), null, "");
  }
  
  public static long copy(PgConnection con, String table, String resourceName, Map<String, Object> defaults) throws IOException, SQLException {
    return copy(con, table, resourceName, defaults, null, "");
  }
  
  public static long copy(PgConnection con, String table, String resourceName,
                          Map<String, Object> defaults,
                          Map<String, Function<String[], String>> funcs) throws IOException, SQLException {
    return copy(con, table, resourceName, defaults, funcs, "");
  }

  public static long copy(PgConnection con, String table, String resourceName,
                          Map<String, Object> defaults,
                          Map<String, Function<String[], String>> funcs,
                          String nullValue) throws IOException, SQLException {
    return copy(con, table, PgCopyUtils.class.getResourceAsStream(resourceName), defaults, funcs, nullValue);
  }

  /**
   * @param csv input stream of CSV file with header rows being the column names in the postgres table
   */
  public static long copy(PgConnection con, String table, InputStream csv,
                          Map<String, Object> defaults,
                          Map<String, Function<String[], String>> funcs,
                          String nullValue) throws IOException, SQLException {
    con.setAutoCommit(false);
    CopyManager copy = ((PGConnection)con).getCopyAPI();
    con.commit();

    LOG.info("Copy to table {}", table);
    HeadlessStream in = new HeadlessStream(csv, defaults, funcs);
    // use quotes to avoid problems with reserved words, e.g. group
    String header = HEADER_JOINER.join(in.header.stream().map(h -> "\"" + h + "\"").collect(Collectors.toList()));
    long cnt = copy.copyIn("COPY " + table + "(" + header + ") FROM STDOUT WITH CSV NULL '"+nullValue+"'", in);

    con.commit();
    return cnt;
  }
  
  /**
   * @return parses a postgres array given as <pre>{Duméril,Bibron}</pre>
   */
  public static String[] splitPgArray(String x) {
    return x == null ? EMPTY_ARRAY : x.substring(1, x.length()-1).split(",");
  }
  
  public static String buildPgArray(String[] x) {
    if (x == null) {
      return null;
    }
    return "{" + HEADER_JOINER.join(x) + "}";
  }

  static class HeadlessStream extends InputStream {
    private final static char lineend = '\n';
    private final CsvParser parser;
    private final CsvWriter writer;
    private final List<String> header;
    private final String[] defaultValues;
    private final List<Function<String[], String>> funcs;
    private byte[] bytes;
    private int idx;

    public HeadlessStream(InputStream in, Map<String, Object> defaults, Map<String, Function<String[], String>> funcs) throws IOException {
      CsvParserSettings cfg = new CsvParserSettings();
      cfg.setDelimiterDetectionEnabled(false);
      cfg.setQuoteDetectionEnabled(false);
      cfg.setReadInputOnSeparateThread(false);
      cfg.setSkipEmptyLines(true);
      cfg.setNullValue(null);
      cfg.setMaxColumns(128);
      cfg.setMaxCharsPerColumn(1024 * 128);
      parser = new CsvParser(cfg);
      parser.beginParsing(in, StandardCharsets.UTF_8);
      
      CsvWriterSettings cfg2 = new CsvWriterSettings();
      cfg2.setQuoteEscapingEnabled(true);
      writer = new CsvWriter(cfg2);
      
      header        = Lists.newArrayList(parser.parseNext());
      defaultValues = parseDefaults(defaults);
      this.funcs = parseFuncs(funcs);
      next();
    }

    private List<Function<String[], String>> parseFuncs(Map<String, Function<String[], String>> calculators) {
      if (calculators == null) {
        return null;
      }
      List<Function<String[], String>> funcs = new ArrayList<>();
      for (Map.Entry<String, Function<String[], String>> f : calculators.entrySet()) {
        header.add(f.getKey());
        funcs.add(f.getValue());
      }
      return funcs;
    }
  
    @Override
    public int read() throws IOException {
      if (bytes != null && idx == bytes.length) {
        next();
        return lineend;
      }
      if (bytes == null) {
        return -1;
      }
      return bytes[idx++];
    }

    /**
     * Adds default values as new columns (header & data), but only if the column is not already existing
     */
    private String[] parseDefaults(Map<String, Object> defaults) {
      if (defaults==null || defaults.isEmpty()) {
        return null;
      }
      
      List<String> values = new ArrayList<>();
      for (Map.Entry<String, Object> col : defaults.entrySet()) {
        if (header.contains(col.getKey())) {
          LOG.debug("Default column {} already exists. Ignore default {}.", col.getKey(), col.getValue());

        } else {
          header.add(col.getKey());
          Object val = col.getValue();
          if (val == null) {
            // empty string
            values.add(null);
          } else if (val.getClass().isEnum()) {
            values.add(((Enum) val).name());
          } else {
            values.add(val.toString());
          }
        }
      }
      LOG.debug("Convert defaults {} to value columns {}", defaults, values.toString());
      return values.toArray(new String[0]);
    }
  
    private boolean next() throws IOException {
      String[] line = parser.parseNext();
      if (line == null || line.length == 0) {
        bytes = null;
        return false;
      }
      // add defaults
      line = ArrayUtils.addAll(line, defaultValues);
      
      // add calculated values
      if (funcs != null) {
        String[] calcVals = new String[funcs.size()];
        for (int x = 0; x<funcs.size(); x++) {
          calcVals[x] = funcs.get(x).apply(line);
        }
        line = ArrayUtils.addAll(line, calcVals);
      }

      // serialize row as char array
      String x = writer.writeRowToString(line);
      bytes = x.getBytes(StandardCharsets.UTF_8);
      idx = 0;
      return true;
    }
  
    @Override
    public void close() throws IOException {
      parser.stopParsing();
      writer.close();
    }
  }

  /**
   * Uses pg copy to write a select statement to a TSV file with headers encoded in UTF8 using an empty string for NULL values
   * @param sql select statement
   * @param out file to write to
   */
  public static void dumpCSV(PgConnection con, String sql, File out) throws IOException, SQLException {
    dump(con, sql, out, "CSV HEADER NULL '' ENCODING 'UTF8'");
  }

  /**
   * Uses pg copy to write a select statement to a TSV file with headers encoded in UTF8 using an empty string for NULL values
   * @param sql select statement
   * @param out file to write to
   */
  public static void dumpTSV(PgConnection con, String sql, File out) throws IOException, SQLException {
    dump(con, sql, out, "CSV HEADER NULL '' DELIMITER E'\t' QUOTE E'\f' ENCODING 'UTF8'");
  }

  /**
   * Uses pg copy to write a select statement to a UTF8 text file.
   * @param sql select statement
   * @param out file to write to
   * @param with with clause for the copy command. Example: CSV HEADER NULL ''
   */
  public static void dump(PgConnection con, String sql, File out, String with) throws IOException, SQLException {
    con.setAutoCommit(false);
    
    try (Writer writer = UTF8IoUtils.writerFromFile(out)) {
      CopyManager copy = con.getCopyAPI();
      copy.copyOut("COPY (" + sql + ") TO STDOUT WITH "+with, writer);
    }
  }
}
