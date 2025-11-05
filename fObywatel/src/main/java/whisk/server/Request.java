package whisk.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import whisk.server.utils.Body;
import whisk.server.utils.URLParameters;

import java.net.URI;

public class Request {

    private HttpExchange exchange;
    private Body body;
    private URI uri;
    private URLParameters parameters;

    public Request(HttpExchange exchange, WebServer server){
        this.exchange = exchange;
        this.body = new Body(exchange, server);
        this.uri = exchange.getRequestURI();
        this.parameters = new URLParameters(uri);
    }

    public String getHost(){
        return exchange.getRequestHeaders().getFirst("Host");
    }

    public String getClientIp(){
        String ip = null;

        Headers headers = getHeaders();
        String forwarderFor = headers.getFirst("X-Forwarded-For");
        String realIp = headers.getFirst("X-Real-Ip");
        String clientIp = headers.getFirst("X-Client-IP");

        if (forwarderFor != null){
            ip = forwarderFor;
        }else if (realIp != null){
            ip = realIp;
        }else if (clientIp != null){
            ip = clientIp;
        }

        return ip;
    }

    public Headers getHeaders(){
        return exchange.getRequestHeaders();
    }

    public Body getBody(){
        return body;
    }

    public URI getURL(){
        return uri;
    }

    public URLParameters getURLParameters(){
        return parameters;
    }

}
