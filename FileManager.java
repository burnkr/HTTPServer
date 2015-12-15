package HTTPServer;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class FileManager {
    private String path;
    private int lifeTime = 5; // file life time fo buffer (minutes)

    private static ConcurrentHashMap<String, byte[]> map = new ConcurrentHashMap<String, byte[]>();
    
    public FileManager(String path) {
        if (path.endsWith("/") || path.endsWith("\\"))
        	path = path.substring(0, path.length() - 1);
        
        this.path = path;
    }
    
    public byte[] get(final String url) {
        try {
            if (map.containsKey(url)) {
                return map.get(url);
            } else {
                String fullPath = path + url.replace('/', '\\');
                byte[] buf;

                RandomAccessFile f = new RandomAccessFile(fullPath, "r");
                try {
                    buf = new byte[(int)f.length()];
                    f.read(buf, 0, buf.length);
                } finally {
                    f.close();
                }

                map.put(url, buf);

                // удаление страницы из буфера по истечении 5-и минут
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        map.remove(url);
                    }
                }, lifeTime * 60 * 1000);
                
                return buf;
            }
        } catch (IOException ex) {
            return null;
        }
    }
}
