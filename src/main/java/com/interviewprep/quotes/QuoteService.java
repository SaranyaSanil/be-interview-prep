package com.interviewprep.quotes;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

/** A fixed in-memory list; the point of this endpoint is to have something to rate-limit. */
@Service
public class QuoteService {

    private static final List<Quote> QUOTES = List.of(
            new Quote("Simplicity is prerequisite for reliability.", "Edsger W. Dijkstra"),
            new Quote("Premature optimization is the root of all evil.", "Donald Knuth"),
            new Quote("Make it work, make it right, make it fast.", "Kent Beck"),
            new Quote("Programs must be written for people to read.", "Harold Abelson"),
            new Quote("The best code is no code at all.", "Jeff Atwood"));

    public Quote random() {
        return QUOTES.get(ThreadLocalRandom.current().nextInt(QUOTES.size()));
    }
}
