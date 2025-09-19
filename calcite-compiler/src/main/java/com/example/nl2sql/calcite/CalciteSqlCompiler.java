package com.example.nl2sql.calcite;

import com.example.nl2sql.core.query.LogicalMetricQuery;
import com.example.nl2sql.core.semantic.SemanticModels;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;
import org.apache.calcite.sql.fun.SqlLibraryOperators;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.RelBuilder;

import java.util.*;

public class CalciteSqlCompiler {

  public String compile(LogicalMetricQuery q, SemanticModels.Registry registry, String dialect) {
    SchemaPlus root = Frameworks.createRootSchema(true);
    // Build simple in-memory schema from semantic registry
    addTables(root, registry);

    FrameworkConfig config = Frameworks.newConfigBuilder()
        .defaultSchema(root)
        .build();

    RelBuilder builder = RelBuilder.create(config);

    String base = q.getEntity();
    builder.scan(base).as(base);

    // LEFT JOINs - support explicit relations like "A -> B on A.col = B.col"
    Set<String> seen = new LinkedHashSet<>();
    for (String spec : q.getJoins()) {
      if (spec == null || spec.isBlank()) continue;
      if (!seen.add(spec)) continue;
      JoinSpec js = parseJoinSpec(spec);
      if (js == null) continue;
      // scan right side
      builder.scan(js.right).as(js.right);
      // Build join condition against two inputs (left cumulative input index 0, right index 1)
      RexNode lref = builder.field(2, 0, js.leftField);
      RexNode rref = builder.field(2, 1, js.rightField);
      RexNode cond = builder.call(SqlStdOperatorTable.EQUALS, lref, rref);
      builder.join(org.apache.calcite.rel.core.JoinRelType.LEFT, cond);
    }

    // Simple predicate pushdown: apply filters as early as possible (before grouping)
    if (!q.getWhere().isEmpty()) {
      List<RexNode> predicates = new ArrayList<>();
      for (String w : q.getWhere()) {
        RexNode pred = parsePredicate(w, builder);
        if (pred != null) predicates.add(pred);
      }
      if (!predicates.isEmpty()) builder.filter(predicates);
    }

    // Group by and aggregate COUNT(*)
    List<RexNode> groupExprs = new ArrayList<>();
    Map<String, Integer> aliasIndex = new LinkedHashMap<>();
    int idx = 0;
    for (String g : q.getGroupBy()) {
      RexNode expr = parseGroupExpr(g, base, builder);
      groupExprs.add(expr);
      aliasIndex.put(g, idx++);
    }

    RelBuilder.AggCall countAll = builder.count(false, "deployment_frequency");
    builder.aggregate(builder.groupKey(groupExprs), countAll);

    // Projection: include aggregated count and group expressions by index
    List<RexNode> projectExprs = new ArrayList<>();
    List<String> projAliases = new ArrayList<>();
    for (String sel : q.getSelect()) {
      String sl = sel.toLowerCase(Locale.ROOT).trim();
      if (sl.startsWith("count(*)")) {
        projectExprs.add(builder.field("deployment_frequency"));
        // extract alias if provided
        String alias = sl.contains(" as ") ? sel.substring(sl.indexOf(" as ") + 4).trim() : "deployment_frequency";
        projAliases.add(alias);
        continue;
      }
      String exprPart = sel;
      String alias = null;
      if (sl.contains(" as ")) {
        String[] pa = sel.split("(?i) as ");
        exprPart = pa[0].trim();
        alias = pa[1].trim();
      }
      Integer gIdx = aliasIndex.get(exprPart);
      if (gIdx != null) {
        projectExprs.add(builder.field(gIdx));
        projAliases.add(alias);
      } else {
        // fallback: try to parse simple column
        projectExprs.add(parseSelectExpr(exprPart, base, builder));
        projAliases.add(alias);
      }
    }
    if (!projectExprs.isEmpty()) builder.project(projectExprs, projAliases);

    // ORDER BY (support first item only for MVP, ascending)
    if (!q.getOrderBy().isEmpty()) {
      builder.sort(builder.field("deployment_frequency"));
    }

    RelNode rel = builder.build();

    // Render SQL via RelToSqlConverter (Postgres dialect)
    org.apache.calcite.rel.rel2sql.RelToSqlConverter converter =
        new org.apache.calcite.rel.rel2sql.RelToSqlConverter(PostgresqlSqlDialect.DEFAULT);
    String sql = converter.visitRoot(rel).asStatement().toSqlString(PostgresqlSqlDialect.DEFAULT).getSql();
    return sql;
  }

  private void addTables(SchemaPlus root, SemanticModels.Registry registry) {
    for (SemanticModels.Entity e : registry.getEntities()) {
      root.add(e.getName(), new AbstractTable() {
        @Override public RelDataType getRowType(RelDataTypeFactory factory) {
          RelDataTypeFactory.Builder b = new RelDataTypeFactory.Builder(factory);
          for (SemanticModels.Field f : e.getFields()) {
            String t = f.getType().toLowerCase(Locale.ROOT);
            RelDataType dt;
            switch (t) {
              case "bigint": dt = factory.createSqlType(org.apache.calcite.sql.type.SqlTypeName.BIGINT); break;
              case "timestamp": dt = factory.createSqlType(org.apache.calcite.sql.type.SqlTypeName.TIMESTAMP); break;
              case "string":
              default: dt = factory.createSqlType(org.apache.calcite.sql.type.SqlTypeName.VARCHAR);
            }
            b.add(f.getName(), dt);
          }
          return b.build();
        }
      });
    }
  }

  private static class JoinSpec {
    final String left; final String right; final String leftField; final String rightField;
    JoinSpec(String left, String right, String leftField, String rightField) {
      this.left = left; this.right = right; this.leftField = leftField; this.rightField = rightField;
    }
  }

  private JoinSpec parseJoinSpec(String spec) {
    // Expected: "LeftEntity -> RightEntity on LeftEntity.col = RightEntity.col"
    String s = spec.trim();
    String[] onSplit = s.split("(?i)\\s+on\\s+");
    String lrPart = onSplit[0];
    String onPart = onSplit.length > 1 ? onSplit[1] : null;
    String[] lr = lrPart.split("->");
    if (lr.length < 2) return null;
    String left = lr[0].trim();
    String right = lr[1].trim();
    String leftField = "id"; // default
    String rightField = "id";
    if (onPart != null && !onPart.isBlank()) {
      String[] eq = onPart.split("\\s*=\\s*");
      if (eq.length == 2) {
        String[] lq = eq[0].trim().split("\\.");
        String[] rq = eq[1].trim().split("\\.");
        leftField = lq.length == 2 ? lq[1].trim() : lq[0].trim();
        rightField = rq.length == 2 ? rq[1].trim() : rq[0].trim();
      }
    }
    return new JoinSpec(left, right, leftField, rightField);
  }

  private RexNode parsePredicate(String w, RelBuilder b) {
    // Support simple: qualified = 'value' | >= | <=
    String op = null;
    if (w.contains(" >= ")) op = ">=";
    else if (w.contains(" <= ")) op = "<=";
    else if (w.contains(" = ")) op = "=";
    if (op == null) return null;
    int pos = w.indexOf(" " + op + " ");
    if (pos < 0) return null;
    String left = w.substring(0, pos).trim();
    String right = w.substring(pos + op.length() + 2).trim();
    if (right.startsWith("'") && right.endsWith("'")) right = right.substring(1, right.length()-1);
    String[] qn = left.split("\\.");
    RexNode lref = qn.length == 2 ? b.field(qn[0], qn[1]) : b.field(qn[0]);
    RexNode rlit = b.literal(right);
    switch (op) {
      case "=": return b.call(SqlStdOperatorTable.EQUALS, lref, rlit);
      case ">=": return b.call(SqlStdOperatorTable.GREATER_THAN_OR_EQUAL, lref, rlit);
      case "<=": return b.call(SqlStdOperatorTable.LESS_THAN_OR_EQUAL, lref, rlit);
      default: return null;
    }
  }

  private RexNode parseGroupExpr(String g, String base, RelBuilder b) {
    String gl = g.toLowerCase(Locale.ROOT).trim();
    if (gl.startsWith("date_trunc('week")) {
      // Render as Postgres DATE_TRUNC('week', started_at)
      return b.call(SqlLibraryOperators.DATE_TRUNC, b.literal("week"), b.field(base, "started_at"));
    }
    if (g.contains(".")) {
      String[] qn = g.split("\\.");
      return b.field(qn[0], qn[1]);
    }
    return b.field(g);
  }

  private RexNode parseSelectExpr(String s, String base, RelBuilder b) {
    String sl = s.toLowerCase(Locale.ROOT).trim();
    if (sl.startsWith("date_trunc('week")) {
      return parseGroupExpr(s, base, b);
    }
    if (s.contains(".")) {
      String[] qn = s.split("\\.");
      return b.field(qn[0], qn[1]);
    }
    if (sl.equals("count(*)")) return b.field("deployment_frequency");
    return b.field(s);
  }
}
