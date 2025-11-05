package whisk;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import whisk.contexts.*;
import whisk.discord.*;
import whisk.server.Request;
import whisk.server.Response;
import whisk.server.WebServer;
import whisk.server.utils.URLParameters;
import whisk.utils.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static whisk.utils.SqlTables.loadTables;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static GeneratedCodes generatedCodes = new GeneratedCodes();

    public static JDA bot;
    public static Guild guild;
    public static WebServer server;
    public static JsonConfiguration configuration;
    public static TokenHandler tokens = new TokenHandler();
    public static Connection connection;
    public static Backup backup = new Backup();

    public static String RESET = "\u001B[0m";
    public static String BLACK = "\u001B[30m";
    public static String RED = "\u001B[31m";
    public static String GREEN = "\u001B[32m";
    public static String YELLOW = "\u001B[33m";
    public static String BLUE = "\u001B[34m";
    public static String PURPLE = "\u001B[35m";
    public static String CYAN = "\u001B[36m";
    public static String WHITE = "\u001B[37m";

    public static void main(String[] args) {

        log.info("Starting service");
        log.info("Loading files");

        configuration = new JsonConfiguration(new File("configuration.json"));
        log.info("Connecting to database");

        JSONObject mysql = configuration.getJSONObject("mysql");
        try{
            String url = "jdbc:mysql://{1}:{2}/{3}?autoReconnect=true";
            url = url.replaceFirst("\\{1}", mysql.getString("host"));
            Object port = mysql.get("port");
            if (port == JSONObject.NULL){
                url = url.replaceFirst(":\\{2}", "");
            }else{
                url = url.replaceFirst("\\{2}", port.toString());
            }
            url = url.replaceFirst("\\{3}", mysql.getString("database"));

            connection = DriverManager.getConnection(
                    url,
                    mysql.getString("username"),
                    mysql.getString("password")
            );
        }catch (SQLException e){
            e.printStackTrace();
        }
        loadTables();

        log.info("Creating bot");

        JSONObject discord = configuration.getJSONObject("discord");
        bot = JDABuilder.createDefault(discord.getString("token"))
            .enableCache(CacheFlag.VOICE_STATE)
            .enableIntents(GatewayIntent.MESSAGE_CONTENT)
            .enableIntents(GatewayIntent.GUILD_MEMBERS)
            .enableIntents(GatewayIntent.GUILD_MEMBERS)
            .setChunkingFilter(ChunkingFilter.ALL)
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .addEventListeners(new SlashListener(), new ButtonListener(), new MenuListener(), new RemoveListener(), new JoinListener()
        ).build();

        try {
            bot.awaitReady();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        guild = bot.getGuildById(discord.getString("guild"));

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                int count = 0;
                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT * FROM users;");
                    ResultSet set = statement.executeQuery();
                    while (set.next()){
                        count++;
                    }
                    set.close();
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                bot.getPresence().setActivity(Activity.playing("Użytkownicy " + count));
            }
        }, 30000);

        bot.updateCommands().addCommands(
            Commands.slash("ticket", "Wysyła wiadomość, pozwalającą na stworzenie ticketu"),
            Commands.slash("dodaj", "Dodaje użytkownika").addOption(OptionType.STRING, "uzytkownik", "Wprowadź id lub oznacz użytkownika", true),
            Commands.slash("usun", "Usuwa użytkownika").addOption(OptionType.STRING, "uzytkownik", "Wprowadź id lub oznacz użytkownika", true),
            Commands.slash("sprawdz", "Sprawdza status użytkownika").addOption(OptionType.STRING, "uzytkownik", "Wprowadź id lub oznacz użytkownika", true),
            Commands.slash("przypomnij", "Przypomina dane użytkownika"),
            Commands.slash("powitania", "Ustawia kanał z powitaniami"),
            Commands.slash("pozegnania", "Ustawia kanał z pożegnaniami"),
            Commands.slash("wygeneruj", "Generuje nowy token"),
            Commands.slash("odbierz", "Odbiera rangę klienta"),
            Commands.slash("weryfikacja", "Wysyła wiadomość pozwalającą na weyfikację"),
            Commands.slash("resetuj", "Usuwa wszystkie otwarte tickety"),
            Commands.slash("regulamin", "Wysyła regulamin serwera")
        ).queue();

        File backups = new File("backups");
        if (!backups.exists()){
            backups.mkdir();
        }

        File images = new File("images");
        if (!images.exists()){
            images.mkdir();
        }

        log.info("Launching server");

        int port = configuration.getInt("port");
        server = new WebServer(port);

        registerContexts();

        server.start();

        log.info("Using port {}", port);
        log.info("Launch successful!");

        Scanner scanner = new Scanner(System.in);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                backup.createBackup();
            }
        }, configuration.getInt("backup"));
        System.out.print("> ");

        while (scanner.hasNextLine()){
            String message = scanner.nextLine();
            String[] arguments = message.split(" ");

            boolean unkown = false;
            if (arguments[0].equalsIgnoreCase("backup") && arguments.length > 1){
                String type = arguments[1];
                if (type.equalsIgnoreCase("load") && arguments.length > 2){
                    File file = new File("backups/" + arguments[2] + ".json");
                    if (file.exists()){
                        backup.loadBackup(file);
                        System.out.println(PURPLE + "Backup has been loaded into the database" + RESET);
                    }else{
                        System.out.println(RED + "This file doesn't exist" + RESET);
                    }
                }else if (type.equalsIgnoreCase("save")){
                    backup.createBackup();
                    System.out.println(PURPLE + "Backup has been successfully created" + RESET);
                }else unkown = true;
            }else unkown = true;

            if (unkown){
                System.out.println(RED + "Unknown command" + RESET);
            }

            System.out.print("> ");
        }
    }

    public static boolean checkHost(Request request){
        JSONArray hosts = configuration.getJSONArray("hosts");
        boolean allow = false;

        String host = request.getHost();
        for (int i = 0; i < hosts.length(); i++){
            if (host.equalsIgnoreCase(hosts.getString(i))){
                allow = true;
                break;
            }
        }
        return allow;
    }

    private static void registerContexts() {
        server.addContext("/", new Assets());

        List<String> accessPaths = List.of("/dashboard", "/admin", "/generator", "/login", "/demo");
        for (String path : accessPaths) {
            server.addContext(path, new Access(path));
        }

        List<String> restrictedPaths = List.of("/qr", "/scan", "/id", "/card", "/home", "/documents", "/more", "/services", "/show", "/shortcuts", "/document", "/pesel", "/share", "/confirm", "/display");
        for (String path : restrictedPaths) {
            server.addContext(path, new Restricted(path));
        }

        server.addContext("/auth/discord", new Login());
        server.addContext("/panel/default", new Default());
        server.addContext("/panel/admin", new Admin());
        server.addContext("/panel/delete", new Delete());
        server.addContext("/validate", new Validate());
        server.addContext("/get/card", new Card());
        server.addContext("/submit", new Submit());
        server.addContext("/cache/files", new CacheFiles());
        server.addContext("/qr/generate", new GenerateQR());
        server.addContext("/qr/check", new CheckStatus());
        server.addContext("/qr/scan", new ValidateCode());
        server.addContext("/qr/validate", new CheckQR());
        server.addContext("/discord", new Discord());
        server.addContext("/get/branding", new Branding());

        server.addContext("/worker.js",  new Worker());
    }

    public static boolean match(String text, String regex){
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        return matcher.matches();
    }

    public static String formatDate(String unformatted){
        String formatted = null;

        try {
            String[] split = unformatted.split("\\.");
            int day = Integer.parseInt(split[0]);
            int month = Integer.parseInt(split[1]);
            int year = Integer.parseInt(split[2]);

            LocalDate localDate = LocalDate.of(year, month, day);

            formatted = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }catch (NumberFormatException e){}

        return formatted;
    }

    public static String transformUrl(String url){
        if (url.contains("?")){
            url = url.split("\\?")[0];
        }
        url = url.substring(1);
        return url;
    }

    public static String authRequest(Request request){
        String token = null;
        JSONObject object = new JSONObject();
        try {
            object = request.getBody().toJSON();
        }catch (JSONException e){}
        try {
            URLParameters parameters = request.getURLParameters();

            if (object.has("token")){
                token = object.getString("token");
            }else if (parameters.has("token")) {
                token = parameters.getParameter("token");
            }

            if (token != null){
                PreparedStatement statement = connection.prepareStatement("SELECT discordId FROM users WHERE token = ?;");
                statement.setString(1, token);
                ResultSet set = statement.executeQuery();
                if (set.next()) {
                    return set.getString("discordId");
                }
                set.close();
                statement.close();
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static JSONObject requestDiscord(String token){
        try {
            CloseableHttpClient client = HttpClients.createDefault();

            HttpGet get = new HttpGet("https://discordapp.com/api/users/@me");
            get.addHeader("Authorization", "Bearer " + token);
            CloseableHttpResponse response = client.execute(get);
            JSONObject user = new JSONObject(readInputStream(response.getEntity().getContent()));

            return user;
        } catch (IOException e) {}
        return null;
    }

    public static String readInputStream(InputStream inputStream){
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        for (String line : reader.lines().toList()){
            builder.append("\n").append(line);
        }
        if (!builder.isEmpty()){
            builder.deleteCharAt(0);
        }
        return builder.toString();
    }

    public static String generateCardToken(){
        return tokens.generateToken(20, new TokenCheck() {
            @Override
            public void call(String token) {
                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM cards WHERE token = ?;");
                    statement.setString(1, token);
                    ResultSet set = statement.executeQuery();
                    if (set.next()){
                        setSafe(false);
                    }
                    set.close();
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static String generateUserToken(){
        return tokens.generateToken(20, new TokenCheck() {
            @Override
            public void call(String token) {
                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM users WHERE token = ?;");
                    statement.setString(1, token);
                    ResultSet set = statement.executeQuery();
                    if (set.next()){
                        setSafe(false);
                    }
                    set.close();
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static String createUser(String id, String type){
        String token = findUser(id);
        String password = null;

        try {
            PreparedStatement statement;
            if (token != null){
                password = token;
                statement = connection.prepareStatement("UPDATE users SET type = ? WHERE token = ?;");
                statement.setString(1, type);
                statement.setString(2, password);
                statement.execute();
            }else{
                password = generateUserToken();
                statement = connection.prepareStatement("INSERT INTO users (discordId,token,type) VALUES (?,?,?);");
                statement.setString(1, id);
                statement.setString(2, password);
                statement.setString(3, type);
            }
            statement.execute();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return password;
    }

    public static void sendPage(Request request, Response response){
        String url = request.getURL().toString();
        url = transformUrl(url);

        File page = new File(url + ".html");
        if (page.exists()){
            response.sendFile(page);
            return;
        }

        response.close();
    }

    public static boolean checkWebAdmin(String id){
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT type FROM users WHERE discordId = ?;");
            statement.setString(1, id);
            ResultSet set = statement.executeQuery();
            if (set.next() && set.getString("type").equalsIgnoreCase("admin")){
                return true;
            }
            set.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void writeFile(File file, String content){
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean checkAdmin(String user){
        Member member = guild.getMemberById(user);
        return member.hasPermission(Permission.ADMINISTRATOR) || member.isOwner();
    }

    public static boolean checkLimit(String user){
        try {
            int current = 0;

            PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM cards WHERE discordId = ?;");
            statement.setString(1, user);
            ResultSet set = statement.executeQuery();
            while (set.next()){
                current++;
            }
            set.close();
            statement.close();

            int limit = getLimit(user);
            if (current >= limit){
                return true;
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int getLimit(String user){
        int limit = 0;
        try {
            String token = findUser(user);
            if (token == null) return limit;
            String type = getType(token);
            limit = configuration.getJSONObject("types").getJSONObject(type).getInt("limit");
        }catch (JSONException e){}
        return limit;
    }

    public static String getType(String token){
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT type FROM users WHERE token = ?;");
            statement.setString(1, token);
            ResultSet set = statement.executeQuery();
            if (set.next()){
                return set.getString("type");
            }
            set.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getTypeName(String token){
        return  configuration.getJSONObject("types").getJSONObject(getType(token)).getString("name");
    }

    public static void sendNotFound(Response response){
        response.setCode(404);
        response.send("Not found");
    }

    public static void sendUnauthorized(Response response){
        response.setCode(403);
        response.send(Errors.UNAUTHORIZED);
    }

    public static String findUser(String id){
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT token FROM users WHERE discordId = ?;");
            statement.setString(1, id);
            ResultSet set = statement.executeQuery();
            if (set.next()){
                return set.getString("token");
            }
            set.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String readFile(File file){
        try {
            StringBuilder result = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            for (String line : reader.lines().toList()){
                result.append("\n").append(line);
            }
            if (!result.isEmpty()){
                result.deleteCharAt(0);
            }
            return result.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean checkUserAccess(Request request){
        boolean authed = false;
        try {
            URLParameters parameters = request.getURLParameters();
            PreparedStatement statement = null;
            if (parameters.has("token")){
                statement = connection.prepareStatement("SELECT 1 FROM users WHERE token = ?;");
                statement.setString(1, parameters.getParameter("token"));
            }else if (parameters.has("card_token")){
                statement = connection.prepareStatement("SELECT 1 FROM cards WHERE token = ?;");
                statement.setString(1, parameters.getParameter("card_token"));
            }

            authed = statement.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return authed;
    }

    public static int checkCardAccess(Request request){
        try {
            URLParameters params = request.getURLParameters();
            if (params.has("id") && params.has("token")) {
                PreparedStatement userStatement = connection.prepareStatement("SELECT discordId FROM users WHERE token = ?;");
                userStatement.setString(1, params.getParameter("token"));
                ResultSet userSet = userStatement.executeQuery();

                if (userSet.next()){
                    String id = userSet.getString("discordId");

                    PreparedStatement statement = connection.prepareStatement("SELECT discordId FROM cards WHERE id = ?;");
                    int cardId = Integer.parseInt(params.getParameter("id"));
                    statement.setInt(1, cardId);

                    ResultSet set = statement.executeQuery();
                    if (set.next()){
                        String cardOwner = set.getString("discordId");

                        set.close();
                        statement.close();

                        if (cardOwner.equalsIgnoreCase(id) || checkWebAdmin(id)) {
                            return cardId;
                        }
                    }
                }
            }else if (params.has("card_token")){
                PreparedStatement statement = connection.prepareStatement("SELECT id FROM cards WHERE token = ?;");
                statement.setString(1, params.getParameter("card_token"));

                ResultSet set = statement.executeQuery();
                if (set.next()){
                    int cardId = set.getInt("id");

                    set.close();
                    statement.close();

                    return cardId;
                }
            }
        }catch (NumberFormatException e){}
        catch (SQLException e){
            e.printStackTrace();
        }
        return 0;
    }

    public static String getReferer(){
        return configuration.getJSONObject("discord").getString("referer");
    }

    public static JSONArray createCards(ResultSet set){
        JSONArray array = new JSONArray();

        String[] stringFields = {
                "name", "surname", "sex", "nationality", "familyName",
                "fathersFamilyName", "mothersFamilyName", "birthPlace",
                "countryOfBirth", "address1", "address2", "city", "discordId",
                "token", "mothersName", "fathersName"
        };
        String[] intFields = { "day", "month", "year", "id" };

        try {
            while (set.next()){
                JSONObject object = new JSONObject();
                for (String field : stringFields) {
                    object.put(field, set.getString(field));
                }
                for (String field : intFields) {
                    object.put(field, set.getInt(field));
                }
                array.put(object);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return array;
    }

    public static String getGuild(){
        return configuration.getJSONObject("discord").getString("guild");
    }

    public static BufferedImage base64ToPng(String base64){
        try {
            String base = base64.contains(",") ? base64.split(",")[1] : base64;

            byte[] imageBytes = Base64.decodeBase64(base);
            ByteArrayInputStream bais1 = new ByteArrayInputStream(imageBytes);
            ByteArrayInputStream bais2 = new ByteArrayInputStream(imageBytes);
            
            BufferedImage image = ImageIO.read(bais1);
            bais1.close();

            Metadata metadata = ImageMetadataReader.readMetadata(bais2);
            bais2.close();

            int orientation = 1;
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }

            return applyOrientation(image, orientation);
        } catch (IOException | RuntimeException | MetadataException | ImageProcessingException e){
            throw new RuntimeException(e);
        }
    }

    private static BufferedImage applyOrientation(BufferedImage image, int orientation) {
        AffineTransform transform = new AffineTransform();
        int w = image.getWidth();
        int h = image.getHeight();
        int newW = w;
        int newH = h;

        switch (orientation) {
            case 6:
                transform.translate(h, 0);
                transform.rotate(Math.toRadians(90));
                newW = h;
                newH = w;
                break;
            case 3:
                transform.translate(w, h);
                transform.rotate(Math.toRadians(180));
                break;
            case 8:
                transform.translate(0, w);
                transform.rotate(Math.toRadians(270));
                newW = h;
                newH = w;
                break;
            default:
                return image;
        }

        BufferedImage rotatedImage = new BufferedImage(newW, newH, image.getType());
        Graphics2D g2d = rotatedImage.createGraphics();
        g2d.drawImage(image, transform, null);
        g2d.dispose();
        return rotatedImage;
    }

}