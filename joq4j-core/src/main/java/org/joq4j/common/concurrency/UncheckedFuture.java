package org.joq4j.common.concurrency;

import com.google.common.util.concurrent.ListenableFuture;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * A future can be invoke get() without checked exception
 *
 * @author <a href="https://github.com/tjeubaoit">tjeubaoit</a>
 */
public interface UncheckedFuture<V> extends ListenableFuture<V> {

    @Override
    V get();

    @Override
    V get(long timeout, @NotNull TimeUnit unit);
}
