package whisk.contexts;

import org.json.JSONArray;
import org.json.JSONObject;
import whisk.server.Request;
import whisk.server.Response;
import whisk.server.utils.Context;
import whisk.utils.Errors;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static whisk.Main.*;

public class Admin extends Context {

    @Override
    public void all(Request request, Response response) {
        if (!checkHost(request)){
            response.close();
            return;
        }

        String id = authRequest(request);
        if (id == null || !checkWebAdmin(id)) {
            sendUnauthorized(response);
            return;
        }

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM cards;");
            ResultSet set = statement.executeQuery();

            JSONArray cards = createCards(set);

            set.close();
            statement.close();

            response.sendJSON(new JSONObject().put("ids", cards));
        } catch (SQLException e) {
            response.setCode(400);
            response.send(Errors.SQL);
        }catch (RuntimeException e){
            response.setCode(500);
            response.send(Errors.UNAUTHORIZED);
        }
    }

}
