package HTTPServer;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.lang.Thread;

public class HTTPServer {
    private int port;
    private String path;
    private Thread listenThread;
    private List<Client> clients = new ArrayList<Client>();

    public HTTPServer(int port) {
        this.port = port;
    }
    
    public void start() {
        listenThread = new Thread() {
              public void run() {
                    try {
                        ServerSocket srv = new ServerSocket(port);
                        try {
	                        while ( ! isInterrupted()) {
	                            Client client = new Client(srv.accept(), clients, path);
	                            clients.add(client);
	                            client.start();

                                Thread.sleep(50);
	                        }
                        } finally {
                        	srv.close();
                        }
                    } catch (Exception ex) {
                        return;
                    }
              }
        };
        listenThread.start();
    }
    
    public void stop() {
    	listenThread.interrupt();

        for (Client client : clients)
            client.interrupt();
    }
    
    public void setPath(String path) {
        this.path = path;
    }
}