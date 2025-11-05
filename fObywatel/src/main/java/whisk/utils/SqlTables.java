package whisk.utils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static whisk.Main.*;

public class SqlTables {

    public static void loadTables(){

        List<String> statements = new ArrayList<>();
        statements.add("CREATE TABLE IF NOT EXISTS users(" +
                "discordId VARCHAR(100)," +
                "token VARCHAR(100)," +
                "type VARCHAR(100)," +
                "PRIMARY KEY (discordId)" +
                ");");

        statements.add("CREATE TABLE IF NOT EXISTS info(" +
                "welcome VARCHAR(100)," +
                "goodbye VARCHAR(100)" +
                ");");

        statements.add("CREATE TABLE IF NOT EXISTS cards(" +
                "id MEDIUMINT AUTO_INCREMENT," +
                "name VARCHAR(100)," +
                "surname VARCHAR(100)," +
                "discordId VARCHAR(100)," +
                "sex VARCHAR(100)," +
                "day INT," +
                "month INT," +
                "year INT," +
                "token VARCHAR(100)," +
                "familyName VARCHAR(100)," +
                "fathersFamilyName VARCHAR(100)," +
                "mothersFamilyName VARCHAR(100)," +
                "nationality VARCHAR(100)," +
                "countryOfBirth VARCHAR(100)," +
                "birthPlace VARCHAR(100)," +
                "address1 VARCHAR(100)," +
                "address2 VARCHAR(100)," +
                "city VARCHAR(100)," +
                "fathersName VARCHAR(100)," +
                "mothersName VARCHAR(100)," +
                "PRIMARY KEY (id)" +
                ");");

        statements.add("CREATE TABLE IF NOT EXISTS tickets(" +
                "id MEDIUMINT AUTO_INCREMENT," +
                "channel VARCHAR(100)," +
                "user VARCHAR(100)," +
                "PRIMARY KEY (id)" +
                ");");

        try {
            for (String s : statements){
                PreparedStatement statement = connection.prepareStatement(s);
                statement.execute();
                statement.close();
            }

            PreparedStatement statement = connection.prepareStatement("SELECT * FROM info;");
            ResultSet set = statement.executeQuery();
            if (!set.next()){
                PreparedStatement setStatement = connection.prepareStatement("INSERT INTO info (welcome,goodbye) VALUES (null,null)");
                setStatement.execute();
                setStatement.close();
            }
            set.close();
            statement.close();
        }catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
