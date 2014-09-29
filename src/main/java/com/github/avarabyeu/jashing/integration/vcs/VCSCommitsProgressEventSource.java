package com.github.avarabyeu.jashing.integration.vcs;

import com.github.avarabyeu.jashing.events.Events;
import com.github.avarabyeu.jashing.events.NumberEvent;
import com.github.avarabyeu.jashing.eventsource.HandlesEvent;
import com.github.avarabyeu.jashing.eventsource.ScheduledEventSource;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * @author Andrey Vorobyov
 */
@HandlesEvent(Events.SVN_COMMITS_PROGRESS)
public class VCSCommitsProgressEventSource extends ScheduledEventSource<NumberEvent> {

    @Inject
    private VCSClient svnClient;

    @Inject
    @Named("logPeriod")
    private Double daysBefore;

    /* recalculate yestarday commits count each our. Think about better approach of expiration */
    private Supplier<Long> yestardayCommitsCount = Suppliers.memoizeWithExpiration(new Supplier<Long>() {
        @Override
        public Long get() {
            LocalDate today = LocalDate.now();
            return svnClient.getCommitsForPeriod(today.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
    }, 1, TimeUnit.HOURS);

    public VCSCommitsProgressEventSource(String eventId, Duration period) {
        super(eventId, period);
    }

    @Override
    protected NumberEvent produceEvent() {
        Instant fromDateTime = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Long todaysCommitsCount = svnClient.getCommitsForPeriod(fromDateTime);
        return new NumberEvent(todaysCommitsCount.intValue(), yestardayCommitsCount.get().intValue());
    }
}