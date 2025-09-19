package com.example.nl2sql.agents;

import com.example.nl2sql.core.a2a.A2AEnvelope;
import com.example.nl2sql.core.agent.AgentRegistry;
import com.example.nl2sql.core.intent.Intent;
import com.example.nl2sql.core.query.LogicalMetricQuery;
import com.example.nl2sql.core.semantic.SemanticModels;
import com.example.nl2sql.semantic.RegistryLoader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.example.nl2sql.core.observability.A2AEvent;
import com.example.nl2sql.core.observability.EventBus;
import com.example.nl2sql.core.observability.InMemoryEventBus;

public class HttpGateway {
  private final ObjectMapper mapper = new ObjectMapper();
  private final AgentRegistry registry;
  private final InMemoryEventBus eventBus;
  private final SemanticModels.Registry registryModel;
  private final RegistryLoader registryLoader;
  private final Path registryFile;

  public HttpGateway(AgentRegistry registry) {
    this.registry = registry;
    this.eventBus = null;
    this.registryModel = null;
    this.registryLoader = null;
    this.registryFile = null;
  }

  public HttpGateway(AgentRegistry registry, InMemoryEventBus eventBus) {
    this.registry = registry;
    this.eventBus = eventBus;
    this.registryModel = null;
    this.registryLoader = null;
    this.registryFile = null;
  }

  public HttpGateway(AgentRegistry registry, InMemoryEventBus eventBus, SemanticModels.Registry registryModel) {
    this.registry = registry;
    this.eventBus = eventBus;
    this.registryModel = registryModel;
    this.registryLoader = null;
    this.registryFile = null;
  }

  public HttpGateway(AgentRegistry registry, InMemoryEventBus eventBus, SemanticModels.Registry registryModel, RegistryLoader loader, Path file) {
    this.registry = registry;
    this.eventBus = eventBus;
    this.registryModel = registryModel;
    this.registryLoader = loader;
    this.registryFile = file;
  }

  public void start(int port) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext("/a2a", new A2AHandler());
    server.createContext("/query", new QueryHandler());
    server.createContext("/chat", new ChatHandler());
    server.createContext("/semantic-registry", new SemanticRegistryHandler());
    if (eventBus != null) server.createContext("/events", new EventsHandler());
    server.setExecutor(null);
    server.start();
    System.out.println("HTTP A2A gateway listening on http://localhost:" + port);
  }

  class A2AHandler implements HttpHandler {
    @Override public void handle(HttpExchange ex) throws IOException {
      addCors(ex);
      if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { respondNoContent(ex); return; }
      if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
      try (InputStream is = ex.getRequestBody()) {
        // Generic envelope with Object payload
        A2AEnvelope<?> env = mapper.readValue(is, new TypeReference<A2AEnvelope<Object>>() {});
        // Best-effort payload shaping for known agents
        Object shaped = shapePayloadFor(env.getTo(), env.getPayload());
        @SuppressWarnings({"rawtypes","unchecked"})
        A2AEnvelope shapedEnv = new A2AEnvelope(env.getFrom(), env.getTo(), env.getType(), shaped);
        A2AEnvelope<?> resp = registry.send(shapedEnv);
        byte[] out = mapper.writeValueAsBytes(resp);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(200, out.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(out); }
      } catch (Exception e) {
        byte[] out = ("{\"error\":\"" + e.getMessage() + "\"}").getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(400, out.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(out); }
      }
    }
  }

  class QueryHandler implements HttpHandler {
    @Override public void handle(HttpExchange ex) throws IOException {
      addCors(ex);
      if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { respondNoContent(ex); return; }
      if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
      Map<String,Object> req = mapper.readValue(ex.getRequestBody(), new TypeReference<Map<String,Object>>(){});
      String text = String.valueOf(req.getOrDefault("text", ""));
      A2AEnvelope<?> resp = registry.send(new A2AEnvelope<>("http", OrchestratorAgent.NAME, A2AEnvelope.Type.request, text));
      byte[] out = mapper.writeValueAsBytes(resp.getPayload());
      ex.getResponseHeaders().add("Content-Type", "application/json");
      ex.sendResponseHeaders(200, out.length);
      try (OutputStream os = ex.getResponseBody()) { os.write(out); }
    }
  }

  class ChatHandler implements HttpHandler {
    @Override public void handle(HttpExchange ex) throws IOException {
      addCors(ex);
      if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { respondNoContent(ex); return; }
      if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
      Map<String,Object> req = mapper.readValue(ex.getRequestBody(), new TypeReference<Map<String,Object>>(){});
      String text = String.valueOf(req.getOrDefault("text", ""));
      // Orchestrate to get result
      A2AEnvelope<?> orchResp = registry.send(new A2AEnvelope<>("http", OrchestratorAgent.NAME, A2AEnvelope.Type.request, text));
      // Also compute SQL for display
      A2AEnvelope<?> ipResp = registry.send(new A2AEnvelope<>("http", IntentParserAgent.NAME, A2AEnvelope.Type.request, text));
      A2AEnvelope<?> lmqResp = registry.send(new A2AEnvelope<>("http", MetricResolverAgent.NAME, A2AEnvelope.Type.request, ipResp.getPayload()));
      A2AEnvelope<?> sqlResp = registry.send(new A2AEnvelope<>("http", SqlCompilerAgent.NAME, A2AEnvelope.Type.request, lmqResp.getPayload()));
      Map<String,Object> outObj = new HashMap<>();
      outObj.put("result", orchResp.getPayload());
      outObj.put("intent", ipResp.getPayload());
      outObj.put("logicalQuery", lmqResp.getPayload());
      outObj.put("sql", sqlResp.getPayload());
      byte[] out = mapper.writeValueAsBytes(outObj);
      ex.getResponseHeaders().add("Content-Type", "application/json");
      ex.sendResponseHeaders(200, out.length);
      try (OutputStream os = ex.getResponseBody()) { os.write(out); }
    }
  }

  class EventsHandler implements HttpHandler {
    @Override public void handle(HttpExchange ex) throws IOException {
      addCors(ex);
      if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { respondNoContent(ex); return; }
      if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
      ex.getResponseHeaders().add("Content-Type", "text/event-stream");
      ex.getResponseHeaders().add("Cache-Control", "no-cache");
      ex.sendResponseHeaders(200, 0);
      OutputStream os = ex.getResponseBody();
      EventBus.Subscription sub = eventBus.subscribe(event -> {
        try {
          byte[] data = ("data: " + toJson(event) + "\n\n").getBytes(StandardCharsets.UTF_8);
          os.write(data);
          os.flush();
        } catch (IOException ignored) {}
      });
      // keep open until client disconnects; if they disconnect, close subscription
      ex.getHttpContext().getServer().createContext("/noop", http -> {}); // no-op to keep reference
      // this thread will end when client disconnects; ensure we clean up
      try { /* block by not closing */ } finally { sub.close(); os.close(); }
    }
    private String toJson(A2AEvent e) {
      try { return mapper.writeValueAsString(e); } catch (Exception ex) { return "{}"; }
    }
  }

  class SemanticRegistryHandler implements HttpHandler {
    @Override public void handle(HttpExchange ex) throws IOException {
      addCors(ex);
      if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { respondNoContent(ex); return; }
      if (registryModel == null) { ex.sendResponseHeaders(503, -1); return; }
      switch (ex.getRequestMethod()) {
        case "GET":
          byte[] out = mapper.writeValueAsBytes(registryModel);
          ex.getResponseHeaders().add("Content-Type", "application/json");
          ex.sendResponseHeaders(200, out.length);
          try (OutputStream os = ex.getResponseBody()) { os.write(out); }
          break;
        case "PUT":
          Map<String,Object> body = mapper.readValue(ex.getRequestBody(), new TypeReference<Map<String,Object>>(){});
          // Replace contents in-place to keep references alive in agents
          SemanticModels.Registry newReg = mapper.convertValue(body, SemanticModels.Registry.class);
          synchronized (registryModel) {
            registryModel.getEntities().clear();
            registryModel.getEntities().addAll(newReg.getEntities());
            registryModel.getMetrics().clear();
            registryModel.getMetrics().addAll(newReg.getMetrics());
          }
          // Persist to file if configured
          if (registryLoader != null && registryFile != null) {
            try {
              registryLoader.saveToFile(registryFile, registryModel);
            } catch (RuntimeException re) {
              byte[] err = ("{\"error\":\"persist_failed: " + re.getMessage() + "\"}").getBytes(StandardCharsets.UTF_8);
              ex.getResponseHeaders().add("Content-Type", "application/json");
              ex.sendResponseHeaders(500, err.length);
              try (OutputStream os = ex.getResponseBody()) { os.write(err); }
              return;
            }
          }
          ex.sendResponseHeaders(204, -1);
          break;
        default:
          ex.sendResponseHeaders(405, -1);
      }
    }
  }

  private Object shapePayloadFor(String to, Object payload) {
    if (to == null) return payload;
    switch (to) {
      case MetricResolverAgent.NAME:
        return mapper.convertValue(payload, Intent.class);
      case SqlCompilerAgent.NAME:
        return mapper.convertValue(payload, LogicalMetricQuery.class);
      default:
        return payload;
    }
  }

  private void addCors(HttpExchange ex) {
    ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,PUT,OPTIONS");
    ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
  }
  private void respondNoContent(HttpExchange ex) throws IOException {
    ex.sendResponseHeaders(204, -1);
  }

  public static void main(String[] args) throws Exception {
    // Load semantic registry (from file if present, else classpath)
    RegistryLoader loader = new RegistryLoader();
    String fileProp = System.getProperty("registryFile", System.getenv().getOrDefault("REGISTRY_FILE", "semantic-registry.yml"));
    Path regFile = Path.of(fileProp);
    SemanticModels.Registry registryModel;
    if (Files.exists(regFile)) {
      registryModel = loader.loadFromFile(regFile);
      System.out.println("Loaded semantic registry from file: " + regFile.toAbsolutePath());
    } else {
      registryModel = loader.loadFromClasspath("semantic-registry.yml");
      System.out.println("Loaded semantic registry from classpath resource semantic-registry.yml; will persist to: " + regFile.toAbsolutePath());
    }

    // Wire agents in-process
    InMemoryEventBus bus = new InMemoryEventBus();
    AgentRegistry ar = new AgentRegistry(bus);
    var orchestrator = new OrchestratorAgent(ar);
    var dc = new DomainClassifierAgent();
    var ip = new IntentParserAgent(ar);
    var mr = new MetricResolverAgent(registryModel);
    var sc = new SqlCompilerAgent(registryModel, "postgres");
    var db = new DbExecutorAgent();
    var tool = new ToolAgent();
    // Register LLM tool with registry context for better prompts (overrides default)
    String googleKey = System.getenv("GOOGLE_API_KEY");
    if (googleKey != null && !googleKey.isBlank()) {
      tool.register(new com.example.nl2sql.agents.tools.LlmTool(
          new com.example.nl2sql.core.llm.GeminiProLlmClient(googleKey),
          registryModel
      ));
    } else {
      tool.register(new com.example.nl2sql.agents.tools.LlmTool(
          new com.example.nl2sql.core.llm.MockLlmClient(),
          registryModel
      ));
    }

    ar.register(orchestrator);
    ar.register(dc);
    ar.register(ip);
    ar.register(mr);
    ar.register(sc);
    ar.register(db);
    ar.register(tool);

    int port = Integer.parseInt(System.getProperty("port", "8080"));
    new HttpGateway(ar, bus, registryModel, loader, regFile).start(port);
  }
}
