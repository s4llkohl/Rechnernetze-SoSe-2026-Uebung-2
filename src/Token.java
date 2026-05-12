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

    public enum MessageType {
        TOKEN,
        ACK
    }

    public record Endpoint(String ip, int port) {}

    /**
     * Enthält zusätzlich zum gelesenen Token den Absender des UDP-Pakets.
     * Das wird benötigt, damit ein empfangener Token bestätigt werden kann.
     */
    public record ReceivedToken(Token token, Endpoint sender) {}

    private MessageType type = MessageType.TOKEN;

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public boolean isAck() {
        return type == MessageType.ACK;
    }

    public boolean isToken() {
        return type == MessageType.TOKEN;
    }

    public static Token ackFor(int sequence) {
        Token ack = new Token();
        ack.setType(MessageType.ACK);
        ack.setSequence(sequence);
        return ack;
    }

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

    public boolean removeEndpoint(Endpoint endpoint) {
        return ring.remove(endpoint);
    }

    public int length() {
        return ring.size();
    }

    private int sequence = 0;

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public void incrementSequence() {
        sequence++;
    }

    public void send(DatagramSocket s, String ip_address, int port) throws IOException {
        String rc_json = toJSON();
        byte[] rc_json_bytes = rc_json.getBytes(StandardCharsets.UTF_8);
        InetAddress address = InetAddress.getByName(ip_address);
        DatagramPacket packet = new DatagramPacket(rc_json_bytes, rc_json_bytes.length, address, port);
        System.out.printf("Sending %s to %s:%d\n", rc_json, ip_address, port);
        s.send(packet);
    }

    public void send(DatagramSocket s, Endpoint endpoint) throws IOException {
        send(s, endpoint.ip(), endpoint.port());
    }

    public static Token receive(DatagramSocket s) throws IOException {
        return receiveMessage(s).token();
    }

    public static ReceivedToken receiveMessage(DatagramSocket s) throws IOException {
        byte[] buf = new byte[max_buffer_size];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        s.receive(packet);
        String rc_json = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        System.out.printf("Received %s from %s:%d\n", rc_json, packet.getAddress().getHostAddress(), packet.getPort());
        Token token = fromJSON(rc_json);
        Endpoint sender = new Endpoint(packet.getAddress().getHostAddress(), packet.getPort());
        return new ReceivedToken(token, sender);
    }

    public static void sendAck(DatagramSocket socket, Endpoint receiver, int sequence) throws IOException {
        ackFor(sequence).send(socket, receiver);
    }

    @JsonProperty
    private final Queue<Endpoint> ring = new LinkedList<>();

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
