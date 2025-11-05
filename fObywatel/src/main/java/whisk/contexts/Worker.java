package whisk.contexts;

import whisk.server.Request;
import whisk.server.Response;
import whisk.server.utils.Context;

import java.io.File;

public class Worker extends Context {

    @Override
    public void all(Request request, Response response) {
        response.sendFile(new File("worker.js"));
    }
}
