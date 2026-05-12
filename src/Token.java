import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

public class Token {

    private static final int max_buffer_size = 4096;

    public enum Direction {
        CLOCKWISE,
        COUNTERCLOCKWISE;

        public Direction opposite() {
            return this == CLOCKWISE ? COUNTERCLOCKWISE : CLOCKWISE;
        }
    }

    public record Endpoint(String ip, int port) {}

    public Token append(String ip, int port) {
        ring.offer(new Endpoint(ip, port));
        return this;
    }

    public Token append(Endpoint endpoint) {
        ring.offer(endpoint);
        return this;
    }

    public Endpoint first() {
        return ring.peek();
    }

    public Endpoint poll() {
        return ring.poll();
    }

    public void removeEndpoint(Endpoint endpoint) { ring.remove(endpoint); } //Nutzung einer LinkedList-Methode

    public int indexOf(Endpoint endpoint) {
        return ring.indexOf(endpoint);
    }

    public Endpoint get(int index) {
        return ring.get(index);
    }

    public Endpoint neighbor(Endpoint endpoint, Direction direction) {
        int index = indexOf(endpoint);
        if (index < 0 || ring.size() < 2) {
            return null;
        }
        int offset = direction == Direction.CLOCKWISE ? 1 : -1;
        int neighborIndex = Math.floorMod(index + offset, ring.size());
        return ring.get(neighborIndex);
    }

    public Endpoint nextEndpoint(Endpoint endpoint) {
        return neighbor(endpoint, direction);
    }

    public Endpoint previousEndpoint(Endpoint endpoint) {
        return neighbor(endpoint, direction.opposite());
    }

    public int length () {
        return ring.size();
    }

    private int sequence = 0;

    @JsonProperty
    private Direction direction = Direction.CLOCKWISE;

    public int getSequence() {
        return sequence;
    }

    public void incrementSequence() {
        sequence++;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void flipDirection() {
        direction = direction.opposite();
    }

    public void send (DatagramSocket s, String ip_address, int port ) throws IOException {
        String rc_json = toJSON();
        byte[] rc_json_bytes = rc_json.getBytes(StandardCharsets.UTF_8);
        InetAddress address = InetAddress.getByName(ip_address);
        DatagramPacket packet = new DatagramPacket(rc_json_bytes, rc_json_bytes.length, address, port);
        System.out.printf("Sending %s to %s:%d\n", rc_json, ip_address, port);
        s.send(packet);
    }

    public void send (DatagramSocket s, Endpoint endpoint) throws IOException {
        send(s, endpoint.ip(), endpoint.port());
    }

    public static Token receive(DatagramSocket s) throws IOException {
        byte[] buf = new byte[max_buffer_size];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        s.receive(packet);
        String rc_json = new String(packet.getData(),0,packet.getLength(), StandardCharsets.UTF_8);
        System.out.printf("Received %s from %s:%d\n", rc_json, packet.getAddress().getHostAddress(), packet.getPort());
        return fromJSON(rc_json);
    }

    @JsonProperty
    private final LinkedList<Endpoint> ring = new LinkedList<>();

    public Queue<Endpoint> getRing() {
        return ring;
    }

    private static final ObjectMapper serializer = new ObjectMapper();

    public String toJSON() throws JsonProcessingException {
        return serializer.writeValueAsString(this);
    }

    public static Token fromJSON(String json) throws IOException {
        return serializer.readValue(json, Token.class);
    }
}
