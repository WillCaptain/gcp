package org.twelve.gcp.exception;

import org.twelve.gcp.ast.Node;

public class GCPError {
    private final GCPErrCode errorCode;
    private final Node node;
    private final String message;

    public GCPError(Node node, GCPErrCode errorCode, String message) {
        this.errorCode = errorCode;
        this.node = node;
        this.message = message;
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

    /**
     * Renders a human-readable diagnostic line, e.g.:
     * <pre>
     *   [error] type mismatch – 'x + "hello"'  (line 5:3)
     * </pre>
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(errorCode().getCategory().name().toLowerCase()).append("] ");
        sb.append(errorCode().description());

        // Append the source snippet (lexeme) if available and not trivially empty
        if (node != null) {
            String lexeme = node.lexeme();
            if (lexeme != null && !lexeme.isBlank() && lexeme.length() <= 80) {
                sb.append(" – '").append(lexeme.replace("\n", "↵")).append("'");
            }
        }

        // Append additional detail message if provided
        if (!message.isEmpty()) {
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
