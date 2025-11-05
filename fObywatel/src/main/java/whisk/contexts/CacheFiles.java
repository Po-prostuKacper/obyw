package whisk.contexts;

import org.json.JSONArray;
import org.json.JSONObject;
import whisk.server.Request;
import whisk.server.Response;
import whisk.server.utils.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static whisk.Main.checkHost;

public class CacheFiles extends Context {

    @Override
    public void all(Request request, Response response) {
        if (!checkHost(request)){
            response.close();
            return;
        }

        File assets = new File("assets/app");
        List<File> files = getFilesFromDirectory(assets);

        JSONArray array = new JSONArray();
        for (File file : files){
            array.put(file.toString().replaceAll("\\\\", "/"));
        }
        response.sendJSON(new JSONObject().put("files", array));
    }

    public List<File> getFilesFromDirectory(File file){
        List<File> files = new ArrayList<>();
        for (File f : file.listFiles()){
            if (f.isFile()){
                files.add(f);
            }else if (f.isDirectory()){
                files.addAll(getFilesFromDirectory(f));
            }
        }
        return files;
    }

}
