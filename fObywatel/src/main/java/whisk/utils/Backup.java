package whisk.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static whisk.Main.*;

public class Backup {

    public void createBackup(){
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSSS");

        try {
            File file = new File("backups/backup-" + format.format(date) + ".json");
            if (!file.exists()){
                file.createNewFile();
            }
            JSONObject object = new JSONObject();
            String[] tables = {"cards", "info", "tickets", "users"};
            for (String table : tables){
                object.put(table, backupTable(table));
            }
            writeFile(file, object.toString(4));
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadBackup(File file){
        try {
            JSONObject object = new JSONObject(readFile(file));
            for (String key : object.keySet()){
                if (key.equalsIgnoreCase("info")){
                    JSONObject info = object.getJSONArray(key).getJSONObject(0);
                    PreparedStatement statement = connection.prepareStatement("UPDATE info SET welcome = '" + info.getString("welcome") + "' AND goodbye = '" + info.getString("goodbye") + "';");
                    statement.execute();
                    statement.close();
                }else{
                    JSONArray array = object.getJSONArray(key);
                    for (int i = 0; i < array.length(); i++){
                        JSONObject data = array.getJSONObject(i);
                        StringBuilder keys = new StringBuilder();
                        StringBuilder values = new StringBuilder();
                        for (String column : data.keySet()){
                            keys.append(",").append(column);
                            values.append(",?");
                        }
                        keys.deleteCharAt(0);
                        values.deleteCharAt(0);

                        PreparedStatement statement = connection.prepareStatement("INSERT INTO " + key + " (" + keys + ") VALUES (" + values + ");");
                        int j = 0;
                        for (String column : data.keySet()){
                            j++;
                            statement.setObject(j, data.get(column));
                        }
                        statement.execute();
                        statement.close();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private JSONArray backupTable(String table){
        JSONArray array = new JSONArray();
        try {
            ResultSet set = getResultSet(table);
            List<String> columns = getColumnNames(table);
            while (set.next()){
                JSONObject object = new JSONObject();
                for (String column : columns){
                    object.put(column, set.getObject(column));
                }
                array.put(object);
            }
            set.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return array;
    }

    private List<String> getColumnNames(String table){
        List<String> columns = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement("SHOW COLUMNS FROM `" + table + "`;");
            ResultSet set = statement.executeQuery();
            while (set.next()){
                columns.add(set.getString("Field"));
            }
            set.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return columns;
    }

    private ResultSet getResultSet(String table){
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table);
            ResultSet set = statement.executeQuery();
            return set;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

}
