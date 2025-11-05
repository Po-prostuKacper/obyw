package whisk.contexts;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;
import whisk.server.Request;
import whisk.server.Response;
import whisk.server.utils.Context;
import whisk.utils.Errors;

import java.io.IOException;
import java.security.*;
import java.util.Base64;
import java.util.UUID;

import static whisk.Main.*;

public class GenerateQR extends Context {

    @Override
    public void all(Request request, Response response) {
        if (!checkHost(request)){
            response.close();
            return;
        }

        if (!checkUserAccess(request)){
            sendUnauthorized(response);
            return;
        }

        try {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost("https://weryfikator.mobywatel.gov.pl/web/api/verifications");
            post.addHeader("Content-Type", "application/json");

            JSONObject payload = new JSONObject();

            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();

            PublicKey key = pair.getPublic();
            PrivateKey privateKey = pair.getPrivate();

            String base64PrivateKey = Base64.getEncoder().encodeToString(privateKey.getEncoded());

            byte[] encoded = key.getEncoded();
            String base64PublicKey = Base64.getEncoder().encodeToString(encoded);
            String sessionUuid = UUID.randomUUID().toString();

            JSONObject publicKey = new JSONObject();
            publicKey.put("algorithm", "RSA");
            publicKey.put("encoded", base64PublicKey);

            payload.put("publicKey", publicKey);
            payload.put("sessionUuid", sessionUuid);

            post.setEntity(new StringEntity(payload.toString()));

            CloseableHttpResponse clientResponse = client.execute(post);

            JSONObject object = new JSONObject(readInputStream(clientResponse.getEntity().getContent()));

            String code = object.getString("code");
            String qrCode = object.getString("qrCode");

            generatedCodes.addCode(qrCode, code);

            object.put("encodedPublicKey", base64PublicKey);
            object.put("encodedPrivateKey", base64PrivateKey);
            object.put("sessionUuid", sessionUuid);

            response.sendJSON(object);
        }catch (JSONException e){
            response.setCode(400);
            response.send(Errors.JSON);
        }catch (RuntimeException | NoSuchAlgorithmException | IOException e){
            response.setCode(500);
            response.send(Errors.UNKNOWN);
        }
    }

}
