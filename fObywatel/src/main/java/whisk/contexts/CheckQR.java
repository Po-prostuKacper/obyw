package whisk.contexts;

import org.json.JSONException;
import org.json.JSONObject;
import whisk.server.Request;
import whisk.server.Response;
import whisk.server.utils.Context;
import whisk.utils.Errors;

import static whisk.Main.*;

public class CheckQR extends Context {

    @Override
    public void all(Request request, Response response) {

        if (!checkHost(request)) {
            response.close();
            return;
        }

        if (!checkUserAccess(request)) {
            sendUnauthorized(response);
            return;
        }

        try {
            JSONObject body = request.getBody().toJSON();

            boolean found = false;
            if (body.has("code")){
                found = generatedCodes.checkCode(null, body.getString("code"));
            }else if (body.has("qrCode")){
                found = generatedCodes.checkCode(body.getString("qrCode"), null);
            }

            response.sendJSON(new JSONObject().put("found", found));
        }catch (JSONException e){
            response.setCode(400);
            response.send(Errors.JSON);
        }catch (RuntimeException e){
            response.setCode(500);
            response.send(Errors.UNKNOWN);
        }

    }
}
