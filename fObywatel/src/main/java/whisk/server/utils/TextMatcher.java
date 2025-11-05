package whisk.server.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextMatcher {

    public boolean match(String text, Pattern pattern){
        Matcher matcher = pattern.matcher(text);
        return matcher.matches();
    }

}
