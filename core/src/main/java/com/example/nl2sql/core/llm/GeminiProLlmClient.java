package com.example.nl2sql.core.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Minimal Gemini (Google Generative Language API) client implementing LlmClient.
 * Uses Java 11+ HttpClient and Jackson (already in project).
 *
 * Endpoint: https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key=API_KEY
 * Docs: https://ai.google.dev/gemini-api/docs/text-generation?lang=java
 */
public class GeminiProLlmClient implements LlmClient {
  private final String apiKey;
  private final String model; // e.g., "gemini-1.5-pro" or "gemini-pro"
  private final HttpClient http = HttpClient.newHttpClient();
  private final ObjectMapper mapper = new ObjectMapper();

  public GeminiProLlmClient(String apiKey) {
    this(apiKey, "gemini-1.5-pro");
  }

  public GeminiProLlmClient(String apiKey, String model) {
    if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("Gemini API key is required");
    if (model == null || model.isBlank()) model = "gemini-1.5-pro";
    this.apiKey = apiKey;
    this.model = model;
  }

  @Override
  public ChatResponse chat(List<ChatMessage> messages, ChatOptions options) throws Exception {
    if (messages == null || messages.isEmpty()) throw new IllegalArgumentException("messages required");

    // Prepare request body
    Map<String,Object> body = new LinkedHashMap<>();

    // Extract optional system message into system_instruction (first system message only)
    List<Map<String,Object>> contents = new ArrayList<>();
    boolean systemSet = false;
    for (ChatMessage m : messages) {
      if (!systemSet && m.getRole() == Role.system) {
        Map<String,Object> sys = new LinkedHashMap<>();
        sys.put("parts", List.of(Map.of("text", String.valueOf(m.getContent()))));
        body.put("system_instruction", sys);
        systemSet = true;
        continue; // don't include this in contents
      }
      String role = m.getRole() == Role.assistant ? "model" : "user"; // Gemini uses "user"/"model"
      contents.add(Map.of(
          "role", role,
          "parts", List.of(Map.of("text", String.valueOf(m.getContent())))
      ));
    }
    body.put("contents", contents);

    Map<String,Object> gen = new LinkedHashMap<>();
    if (options != null) {
      if (options.getTemperature() != null) gen.put("temperature", options.getTemperature());
      if (options.getMaxTokens() != null) gen.put("maxOutputTokens", options.getMaxTokens());
      if (options.isJsonMode()) {
        // Best-effort: request JSON responses (supported on 1.5 models)
        gen.put("response_mime_type", "application/json");
      }
    }
    if (!gen.isEmpty()) body.put("generationConfig", gen);

    String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
    String json = mapper.writeValueAsString(body);

    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
        .build();

    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (resp.statusCode() / 100 != 2) {
      throw new RuntimeException("Gemini API error: HTTP " + resp.statusCode() + " - " + resp.body());
    }

    // Response shape: { candidates: [ { content: { parts: [ { text: ... } ] } } ] }
    Map<String,Object> map = mapper.readValue(resp.body(), new TypeReference<Map<String,Object>>(){});
    String content = extractText(map);
    ChatResponse out = new ChatResponse(content);
    return out;
  }

  @SuppressWarnings("unchecked")
  private String extractText(Map<String,Object> map) {
    Object cands = map.get("candidates");
    if (!(cands instanceof List)) return "";
    List<Object> list = (List<Object>) cands;
    if (list.isEmpty()) return "";
    Object first = list.get(0);
    if (!(first instanceof Map)) return "";
    Map<String,Object> f = (Map<String,Object>) first;
    Object content = f.get("content");
    if (!(content instanceof Map)) return "";
    Map<String,Object> cont = (Map<String,Object>) content;
    Object parts = cont.get("parts");
    if (!(parts instanceof List)) return "";
    List<Object> pList = (List<Object>) parts;
    if (pList.isEmpty()) return "";
    Object p0 = pList.get(0);
    if (!(p0 instanceof Map)) return "";
    Map<String,Object> pm = (Map<String,Object>) p0;
    Object text = pm.get("text");
    return text == null ? "" : String.valueOf(text);
  }
}
