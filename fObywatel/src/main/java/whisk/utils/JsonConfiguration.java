package whisk.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

public class JsonConfiguration extends JSONObject{

    private File file;

    public JsonConfiguration(File file){

        super();
        this.file = file;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));

            StringBuilder read = new StringBuilder();
            for (String line : reader.lines().toList()){
                read.append("\n").append(line);
            }
            if (!read.isEmpty()){
                read.deleteCharAt(0);
            }
            JSONObject object = new JSONObject(read.toString());
            for (String key : object.keySet()){
                put(key, object.get(key));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (JSONException e){}

    }

    public void save(){

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(toString(4));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
