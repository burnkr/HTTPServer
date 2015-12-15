package HTTPServer;

import java.lang.Exception;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.lang.Thread;

public class Client extends Thread {
    private Socket socket;
    private List<Client> clients;
    private FileManager fm;
    
    public Client(Socket socket, List<Client> clients, String path) {
        this.socket = socket;
        this.clients = clients;
        fm = new FileManager(path);
    }
    
    private void return400(OutputStream os) throws IOException {
        byte[] resp = "HTTP/1.1 400 Bad Request\r\n\r\n".getBytes();
        os.write(resp);
    }
    
    private byte[] getBinaryHeaders(List<String> headers) {
        StringBuilder res = new StringBuilder();

        for (String s : headers) 
            res.append(s);
            
        return res.toString().getBytes();
    }
    
    private void process(String request, OutputStream os) throws IOException {
        System.out.println(request);
        System.out.println("---------------------------------------------");

        int idx = request.indexOf("\r\n");
        request = request.substring(0, idx);

        String[] parts = request.split(" ");
        if (parts.length != 3) {
            return400(os);
            return;
        }

        String req = parts[0], url = parts[1], http = parts[2];
        
        if (( ! http.equalsIgnoreCase("HTTP/1.0")) && ( ! http.equalsIgnoreCase("HTTP/1.1"))) {
            return400(os);
            return;
        }
        if ( ! req.equalsIgnoreCase("GET")) {
            return400(os);
            return;
        }
        if ("/".equals(url))
            url = "/index.html";
        
        List<String> headers = new ArrayList<String>();
        headers.add("HTTP/1.1 200 OK\r\n");
        
        byte[] content = fm.get(url);
        
        ProcessorsList pl = new ProcessorsList();
        pl.add(new Compressor(9));
        pl.add(new Chunker(30)); // comment
        content = pl.process(content, headers);
        
        if (content != null) {
        	// uncomment next line
            //headers.add("Content-Length: " + content.length + "\r\n");
            headers.add("Connection: close\r\n\r\n");
            
            os.write(getBinaryHeaders(headers));
            os.write(content);
        } else {
            byte[] resp = "HTTP/1.1 404 Not Found\r\n\r\n".getBytes();
            os.write(resp);
        }
    }

    @Override
    public void run() {
        try {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            int r, offset = 0;
            byte[] buf = new byte[10240];

            do {
                r = is.read(buf, offset, buf.length - offset);
                if (r > 0) {
                    offset += r;
                    
                    for (int i = 0; i < offset; i++) {
                        if (buf[i] == (byte)13) {
                            if ((i + 3 < offset) &&
                                (buf[i + 1] == (byte)10) &&
                                (buf[i + 2] == (byte)13) &&
                                (buf[i + 3] == (byte)10))
                            {
                                String request = new String(buf, 0, offset - 4);
                                
                                buf = new byte[10240];
                                offset = 0;
                                
                                process(request, os);
                            }
                        }
                    }
                }
            } while ( ! isInterrupted());
            
            clients.remove(this);

        } catch (Exception ex) {
            return;
        }
    }
}