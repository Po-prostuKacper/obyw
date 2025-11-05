package whisk.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.internal.components.mediagallery.MediaGalleryItemFileUpload;
import org.json.JSONObject;

import java.awt.*;
import java.io.InputStream;

import static whisk.Main.*;

public class DiscordUtils {

    public static Color color = new Color(0xf70f0f);
    public static String icon = "https://i.imgur.com/NXs9usG.png";
    public static String NO_PURCHASE = "Nie dokonałeś(-aś) zakupu.";
    public static String ERROR = "Wystąpił błąd, przepraszamy.";

    public static MessageTopLevelComponent getError(String text){
        Container container = Container.of(
                TextDisplay.of("## Wystąpił błąd\n" +
                        text)
        );
        return container;
    }

    public static MessageTopLevelComponent getSuccess(String text) {
        Container container = Container.of(
                TextDisplay.of("## Sukces\n" +
                        text)
        );
        return container;
    }

    public static FileUpload getUploadFromResources(String name){
        InputStream stream = DiscordUtils.class.getResourceAsStream("/" + name);
        return FileUpload.fromData(stream, name);
    }

    public static StringSelectMenu.Builder getMenu(){
        StringSelectMenu.Builder builder = StringSelectMenu.create("ticket");
        builder.setPlaceholder("❌ Nie wybrałeś(-aś) żadnej kategorii");
        builder.addOption("💰 Zakup", "buy");
        builder.addOption("➕ Uzyskaj pomoc", "help");
        builder.addOption("❔ Zadaj pytanie", "question");
        builder.addOption("💭 Inne", "other");
        return builder;
    }

    public static MessageTopLevelComponent getTicketsComponents(){
        ActionRow row = ActionRow.of(getMenu().build());

        Container container = Container.of(
                TextDisplay.of("## Stwórz ticket\n" +
                        "Chciałbyś(-a) dokonać zakupu, uzyskać pomoc lub masz inną sprawę?\nKliknij w menu poniżej, aby otworzyć nowy ticket."),
                Separator.create(false, Separator.Spacing.SMALL),
                row
        );

        return container;
    }

    public static MediaGallery getGallery(String name){
        MediaGalleryItem item = new MediaGalleryItemFileUpload(getUploadFromResources(name));
        return MediaGallery.of(item);
    }

    public static StringSelectMenu.Builder getTypeMenu(String user){
        StringSelectMenu.Builder builder = StringSelectMenu.create(user);
        builder.setPlaceholder("❌ Nie wybrałeś(-aś) żadnego planu");
        JSONObject types = configuration.getJSONObject("types");
        for (String key : types.keySet()){
            JSONObject type = types.getJSONObject(key);
            String name = type.getString("name");
            builder.addOption(name, key);
        }
        return builder;
    }

    public static MessageTopLevelComponent getDataComponent(String title, String token, String type){
        Container container = Container.of(
                TextDisplay.of("## " + title + "\n" +
                        "Nie udostępniaj swojego tokenu."
                ),
                Separator.create(false, Separator.Spacing.SMALL),
                Section.of(net.dv8tion.jda.api.components.buttons.Button.secondary("claim", "⭐ Odbierz rangę"),
                        TextDisplay.of("**Plan**\n" + type)
                ),
                Separator.create(false, Separator.Spacing.SMALL),
                Section.of(net.dv8tion.jda.api.components.buttons.Button.secondary("copy", "📃 Skopiuj"),
                        TextDisplay.of("**Token**\n" + token)
                ),
                Separator.create(false, Separator.Spacing.SMALL),
                Section.of(Button.link(getReferer(), "🌐 Otwórz"),
                        TextDisplay.of("**Strona**\n" + getReferer())
                )
        );
        return container;
    }

    public static void claimRank(SlashCommandInteractionEvent se, ButtonInteractionEvent be){
        Member member;
        if (se == null) member = be.getMember();
        else member = se.getMember();

        String id = member.getId();
        String token = findUser(id);

        MessageTopLevelComponent component = getError(NO_PURCHASE);

        Role role = guild.getRoleById(configuration.getJSONObject("discord").getString("role"));
        if (role != null && token != null) {
            if (member.getRoles().contains(role)){
                component = getError("Posiadasz już rangę " + role.getAsMention() + ".");
            }else{
                guild.addRoleToMember(member, role).queue();
                component = getSuccess("Ranga " + role.getAsMention() + " została odebrana.");
            }
        }

        if (se == null) be.replyComponents(component).useComponentsV2().setEphemeral(true).queue();
        else se.replyComponents(component).useComponentsV2().setEphemeral(true).queue();
    }

}
