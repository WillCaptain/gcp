package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.outline.adt.Array;
import org.twelve.gcp.outline.adt.EntityMember;
import org.twelve.gcp.outline.builtin.String_;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;

/**
 * String type in the GCP type system.
 * <p>
 * Provides built-in string operations covering inspection, transformation, searching, and conversion.
 *
 * @author huizi 2025
 */
public class STRING extends Primitive {
    private final static String_ string_ = new String_();

    public STRING(AbstractNode node) {
        super(string_, node, node.ast());
        this.loadBuiltInMethods();
    }

    public STRING(AST ast) {
        super(string_, null, ast);
    }

    /**
     * Loads built-in string methods.
     * <ul>
     *   <li>{@code len()}             : Unit → Integer      — number of characters</li>
     *   <li>{@code trim()}            : Unit → String       — remove leading/trailing whitespace</li>
     *   <li>{@code to_upper()}        : Unit → String       — convert to uppercase</li>
     *   <li>{@code to_lower()}        : Unit → String       — convert to lowercase</li>
     *   <li>{@code split(String)}     : String → [String]   — split by separator</li>
     *   <li>{@code contains(String)}  : String → Bool       — substring containment check</li>
     *   <li>{@code starts_with(String)}: String → Bool      — prefix check</li>
     *   <li>{@code ends_with(String)} : String → Bool       — suffix check</li>
     *   <li>{@code index_of(String)}  : String → Integer    — first occurrence index (−1 if absent)</li>
     *   <li>{@code sub_str(Integer,Integer)}: Integer → Integer → String — substring by range</li>
     *   <li>{@code replace(String,String)}  : String → String → String  — replace all occurrences</li>
     *   <li>{@code to_int()}          : Unit → Integer      — parse as integer</li>
     *   <li>{@code to_number()}       : Unit → Number       — parse as number</li>
     *   <li>{@code chars()}           : Unit → [String]     — list of single-character strings</li>
     *   <li>{@code repeat(Integer)}   : Integer → String    — repeat the string N times</li>
     * </ul>
     */
    @Override
    public boolean loadBuiltInMethods() {
        if (!super.loadBuiltInMethods()) return false;
        AST ast = this.ast();
        members.put("len",         EntityMember.from("len",         FirstOrderFunction.from(ast, ast.Integer,                        ast.Unit),    Modifier.PUBLIC, false, null, true));
        members.put("trim",        EntityMember.from("trim",        FirstOrderFunction.from(ast, ast.String,                         ast.Unit),    Modifier.PUBLIC, false, null, true));
        members.put("to_upper",    EntityMember.from("to_upper",    FirstOrderFunction.from(ast, ast.String,                         ast.Unit),    Modifier.PUBLIC, false, null, true));
        members.put("to_lower",    EntityMember.from("to_lower",    FirstOrderFunction.from(ast, ast.String,                         ast.Unit),    Modifier.PUBLIC, false, null, true));
        members.put("split",       EntityMember.from("split",       FirstOrderFunction.from(ast, Array.from(ast, ast.String),         ast.String),  Modifier.PUBLIC, false, null, true));
        members.put("contains",    EntityMember.from("contains",    FirstOrderFunction.from(ast, ast.Boolean,                        ast.String),  Modifier.PUBLIC, false, null, true));
        members.put("starts_with", EntityMember.from("starts_with", FirstOrderFunction.from(ast, ast.Boolean,                        ast.String),  Modifier.PUBLIC, false, null, true));
        members.put("ends_with",   EntityMember.from("ends_with",   FirstOrderFunction.from(ast, ast.Boolean,                        ast.String),  Modifier.PUBLIC, false, null, true));
        members.put("index_of",    EntityMember.from("index_of",    FirstOrderFunction.from(ast, ast.Integer,                        ast.String),  Modifier.PUBLIC, false, null, true));
        members.put("sub_str",     EntityMember.from("sub_str",     FirstOrderFunction.from(ast, ast.String,   ast.Integer, ast.Integer),           Modifier.PUBLIC, false, null, true));
        members.put("replace",     EntityMember.from("replace",     FirstOrderFunction.from(ast, ast.String,   ast.String,  ast.String),            Modifier.PUBLIC, false, null, true));
        members.put("to_int",      EntityMember.from("to_int",      FirstOrderFunction.from(ast, ast.Integer,                        ast.Unit),    Modifier.PUBLIC, false, null, true));
        members.put("to_number",   EntityMember.from("to_number",   FirstOrderFunction.from(ast, ast.Number,                         ast.Unit),    Modifier.PUBLIC, false, null, true));
        members.put("chars",       EntityMember.from("chars",       FirstOrderFunction.from(ast, Array.from(ast, ast.String),         ast.Unit),    Modifier.PUBLIC, false, null, true));
        members.put("repeat",      EntityMember.from("repeat",      FirstOrderFunction.from(ast, ast.String,                         ast.Integer), Modifier.PUBLIC, false, null, true));
        return true;
    }
}
