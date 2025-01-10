package com.example.gcpcloudstreambug;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Component
public class LogCaptor extends AppenderBase<ILoggingEvent> {

    private final Queue<ILoggingEvent> events = new LinkedList<>();

    public LogCaptor() {
        Logger logbackLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logbackLogger.addAppender(this);
    }

    @Override
    protected void append(ILoggingEvent e) {
        events.add(e);
    }

    public List<ILoggingEvent> getLogEvents() {
        return List.copyOf(events);
    }
}
