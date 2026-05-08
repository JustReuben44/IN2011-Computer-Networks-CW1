Build Instructions
==================

1. Extract the ZIP into a single directory containing the .java files.
2. Compile from the directory with: javac *.java

Run Instructions
================

1. To run the local test program (creates 2 nodes by default):

    java LocalTest

2. To run with a different number of nodes (between 2 and 10):

    java LocalTest 4

Working Functionality
=====================

1. setNodeName:
   Verifies node name and computes hashID

2. openPort
   Opens a UDP socket

3. handleIncomingMessages
   Receives loop with delay

4. isActive
   Sends G and waits for H response. returns name

5. Nearest (N/O)
   Both incoming handler and outgoing sendNearest

6. Key Existence (E/F)
   Incoming handler and exists() outgoing

7. Read (R/S)
   Incoming handler in handle and read() outgoing

8. Write (W/X)
   Incoming handler and write() outgoing

9. Relay
   Functionality maintains the relay stack

10. LocalTest parses successfully

Known Limitations
=================
1. UDP reliability: no retransmission on timeout, no retry counting
2. Compare-and-Swap is not implemented
3. The Relay stack is maintained but outgoing messages are not wrapped in V envelopes, and incoming V messages are not forwarded
4. Address pair storage does not enforce the "max three per distance" rule
5. Data rebalancing: not implemented; data is not transferred when closer nodes are discovered
6. Minimal robustness against malformed messages

