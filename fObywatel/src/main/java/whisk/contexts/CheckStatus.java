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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import static whisk.Main.*;

public class CheckStatus extends Context {

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

            String base64PrivateKey = body.getString("encodedPrivateKey");
            String encodedPublicKey = body.getString("encodedPublicKey");
            String sessionUuid = body.getString("sessionUuid");
            String secret = body.getString("secret");
            String qrCode = body.getString("qrCode");

            JSONObject object = generatedCodes.checkScanned(qrCode);
            if (object != null){
                generatedCodes.deleteCode(qrCode);

                int cardId = object.getInt("id");

                PreparedStatement statement = connection.prepareStatement("SELECT * FROM cards WHERE id = ?;");
                statement.setInt(1, cardId);
                ResultSet set = statement.executeQuery();

                JSONObject card = createCards(set).getJSONObject(0);
                object.put("fatherName", card.getString("fathersName"));
                object.put("names", card.getString("name"));
                object.put("surname", card.getString("surname"));
                object.put("citizenship", card.getString("nationality"));
                object.put("motherName", card.getString("mothersName"));

                File file = new File("images/" + cardId + ".png");
                if (file.exists()){
                    FileInputStream inputStream = new FileInputStream(file);
                    byte[] imageData = new byte[(int) file.length()];
                    inputStream.read(imageData);
                    inputStream.close();

                    object.put("picture", Base64.getEncoder().encodeToString(imageData));
                }

                object.remove("id");

                LocalDate localDate = LocalDate.of(card.getInt("year"), card.getInt("month"), card.getInt("day"));
                object.put("birthDate", localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

                response.sendJSON(object);
                return;
            }

            byte[] keyBytes = Base64.getDecoder().decode(base64PrivateKey);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost("https://weryfikator.mobywatel.gov.pl/web/api/verifications/" + sessionUuid + "/data/encrypt-and-get");
            post.addHeader("Content-Type", "application/json");

            JSONObject payload = new JSONObject();
            payload.put("secret", secret);

            JSONObject publicKey = new JSONObject();
            publicKey.put("algorithm", "RSA");
            publicKey.put("encoded", encodedPublicKey);
            payload.put("publicKey", publicKey);
            post.setEntity(new StringEntity(payload.toString()));
            CloseableHttpResponse clientResponse = client.execute(post);

            if (clientResponse.getStatusLine().getStatusCode() == 200) {
                JSONObject jsonObject = new JSONObject(readInputStream(clientResponse.getEntity().getContent()));

                OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                        "SHA-256",
                        "MGF1",
                        MGF1ParameterSpec.SHA256,
                        PSource.PSpecified.DEFAULT
                );

                Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
                rsaCipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);
                byte[] decryptedAesKey = rsaCipher.doFinal(Base64.getDecoder().decode(jsonObject.getString("encryptedEncryptionKey")));

                Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                aesCipher.init(Cipher.DECRYPT_MODE,
                        new SecretKeySpec(decryptedAesKey, "AES"),
                        new IvParameterSpec(Base64.getDecoder().decode(jsonObject.getString("dataEncryptionIv")))
                );
                byte[] decryptedBytes = aesCipher.doFinal(Base64.getDecoder().decode(jsonObject.getString("encryptedData")));

                response.sendJSON(new JSONObject(new String(decryptedBytes, StandardCharsets.UTF_8)));
            }else {
                response.setCode(204);
                response.send();
            }
        }catch (SQLException e){
            response.setCode(400);
            response.send(Errors.SQL);
        }catch (JSONException e){
            response.setCode(400);
            response.send(Errors.JSON);
        }catch (RuntimeException | IOException | IllegalBlockSizeException | BadPaddingException |
                NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                InvalidAlgorithmParameterException | InvalidKeySpecException e){
            response.setCode(500);
            response.send(Errors.UNKNOWN);
        }
    }

}
