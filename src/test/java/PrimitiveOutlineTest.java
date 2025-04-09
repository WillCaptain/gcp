import org.junit.jupiter.api.Test;

import org.twelve.gcp.outline.Outline;

import static org.junit.jupiter.api.Assertions.*;

public class PrimitiveOutlineTest {

    @Test
    void test_outline_to_string(){
        Outline none = Outline.Nothing;
        assertEquals("null",none.toString());

        Outline number = Outline.Integer;
        assertEquals("Integer",number.toString());

        Outline string = Outline.String;
        assertEquals("String",string.toString());

        Outline ignore = Outline.Ignore;
        assertEquals("Ignore",ignore.name());

        Outline unit = Outline.Unit;
        assertEquals("Unit",unit.name());
    }
    @Test
    void test_number_is(){
        Outline number1 = Outline.Integer;
        Outline number2 = Outline.Long;
        Outline none = Outline.Nothing;
        assertTrue(number1.is(number2));
        assertFalse(number2.is(number1));
        assertFalse(number1.is(none));
    }

    @Test
    void test_string_is(){
        Outline str1 = Outline.String;
        Outline str2 = Outline.String;
        Outline none = Outline.Nothing;
        assertTrue(str1.is(str2));
        assertFalse(str1.is(none));
    }

    @Test
    void test_unit_is(){
        Outline unit1 = Outline.Unit;
        Outline unit2 = Outline.Unit;
        Outline none = Outline.Nothing;
        assertTrue(unit1.is(unit2));
        assertFalse(unit1.is(none));
        assertTrue(none.is(unit1));
    }

    @Test
    void test_ignore_is(){
        Outline ignore1 = Outline.Ignore;
        Outline ignore2 = Outline.Ignore;
        Outline none = Outline.Nothing;
        assertFalse(ignore1.is(ignore2));
        assertFalse(ignore1.is(none));
    }

    @Test
    void test_any_is(){
        Outline number = Outline.Integer;
        Outline string = Outline.Integer;
        Outline unit = Outline.Unit;
        Outline ignore = Outline.Ignore;
        Outline any1 = Outline.Any;
        Outline any2 = Outline.Any;
        assertTrue(number.is(any1));
        assertTrue(string.is(any1));
        assertFalse(unit.is(any1));
        assertFalse(ignore.is(any1));
        assertFalse(any1.is(number));
        assertTrue(any1.is(any2));
        assertTrue(any1.is(any1));
    }

    @Test
    void test_primitive_is(){
        Outline number = Outline.Integer;
        Outline string = Outline.String;
        Outline unit = Outline.Unit;
        Outline ignore = Outline.Ignore;
        assertFalse(number.is(string));
        assertFalse(string.is(ignore));
        assertFalse(string.is(unit));
    }
}
