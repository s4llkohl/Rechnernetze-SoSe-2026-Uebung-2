import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

public class Token {

    private static final int MAX_BUFFER_SIZE = 4096;

    // Nachrichtentypen für Fehlertoleranz
    public enum MessageType {
        TOKEN,      // Normales Token
        ACK         // Bestätigung
    }

    public record Endpoint(String ip, int port) {}

    @JsonProperty
    private MessageType messageType = MessageType.TOKEN;

    @JsonProperty
    private final Queue<Endpoint> ring = new LinkedList<>();

    @JsonProperty
    private int sequence = 0;

    public Token() {
        this.messageType = MessageType.TOKEN;
    }

    public Token(MessageType type) {
        this.messageType = type;
    }

    // Getter und Setter für MessageType
    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    // Erstellt ein ACK-Token als Antwort
    public static Token createAck(int sequence) {
        Token ack = new Token(MessageType.ACK);
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

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public void incrementSequence() {
        sequence++;
    }

    public Queue<Endpoint> getRing() {
        return ring;
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

    /**
     * Empfängt ein Token/ACK mit optionalem Timeout.
     * @param s DatagramSocket
     * @param timeoutMs Timeout in Millisekunden (0 = kein Timeout)
     * @return Empfangenes Token oder null bei Timeout
     */
    public static Token receive(DatagramSocket s, int timeoutMs) throws IOException {
        int oldTimeout = s.getSoTimeout();
        try {
            s.setSoTimeout(timeoutMs);
            byte[] buf = new byte[MAX_BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            s.receive(packet);
            String rc_json = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            System.out.printf("Received %s from %s:%d\n", rc_json, packet.getAddress().getHostAddress(), packet.getPort());
            return fromJSON(rc_json);
        } catch (SocketTimeoutException e) {
            return null; // Timeout erreicht
        } finally {
            s.setSoTimeout(oldTimeout);
        }
    }

    // Ursprüngliche receive-Methode für Abwärtskompatibilität (blockiert unbegrenzt)
    public static Token receive(DatagramSocket s) throws IOException {
        return receive(s, 0);
    }

    private static final ObjectMapper serializer = new ObjectMapper();

    public String toJSON() throws JsonProcessingException {
        return serializer.writeValueAsString(this);
    }

    public static Token fromJSON(String json) throws IOException {
        return serializer.readValue(json, Token.class);
    }
}
