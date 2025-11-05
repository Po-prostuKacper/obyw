package whisk.discord;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static whisk.Main.*;
import static whisk.discord.DiscordUtils.*;

public class ButtonListener extends ListenerAdapter {

    private Random random = new Random();

    @Override
    public void onButtonInteraction(ButtonInteractionEvent e) {
        String button = e.getButton().getId();
        Member member = e.getMember();
        if (member == null) return;

        if (!getGuild().equalsIgnoreCase(e.getGuild().getId())) return;
        if (button.equalsIgnoreCase("close")){
            String channel = e.getChannel().getId();
            try {
                PreparedStatement statement = connection.prepareStatement("DELETE FROM tickets WHERE channel = ?;");
                statement.setString(1, channel);
                statement.execute();
                statement.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.getChannel().delete().queue();
        }else if (button.equalsIgnoreCase("claim")){
            claimRank(null, e);
        }else if (button.equalsIgnoreCase("copy")){
            String message = e.getMessage().getComponents().get(0).toString();
            Pattern pattern = Pattern.compile(".*\\*\\*Token\\*\\*\\n([A-Za-z0-9_]+).*");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()){
                e.reply(matcher.group(1)).setEphemeral(true).queue();
            }else {
                e.replyComponents(getError(ERROR)).useComponentsV2().setEphemeral(true).queue();
            }
        }
    }
}
