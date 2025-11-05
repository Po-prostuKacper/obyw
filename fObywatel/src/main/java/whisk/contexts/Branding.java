package whisk.contexts;

import org.json.JSONObject;
import whisk.server.Request;
import whisk.server.Response;
import whisk.server.utils.Context;

import static whisk.Main.checkHost;
import static whisk.Main.configuration;

public class Branding extends Context {

    @Override
    public void all(Request request, Response response) {
        if (!checkHost(request)){
            response.close();
            return;
        }

        JSONObject branding = configuration.getJSONObject("branding");
        response.sendJSON(branding);
    }

}
