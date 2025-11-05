package whisk.contexts;

import org.json.JSONObject;
import whisk.server.Request;
import whisk.server.Response;
import whisk.server.utils.Context;

import java.net.URLEncoder;

import static whisk.Main.*;

public class Login extends Context {

    @Override
    public void all(Request request, Response response) {
        if (!checkHost(request)){
            response.close();
            return;
        }

        JSONObject discord = configuration.getJSONObject("discord");
        String id = discord.getString("id");
        String referer = getReferer();
        response.redirect("https://discord.com/oauth2/authorize?client_id=" + id + "&response_type=token&redirect_uri=" + URLEncoder.encode(referer + "/login") + "&scope=identify");
    }
}
