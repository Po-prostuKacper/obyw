package whisk.discord;

import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static whisk.Main.*;

public class JoinListener extends ListenerAdapter {

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent e) {
        try {
            if (!getGuild().equalsIgnoreCase(e.getGuild().getId())) return;

            PreparedStatement statement = connection.prepareStatement("SELECT welcome FROM info;");
            ResultSet set = statement.executeQuery();
            set.next();
            Object o = set.getObject("welcome");

            set.close();
            statement.close();

            if (o == null) return;

            TextChannel channel = e.getGuild().getTextChannelById(o.toString());
            if (channel == null) return;

            Member member = e.getMember();

            Section section = Section.of(
                    Thumbnail.fromUrl(e.getMember().getEffectiveAvatarUrl()),
                    TextDisplay.of("## Witamy\n" +
                            "Cześć " + member.getAsMention() + ", witaj na naszym\ndiscordzie. Baw się dobrze.")
            );

            Container container = Container.of(
                    section,
                    Separator.create(true, Separator.Spacing.LARGE),
                    TextDisplay.of("-# Jest nas teraz __" + guild.getMembers().size() + "__, prosimy o zapoznanie się z regulaminem.")
            );

            channel.sendMessageComponents(container).useComponentsV2().queue();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
