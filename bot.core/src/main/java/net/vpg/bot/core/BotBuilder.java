package net.vpg.bot.core;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.data.SerializableData;
import net.vpg.bot.commands.BotCommand;
import net.vpg.bot.database.Database;
import net.vpg.bot.event.handler.EventHandler;
import net.vpg.bot.event.handler.EventProcessor;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.util.*;
import java.util.function.Consumer;

public class BotBuilder implements SerializableData {
    final Map<String, ButtonHandler> buttonHandlers = new CaseInsensitiveMap<>();
    final Map<String, BotCommand> commands = new CaseInsensitiveMap<>();
    final Map<String, Object> properties = new HashMap<>();
    final EventProcessor processor;
    final boolean light;
    int intents = 0;
    int shardsTotal;
    String id;
    String token;
    String prefix;
    Set<Long> managers;
    ClassFilter filter;
    Database database;

    private BotBuilder(@Nonnull String id, @Nonnull String token, boolean light) {
        setId(id);
        setToken(token);
        this.light = light;
        this.processor = new EventProcessor();
    }

    public static BotBuilder createLight(@Nonnull String id, @Nonnull String token) {
        return new BotBuilder(id, token, true);
    }

    public static BotBuilder createDefault(@Nonnull String id, @Nonnull String token) {
        return new BotBuilder(id, token, false);
    }

    @Nonnull
    public BotBuilder enableIntents(@Nonnull GatewayIntent intent, @Nonnull GatewayIntent... intents) {
        int raw = GatewayIntent.getRaw(intent, intents);
        this.intents |= raw;
        return this;
    }

    @Nonnull
    public BotBuilder disableIntents(@Nonnull GatewayIntent intent, @Nonnull GatewayIntent... intents) {
        int raw = GatewayIntent.getRaw(intent, intents);
        this.intents &= ~raw;
        return this;
    }

    @Nonnull
    public BotBuilder setShardsTotal(int shardsTotal) {
        this.shardsTotal = shardsTotal;
        return this;
    }

    @Nonnull
    public BotBuilder setId(@Nonnull String id) {
        this.id = Objects.requireNonNull(id);
        return this;
    }

    @Nonnull
    public BotBuilder setPrefix(@Nonnull String prefix) {
        this.prefix = Objects.requireNonNull(prefix);
        return this;
    }

    @Nonnull
    public BotBuilder setToken(@Nonnull String token) {
        this.token = Objects.requireNonNull(token);
        return this;
    }

    @Nonnull
    public BotBuilder removeManager(long id) {
        if (managers != null) {
            managers.remove(id);
        }
        return this;
    }

    @Nonnull
    public BotBuilder addManagers(long... ids) {
        if (managers == null) {
            managers = new HashSet<>();
        }
        for (long id : ids) {
            managers.add(id);
        }
        return this;
    }

    @Nonnull
    public BotBuilder addManagers(@Nonnull Collection<Long> ids) {
        if (managers == null) {
            managers = new HashSet<>(ids);
        } else {
            managers.addAll(ids);
        }
        return this;
    }

    @Nonnull
    public BotBuilder setFilter(@Nonnull ClassFilter filter) {
        this.filter = filter;
        return this;
    }

    @Nonnull
    public BotBuilder setDatabase(@Nonnull Database database) {
        this.database = database;
        return this;
    }

    @Nonnull
    public BotBuilder setEventHandler(EventHandler eventHandler) {
        processor.setSubject(eventHandler);
        return this;
    }

    @Nonnull
    public BotBuilder addButtonHandler(String name, ButtonHandler handler) {
        buttonHandlers.put(name, handler);
        return this;
    }

    @Nonnull
    public BotBuilder removeButtonHandler(String name) {
        buttonHandlers.remove(name);
        return this;
    }

    @Nonnull
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Nonnull
    public BotBuilder putProperties(@Nonnull DataObject data) {
        return putProperties(data.toMap());
    }

    @Nonnull
    public BotBuilder putProperties(@Nonnull Map<String, Object> data) {
        data.forEach(this::put);
        return this;
    }

    @Nonnull
    public BotBuilder put(@Nonnull String key, @Nullable Object value) {
        if (value == null) {
            properties.put(key, null);
            return this;
        }
        switch (key) {
            case "id":
                setId((String) value);
                break;
            case "token":
                setToken((String) value);
                break;
            case "prefix":
                setPrefix((String) value);
                break;
            case "managers":
                if (value instanceof long[]) {
                    addManagers((long[]) value);
                } else if (value instanceof Long[]) {
                    addManagers(Arrays.asList((Long[]) value));
                } else if (value instanceof Collection) {
                    try {
                        //noinspection unchecked
                        addManagers((Collection<Long>) value);
                    } catch (ClassCastException ignore) {
                        // ignore
                    }
                }
                break;
            case "shards":
            case "shardsTotal":
                setShardsTotal((int) value);
                break;
            default:
                properties.put(key, value);
        }
        return this;
    }

    public <T extends GenericEvent> BotBuilder addListener(String id, Class<T> type, Consumer<T> action) {
        processor.addListener(id, type, action);
        return this;
    }

    public Bot build() throws LoginException {
        return shardsTotal <= 1 ? new SingleShardBot(this) : new MultiShardBot(this);
    }

    @Nonnull
    @Override
    public DataObject toData() {
        DataObject data = DataObject.empty();
        properties.forEach(data::put);
        return data.put("id", id)
            .put("token", token)
            .put("prefix", prefix)
            .put("managers", managers)
            .put("shardsTotal", shardsTotal);
    }
}
