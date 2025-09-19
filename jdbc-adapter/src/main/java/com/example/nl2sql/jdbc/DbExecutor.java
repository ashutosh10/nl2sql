package com.example.nl2sql.jdbc;

import com.example.nl2sql.core.query.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// MVP: deterministic mock. TODO: implement JDBC execution with DataSource config.
public class DbExecutor {
  private static final Logger log = LoggerFactory.getLogger(DbExecutor.class);

  public QueryResult execute(String sql) {
    log.info("Executing SQL (mock): {}", sql);
    // Return deterministic mock result
    return QueryResult.single(new String[]{"deployment_frequency"}, new Object[]{42});
  }
}
