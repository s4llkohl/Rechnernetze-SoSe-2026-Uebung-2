import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenTest {

    @Test
    void testMessage() throws IOException {
        Token m = new Token().append("ip0", 0).append("ip1", 1);
        String json_1 = m.toJSON();
        Token.Endpoint ipr = m.poll();
        m.append(ipr.ip(), ipr.port());
        ipr = m.poll();
        m.append(ipr.ip(), ipr.port());
        String json_2 = m.toJSON();
        assertEquals(json_1, json_2);
    }

    @Test
    void testRemoveEndpoint() {
        Token token = new Token().append("ip0", 0).append("ip1", 1).append("ip2", 2);
        assertTrue(token.removeEndpoint(new Token.Endpoint("ip1", 1)));
        assertEquals(2, token.length());
        assertEquals("ip0", token.first().ip());
    }
}