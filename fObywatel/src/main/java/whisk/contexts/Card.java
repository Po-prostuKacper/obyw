package whisk.contexts;

import org.json.JSONObject;
import whisk.server.Request;
import whisk.server.Response;
import whisk.server.utils.Context;
import whisk.utils.Errors;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static whisk.Main.*;

public class Card extends Context {

    @Override
    public void all(Request request, Response response) {
        if (!checkHost(request)){
            response.close();
            return;
        }

        try {
            int id = checkCardAccess(request);
            if (id == 0){
                sendUnauthorized(response);
            }else{
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM cards WHERE id = ?;");
                statement.setInt(1, id);
                ResultSet set = statement.executeQuery();

                JSONObject card = createCards(set).getJSONObject(0);
                response.sendJSON(card);
            }
        }catch (NumberFormatException e){
            response.setCode(400);
            response.send("There was an error while parsing a number");
        }catch (SQLException e){
            response.setCode(400);
            response.send(Errors.SQL);
        }catch (RuntimeException e){
            response.setCode(500);
            response.send(Errors.UNKNOWN);
        }
    }
}
