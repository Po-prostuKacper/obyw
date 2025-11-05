package whisk.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

public class Response {

    private HttpExchange exchange;
    private int code;
    private String mimeType;
    private WebServer server;

    public Response(HttpExchange exchange, WebServer server){
        this.exchange = exchange;
        this.code = 200;
        this.server = server;
    }

    public void setCode(int code){
        this.code = code;
    }

    public void sendFile(File file){
        try {
            if (mimeType == null){
                mimeType = Files.probeContentType(file.toPath());
            }

            if (server.getMatcher().match(mimeType, Pattern.compile("image/.*"))){
                exchange.sendResponseHeaders(code, file.length());
                OutputStream outputStream = exchange.getResponseBody();
                Files.copy(file.toPath(), outputStream);
                outputStream.flush();
                outputStream.close();
            }else{
                String read = server.getReader().read(file);
                sendResponse(read);
                writeOutputStream(read.getBytes(server.getCharset()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void redirect(String url){
        setMimeType("text/html");
        String html = "<script>location.href = '" + url + "'</script>";
        sendResponse(html);
        writeOutputStream(html.getBytes(server.getCharset()));
    }

    public void setMimeType(String mimeType){
        this.mimeType = mimeType;
    }

    public void sendJSON(JSONObject object){
        if (mimeType == null){
            this.mimeType = "application/json";
        }

        sendResponse(object.toString());
        writeOutputStream(object.toString().getBytes(server.getCharset()));
    }

    public void sendFile(InputStream stream){
        String read = server.getReader().read(stream);
        sendResponse(read);
        writeOutputStream(read.getBytes(server.getCharset()));
    }

    public void send(){
        try {
            exchange.sendResponseHeaders(code, -1);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void send(String message){
        if (mimeType == null) {
            this.mimeType = "text/plain";
        }
        sendResponse(message);
        writeOutputStream(message.getBytes(server.getCharset()));
    }

    public void close(){
        exchange.close();
    }

    public Headers getHeaders(){
        return exchange.getResponseHeaders();
    }

    private void writeOutputStream(byte[] bytes){
        OutputStream outputStream = exchange.getResponseBody();
        try {
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void sendResponse(String content){
        try {
            if (mimeType != null){
                String contentType = mimeType;
                contentType = contentType + "; charset=" + server.getCharset();
                exchange.getResponseHeaders().set("Content-Type", contentType);
            }

            exchange.sendResponseHeaders(code, content.getBytes(server.getCharset()).length);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
