package com.finance.eclipse.suggestion.context;

import java.util.Objects;
import org.eclipse.core.runtime.CoreException;
import com.finance.eclipse.suggestion.utils.LambdaExceptionUtils.Supplier_WithExceptions;

public final class ContextEntryFactory {

    private final String prefix;
    private final Supplier_WithExceptions<ContextEntry, CoreException> supplier;

    // 생성자 (Canonical Constructor)
    public ContextEntryFactory(String prefix, Supplier_WithExceptions<ContextEntry, CoreException> supplier) {
        this.prefix = prefix;
        this.supplier = supplier;
    }

    // Getter 메서드 (Record 스타일의 이름을 유지하거나 getPrefix 형태로 변경 가능)
    public String prefix() {
        return prefix;
    }

    public Supplier_WithExceptions<ContextEntry, CoreException> supplier() {
        return supplier;
    }

    // equals 구현
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContextEntryFactory that = (ContextEntryFactory) o;
        return Objects.equals(prefix, that.prefix) && 
               Objects.equals(supplier, that.supplier);
    }

    // hashCode 구현
    @Override
    public int hashCode() {
        return Objects.hash(prefix, supplier);
    }

    // toString 구현
    @Override
    public String toString() {
        return "ContextEntryFactory[" +
                "prefix=" + prefix + ", " +
                "supplier=" + supplier +
                ']';
    }
}