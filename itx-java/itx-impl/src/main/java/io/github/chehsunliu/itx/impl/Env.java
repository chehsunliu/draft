package io.github.chehsunliu.itx.impl;

public final class Env {
  private Env() {}

  public static String requireEnv(String name) {
    String v = System.getenv(name);
    if (v == null) throw new IllegalStateException("missing env var: " + name);
    return v;
  }

  public static int envInt(String name, int defaultValue) {
    String v = System.getenv(name);
    if (v == null || v.isBlank()) return defaultValue;
    return Integer.parseInt(v);
  }
}
