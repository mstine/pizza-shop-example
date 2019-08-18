package com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Event;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventHandler;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Matt Stine
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InProcessEventLog implements EventLog {
    private static InProcessEventLog singleton;
    private final Map<Topic, List<EventHandler>> topics = new HashMap<>();
    private final Map<Topic, List<Event>> events = new HashMap<>();

    public static InProcessEventLog instance() {
        if (singleton == null) {
            singleton = new InProcessEventLog();
        }
        return singleton;
    }

    @Override
    public void publish(Topic topic, Event event) {
        List<Event> events = this.events.computeIfAbsent(topic, k -> new ArrayList<>());
        events.add(event);
        List<EventHandler> subscribers = this.topics.computeIfAbsent(topic, k -> new ArrayList<>());
        subscribers
                .forEach(subscriber -> subscriber.handleEvent(event));
    }

    @Override
    public void subscribe(Topic topic, EventHandler handler) {
        List<EventHandler> subscribers = this.topics.computeIfAbsent(topic, k -> new ArrayList<>());
        subscribers.add(handler);
    }

    @Override
    public int getNumberOfSubscribers(Topic topic) {
        return this.topics.computeIfAbsent(topic, k -> new ArrayList<>()).size();
    }

    @Override
    public List<Event> eventsBy(Topic topic) {
        return this.events.computeIfAbsent(topic, k -> new ArrayList<>());
    }

    public void purgeSubscribers() {
        this.topics.clear();
    }
}
