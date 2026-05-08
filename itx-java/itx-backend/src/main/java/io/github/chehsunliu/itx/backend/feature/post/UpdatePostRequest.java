package io.github.chehsunliu.itx.backend.feature.post;

import java.util.List;

public record UpdatePostRequest(String title, String body, List<String> tags) {}
