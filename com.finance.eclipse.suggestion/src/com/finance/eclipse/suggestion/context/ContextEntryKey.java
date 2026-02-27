package com.finance.eclipse.suggestion.context;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

public final class ContextEntryKey {
	
	// field
    private final String prefix;
    private final String value;

    // cons
    public ContextEntryKey(String prefix, String value) {
        this.prefix = prefix;
        this.value = value;
    }

    // method
    public String getKeyString() {
        return String.format("%s::%s",  this.prefix, Base64.getEncoder().encodeToString(this.value.getBytes(StandardCharsets.UTF_8)));
    }

    // 비즈니스 로직: Key 문자열 파싱 (Static Factory Method)
    public static Optional<ContextEntryKey> parseKeyString(String keyString) {
        if (keyString == null) {
            return Optional.empty();
        }
        
        final String[] parts = keyString.split("::");
        if (parts.length != 2) {
            return Optional.empty();
        }
        
        try {
            String decodedValue = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return Optional.of(new ContextEntryKey(parts[0], decodedValue));
        } catch (IllegalArgumentException e) {
            // Base64 디코딩 실패 시 안전하게 빈 Optional 반환
            return Optional.empty();
        }
    }

    // Getter (Record 스타일 유지)
    public String prefix() {
        return prefix;
    }

    public String value() {
        return value;
    }

    // 데이터 동등성 비교 (equals & hashCode)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContextEntryKey that = (ContextEntryKey) o;
        return Objects.equals(prefix, that.prefix) && 
               Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, value);
    }

    @Override
    public String toString() {
        return "ContextEntryKey[" +
                "prefix='" + prefix + '\'' +
                ", value='" + value + '\'' +
                ']';
    }
}