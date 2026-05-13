import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;


public class TokenRing {

    private static void loop(DatagramSocket socket, String ip, int port, boolean first){
        LinkedList<Token.Endpoint> candidates = new LinkedList<>();
        if (first) {
            candidates.add(new Token.Endpoint(ip, port));
        }
        while (true) {
            try {
                byte[] buf = new byte[4096];

                DatagramPacket packet =
                        new DatagramPacket(buf, buf.length);

                socket.receive(packet);

                String json = new String(
                        packet.getData(),
                        0,
                        packet.getLength(),
                        StandardCharsets.UTF_8);

                Token rc = Token.fromJSON(json);

                sendAck(
                        socket,
                        packet.getAddress(),
                        packet.getPort());

                System.out.printf("Token: seq=%d, #members=%d", rc.getSequence(), rc.length());
                for (Token.Endpoint endpoint : rc.getRing()) {
                    System.out.printf(" (%s, %d)", endpoint.ip(), endpoint.port());
                }
                System.out.println();
                if (rc.length() == 1) {
                    candidates.add(rc.poll());
                    if (!first) {
                        continue;
                    }
                }
                first = false;
                for (Token.Endpoint candidate : candidates) {
                    rc.append(candidate);
                }
                candidates.clear();
                Token.Endpoint next = rc.poll();
                rc.append(next);
                rc.incrementSequence();
                Thread.sleep(1000);
                rc.send(socket, next);

                boolean sent = false;
                int maxRetries = 3;
                for (int i = 0; i < maxRetries && !sent; i++) {
                    try {
                        rc.send(socket, next);

                        if (waitForAck(socket)) {
                            sent = true;
                        } else {
                            System.out.printf("No ACK from %s:%d (%d/%d)\n", next.ip(), next.port(), i + 1, maxRetries);
                        }
                    }
                    catch (IOException e) {
                        System.out.println("Send failed");
                    }
                }
                //Hier wird eine maximale Anzahl von Versuchen definiert, um einen bestimmten Knoten zu erreichen.
                //Wenn es scheitert, soll der entsprechende Endpoint aus der Queue im nächsten Schritt gelöscht werde
                if (!sent) {

                    System.out.printf(
                            "Removing %s:%d\n",
                            next.ip(),
                            next.port());

                    rc.removeEndpoint(next);
                }
            }
            catch (IOException e) {
                System.out.println("Error receiving packet: " + e.getMessage());
            }
            catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void sendAck(
            DatagramSocket socket,
            InetAddress address,
            int port) throws IOException {

        byte[] ackData = "ACK".getBytes();

        DatagramPacket ackPacket =
                new DatagramPacket(
                        ackData,
                        ackData.length,
                        address,
                        port);

        socket.send(ackPacket);
    }

    private static boolean waitForAck(DatagramSocket socket) {
        try {
            byte[] buf = new byte[16];

            DatagramPacket packet =
                    new DatagramPacket(buf, buf.length);

           socket.receive(packet);

           String msg = new String(
                   packet.getData(),
                   0,
                    packet.getLength());

            return msg.equals("ACK");
        }
        catch (SocketTimeoutException e) {
            return false;
        }
        catch (IOException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(2000);
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            socket.disconnect();
            int port = socket.getLocalPort();
            System.out.printf("UDP endpoint is (%s, %d)\n", ip, port);
            if (args.length == 0) {
                loop(socket,ip,port,true);
            }
            else if (args.length == 2) {
                Token rc = new Token().append(ip,port);
                rc.send(socket,args[0],Integer.parseInt(args[1]));
                loop(socket,ip,port,false);
            }
            else {
                System.out.println("Usage: \"java TokenRing\" or \"java TokenRing <ip> <port>\"");
            }
        }
        catch (SocketException e) {
            System.out.println("Error creating socket: " + e.getMessage());
        }
        catch (UnknownHostException e) {
            System.out.println("Error while determining IP address: " + e.getMessage());
        }
        catch (IOException e) {
            System.out.println("IO error: " + e.getMessage());
            System.out.println(e.getStackTrace());
        }
    }
}