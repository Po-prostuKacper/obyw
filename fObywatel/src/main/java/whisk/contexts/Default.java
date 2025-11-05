package whisk.contexts;

import org.json.JSONArray;
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

public class Default extends Context {

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
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM cards WHERE discordId = ?;");
            statement.setString(1, id);
            ResultSet set = statement.executeQuery();

            JSONArray cards = createCards(set);

            set.close();
            statement.close();

            JSONObject object = new JSONObject();
            object.put("limit", checkLimit(id));
            object.put("admin", checkWebAdmin(id));
            object.put("ids", cards);

            response.sendJSON(object);
        }catch (SQLException e){
            response.setCode(400);
            response.send(Errors.SQL);
        }catch (RuntimeException e){
            response.setCode(500);
            response.send(Errors.UNKNOWN);
        }
    }

}
