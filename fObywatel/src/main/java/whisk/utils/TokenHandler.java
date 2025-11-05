package whisk.utils;

import java.util.List;
import java.util.Random;

public class TokenHandler {

    private List<String> chars = List.of("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".split(""));
    private Random random = new Random();

    public String generateToken(int length, TokenCheck check){
        StringBuilder token = new StringBuilder();
        while (token.isEmpty()){
            for (int i = 0; i < length; i++){
                token.append(chars.get(random.nextInt(chars.size())));
            }
            check.call(token.toString());
            if (!check.getSafe()){
                token = new StringBuilder();
            }
        }
        return token.toString();
    }

}
