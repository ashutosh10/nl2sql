package com.example.nl2sql.core.query;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QueryResult {
  private List<String> columns = new ArrayList<>();
  private List<Map<String,Object>> rows = new ArrayList<>();

  public List<String> getColumns() { return columns; }
  public void setColumns(List<String> columns) { this.columns = columns; }
  public List<Map<String, Object>> getRows() { return rows; }
  public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }

  public static QueryResult single(String[] cols, Object[] values) {
    QueryResult r = new QueryResult();
    for (String c : cols) r.columns.add(c);
    Map<String,Object> m = new LinkedHashMap<>();
    for (int i=0;i<cols.length;i++) m.put(cols[i], values[i]);
    r.rows.add(m);
    return r;
  }
}
