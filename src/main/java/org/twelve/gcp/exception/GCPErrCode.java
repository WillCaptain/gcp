package org.twelve.gcp.exception;

/**
 * Enumerates all error codes in the GCP compiler/interpreter system.
 *
 * <p>Errors are categorized by phase and severity for better handling.
 * Each code should have a corresponding user-friendly message in resources.
 */
public enum GCPErrCode {
    // --- Syntax/Structure Errors ---
    /**
     * Attempted to connect nodes from different ASTs
     */
    NODE_AST_MISMATCH,

    /**
     * Unreachable code detected during control flow analysis
     */
    UNREACHABLE_STATEMENT,

    /**
     * Duplicate definition of variable/function/type
     */
    DUPLICATED_DEFINITION,

    // --- Type System Errors ---
    /**
     * Type mismatch during assignment or operation
     */
    OUTLINE_MISMATCH,

    /**
     * Type mismatch specifically in GCP operations
     */
    CONSTRUCT_CONSTRAINTS_FAIL,

    /**
     * Operation cannot be performed on the given type
     */
    UNSUPPORTED_UNARY_OPERATION,

    /**
     * Argument type doesn't match parameter type
     */
    ARGUMENT_MISMATCH,

    /**
     * Polymorphic type resolution failed
     */
    POLY_SUM_FAIL,

    // --- Semantic Errors ---
    /**
     * Reference to undefined variable
     */
    VARIABLE_NOT_DEFINED,

    /**
     * Reference to undefined module
     */
    MODULE_NOT_DEFINED,

    /**
     * Reference to undefined function
     */
    FUNCTION_NOT_DEFINED,

    /**
     * Attempt to call non-function value
     */
    NOT_A_FUNCTION,

    /**
     * Field not found in type
     */
    FIELD_NOT_FOUND,

    /**
     * Variable used before initialization
     */
    NOT_INITIALIZED,

    /**
     * Assignment to non-assignable target (e.g., constant)
     */
    NOT_ASSIGNABLE,

    /**
     * Illegal assignment to 'this' reference
     */
    THIS_IS_NOT_ASSIGNABLE,

    // --- Control Flow Errors ---
    /**
     * Condition expression is not boolean
     */
    CONDITION_IS_NOT_BOOL,

    /**
     * Potential infinite loop detected
     */
    POSSIBLE_ENDLESS_LOOP,

    // --- Name Resolution Errors ---
    /**
     * Ambiguous reference to variable
     */
    AMBIGUOUS_VARIABLE_REFERENCE,

    // --- Type Inference Errors ---
    /**
     * Type inference failed
     */
    INFER_ERROR,

    /**
     * Could not determine type for assignment
     */
    UNAVAILABLE_OUTLINE_ASSIGNMENT,

    // --- Implementation Limitations ---
    /**
     * Feature not yet implemented in interpreter
     */
    INTERPRETER_NOT_IMPLEMENTED,

    // --- System Errors ---
    /**
     * Critical project compilation failure
     */
    PROJECT_FAIL,

    /**
     * Unary operator position mismatch
     */
    UNARY_POSITION_MISMATCH, AMBIGUOUS_RETURN, UNAVAILABLE_THIS, FUNCTION_NOT_FOUND, TYPE_CAST_NEVER_SUCCEED, NOT_BE_ASSIGNEDABLE, OUTLINE_NOT_FOUND;

    /**
     * Returns the error category for grouping related errors.
     */
    public ErrorCategory getCategory() {
        return ErrorCategory.forError(this);
    }

    /**
     * Checks if this error is recoverable (warnings or non-fatal errors).
     */
    public boolean isRecoverable() {
        return switch (this) {
            case POSSIBLE_ENDLESS_LOOP, UNREACHABLE_STATEMENT, TYPE_CAST_NEVER_SUCCEED -> true;
            default -> false;
        };
    }

    /**
     * Categories for error classification.
     */
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
                case AMBIGUOUS_VARIABLE_REFERENCE -> NAME_RESOLUTION;
                case INFER_ERROR, UNAVAILABLE_OUTLINE_ASSIGNMENT -> INFERENCE;
                default -> SYSTEM;
            };
        }
    }
}