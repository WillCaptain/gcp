import org.junit.jupiter.api.Test;
import org.twelve.gcp.common.LiteralParser;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LiteralUnionParserTest {

    @Test
    void test_literal_parse() {
        assertEquals(true, LiteralParser.parse("true"));         // Output: true (Boolean)
        assertEquals(123, LiteralParser.parse("123"));          // Output: 123 (Integer)
        assertEquals(1234567890123L, LiteralParser.parse("1234567890123L"));// Output: 1234567890123 (Long)
        assertEquals(45.67f, LiteralParser.parse("45.67f"));       // Output: 45.67 (Float)
        assertEquals(45.67d, LiteralParser.parse("45.67d"));       // Output: 45.67 (Double)
        assertEquals("hello", LiteralParser.parse("hello"));
        assertEquals(BigDecimal.valueOf(45.67), LiteralParser.parse("45.67"));        // Output: 45.67 (BigDecimal)
    }
}
