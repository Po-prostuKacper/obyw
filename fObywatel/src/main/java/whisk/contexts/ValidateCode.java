package whisk.contexts;

import org.json.JSONException;
import org.json.JSONObject;
import whisk.server.Request;
import whisk.server.Response;
import whisk.server.utils.Context;
import whisk.utils.Errors;

import static whisk.Main.*;

public class ValidateCode extends Context {

    @Override
    public void all(Request request, Response response) {
        if (!checkHost(request)){
            response.close();
            return;
        }

        int cardId = checkCardAccess(request);
        if (cardId == 0){
            sendUnauthorized(response);
            return;
        }

        try {
            JSONObject body = request.getBody().toJSON();
            String validFrom = body.getString("validFrom");
            String validTo = body.getString("validTo");
            String seriesAndNumber = body.getString("seriesAndNumber");
            String pesel = body.getString("pesel");

            if (body.has("qrCode")){
                generatedCodes.setScanned(cardId, null, body.getString("qrCode"), seriesAndNumber, validFrom, validTo, pesel);
            }else if (body.has("code")){
                generatedCodes.setScanned(cardId, body.getString("code"), null, seriesAndNumber, validFrom, validTo, pesel);
            }

            response.send("OK");
        }catch (JSONException e){
            response.setCode(400);
            response.send(Errors.JSON);
        }catch (RuntimeException e){
            response.setCode(400);
            response.send(Errors.UNKNOWN);
        }
    }
}
