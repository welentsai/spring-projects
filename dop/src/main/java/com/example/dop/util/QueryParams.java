package com.example.dop.util;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

public class QueryParams {
    private final MultiValueMap<String, String> params;

    private QueryParams(MultiValueMap<String, String> params) {
        this.params = params;
    }

    public static QueryParamsBuilder builder() {
        return new QueryParamsBuilder();
    }

    public MultiValueMap<String, String> toMultiValueMap() {
        return new LinkedMultiValueMap<>(params);
    }

    public static class QueryParamsBuilder {
        private final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        public QueryParamsBuilder add(String key, String value) {
            if (value != null) {
                params.add(key, value);
            }
            return this;
        }

        public QueryParamsBuilder add(String key, List<String> values) {
            if (values != null && !values.isEmpty()) {
                params.addAll(key, values);
            }
            return this;
        }

        public QueryParamsBuilder addNonNull(String key, Object value) {
            if (value != null) {
                params.add(key, value.toString());
            }
            return this;
        }

        public QueryParamsBuilder addAll(Map<String, String> parameters) {
            if (parameters != null) {
                parameters.forEach(this::add);
            }
            return this;
        }

        public QueryParams build() {
            return new QueryParams(params);
        }
    }
}
