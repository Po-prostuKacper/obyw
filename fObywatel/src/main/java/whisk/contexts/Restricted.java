package whisk.contexts;

import whisk.server.Request;
import whisk.server.Response;
import whisk.server.utils.Context;
import whisk.server.utils.URLParameters;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static whisk.Main.*;

public class Restricted extends Context {

    private File file;

    public Restricted(String file){
        this.file = new File(file.substring(1) + ".html");
    }

    @Override
    public void all(Request request, Response response) {
        if (!checkHost(request)){
            response.close();
            return;
        }

        if (checkUserAccess(request)) {
            response.sendFile(file);
        }else{
            sendUnauthorized(response);
        }
    }
}
