package whisk.contexts;

import whisk.server.Request;
import whisk.server.Response;
import whisk.server.utils.Context;
import whisk.utils.Errors;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static whisk.Main.*;

public class Assets extends Context {

    @Override
    public void all(Request request, Response response){
        if (!checkHost(request)){
            response.close();
            return;
        }

        String url = request.getURL().toString();
        url = transformUrl(url);

        try {
            if (url.equalsIgnoreCase("")){
                response.sendFile(new File("index.html"));
                return;
            }

            if (url.startsWith("assets/")){
                Path basePath = Paths.get("assets").toAbsolutePath().normalize();
                String relativePath = url.substring("assets/".length());
                Path requestedPath = basePath.resolve(relativePath).normalize();

                if (requestedPath.startsWith(basePath)){
                    File asset = requestedPath.toFile();
                    if (asset.exists() && asset.isFile()) {
                        response.sendFile(asset);
                        return;
                    }
                }
            }

            if (match(url, "images")){
                int id = checkCardAccess(request);
                if (id != 0){
                    File image = new File("images/" + id + ".png");
                    if (image.exists()){
                        response.sendFile(image);
                        return;
                    }
                }
            }

            sendNotFound(response);
        }catch (RuntimeException e){
            response.setCode(500);
            response.send(Errors.UNAUTHORIZED);
        }
    }
}
