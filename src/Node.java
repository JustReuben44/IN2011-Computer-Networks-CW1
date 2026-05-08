// IN2011 Computer Networks
// Coursework 2024/2025
//
// Submission by
//  YOUR_NAME_GOES_HERE
//  YOUR_STUDENT_ID_NUMBER_GOES_HERE
//  YOUR_EMAIL_GOES_HERE


// DO NOT EDIT starts
// This gives the interface that your code must implement.
// These descriptions are intended to help you understand how the interface
// will be used. See the RFC for how the protocol works.

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;


interface NodeInterface {

    /* These methods configure your node.
     * They must both be called once after the node has been created but
     * before it is used. */
    
    // Set the name of the node.
    public void setNodeName(String nodeName) throws Exception;

    // Open a UDP port for sending and receiving messages.
    public void openPort(int portNumber) throws Exception;


    /*
     * These methods query and change how the network is used.
     */

    // Handle all incoming messages.
    // If you wait for more than delay miliseconds and
    // there are no new incoming messages return.
    // If delay is zero then wait for an unlimited amount of time.
    public void handleIncomingMessages(int delay) throws Exception;
    
    // Determines if a node can be contacted and is responding correctly.
    // Handles any messages that have arrived.
    public boolean isActive(String nodeName) throws Exception;

    // You need to keep a stack of nodes that are used to relay messages.
    // The base of the stack is the first node to be used as a relay.
    // The first node must relay to the second node and so on.
    
    // Adds a node name to a stack of nodes used to relay all future messages.
    public void pushRelay(String nodeName) throws Exception;

    // Pops the top entry from the stack of nodes used for relaying.
    // No effect if the stack is empty
    public void popRelay() throws Exception;
    

    /*
     * These methods provide access to the basic functionality of
     * CRN-25 network.
     */

    // Checks if there is an entry in the network with the given key.
    // Handles any messages that have arrived.
    public boolean exists(String key) throws Exception;
    
    // Reads the entry stored in the network for key.
    // If there is a value, return it.
    // If there isn't a value, return null.
    // Handles any messages that have arrived.
    public String read(String key) throws Exception;

    // Sets key to be value.
    // Returns true if it worked, false if it didn't.
    // Handles any messages that have arrived.
    public boolean write(String key, String value) throws Exception;

    // If key is set to currentValue change it to newValue.
    // Returns true if it worked, false if it didn't.
    // Handles any messages that have arrived.
    public boolean CAS(String key, String currentValue, String newValue) throws Exception;

}
// DO NOT EDIT ends

// Complete this!
public class Node implements NodeInterface {
    private String nodeName;
    private byte[] nodeHashID;
    private DatagramSocket socket;
    private Map<String, String> addressPairs = new HashMap<>();
    private Map<Integer, String> responses = new HashMap<>();
    private Random random = new Random();
    private Map<String, String> dataPairs = new HashMap<>();

    //Sets node's name ensuring it starts with "N:" and computes its hashID
    public void setNodeName(String nodeName) throws Exception {

        if (!nodeName.startsWith("N:")){
            throw new Exception("Node name must start with N:");
        }else {
            this.nodeName = nodeName;
            this.nodeHashID = HashID.computeHashID(nodeName);
        }
    }

    public void openPort(int portNumber) throws Exception{
        this.socket = new DatagramSocket(portNumber);
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hash = new StringBuilder();
        for (byte b: bytes) {
            hash.append(String.format("%02x", b & 0xFF));
        }
        return hash.toString();
    }

    private static byte[] hexToBytes(String hex) {
        byte[] out = new byte[32];
        for (int i = 0; i < 32; i++) {
            int hi = Character.digit(hex.charAt(i*2), 16);
            int lo = Character.digit(hex.charAt(i*2+1), 16);
            out[i] = (byte)((hi << 4) | lo);
        }
        return out;
    }

    public static int calculateDistance(byte[] a, byte[] b){
        int count = 0;
        for(int i = 0; i < a.length; i++) {
            int xor = (a[i] ^ b[i]) & 0xFF;

            if (xor == 0) {
                count += 8;
            }
            else{
                for (int bit = 7; bit >= 0; bit--) {
                    if (((xor >> bit) & 1) == 0) {
                        count++;
                    } else {
                        return 256 - count;
                    }
                }
            }
        }
        return 256 - count;
    }

    public static String encodeString(String message){
        int spaceCount = 0;
        for (int i = 0; i < message.length(); i++) {
            if (message.charAt(i) == ' ') {
                spaceCount++;
            }
        }
        return spaceCount + " " + message + " ";
    }

    private static DecodeResult decodeString(String message, int startPos) throws Exception {
        int firstSpace = message.indexOf(' ', startPos);
        int count = Integer.parseInt(message.substring(startPos, firstSpace));
        int valueStart = firstSpace + 1;

        int pos = valueStart;
        int spacesSeen = 0;

        while (pos < message.length()) {
            if (message.charAt(pos) == ' ') {
                if (spacesSeen == count) break;  // this is the terminator
                spacesSeen++;
            }
            pos++;
        }

        String value = message.substring(valueStart, pos);
        return new DecodeResult(value, pos + 1);
    }

    public static void main(String[] args) throws Exception {
        Node n1 = new Node();
        n1.setNodeName("N:test0");
        n1.openPort(20110);

        Node n2 = new Node();
        n2.setNodeName("N:test1");
        n2.openPort(20111);
        n2.dataPairs.put("D:hello", "world");

        n1.addressPairs.put("N:test1", "127.0.0.1:20111");

        Thread t = new Thread(() -> {
            try { n2.handleIncomingMessages(0); } catch (Exception e) {}
        });
        t.setDaemon(true);
        t.start();

        System.out.println("exists D:hello: " + n1.exists("D:hello"));
        System.out.println("exists D:missing: " + n1.exists("D:missing"));
    }

    public void handleIncomingMessages(int delay) throws Exception {
        socket.setSoTimeout(delay == 0 ? 0 : delay);
        long start = System.currentTimeMillis();

        while (true) {
            byte[] buffer = new byte[65536];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                System.out.println("Got packet from " + packet.getAddress() + ":"
                        + packet.getPort() + " length=" + packet.getLength());

                byte[] data = packet.getData();
                int len = packet.getLength();

                byte txId0 = data[0];
                byte txId1 = data[1];

                String body = new String(data, 3, len - 3, java.nio.charset.StandardCharsets.UTF_8);

                if (body.startsWith("G")) {
                    sendReply(packet, txId0, txId1, "H " + encodeString(nodeName));
                }

                else if (body.startsWith("H")) {
                    int rxTxID = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
                    responses.put(rxTxID, body);
                }

                else if (body.startsWith("O")) {
                    int rxTxID = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
                    responses.put(rxTxID, body);
                }

                else if (body.startsWith("N ")) {
                    String hexHash = body.substring(2, 66);  // 64 hex chars after "N "
                    byte[] targetHash = hexToBytes(hexHash);

                    List<String> sorted = new ArrayList<>(addressPairs.keySet());
                    sorted.sort((a, b) -> {
                        try {
                            int da = calculateDistance(HashID.computeHashID(a), targetHash);
                            int db = calculateDistance(HashID.computeHashID(b), targetHash);
                            return Integer.compare(da, db);
                        } catch (Exception e) { return 0; }
                    });

                    StringBuilder reply = new StringBuilder("O ");
                    for (int i = 0; i < Math.min(3, sorted.size()); i++) {
                        String name = sorted.get(i);
                        reply.append(encodeString(name));
                        reply.append(encodeString(addressPairs.get(name)));
                    }

                    sendReply(packet, txId0, txId1, reply.toString());
                }

                else if (body.startsWith("E ")) {
                    DecodeResult key = decodeString(body.substring(2), 0);
                    boolean haveIt = dataPairs.containsKey(key.value) || addressPairs.containsKey(key.value);
                    String reply;
                    if (haveIt) {
                        reply = "F Y";
                    } else {
                        byte[] keyHash = HashID.computeHashID(key.value);
                        reply = amIClosest(keyHash) ? "F N" : "F ?";
                    }
                    sendReply(packet, txId0, txId1, reply);
                }

                else if (body.startsWith("F")) {
                    int rxTxID = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
                    responses.put(rxTxID, body);
                }

            } catch (SocketTimeoutException e) {
                return;
            }

            if (delay > 0 && System.currentTimeMillis() - start >= delay) return;
        }
    }

    private void sendReply(DatagramPacket original, byte t0, byte t1, String body) throws Exception {
        byte[] bodyBytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] full = new byte[3 + bodyBytes.length];
        full[0] = t0;
        full[1] = t1;
        full[2] = ' ';
        System.arraycopy(bodyBytes, 0, full, 3, bodyBytes.length);
        DatagramPacket out = new DatagramPacket(full, full.length,
                original.getAddress(), original.getPort());
        socket.send(out);
    }

    private void parseNearestResponse(String content) throws Exception {
        int pos = 0;
        while (pos < content.length()) {
            DecodeResult name = decodeString(content, pos);
            pos = name.endPos;
            if (pos >= content.length()) break;
            DecodeResult addr = decodeString(content, pos);
            pos = addr.endPos;
            if (name.value.startsWith("N:")) {
                addressPairs.put(name.value, addr.value);
            }
        }
    }

    private void sendNearest(byte[] targetHash, String addr, int port) throws Exception {
        int txID = generateTransactionID();
        byte[] txBytes = { (byte)(txID >> 8), (byte)(txID & 0xFF) };

        String body = "N " + bytesToHex(targetHash);
        byte[] bodyBytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] full = new byte[3 + bodyBytes.length];
        full[0] = txBytes[0];
        full[1] = txBytes[1];
        full[2] = ' ';
        System.arraycopy(bodyBytes, 0, full, 3, bodyBytes.length);

        DatagramPacket out = new DatagramPacket(full, full.length,
                java.net.InetAddress.getByName(addr), port);
        socket.send(out);

        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            handleIncomingMessages(500);
            if (responses.containsKey(txID)) {
                String reply = responses.remove(txID);
                if (reply.startsWith("O ")) parseNearestResponse(reply.substring(2));
                return;
            }
        }
    }

    private int generateTransactionID() {
        int id;
        do {
            id = random.nextInt(65536);
        } while ((id & 0xFF) == 0x20 || ((id >> 8) & 0xFF) == 0x20);
        return id;
    }

    private boolean amIClosest(byte[] keyHash) throws Exception {
        List<String> all = new ArrayList<>(addressPairs.keySet());
        all.add(nodeName);  // include self
        all.sort((a, b) -> {
            try {
                int da = calculateDistance(HashID.computeHashID(a), keyHash);
                int db = calculateDistance(HashID.computeHashID(b), keyHash);
                return Integer.compare(da, db);
            } catch (Exception e) { return 0; }
        });
        int idx = all.indexOf(nodeName);
        return idx >= 0 && idx < 3;
    }
    
    public boolean isActive(String nodeName) throws Exception {
        String addrPort = addressPairs.get(nodeName);
        if (addrPort == null) return false;

        String[] parts = addrPort.split(":");
        String addr = parts[0];
        int port = Integer.parseInt(parts[1]);

        int txID = generateTransactionID();
        byte[] txBytes = { (byte)(txID >> 8), (byte)(txID & 0xFF) };

        byte[] full = { txBytes[0], txBytes[1], ' ', 'G' };

        DatagramPacket out = new DatagramPacket(full, full.length,
                java.net.InetAddress.getByName(addr), port);
        socket.send(out);


        long deadline = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < deadline) {
            handleIncomingMessages(500);
            if (responses.containsKey(txID)) {
                String reply = responses.remove(txID);

                if (reply.startsWith("H ")) {
                    DecodeResult r = decodeString(reply.substring(2), 0);
                    return r.value.equals(nodeName);
                }
                return false;
            }
        }
        return false;
    }
    
    public void pushRelay(String nodeName) throws Exception {
	throw new Exception("Not implemented");
    }

    public void popRelay() throws Exception {
        throw new Exception("Not implemented");
    }

    public boolean exists(String key) throws Exception {
        byte[] keyHash = HashID.computeHashID(key);

        List<String> sorted = new ArrayList<>(addressPairs.keySet());
        sorted.sort((a, b) -> {
            try {
                int da = calculateDistance(HashID.computeHashID(a), keyHash);
                int db = calculateDistance(HashID.computeHashID(b), keyHash);
                return Integer.compare(da, db);
            } catch (Exception e) { return 0; }
        });

        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            String node = sorted.get(i);
            String addrPort = addressPairs.get(node);
            String[] parts = addrPort.split(":");

            int txID = generateTransactionID();
            byte[] txBytes = { (byte)(txID >> 8), (byte)(txID & 0xFF) };
            String reqBody = "E " + encodeString(key);
            byte[] bodyBytes = reqBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] full = new byte[3 + bodyBytes.length];
            full[0] = txBytes[0]; full[1] = txBytes[1]; full[2] = ' ';
            System.arraycopy(bodyBytes, 0, full, 3, bodyBytes.length);
            socket.send(new DatagramPacket(full, full.length,
                    java.net.InetAddress.getByName(parts[0]), Integer.parseInt(parts[1])));

            long deadline = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < deadline) {
                handleIncomingMessages(500);
                if (responses.containsKey(txID)) {
                    String reply = responses.remove(txID);
                    if (reply.startsWith("F Y")) return true;
                    break;
                }
            }
        }
        return false;
    }
    
    public String read(String key) throws Exception {
	throw new Exception("Not implemented");
    }

    public boolean write(String key, String value) throws Exception {
	throw new Exception("Not implemented");
    }

    public boolean CAS(String key, String currentValue, String newValue) throws Exception {
	throw new Exception("Not implemented");
    }
}
