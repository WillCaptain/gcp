import org.junit.jupiter.api.Test;

import org.twelve.gcp.ast.ASF;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.EntityMember;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.adt.SumADT;
import org.twelve.gcp.outline.primitive.NOTHING;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PrimitiveOutlineTest {

    private AST ast = new ASF().newAST();
    @Test
    void test_outline_to_string(){

        Outline none = ast.Nothing;
        assertEquals("null",none.toString());

        Outline number = ast.Integer;
        assertEquals("Integer",number.toString());

        Outline string = ast.String;
        assertEquals("String",string.toString());

        Outline ignore = ast.Ignore;
        assertEquals("Ignore",ignore.name());

        Outline unit = ast.Unit;
        assertEquals("Unit",unit.name());
    }
    @Test
    void test_number_is(){
        Outline number1 = ast.Integer;
        Outline number2 = ast.Long;
        Outline none = ast.Nothing;
        assertTrue(number1.is(number2));
        assertFalse(number2.is(number1));
        assertFalse(number1.is(none));
    }

    @Test
    void test_string_is(){
        Outline str1 = ast.String;
        Outline str2 = ast.String;
        Outline none = ast.Nothing;
        assertTrue(str1.is(str2));
        assertFalse(str1.is(none));
    }

    @Test
    void test_unit_is(){
        Outline unit1 = ast.Unit;
        Outline unit2 = ast.Unit;
        Outline none = ast.Nothing;
        assertTrue(unit1.is(unit2));
        assertFalse(unit1.is(none));
        assertTrue(none.is(unit1));
    }

    @Test
    void test_ignore_is(){
        Outline ignore1 = ast.Ignore;
        Outline ignore2 = ast.Ignore;
        Outline none = ast.Nothing;
        assertFalse(ignore1.is(ignore2));
        assertFalse(ignore1.is(none));
    }

    @Test
    void test_any_is(){        Outline number = ast.Integer;
        Outline string = ast.Integer;
        Outline unit = ast.Unit;
        Outline ignore = ast.Ignore;
        Outline any1 = ast.Any;
        Outline any2 = ast.Any;
        assertTrue(number.is(any1));
        assertTrue(string.is(any1));
        assertTrue(unit.is(any1));
        assertFalse(ignore.is(any1));
        assertFalse(any1.is(number));
        assertTrue(any1.is(any2));
        assertTrue(any1.is(any1));
    }

    @Test
    void test_primitive_is(){
        Outline number = ast.Integer;
        Outline string = ast.String;
        Outline unit = ast.Unit;
        Outline ignore = ast.Ignore;
        assertFalse(number.is(string));
        assertFalse(string.is(ignore));
        assertFalse(string.is(unit));
    }

    @Test
    void test_nullable_type_is_string_or_nothing() {
        // String|Nothing is a union of String and Nothing
        Outline stringOrNothing = Option.from(null, ast, ast.String, ast.Nothing);
        assertTrue(stringOrNothing instanceof SumADT);
        SumADT sum = (SumADT) stringOrNothing;
        assertTrue(sum.options().stream().anyMatch(o -> o instanceof NOTHING));
        assertTrue(sum.options().stream().anyMatch(o -> o.toString().equals("String")));
    }

    @Test
    void test_nullable_entity_field_allows_missing() {
        // actual: {name: String}   formal: {name: String, tag: String|Nothing}
        // actual.is(formal) → true  (tag is nullable, may be absent)
        var testAst = new ASF().newAST();
        Outline stringOrNothing = Option.from(null, testAst, testAst.String, testAst.Nothing);
        var node = testAst.program();

        Entity formal = Entity.from(node, List.of(
                EntityMember.from("name", testAst.String,  null, false),
                EntityMember.from("tag",  stringOrNothing, null, false)
        ));
        Entity actual = Entity.from(node, List.of(
                EntityMember.from("name", testAst.String, null, false)
        ));

        assertTrue(actual.is(formal),
                "actual struct missing nullable field should pass is() check");
    }

    @Test
    void test_non_nullable_missing_field_rejects() {
        // actual: {name: String}   formal: {name: String, required: String}
        // actual.is(formal) → false  (required field is missing)
        var testAst = new ASF().newAST();
        var node = testAst.program();

        Entity formal = Entity.from(node, List.of(
                EntityMember.from("name",     testAst.String, null, false),
                EntityMember.from("required", testAst.String, null, false)
        ));
        Entity actual = Entity.from(node, List.of(
                EntityMember.from("name", testAst.String, null, false)
        ));

        assertFalse(actual.is(formal),
                "actual struct missing required field should fail is() check");
    }
}
