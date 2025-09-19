package com.example.nl2sql.agents.tools;

import com.example.nl2sql.core.tool.Tool;

public class EmbeddingsTool implements Tool {
  @Override public String name() { return "embeddings"; }

  @Override public Object call(String method, Object args) throws Exception {
    String m = method == null ? "" : method.toLowerCase();
    switch (m) {
      case "embed":
        String text = String.valueOf(args);
        return new int[]{text.length(), text.hashCode()};
      default:
        return "unsupported_method:" + method;
    }
  }
}
