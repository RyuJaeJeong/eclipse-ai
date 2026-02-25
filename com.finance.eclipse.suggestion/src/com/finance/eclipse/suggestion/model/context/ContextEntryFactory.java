package com.finance.eclipse.suggestion.model.context;

import org.eclipse.core.runtime.CoreException;

import com.finance.eclipse.suggestion.utils.LambdaExceptionUtils.Supplier_WithExceptions;

public record ContextEntryFactory(String prefix, Supplier_WithExceptions<ContextEntry, CoreException> supplier) {}
