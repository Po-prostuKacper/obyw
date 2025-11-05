package whisk.contexts;

import org.json.JSONException;
import org.json.JSONObject;
import whisk.server.Request;
import whisk.server.Response;
import whisk.server.utils.Context;
import whisk.utils.Errors;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static whisk.Main.*;

public class Delete extends Context {

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
        try{
            int card = body.getInt("id");
            PreparedStatement statement;

            boolean authorized = false;
            if (checkWebAdmin(id)){
                authorized = true;
            }else{
                statement = connection.prepareStatement("SELECT 1 FROM cards WHERE id = ? AND discordId = ?;");
                statement.setInt(1, card);
                statement.setString(2, id);
                ResultSet set = statement.executeQuery();
                if (set.next()){
                    authorized = true;
                }
                set.close();
                statement.close();
            }

            if (authorized){
                statement = connection.prepareStatement("DELETE FROM cards WHERE id = ?;");
                statement.setInt(1, card);
                statement.execute();
                statement.close();

                File file = new File("images/" + card + ".png");
                if (file.exists()){
                    file.delete();
                }

                response.send("OK");
            }else{
                sendUnauthorized(response);
            }

        }catch (JSONException e){
            response.setCode(400);
            response.send(Errors.JSON);
        }catch (SQLException e){
            response.setCode(400);
            response.send(Errors.SQL);
        }catch (RuntimeException e){
            response.setCode(500);
            response.send(Errors.UNKNOWN);
        }
    }

}
