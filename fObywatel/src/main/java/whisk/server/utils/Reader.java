package whisk.server.utils;

import java.io.*;

public class Reader {

    public String read(InputStream stream){
        return read(new BufferedReader(new InputStreamReader(stream)));
    }

    public String read(File file){
        try {
            return read(new BufferedReader(new FileReader(file)));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private String read(BufferedReader reader){

        StringBuilder read = new StringBuilder();
        for (String line : reader.lines().toList()){
            read.append("\n" + line);
        }
        if (read.length() > 0){
            read.deleteCharAt(0);
        }

        return read.toString();

    }

}
