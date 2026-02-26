package org.twelve.gcp.exception;

/**
 * Enumerates all error codes in the GCP compiler/interpreter system.
 *
 * <p>Every enum constant carries a short, human-readable {@link #description()}
 * suitable for display in error messages and IDE diagnostics.
 */
public enum GCPErrCode {

    // ── Syntax / Structure ────────────────────────────────────────────────────
    NODE_AST_MISMATCH           ("internal: node belongs to a different AST"),
    UNREACHABLE_STATEMENT       ("unreachable code"),
    DUPLICATED_DEFINITION       ("duplicate definition"),

    // ── Type System ───────────────────────────────────────────────────────────
    OUTLINE_MISMATCH            ("type mismatch"),
    CONSTRUCT_CONSTRAINTS_FAIL  ("type constraint violated"),
    UNSUPPORTED_UNARY_OPERATION ("unsupported unary operation for this type"),
    ARGUMENT_MISMATCH           ("argument type does not match parameter type"),
    POLY_SUM_FAIL               ("polymorphic type resolution failed"),

    // ── Semantics ─────────────────────────────────────────────────────────────
    VARIABLE_NOT_DEFINED        ("variable is not defined"),
    MODULE_NOT_DEFINED          ("module is not defined"),
    FUNCTION_NOT_DEFINED        ("function is not defined"),
    NOT_A_FUNCTION              ("expression is not callable"),
    FIELD_NOT_FOUND             ("field not found in type"),
    NOT_INITIALIZED             ("variable used before initialization"),
    NOT_ASSIGNABLE              ("cannot assign to an immutable binding"),
    THIS_IS_NOT_ASSIGNABLE      ("'this' is not assignable"),

    // ── Control Flow ──────────────────────────────────────────────────────────
    CONDITION_IS_NOT_BOOL       ("condition must be a Bool expression"),
    POSSIBLE_ENDLESS_LOOP       ("possible infinite loop"),

    // ── Name Resolution ───────────────────────────────────────────────────────
    AMBIGUOUS_VARIABLE_REFERENCE("ambiguous variable reference"),
    AMBIGUOUS_DECLARATION       ("ambiguous declaration"),

    // ── Type Inference ────────────────────────────────────────────────────────
    INFER_ERROR                 ("type inference failed"),
    UNAVAILABLE_OUTLINE_ASSIGNMENT("cannot determine type for assignment"),
    DECLARED_CAN_NOT_BE_GENERIC ("declared type cannot be generic"),

    // ── Functions / References ────────────────────────────────────────────────
    AMBIGUOUS_RETURN            ("ambiguous return type"),
    FUNCTION_NOT_FOUND          ("function overload not found"),
    NOT_REFER_ABLE              ("expression is not referable"),
    REFERENCE_MIS_MATCH         ("reference type mismatch"),

    // ── Operators ─────────────────────────────────────────────────────────────
    UNARY_POSITION_MISMATCH     ("unary operator used in wrong position"),

    // ── Type Cast ─────────────────────────────────────────────────────────────
    TYPE_CAST_NEVER_SUCCEED     ("this type cast can never succeed"),

    // ── Collections / Arrays ──────────────────────────────────────────────────
    NOT_INTEGER                 ("array index must be an integer"),
    NOT_AN_ARRAY_OR_DICT        ("expression is not an array or dict"),
    UNPACK_INDEX_OVER_FLOW      ("unpack index out of range"),

    // ── ADT / Symbols ─────────────────────────────────────────────────────────
    INVALID_SYMBOL              ("invalid symbol"),
    NOT_ENTITY_INHERITED        ("type is not an entity subtype"),

    // ── Assignability ─────────────────────────────────────────────────────────
    NOT_BE_ASSIGNEDABLE         ("value cannot be assigned to this binding"),
    OUTLINE_NOT_FOUND           ("outline (type) not found"),
    UNAVAILABLE_THIS            ("'this' is not available in this context"),

    // ── Implementation ────────────────────────────────────────────────────────
    INTERPRETER_NOT_IMPLEMENTED ("interpreter support for this feature is not yet implemented"),
    PROJECT_FAIL                ("project compilation failed");

    // ─────────────────────────────────────────────────────────────────────────

    private final String description;

    GCPErrCode(String description) {
        this.description = description;
    }

    /** Short, human-readable description of the error. */
    public String description() {
        return description;
    }

    /** Returns the error category for grouping related errors. */
    public ErrorCategory getCategory() {
        return ErrorCategory.forError(this);
    }

    /** Checks if this error is recoverable (warnings or non-fatal errors). */
    public boolean isRecoverable() {
        return switch (this) {
            case POSSIBLE_ENDLESS_LOOP, UNREACHABLE_STATEMENT, TYPE_CAST_NEVER_SUCCEED -> true;
            default -> false;
        };
    }

    /** Categories for error classification. */
    public enum ErrorCategory {
        SYNTAX, TYPE_SYSTEM, SEMANTIC, CONTROL_FLOW, NAME_RESOLUTION, INFERENCE, SYSTEM;

        public static ErrorCategory forError(GCPErrCode code) {
            return switch (code) {
                case NODE_AST_MISMATCH, DUPLICATED_DEFINITION, UNREACHABLE_STATEMENT -> SYNTAX;
                case OUTLINE_MISMATCH, CONSTRUCT_CONSTRAINTS_FAIL, UNSUPPORTED_UNARY_OPERATION,
                     ARGUMENT_MISMATCH, POLY_SUM_FAIL -> TYPE_SYSTEM;
                case VARIABLE_NOT_DEFINED, MODULE_NOT_DEFINED, FUNCTION_NOT_DEFINED,
                     NOT_A_FUNCTION, FIELD_NOT_FOUND, NOT_INITIALIZED -> SEMANTIC;
                case CONDITION_IS_NOT_BOOL, POSSIBLE_ENDLESS_LOOP -> CONTROL_FLOW;
                case AMBIGUOUS_VARIABLE_REFERENCE, AMBIGUOUS_DECLARATION -> NAME_RESOLUTION;
                case INFER_ERROR, UNAVAILABLE_OUTLINE_ASSIGNMENT -> INFERENCE;
                default -> SYSTEM;
            };
        }
    }
}