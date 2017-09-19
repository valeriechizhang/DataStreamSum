# DataStreamSum

The program runs two threads: one thread takes an input stream of 16-bit unsigned integers from a host:port pair read from stdin, 
and the second thread accepts queries from stdin and output the andwers in a text file. 
The queries are in the format of "What is the sum of last <k> integers."
The answer can be exact or estimated
depending on k (k ≤ N where N is the input size) is inside a DGIM bucket
or not.


### Algorithm: 

DGIM

### How to run:
1. Compile the java program: 

javac DataStreamSum.java

2. Start the server (which sends out integer streams) with command: 

server.py [number of total integers] [maximum delay] [minimum delay]

3. Start the client with command:

java DataStreamSum [host:port] > output.txt
