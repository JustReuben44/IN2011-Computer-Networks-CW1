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
        StringBuilder sb = new StringBuilder();
        for (byte b: bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
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

    

    public static void main(String[] args) throws Exception {
        System.out.println(bytesToHex(HashID.computeHashID("hello")));
        byte[] h1 = HashID.computeHashID("N:test0");
        byte[] h2 = HashID.computeHashID("N:test1");
        System.out.println(calculateDistance(h1, h1));  // 0
        System.out.println(calculateDistance(h1, h2));
        System.out.println(encodeString("Hello World"));  // "1 Hello World "
        System.out.println(encodeString("Hello"));    // some number
    }

    public void handleIncomingMessages(int delay) throws Exception {
	throw new Exception("Not implemented");
    }
    
    public boolean isActive(String nodeName) throws Exception {
	throw new Exception("Not implemented");
    }
    
    public void pushRelay(String nodeName) throws Exception {
	throw new Exception("Not implemented");
    }

    public void popRelay() throws Exception {
        throw new Exception("Not implemented");
    }

    public boolean exists(String key) throws Exception {
	throw new Exception("Not implemented");
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
