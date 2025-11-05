package whisk.server.utils;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;
import whisk.server.WebServer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Body {

    private HttpExchange exchange;
    private WebServer server;
    private String read;
    private InputStream body;

    public Body(HttpExchange exchange, WebServer server){
        this.exchange = exchange;
        this.server = server;
        this.body = exchange.getRequestBody();
        this.read = server.getReader().read(body);
    }

    public JSONObject toJSON(){
        return new JSONObject(read);
    }

    public String toString(){
        return read;
    }

    public List<String> toList(){
        List<String> list = new ArrayList<>();
        for (String line : read.split("\n")){
            list.add(line);
        }
        return list;
    }

    public String[] toArray(){
        return read.split("\n");
    }

    public InputStream toInputStream(){
        return body;
    }

}
