import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

public class DataStreamSum {
    public static void main(String[] args) throws IOException {

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        String network = "";
        List<String> queries = new ArrayList<String>();
        String s;

        // command line : java P2 localhost:xxxxx > output.txt
        if (args.length > 0) {
            network = args[0];
            System.out.println(args[0]);
        }

        else if ((s = in.readLine()) != null) {
            System.out.println(s);
            if (!checkHostPort(s)) {
                System.out.println("Error: host:port should be the first line");
                return;
            }
            network = s;
        }


        String[] net_split = network.split(":");
        String host = net_split[0];
        int port = Integer.parseInt(net_split[1]);

        Socket clientSocket = null;
        Storage storage = new Storage();


        try {
            System.out.println("Connecting to host " + host + " on port " + port);
            clientSocket = new Socket(host, port);

            InputNumber input = new InputNumber(clientSocket, storage);
            ReadQuery rq = new ReadQuery(queries, clientSocket, storage, in);
            ExecuteQuery eq = new ExecuteQuery(queries, clientSocket, storage);

            new Thread(rq).start();
            new Thread(input).start();
            new Thread(eq).start();

        } catch (UnknownHostException e) {
            System.out.println("Don't know about host: " + host);
            System.out.println("The program terminates");
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Couldn't get I/O for " + "the connection to: " + host);
            System.out.println("The program terminates");
            System.exit(1);
        }
    }


    public static boolean checkHostPort(String s) {
        String ip_regex =  "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

        Pattern ip_pattern = Pattern.compile(ip_regex);

        String[] split = s.split(":");
        if (split.length != 2) { return false; }


        Matcher matcher_1 = ip_pattern.matcher(split[0]);
        if (!matcher_1.matches()) {
            InetAddress inetAddress;
            Matcher matcher_2;
            try {
                inetAddress = InetAddress.getByName(split[0]);
                String ip_addr = inetAddress.getHostAddress() + "";
                matcher_2 = ip_pattern.matcher(ip_addr);
            } catch (Exception ex) {
                return false;
            }

            if (!matcher_2.matches()) {
                return false;
            }
        }

        if (!split[1].matches("^\\d+$")) { return false; }

        return true;
    }

}

class Bucket {
    public int timestamp;
    public int size;

    public Bucket() {
        this.timestamp = 0;
    }

    public Bucket(int size, int timestamp) {
        this.size = size;
        this.timestamp = timestamp;
    }
}

class Stream {
    List<Bucket> bucket_list;

    public Stream() {
        this.bucket_list = new ArrayList<Bucket>();
    }

    public void addBit(int timestamp) {
        Bucket new_bucket = new Bucket(1, timestamp);
        this.bucket_list.add(new_bucket);
        this.update_stream();
    }

    public void update_stream() {
        int n = this.bucket_list.size();
        int i = n-3;
        while (i >= 0) {
            Bucket a = this.bucket_list.get(i);
            Bucket b = this.bucket_list.get(i+1);
            Bucket c = this.bucket_list.get(i+2);

            // if three buckets have the same capacity, merge happens
            if (a.size == b.size && b.size == c.size) {
                Bucket new_bucket = new Bucket(b.size*2, b.timestamp);
                this.bucket_list.remove(a);
                this.bucket_list.remove(b);
                this.bucket_list.add(i, new_bucket);
            }
            i--;
        }
    }

    public double count_since_S(int s) {
        double count = 0.0;
        int first = 0;
        int last_timestamp = 0;

        while (first < this.bucket_list.size()) {
            Bucket b = this.bucket_list.get(first);
            if (first == this.bucket_list.size() - 1) {
                if (s > b.timestamp) {
                    count = 0;
                    break;
                }
                count = b.size;
                break;
            }

            if (s == last_timestamp + 1) {
                count = b.size;
                break;
            }

            if (s <= b.timestamp) {
                count = b.size/2.0;
                break;
            }

            first++;
            last_timestamp = b.timestamp;
        }

        for (int i = first+1; i < this.bucket_list.size(); i++) {
            count += this.bucket_list.get(i).size;
        }
        return count;
    }
}

class Storage {
    int count;
    List<Stream> stream_list;

    //inkedList<Integer> queue = new LinkedList<Integer>();
    //double sumQue = 0.0;

    public Storage() {
        this.count = 0;
        this.stream_list = new ArrayList<Stream>();
        for (int i = 0; i < 16; i++) {
            Stream new_stream = new Stream();
            this.stream_list.add(new_stream);
        }
    }

    public void addNumber(int[] binary) {
        this.count++;
        for (int i = 0; i < 16; i++) {
            if (binary[i] == 1) {
                this.stream_list.get(15-i).addBit(this.count);
            }
        }
    }

    public double sumLastK(int k) {
        int since = this.count - k + 1;
        double sum = 0.0;

        double[] c = new double[16];
        for (int i = 0; i < 16; i++) {
            c[i] = this.stream_list.get(i).count_since_S(since);
        }

        /*
        System.out.print("**");
        for (int i = 15; i >= 0; i--) {
            System.out.print((int)c[i] + " ");
        }
        System.out.println("**");
        */

        for (int i = 0; i < 16; i++) {
            sum += c[i] * Math.pow(2, i);
        }

        return sum;
    }


    public int getCount() {
        return this.count;
    }


    /*
    public void updateQueue(int k, int n) {
        if (this.queue.size() >= k) {
            this.sumQue -= this.queue.get(0);
            this.queue.removeFirst();
        }
        this.queue.add(n);
        this.sumQue += n;
    }
    */


}


class InputNumber implements Runnable {

    protected Socket clientSocket;
    private Storage storage;

    public InputNumber(Socket clientSocket, Storage storage) {
        this.clientSocket = clientSocket;
        this.storage = storage;
    }

    public void run() {
        try {
            String s;
            BufferedReader br = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));

            while ((s = br.readLine()) != null) {
                int n = Integer.parseInt(s.trim());
                int[] binary = toBinary16(n);
                System.out.println(n);
                this.storage.addNumber(binary);
                /*
                System.out.print(n + ": ");
                for (int i = 0; i < 16; i++) {
                    System.out.print(binary[i] + " ");
                }
                System.out.println();
                */
                //this.storage.updateQueue(1, n);
            }
            System.out.println("The number stream has stopped.");
            this.clientSocket.close();
            System.exit(1);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // http://stackoverflow.com/questions/4158428/decimal-to-binary-conversion
    public int[] toBinary16(int d) {
        int[] binary = new int[16];

        int res = d;
        int pos = 15;
        while (res > 0) {
            binary[pos] = res & 0x1;
            res = res >> 1;
            pos--;
        }
        while (pos >= 0) {
            binary[pos] = 0;
            pos--;
        }
        return binary;
    }

}


class ReadQuery implements Runnable {
    private List<String> queries;
    protected Socket clientSocket;
    private Storage storage;
    private BufferedReader in;

    public ReadQuery(List<String> queries, Socket clientSocket, Storage storage, BufferedReader in) {
        this.queries = queries;
        this.storage = storage;
        this.clientSocket = clientSocket;
        this.in = in;
    }

    public void run() {

        try {
            String s;
            while ((s = this.in.readLine()) != null) {
                this.queries.add(s);
            }
            // if finished reading the file and the last query isn't "end"
            // output an error message
            String last = queries.get(queries.size()-1);
            if (!last.equals("end")) {
                System.out.println("Error: missing end");
                return;
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}


class ExecuteQuery implements Runnable {
    private List<String> queries;
    private Storage storage;
    protected Socket clientSocket;

    public ExecuteQuery(List<String> queries, Socket clientSocket,  Storage storage) {
        this.queries = queries;
        this.storage = storage;
        this.clientSocket = clientSocket;
    }

    public void run() {
        try {
            int count = 0;
            while (true) {

                Thread.sleep(0, 1);

                if (this.queries.size() > 0) {
                    String query = this.queries.get(0);
                    this.queries.remove(0);

                    if (query.equals("end")) {
                        System.out.println(query);
                        this.clientSocket.close();
                        System.out.println("Program terminates");
                        System.out.println("Totally " + this.storage.getCount() + " numbers have been received");
                        System.exit(1);
                    }

                    String[] sp = query.split(" ");
                    if (!checkQuery(sp)) {
                        System.out.println(query);
                        System.out.println("Wrong query format.");
                        continue;
                    }

                    int k = Integer.parseInt(sp[6]);

                    while (k > this.storage.getCount()) {
                        Thread.sleep(0, 1);
                    }

                    System.out.println(query);
                    double sumK = this.storage.sumLastK(k);
                    System.out.println("The sum of last " + k + " integers is " + sumK + ".");
                    //System.out.println("Totally " + this.storage.getCount() + " numbers.");
                    //System.out.println("The actual sum is " + this.storage.sumQue);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }


    private boolean checkQuery(String[] sp) {
        String[] pattern = {"What", "is", "the", "sum", "for", "last", "0", "integers", "integers?"};

        if (sp.length != 8) { return false;}

        for (int i = 0; i < 6; i++) {
            if (!sp[i].equals(pattern[i])) { return false;}
        }

        int n = Integer.parseInt(sp[6]);
        String ns = n + "";
        if (!ns.equals(sp[6])) { return false;}

        if (!sp[7].equals(pattern[7]) && !sp[7].equals(pattern[8])) { return false; }

        return true;
    }

}




