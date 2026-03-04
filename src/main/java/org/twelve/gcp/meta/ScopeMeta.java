package org.twelve.gcp.meta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only snapshot of a single scope in the AST.
 * <p>
 * Each scope has a source range [{@link #startOffset()}, {@link #endOffset()}],
 * a parent scope (null for the root), and a list of symbols defined directly
 * within it.  Used by {@link ModuleMeta#scopeAt(long)} and
 * {@link ModuleMeta#resolve(String, long)} for position-aware symbol resolution.
 */
public final class ScopeMeta {

    private final long scopeId;
    private final long startOffset;
    private final long endOffset;
    private final Long parentScopeId;
    private final List<SymbolMeta> symbols;

    ScopeMeta(long scopeId, long startOffset, long endOffset,
              Long parentScopeId, List<SymbolMeta> symbols) {
        this.scopeId = scopeId;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.parentScopeId = parentScopeId;
        this.symbols = symbols != null ? symbols : List.of();
    }

    public long scopeId() { return scopeId; }
    public long startOffset() { return startOffset; }
    public long endOffset() { return endOffset; }
    public Long parentScopeId() { return parentScopeId; }

    /** Symbols defined directly in this scope (not inherited from parent). */
    public List<SymbolMeta> symbols() { return symbols; }

    public boolean contains(long offset) {
        return offset >= startOffset && offset <= endOffset;
    }

    public long length() { return endOffset - startOffset; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("scopeId", scopeId);
        m.put("start", startOffset);
        m.put("end", endOffset);
        if (parentScopeId != null) m.put("parentScopeId", parentScopeId);
        m.put("symbols", symbols.stream().map(SymbolMeta::toMap).toList());
        return m;
    }
}
