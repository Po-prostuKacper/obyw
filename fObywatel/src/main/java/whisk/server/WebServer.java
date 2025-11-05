package whisk.server;

import com.sun.net.httpserver.HttpServer;
import whisk.server.utils.Context;
import whisk.server.utils.Method;
import whisk.server.utils.Reader;
import whisk.server.utils.TextMatcher;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

public class WebServer {

    private HttpServer server;
    private WebServer webServer;
    private Charset charset;
    private Reader reader;
    private TextMatcher matcher;

    public WebServer(int port){

        webServer = this;
        charset = StandardCharsets.UTF_8;
        reader = new Reader();
        matcher = new TextMatcher();

        try {
            server = HttpServer.create(new InetSocketAddress(port), 200);
            server.setExecutor(Executors.newCachedThreadPool());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

    }

    public TextMatcher getMatcher(){
        return matcher;
    }

    public void start(){
        server.start();
    }

    public void stop(){
        server.stop(0);
    }

    public void setCharset(Charset charset){
        this.charset = charset;
    }

    public Charset getCharset(){
        return this.charset;
    }

    public Reader getReader(){
        return reader;
    }

    public void addContext(String route, Context context){

        server.createContext(route, exchange -> {

            Request request = new Request(exchange, webServer);
            Response response = new Response(exchange, webServer);

            String method = exchange.getRequestMethod();

            context.all(request, response);

            if (method.equalsIgnoreCase(Method.GET)){
                context.get(request, response);
            }
            else if (method.equalsIgnoreCase(Method.POST)){
                context.post(request, response);
            }
            else if (method.equalsIgnoreCase(Method.PUT)){
                context.put(request, response);
            }
            else if (method.equalsIgnoreCase(Method.DELETE)){
                context.delete(request, response);
            }
            else if (method.equalsIgnoreCase(Method.CONNECT)){
                context.connect(request, response);
            }
            else if (method.equalsIgnoreCase(Method.OPTIONS)){
                context.options(request, response);
            }
            else if (method.equalsIgnoreCase(Method.PATCH)){
                context.patch(request, response);
            }
            else if (method.equalsIgnoreCase(Method.TRACE)){
                context.trace(request, response);
            }
            else if (method.equalsIgnoreCase(Method.HEAD)){
                context.head(request, response);
            }
        });
    }

}