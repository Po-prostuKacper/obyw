package whisk.contexts;

import whisk.server.Request;
import whisk.server.Response;
import whisk.server.utils.Context;

import static whisk.Main.configuration;

public class Discord extends Context {

    @Override
    public void all(Request request, Response response) {
        response.redirect(configuration.getJSONObject("branding").getString("invite"));
    }

}
