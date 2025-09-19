package com.example.nl2sql.core.tool;

public interface Tool {
  String name();
  Object call(String method, Object args) throws Exception;
}
