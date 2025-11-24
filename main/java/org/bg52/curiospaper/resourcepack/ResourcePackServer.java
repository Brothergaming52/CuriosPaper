package org.bg52.curiospaper.resourcepack;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bg52.curiospaper.CuriosPaper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;

public class ResourcePackServer implements ResourcePackHost {
    private final CuriosPaper plugin;
    private final int port;
    private final File packFile;
    private HttpServer server;

    public ResourcePackServer(CuriosPaper plugin, int port, File packFile) {
        this.plugin = plugin;
        this.port = port;
        this.packFile = packFile;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/pack.zip", new PackHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
            plugin.getLogger().info("Resource pack server started on port " + port);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to start resource pack server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("Resource pack server stopped.");
        }
    }

    public int getPort() {
        return port;
    }

    private class PackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (!packFile.exists()) {
                String response = "Resource pack not found.";
                t.sendResponseHeaders(404, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            t.getResponseHeaders().add("Content-Type", "application/zip");
            t.sendResponseHeaders(200, packFile.length());

            OutputStream os = t.getResponseBody();
            FileInputStream fs = new FileInputStream(packFile);
            final byte[] buffer = new byte[0x10000];
            int count = 0;
            while ((count = fs.read(buffer)) >= 0) {
                os.write(buffer, 0, count);
            }
            fs.close();
            os.close();
        }
    }
}
