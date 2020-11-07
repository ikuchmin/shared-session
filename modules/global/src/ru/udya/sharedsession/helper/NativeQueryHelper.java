package ru.udya.sharedsession.helper;

import io.vavr.collection.Stream;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component(NativeQueryHelper.NAME)
public class NativeQueryHelper {

    public static final String NAME = "ss_NativeQueryHelper";

    public String generateArrayParameter(Integer count) {
        var elements = Stream.range(0, count)
                .map(i -> "?")
                .collect(Collectors.joining(","));

        return "(" + elements + ")";
    }
}
