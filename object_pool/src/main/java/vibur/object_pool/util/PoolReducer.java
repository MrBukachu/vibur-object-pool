/**
 * Copyright 2013 Simeon Malchev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vibur.object_pool.util;

import vibur.object_pool.BasePoolService;

import java.util.concurrent.TimeUnit;

/**
 * The allocated objects reducer thread, which is wakening up when an
 * unit/timeout period of time expires, to check whether the number of available
 * allocated objects in the object pool needs to be reduced.

 * @author Simeon Malchev
 */
public class PoolReducer implements Runnable {

    private final BasePoolService poolService;
    private final Reducer reducer;
    private final long timeout;
    private final TimeUnit unit;
    private final Thread reducerThread;

    private volatile boolean terminated = false;

    public PoolReducer(BasePoolService poolService, Reducer reducer,
                        long timeout, TimeUnit unit) {
        if (poolService == null || reducer == null || timeout <= 0 || unit == null)
            throw new IllegalArgumentException();

        this.poolService = poolService;
        this.reducer = reducer;
        this.timeout = timeout;
        this.unit = unit;

        reducerThread = new Thread(this);
        reducerThread.setName(toString());
        reducerThread.setDaemon(true);
        reducerThread.setPriority(Thread.MAX_PRIORITY - 2);
        reducerThread.start();
    }

    @Override
    public void run() {
        while (!terminated) {
            try {
                unit.sleep(timeout);

                int reduction = reducer.reduceBy(poolService);
                if (reduction > 0) {
                    try {
                        poolService.reduceCreated(reduction);
                    }
                    catch (RuntimeException ignored) { }
                    catch (Error ignored) { }
                }
            } catch (InterruptedException ignored) { }
        }
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void terminate() {
        terminated = true;
        reducerThread.interrupt();
    }
}
