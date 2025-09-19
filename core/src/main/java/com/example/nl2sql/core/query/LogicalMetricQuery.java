package com.example.nl2sql.core.query;

import java.util.ArrayList;
import java.util.List;

public class LogicalMetricQuery {
  private String entity;              // root entity/table
  private String metricExpr;          // e.g., COUNT(*)
  private List<String> joins = new ArrayList<>(); // textual "from -> to on ..." for MVP
  private List<String> select = new ArrayList<>();
  private List<String> where = new ArrayList<>();
  private List<String> groupBy = new ArrayList<>();
  private List<String> orderBy = new ArrayList<>();

  public String getEntity() { return entity; }
  public void setEntity(String entity) { this.entity = entity; }
  public String getMetricExpr() { return metricExpr; }
  public void setMetricExpr(String metricExpr) { this.metricExpr = metricExpr; }
  public List<String> getJoins() { return joins; }
  public void setJoins(List<String> joins) { this.joins = joins; }
  public List<String> getSelect() { return select; }
  public void setSelect(List<String> select) { this.select = select; }
  public List<String> getWhere() { return where; }
  public void setWhere(List<String> where) { this.where = where; }
  public List<String> getGroupBy() { return groupBy; }
  public void setGroupBy(List<String> groupBy) { this.groupBy = groupBy; }
  public List<String> getOrderBy() { return orderBy; }
  public void setOrderBy(List<String> orderBy) { this.orderBy = orderBy; }
}
