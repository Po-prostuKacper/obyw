package whisk.server.utils;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

public class URLParameters {

    private String uri;
    private Map<String, String> parameters = new HashMap<>();
    private String url;

    public URLParameters(String uri){
        this.uri = uri;
        prepareURI();
    }

    public URLParameters(URI uri){
        this.uri = uri.toString();
        prepareURI();
    }

    private void prepareURI(){

        String[] querySplit = uri.split("\\?");
        this.url = querySplit[0];

        if (!uri.contains("?") || querySplit.length == 1) return;

        String query = querySplit[1];
        String[] paramsSplit = query.split("&");

        for (String part : paramsSplit) {

            String[] paramSplit = part.split("=");

            if (paramSplit.length > 1){

                String key = URLDecoder.decode(paramSplit[0]);
                String value = URLDecoder.decode(paramSplit[1]);

                parameters.put(key, value);

            }
        }
    }

    public String getParameter(String key){
        return parameters.get(key);
    }

    public boolean has(String key){
        return parameters.containsKey(key);
    }

    public void addParameter(String key, String value){
        parameters.put(key, value);
    }

    public void setParameter(String key, String value){

        if (parameters.containsKey(key)){
            parameters.replace(key, value);
        }else{
            addParameter(key, value);
        }

    }

    public void deleteParameter(String key){
        if (parameters.containsKey(key)){
            parameters.remove(key);
        }
    }

    public void clearParameters(){
        parameters.clear();
    }

    public String toString(){
        return prepareString(false);
    }

    public String toString(boolean encoded){
        return prepareString(encoded);
    }

    public Map<String, String> toMap(){
        return parameters;
    }

    private String prepareString(boolean encoded){

        StringBuilder finalUrl = new StringBuilder(url);

        if (!parameters.isEmpty()){
            finalUrl.append("?");
        }

        List<String> paramList = new ArrayList<>();
        for (Map.Entry<String, String> entry : parameters.entrySet()){

            String key = entry.getKey();
            String value = entry.getValue();

            if (encoded){
                key = URLEncoder.encode(key);
                value = URLEncoder.encode(value);
            }

            paramList.add(key + "=" + value + "&");
        }

        Collections.reverse(paramList);
        for (String param : paramList){
            finalUrl.append(param);
        }

        if (finalUrl.length() > 0){
            finalUrl.deleteCharAt(finalUrl.length() - 1);
        }

        return finalUrl.toString();

    }

}
