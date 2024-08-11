package com.example.dop;

import com.example.dop.domain.Story;
import com.example.dop.util.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

public class OkHttpClinetTest {

    static Try<String> tryGetSearchResult(String url) {
        return Try.of(() -> OkHttpClientUtils.get(url));
    }

    @Test
    public void test() {

        String searchItem = "react";

        var result = Optional.ofNullable(searchItem)
                .map(s -> String.format("https://hn.algolia.com/api/v1/search?query=%s", s))
                .map(OkHttpClinetTest::tryGetSearchResult)
                .map(r -> r.map(data -> data))
                .map(rr -> switch (rr) {
                    case Success data -> data.getResult();
                    case Failure error -> error.getError();
                });

        System.out.println(result);
    }

    @Test
    public void test2() throws IOException {
        var response = Optional.ofNullable(OkHttpClientUtils.get("https://hn.algolia.com/api/v1/search?query=react"));

        if (response.isPresent()) {
            System.out.println(response.get());
            var result = JsonUtils.fromJson(response.get(), Story.class);
            System.out.println(result);
        } else {
            Assertions.fail();
        }
    }
}
