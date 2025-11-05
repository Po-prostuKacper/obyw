package whisk.contexts;

import org.json.JSONException;
import org.json.JSONObject;
import whisk.server.Request;
import whisk.server.Response;
import whisk.server.utils.Context;
import whisk.utils.Errors;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static whisk.Main.*;

public class Submit extends Context {

    @Override
    public void all(Request request, Response response) {
        if (!checkHost(request)){
            response.close();
            return;
        }

        String id = authRequest(request);
        if (id == null){
            sendUnauthorized(response);
            return;
        }
        JSONObject body = request.getBody().toJSON();
        try {
            int card = body.getInt("id");
            if (card == 0){
                if (checkLimit(id)){
                    response.setCode(400);
                    response.send("You have reached the limit");
                }else{
                    setData(body, id, 0, response);
                }
            }else{
                PreparedStatement statement = connection.prepareStatement("SELECT discordId FROM cards WHERE id = ?;");
                statement.setInt(1, card);
                ResultSet set = statement.executeQuery();

                if (!set.next()){
                    response.setCode(400);
                    response.send(Errors.SQL);
                    return;
                }
                String discordId = set.getString("discordId");

                set.close();
                statement.close();

                if (!discordId.equalsIgnoreCase(id) && !checkWebAdmin(id)){
                    response.setCode(403);
                    response.send(Errors.UNAUTHORIZED);
                }else{
                    setData(body, discordId, card, response);
                }
            }
        }catch (JSONException e){
            response.setCode(400);
            response.send(Errors.JSON);
        }catch (SQLException e) {
            response.setCode(400);
            response.send(Errors.SQL);
        }catch (RuntimeException e){
            response.setCode(500);
            response.send(Errors.UNKNOWN);
        }
    }

    private void setData(JSONObject body, String user, int id, Response response){
        try {
            if (body.length() > 2000){
                response.setCode(400);
                response.send(Errors.JSON);
                return;
            }

            Map<String, Object> data = new HashMap<>();
            body = body.getJSONObject("data");

            String[] stringFields = {
                    "name", "surname", "sex", "nationality", "familyName",
                    "fathersFamilyName", "mothersFamilyName", "birthPlace",
                    "countryOfBirth", "address1", "address2", "city", "mothersName",
                    "fathersName"
            };
            String[] intFields = { "day", "month", "year" };

            for (String key : stringFields){
                data.put(key, body.getString(key));
            }
            for (String key : intFields){
                data.put(key, body.getInt(key));
            }
            data.put("discordId", user);

            String base = body.getString("image");
            BufferedImage image = toGray(base64ToPng(base));

            int length = base.getBytes(StandardCharsets.UTF_8).length;

            if (length > 50000000){
                response.setCode(400);
                response.send("The image is too large");
            }

            if (id == 0){
                data.put("token", generateCardToken());

                StringBuilder keys = new StringBuilder();
                StringBuilder values = new StringBuilder();
                for (Map.Entry<String, Object> entry : data.entrySet()){
                    keys.append(",").append(entry.getKey());
                    values.append(",?");
                }
                keys.deleteCharAt(0);
                values.deleteCharAt(0);

                PreparedStatement statement = connection.prepareStatement("INSERT INTO cards (" + keys + ") VALUES (" + values + ");");
                int i = 0;
                for (Map.Entry<String, Object> entry : data.entrySet()){
                    i++;
                    statement.setObject(i, entry.getValue());
                }
                statement.execute();
                statement.close();

                statement = connection.prepareStatement("SELECT LAST_INSERT_ID();");
                ResultSet set = statement.executeQuery();

                if (set.next()){
                    id = set.getInt("LAST_INSERT_ID()");
                }

                set.close();
                statement.close();
            }else{
                StringBuilder query = new StringBuilder();
                for (Map.Entry<String, Object> entry : data.entrySet()){
                    query.append(",").append(entry.getKey()).append(" = ?");
                }
                query.deleteCharAt(0);

                PreparedStatement statement = connection.prepareStatement("UPDATE cards SET " + query + " WHERE id = '" + id + "';");
                int i = 0;
                for (Map.Entry<String, Object> entry : data.entrySet()){
                    i++;
                    statement.setObject(i, entry.getValue());
                }
                statement.execute();
                statement.close();
            }

            File file = new File("images/" + id + ".png");
            if (!file.exists()){
                file.createNewFile();
            }
            ImageIO.write(image, "png", file);

            response.send("OK");
        }catch (Exception e){
            response.setCode(500);
            response.send(Errors.UNKNOWN);
        }
    }

    private BufferedImage toGray(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(original.getRGB(x, y), true);
                float avg = (color.getRed() + color.getGreen() + color.getBlue()) / 3f;
                int gray = Math.round(avg);
                Color grayColor = new Color(gray, gray, gray, color.getAlpha());
                grayImage.setRGB(x, y, grayColor.getRGB());
            }
        }

        return grayImage;
    }

}
