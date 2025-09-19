package com.example.nl2sql.core.semantic;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

public class SemanticModels {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Field {
    private String name;
    private String type;
    private String description; // optional
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Entity {
    private String name;
    private String table;
    private String pk;
    private String description; // optional
    private List<Field> fields = new ArrayList<>();
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTable() { return table; }
    public void setTable(String table) { this.table = table; }
    public String getPk() { return pk; }
    public void setPk(String pk) { this.pk = pk; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<Field> getFields() { return fields; }
    public void setFields(List<Field> fields) { this.fields = fields; }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Metric {
    private String name;
    private String entity; // legacy single-entity, optional
    private List<String> entities = new ArrayList<>(); // optional multi-entity
    private String expression;
    private String timeField;
    private List<String> dimensions = new ArrayList<>();
    private List<String> relations = new ArrayList<>(); // optional explicit join paths
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }
    public List<String> getEntities() { return entities; }
    public void setEntities(List<String> entities) { this.entities = entities; }
    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }
    public String getTimeField() { return timeField; }
    public void setTimeField(String timeField) { this.timeField = timeField; }
    public List<String> getDimensions() { return dimensions; }
    public void setDimensions(List<String> dimensions) { this.dimensions = dimensions; }
    public List<String> getRelations() { return relations; }
    public void setRelations(List<String> relations) { this.relations = relations; }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Registry {
    private List<Entity> entities = new ArrayList<>();
    private List<Metric> metrics = new ArrayList<>();
    public List<Entity> getEntities() { return entities; }
    public void setEntities(List<Entity> entities) { this.entities = entities; }
    public List<Metric> getMetrics() { return metrics; }
    public void setMetrics(List<Metric> metrics) { this.metrics = metrics; }

    public Entity findEntity(String name) {
      return entities.stream().filter(e -> e.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
    public Metric findMetric(String name) {
      return metrics.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
  }
}
