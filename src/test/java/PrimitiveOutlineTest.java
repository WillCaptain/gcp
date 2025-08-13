import org.junit.jupiter.api.Test;

import org.twelve.gcp.ast.ASF;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.outline.Outline;

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
    void test_any_is(){
        Outline number = ast.Integer;
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
}
