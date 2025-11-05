package whisk.discord;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static whisk.Main.*;
import static whisk.discord.DiscordUtils.*;

public class SlashListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        String command = e.getName();
        Member member = e.getMember();
        if (member == null) return;
        String id = member.getId();
        if (!getGuild().equalsIgnoreCase(e.getGuild().getId())) return;
        if (command.equalsIgnoreCase("przypomnij")){
            String token = findUser(id);
            if (token == null){
                e.replyComponents(getError(NO_PURCHASE)).useComponentsV2().setEphemeral(true).queue();
            }else{
                String type = getTypeName(token);
                MessageTopLevelComponent component = getDataComponent("Twoje dane", token, type);
                e.replyComponents(component).useComponentsV2().setEphemeral(true).queue();
            }
        }else if (command.equalsIgnoreCase("wygeneruj")){
            try {
                String password = findUser(id);
                MessageTopLevelComponent component;
                if (password == null){
                    component = getError(NO_PURCHASE);
                }else{
                    String newPassword = generateUserToken();

                    PreparedStatement statement = connection.prepareStatement("UPDATE users SET token = ? WHERE token = ?;");
                    statement.setString(1, newPassword);
                    statement.setString(2, password);
                    statement.execute();
                    statement.close();

                    component = Container.of(
                            TextDisplay.of("## Generowanie\nNie udostępniaj swojego tokenu."),
                            Separator.create(false, Separator.Spacing.SMALL),
                            Section.of(Button.secondary("copy", "📃 Skopiuj"),
                                    TextDisplay.of("**Token**\n" + newPassword)
                            )
                    );
                }
                e.replyComponents(component).useComponentsV2().setEphemeral(true).queue();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }else if (command.equalsIgnoreCase("odbierz")){
            claimRank(e, null);
        }else {
            if (member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR)){
                if (command.equalsIgnoreCase("ticket")){
                    replySent(e);
                    e.getChannel().sendMessageComponents(getTicketsComponents()).useComponentsV2().queue();
                }else if (command.equalsIgnoreCase("dodaj")){
                    String option = e.getOption("uzytkownik").getAsString();
                    String formatted = checkFormat(option);

                    Container container = Container.of(
                            TextDisplay.of("## Dodaj\n" +
                                    "Wybierz plan, który zakupił użytkownik."
                            ),
                            ActionRow.of(getTypeMenu(formatted).build())
                    );
                    e.replyComponents(container).useComponentsV2().setEphemeral(true).queue();
                }else if (command.equalsIgnoreCase("usun")){
                    String option = e.getOption("uzytkownik").getAsString();
                    String token = getTokenFromMention(e);
                    MessageTopLevelComponent component;

                    if (token == null){
                        component = getError("Podany użytkownik nie dokonał zakupu.");
                    }else{
                        component = getSuccess("Użytkownik " + option + " został usunięty.");
                        try {
                            PreparedStatement statement = connection.prepareStatement("DELETE FROM users WHERE token = ?;");
                            statement.setString(1, token);
                            statement.execute();
                            statement.close();
                        }catch (SQLException ex){
                            ex.printStackTrace();
                        }
                    }
                    e.replyComponents(component).useComponentsV2().setEphemeral(true).queue();
                }else if (command.equalsIgnoreCase("resetuj")){
                    try {
                        PreparedStatement statement = connection.prepareStatement("SELECT channel FROM tickets");
                        ResultSet set = statement.executeQuery();
                        while (set.next()){
                            TextChannel channel = guild.getTextChannelById(set.getString("channel"));
                            if (channel != null){
                                channel.delete().queue();
                            }
                        }
                        set.close();
                        statement.close();

                        statement = connection.prepareStatement("DELETE FROM tickets");
                        statement.execute();
                        statement.close();

                        e.replyComponents(getSuccess("Wszystkie tickety zostały zamknięte.")).useComponentsV2().setEphemeral(true).queue();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }else if (command.equalsIgnoreCase("sprawdz")){
                    String token = getTokenFromMention(e);
                    MessageTopLevelComponent component;
                    if (token == null){
                        component = getError("Podany użytkownik nie dokonał zakupu.");
                    }else{
                        String type = getTypeName(token);
                        component = Container.of(
                                TextDisplay.of("## Sprawdź\nUżytkownik dokonał zakupu."),
                                Separator.create(false, Separator.Spacing.SMALL),
                                Section.of(Button.secondary("claim", "⭐ Odbierz rangę"),
                                        TextDisplay.of("**Plan**\n" + type)
                                ),
                                Separator.create(false, Separator.Spacing.SMALL),
                                Section.of(Button.secondary("copy", "📃 Skopiuj"),
                                        TextDisplay.of("**Token**\n" + token)
                                )
                        );
                    }
                    e.replyComponents(component).useComponentsV2().setEphemeral(true).queue();
                }else if (command.equalsIgnoreCase("powitania")){
                    MessageChannelUnion channelUnion = e.getChannel();

                    MessageTopLevelComponent component = getSuccess("Kanał z powitaniami został zmieniony na " + channelUnion.getAsMention() + ".");
                    try {
                        PreparedStatement statement = connection.prepareStatement("UPDATE info SET welcome = ?;");
                        statement.setString(1, channelUnion.getId());
                        statement.execute();
                        statement.close();
                    }catch (SQLException ex){
                        ex.printStackTrace();
                    }
                    e.replyComponents(component).useComponentsV2().setEphemeral(true).queue();
                }else if (command.equalsIgnoreCase("pozegnania")){
                    MessageChannelUnion channelUnion = e.getChannel();

                    MessageTopLevelComponent component = getSuccess("Kanał z pożegnaniami został zmieniony na " + channelUnion.getAsMention() + ".");
                    try {
                        PreparedStatement statement = connection.prepareStatement("UPDATE info SET goodbye = ?;");
                        statement.setString(1, channelUnion.getId());
                        statement.execute();
                        statement.close();
                    }catch (SQLException ex){
                        ex.printStackTrace();
                    }
                    e.replyComponents(component).useComponentsV2().setEphemeral(true).queue();
                }else if (command.equalsIgnoreCase("regulamin")){
                    Container container = Container.of(getGallery("rules.png"),
                            TextDisplay.of(
                                    "## Regulamin serwera fObywatel\n" +
                                            "Nieznajomość regulaminu nie zwalnia z jego przestrzegania.\n"),
                            Separator.create(true, Separator.Spacing.LARGE),
                            TextDisplay.of(
                                    "\n" +
                                            "### 1. Cel usługi\n" +
                                            "\n" +
                                            "Nasza aplikacja (w każdej wersji) jest wyłącznie produktem kolekcjonerskim i nie stanowi żadnej formy autentycznego dokumentu tożsamości ani substytutu takich dokumentów, " +
                                            "w tym dowodu osobistego, paszportu, prawa jazdy czy jakiegokolwiek innego dokumentu wydawanego przez organy państwowe.\n" +
                                            "\n" +
                                            "### 2. Brak powiązania z mObywatel\n" +
                                            "\n" +
                                            "Aplikacja fobywatel nie jest związana, autoryzowana ani wspierana przez twórców aplikacji mObywatel ani jakiekolwiek inne podmioty rządowe.\n" +
                                            "\n" +
                                            "### 3. Zakaz wykorzystania do celów praktycznych\n" +
                                            "\n" +
                                            "- weryfikacji danych osobowych,\n" +
                                            "- weryfikacji tożsamości,\n" +
                                            "- potwierdzania autentyczności dokumentów lub danych,\n" +
                                            "- innych czynności, które są wyłącznie zarezerwowane dla \n" +
                                            "oficjalnych dokumentów tożsamości lub aplikacji państwowych.\n" +
                                            "\n" +
                                            "### 4. Przeznaczenie aplikacji\n" +
                                            "\n" +
                                            "Aplikacja fObywatel przeznaczona jest wyłącznie do celów kolekcjonerskich i nie może być wykorzystywana w żadnym przypadku do tworzenia, modyfikowania lub " +
                                            "wykorzystywania jakichkolwiek dokumentów tożsamości lub danych identyfikacyjnych.\n" +
                                            "\n" +
                                            "### 5. Wykorzystywanie danych\n" +
                                            "\n" +
                                            "Nasza aplikacja przechowuje wprowadzone przez użytkownika dane, w celu pokazania ich w aplikacji fObywatel. " +
                                            "Użytkownik w każdej chwili może usunąć swoje dane korzystając z panelu."),
                            Separator.create(true, Separator.Spacing.LARGE),
                            TextDisplay.of("-# W przypadku aktualizacji regulaminu użytkownicy otrzymają odpowiednią informację.")
                    );

                    replySent(e);
                    e.getChannel().sendMessageComponents(container).useComponentsV2().queue();
                }else if (command.equalsIgnoreCase("weryfikacja")){
                    String url = configuration.getJSONObject("discord").getString("verify");
                    Container container = Container.of(
                            TextDisplay.of("## Weryfikacja\nAby uzyskać dostęp do wszystkich kanałów, zweryfikuj\nswoje konto discord, klikając przycisk poniżej."),
                            Separator.create(true, Separator.Spacing.LARGE),
                            ActionRow.of(Button.link(url, "🤖 Zweryfikuj się"))
                    );

                    replySent(e);
                    e.getChannel().sendMessageComponents(container).useComponentsV2().queue();
                }
            }else{
                e.replyComponents(getError("Nie posiadasz uprawnień, aby użyć tej komendy.")).useComponentsV2().setEphemeral(true).queue();
            }
        }
    }

    private String checkFormat(String option){

        String id;
        if (match(option, "<@.*>")){
            id = option.substring(2, option.length()-1);
        }else{
            id = option;
        }
        return id;

    }

    private String getTokenFromMention(SlashCommandInteractionEvent e){
        String option = e.getOption("uzytkownik").getAsString();
        option = checkFormat(option);
        String token = findUser(option);
        return token;
    }

    private void replySent(SlashCommandInteractionEvent e){
        e.replyComponents(getSuccess("Wiadomość została wysłana.")).useComponentsV2().setEphemeral(true).queue();
    }

}
