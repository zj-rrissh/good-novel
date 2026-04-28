package com.ainovel.cache.support;

import java.util.Optional;

public record CacheLookup<T>(boolean hit, Optional<T> value) {

    public static <T> CacheLookup<T> miss() {
        return new CacheLookup<>(false, Optional.empty());
    }

    public static <T> CacheLookup<T> hit(T value) {
        return new CacheLookup<>(true, Optional.ofNullable(value));
    }

    public static <T> CacheLookup<T> hitNull() {
        return new CacheLookup<>(true, Optional.empty());
    }
}
