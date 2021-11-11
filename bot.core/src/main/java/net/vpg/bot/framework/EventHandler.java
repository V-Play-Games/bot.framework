/*
 * Copyright 2021 Vaibhav Nargwani
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.vpg.bot.framework;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ExceptionEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.vpg.bot.framework.commands.BotCommand;
import net.vpg.bot.framework.commands.CommandReceivedEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class EventHandler extends ListenerAdapter {
    private final Bot bot;
    private final AtomicInteger shardsInit = new AtomicInteger();
    private Pattern selfMention;
    private boolean closed;

    public EventHandler(Bot bot) {
        this.bot = bot;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public Pattern getSelfMentionPattern() {
        return selfMention == null ? selfMention = Pattern.compile("<@!?" + bot.getPrimaryShard().getSelfUser().getIdLong() + ">") : selfMention;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        String prefix = Util.getPrefix(e, bot);
        if (closed) {
            if (e.getMessage().getContentRaw().equalsIgnoreCase(prefix + "activate") && bot.isManager(e.getAuthor().getIdLong())) {
                closed = false;
                e.getChannel().sendMessage("Thanks for activating me again!").queue();
            }
        } else if (!e.getAuthor().isBot() && (!e.isFromGuild() || ((TextChannel) e.getChannel()).canTalk())) {
            Message message = e.getMessage();
            String content = message.getContentRaw();
            String[] args = Util.DELIMITER.split(content);
            if (content.regionMatches(true, 0, prefix, 0, prefix.length())) {
                Optional.ofNullable(bot.getCommands().get(args[0].substring(prefix.length()).toLowerCase()))
                    .ifPresent(command -> CommandReceivedEvent.run(e, args, command, prefix));
            } else if (getSelfMentionPattern().matcher(content).find()) {
                e.getChannel().sendMessage("Prefix: " + prefix).queue();
            }
        }
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent e) {
        if (closed) {
            if (e.getName().equalsIgnoreCase("activate") && bot.isManager(e.getUser().getIdLong())) {
                closed = false;
                e.getChannel().sendMessage("Thanks for activating me again!").queue();
            }
        } else {
            BotCommand command = bot.getCommands().get(e.getName());
            if (command != null) {
                CommandReceivedEvent.run(e, command);
            }
        }
    }

    @Override
    public void onException(@Nonnull ExceptionEvent e) {
        e.getCause().printStackTrace();
    }

    @Override
    public void onReady(@Nonnull ReadyEvent e) {
        try {
            // init on last shard only
            if (shardsInit.get() == e.getJDA().getShardInfo().getShardTotal() - 1)
                bot.load();
            else
                shardsInit.incrementAndGet();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    @Override
    public void onButtonClick(@Nonnull ButtonClickEvent e) {
        String method = Util.getMethod(e.getComponentId());
        String[] args = Util.getArgs(e.getComponentId()).split(":");
        bot.getButtonHandlers().get(method).handle(e, args);
    }
}