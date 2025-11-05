package whisk.discord;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import static whisk.Main.*;
import static whisk.discord.DiscordUtils.*;

public class MenuListener extends ListenerAdapter {

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent e) {
        String menu = e.getSelectMenu().getId();

        if (!getGuild().equalsIgnoreCase(e.getGuild().getId())) return;

        if (!menu.equalsIgnoreCase("ticket")){

            SelectOption option = e.getSelectedOptions().get(0);
            String value = option.getValue();
            String label = option.getLabel();
            String token = createUser(menu, value);

            MessageTopLevelComponent component = getDataComponent("Zakup", token, label);
            e.getChannel().sendMessageComponents(component).useComponentsV2().queue();

            e.getMessage().delete().queue();
            e.replyComponents(getSuccess("Użytkownik został dodany.")).useComponentsV2().setEphemeral(true).queue();
        }else{
            Member member = e.getMember();
            String id = member.getId();

            String channel = null;

            try {
                PreparedStatement statement = connection.prepareStatement("SELECT channel FROM tickets WHERE user = ?;");
                statement.setString(1, id);
                ResultSet set = statement.executeQuery();
                if (set.next()){
                    channel = set.getString("channel");
                }
                set.close();
                statement.close();

                if (channel != null){
                    TextChannel textChannel = guild.getTextChannelById(channel);
                    if (textChannel == null){
                        statement = connection.prepareStatement("DELETE FROM tickets WHERE channel = ?;");
                        statement.setString(1, channel);
                        statement.execute();
                        statement.close();
                    }else{
                        MessageTopLevelComponent component = getError("Posiadasz już otwarty ticket " + textChannel.getAsMention() + ".");
                        e.getMessage().editMessageComponents(getTicketsComponents()).useComponentsV2().queue();
                        e.replyComponents(component).useComponentsV2().setEphemeral(true).queue();
                        return;
                    }
                }

                Collection<Permission> allowMember = new ArrayList<>();
                allowMember.add(Permission.MESSAGE_SEND);
                allowMember.add(Permission.VIEW_CHANNEL);
                Collection<Permission> denyMember = new ArrayList<>();
                Collection<Permission> allowRole = new ArrayList<>();
                Collection<Permission> denyRole = new ArrayList<>();
                denyRole.add(Permission.VIEW_CHANNEL);

                String option = e.getSelectedOptions().get(0).getLabel();

                MessageTopLevelComponent component = Container.of(
                        Section.of(
                                Thumbnail.fromUrl(e.getMember().getEffectiveAvatarUrl()),
                                TextDisplay.of("## Ticket\n" +
                                        "Witaj " + e.getMember().getAsMention() + " jak możemy Ci pomóc?\n\n```" + option + "```"
                                )
                        ),
                        Separator.create(false, Separator.Spacing.SMALL),
                        Separator.create(true, Separator.Spacing.SMALL),
                        Section.of(
                                Button.secondary("close", "❌ zamknij"),
                                TextDisplay.of("-# Za pusty ticket użytkownik może zostać ukarany.")
                        )
                );

                statement = connection.prepareStatement("INSERT INTO tickets (user) VALUES (?);");
                statement.setString(1, id);
                statement.execute();
                statement.close();

                int ticket = 0;
                statement = connection.prepareStatement("SELECT LAST_INSERT_ID();");
                set = statement.executeQuery();
                if (set.next()){
                   ticket = set.getInt("LAST_INSERT_ID()");
                }
                set.close();
                statement.close();

                int finalTicket = ticket;
                guild.createTextChannel("🎫-ticket-" + ticket)
                        .addMemberPermissionOverride(e.getMember().getIdLong(), allowMember, denyMember)
                        .addRolePermissionOverride(guild.getPublicRole().getIdLong(), allowRole, denyRole)
                        .queue(textChannel -> {
                            try {
                                PreparedStatement ticketStatement = connection.prepareStatement("UPDATE tickets SET channel = ? WHERE id = ?;");
                                ticketStatement.setString(1, textChannel.getId());
                                ticketStatement.setInt(2, finalTicket);
                                ticketStatement.execute();
                                ticketStatement.close();

                                MessageTopLevelComponent success = getSuccess("Ticket został otwarty " + textChannel.getAsMention() + ".");

                                e.getMessage().editMessageComponents(getTicketsComponents()).useComponentsV2().queue();
                                e.replyComponents(success).useComponentsV2().setEphemeral(true).queue();

                                textChannel.sendMessage("||" + guild.getPublicRole().getAsMention() + "||").queue();
                                textChannel.sendMessageComponents(component).useComponentsV2().queue();
                            }catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        });

            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

}
