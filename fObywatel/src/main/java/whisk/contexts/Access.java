package whisk.contexts;

import whisk.server.Request;
import whisk.server.Response;
import whisk.server.utils.Context;

import java.io.File;

import static whisk.Main.*;
import static whisk.Main.sendUnauthorized;

public class Access extends Context {


    private File file;

    public Access(String file){
        this.file = new File(file.substring(1) + ".html");
    }

    @Override
    public void all(Request request, Response response) {
        if (!checkHost(request)){
            response.close();
            return;
        }

        response.sendFile(file);
    }

}
