package com.example.nl2sql.core.intent;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Intent {
  private String metric;
  private List<Filter> filters = new ArrayList<>();
  private List<String> groupBy = new ArrayList<>();
  private TimeRange timeRange;
  private String timeGrain; // e.g., day, week, month

  public String getMetric() { return metric; }
  public void setMetric(String metric) { this.metric = metric; }
  public List<Filter> getFilters() { return filters; }
  public void setFilters(List<Filter> filters) { this.filters = filters; }
  public List<String> getGroupBy() { return groupBy; }
  public void setGroupBy(List<String> groupBy) { this.groupBy = groupBy; }
  public TimeRange getTimeRange() { return timeRange; }
  public void setTimeRange(TimeRange timeRange) { this.timeRange = timeRange; }
  public String getTimeGrain() { return timeGrain; }
  public void setTimeGrain(String timeGrain) { this.timeGrain = timeGrain; }

  public static class TimeRange {
    private LocalDate start;
    private LocalDate end;
    public TimeRange() {}
    public TimeRange(LocalDate start, LocalDate end) { this.start = start; this.end = end; }
    public LocalDate getStart() { return start; }
    public void setStart(LocalDate start) { this.start = start; }
    public LocalDate getEnd() { return end; }
    public void setEnd(LocalDate end) { this.end = end; }
  }
}
