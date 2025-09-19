package com.example.nl2sql.core.intent;

public class Filter {
  private String field;
  private String op;
  private String value;

  public Filter() {}
  public Filter(String field, String op, String value) {
    this.field = field; this.op = op; this.value = value;
  }
  public String getField() { return field; }
  public void setField(String field) { this.field = field; }
  public String getOp() { return op; }
  public void setOp(String op) { this.op = op; }
  public String getValue() { return value; }
  public void setValue(String value) { this.value = value; }
}
