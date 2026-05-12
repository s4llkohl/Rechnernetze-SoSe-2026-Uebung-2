import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenTest {

    @Test
    void testMessage() throws IOException {
        Token token = new Token()
                .append("ip0", 0)
                .append("ip1", 1)
                .append("ip2", 2);
        token.setDirection(Token.Direction.COUNTERCLOCKWISE);
        for (int i = 0; i < 7; i++) {
            token.incrementSequence();
        }

        String json = token.toJSON();
        Token decoded = Token.fromJSON(json);

        assertEquals(json, decoded.toJSON());
        assertEquals(Token.Direction.COUNTERCLOCKWISE, decoded.getDirection());
        assertEquals(7, decoded.getSequence());
    }

    @Test
    void testFailoverUsesOppositeDirection() {
        Token.Endpoint a = new Token.Endpoint("ip0", 0);
        Token.Endpoint b = new Token.Endpoint("ip1", 1);
        Token.Endpoint c = new Token.Endpoint("ip2", 2);
        Token.Endpoint d = new Token.Endpoint("ip3", 3);

        Token token = new Token()
                .append(a)
                .append(b)
                .append(c)
                .append(d);

        assertEquals(b, token.nextEndpoint(a));
        token.removeEndpoint(b);
        token.flipDirection();
        assertEquals(d, token.nextEndpoint(a));
    }
}