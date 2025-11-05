package whisk.contexts;

import org.json.JSONException;
import org.json.JSONObject;
import whisk.server.Request;
import whisk.server.Response;
import whisk.server.utils.Context;
import whisk.utils.Errors;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static whisk.Main.*;

public class Validate extends Context {

    @Override
    public void all(Request request, Response response) {
        if (!checkHost(request)){
            response.close();
            return;
        }

        JSONObject object = new JSONObject();
        int status = 0;
        try {
            JSONObject body = request.getBody().toJSON();
            if (body.has("access")){
                JSONObject user = requestDiscord(body.getString("access"));
                String id = user.getString("id");
                String token = findUser(id);

                if (checkAdmin(id) && token == null){
                    token = createUser(id, "admin");
                }

                if (token == null){
                    status = 2;
                }else{
                    status = 4;
                    object.put("token", token);
                }
            }else if (body.has("token")){
                String token = body.getString("token");
                PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM users WHERE token = ?;");
                statement.setString(1, token);
                ResultSet set = statement.executeQuery();

                if (set.next()){
                    status = 4;
                }else{
                    status = 2;
                }

                set.close();
                statement.close();
            }

            object.put("status", status);
            response.sendJSON(object);
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

}
