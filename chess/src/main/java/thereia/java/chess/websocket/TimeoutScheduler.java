package thereia.java.chess.websocket;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public interface TimeoutScheduler {

    ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);
}
