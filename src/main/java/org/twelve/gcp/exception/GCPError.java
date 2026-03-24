package org.twelve.gcp.exception;

import org.twelve.gcp.ast.Node;

public class GCPError {
    public enum Severity {
        ERROR("error"),
        WARNING("warning");

        private final String wireValue;

        Severity(String wireValue) {
            this.wireValue = wireValue;
        }

        public String wireValue() {
            return wireValue;
        }
    }

    private final GCPErrCode errorCode;
    private final Node node;
    private final String message;
    private final Severity severity;

    public GCPError(Node node, GCPErrCode errorCode, String message) {
        this(node, errorCode, message, errorCode != null ? errorCode.defaultSeverity() : Severity.ERROR);
    }

    public GCPError(Node node, GCPErrCode errorCode, String message, Severity severity) {
        this.errorCode = errorCode;
        this.node = node;
        this.message = message == null ? "" : message;
        this.severity = severity == null ? Severity.ERROR : severity;
    }

    public GCPError(Node node, String message, Severity severity) {
        this(node, null, message, severity);
    }

    public Node node() {
        return this.node;
    }

    public GCPErrCode errorCode() {
        return this.errorCode;
    }

    public String message(){
        return this.message;
    }

    public Severity severity() {
        return severity;
    }

    public String displayMessage() {
        if (errorCode != null) {
            return errorCode.description() + (message.isBlank() ? "" : ": " + message);
        }
        return message;
    }

    public String dedupKey() {
        if (errorCode != null) {
            return "code:" + errorCode.name();
        }
        return "severity:" + severity.name() + ":message:" + message;
    }

    /**
     * Renders a human-readable diagnostic line, e.g.:
     * <pre>
     *   [error] type mismatch – 'x + "hello"'  (line 5:3)
     * </pre>
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (errorCode != null) {
            sb.append("[").append(errorCode().getCategory().name().toLowerCase()).append("] ");
            sb.append(errorCode().description());
        } else {
            sb.append("[").append(severity.wireValue()).append("] ");
            sb.append(message);
        }

        // Append the source snippet (lexeme) if available and not trivially empty
        if (node != null) {
            String lexeme = node.lexeme();
            if (lexeme != null && !lexeme.isBlank() && lexeme.length() <= 80) {
                sb.append(" – '").append(lexeme.replace("\n", "↵")).append("'");
            }
        }

        // Append additional detail message if provided
        if (errorCode != null && !message.isEmpty()) {
            sb.append("  (").append(message).append(")");
        }

        // Append source location
        if (node != null) {
            String loc = node.loc().display();
            if (!loc.startsWith("offset -")) {   // skip synthetic/unknown locations
                sb.append("  @").append(loc);
            }
        }

        return sb.toString();
    }
}
