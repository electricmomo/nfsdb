/*
 *  _  _ ___ ___     _ _
 * | \| | __/ __| __| | |__
 * | .` | _|\__ \/ _` | '_ \
 * |_|\_|_| |___/\__,_|_.__/
 *
 * Copyright (c) 2014-2015. The NFSdb project and its contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nfsdb.ha;

import com.nfsdb.ha.bridge.JournalEvent;
import com.nfsdb.ha.bridge.JournalEventBridge;
import com.nfsdb.ha.bridge.JournalEventHandler;
import com.nfsdb.ha.bridge.JournalEventProcessor;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.*;

public class JournalEventBridgeTest {
    @Test
    public void testStartStop() throws Exception {
        JournalEventBridge bridge = new JournalEventBridge(2, TimeUnit.SECONDS);
        bridge.start();
        for (int i = 0; i < 10000; i++) {
            bridge.publish(10, System.currentTimeMillis());
        }
        bridge.halt();
    }

    @Test
    public void testTwoPublishersThreeConsumers() throws Exception {
        ExecutorService service = Executors.newCachedThreadPool();
        final JournalEventBridge bridge = new JournalEventBridge(50, TimeUnit.MILLISECONDS);
        bridge.start();
        final Future[] publishers = new Future[2];
        final Handler[] consumers = new Handler[3];
        final int batchSize = 1000;

        final CyclicBarrier barrier = new CyclicBarrier(publishers.length + consumers.length);
        final CountDownLatch latch = new CountDownLatch(publishers.length + consumers.length);

        for (int i = 0; i < publishers.length; i++) {
            final int index = i;
            publishers[i] = service.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    int count = 0;
                    try {
                        barrier.await();
                        for (int k = 0; k < batchSize; k++) {
                            long ts = System.nanoTime();
                            bridge.publish(index, ts);
                            count++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }

                    return count;
                }
            });
        }


        for (int i = 0; i < consumers.length; i++) {
            final JournalEventProcessor processor = new JournalEventProcessor(bridge);
            final Handler handler = new Handler(i);
            consumers[i] = handler;
            service.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        barrier.await();
                        while (true) {
                            if (!processor.process(handler, true)) {
                                break;
                            }
                        }
                    } catch (InterruptedException | BrokenBarrierException e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

//        service.submit(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    barrier.await();
//                    for (int i = 0; i < 1000; i++) {
//                        Sequence sequence = bridge.createAgentSequence();
//                        LockSupport.parkNanos(TimeUnit.MICROSECONDS.toNanos(10));
//                        bridge.removeAgentSequence(sequence);
//                    }
//                } catch (InterruptedException | BrokenBarrierException e) {
//                    e.printStackTrace();
//                } finally {
//                    latch.countDown();
//                }
//            }
//        });

        latch.await();
        bridge.halt();

        for (Future f : publishers) {
            Assert.assertEquals(batchSize, f.get());
        }

        Assert.assertEquals(batchSize, consumers[0].getCounter());
        Assert.assertEquals(batchSize, consumers[1].getCounter());
        Assert.assertEquals(0, consumers[2].getCounter());
    }

    private class Handler implements JournalEventHandler {
        private final int index;
        private int counter;

        private Handler(int index) {
            this.index = index;
        }

        public int getCounter() {
            return counter;
        }

        @Override
        public void handle(JournalEvent event) {
            if (event.getIndex() == index) {
                counter++;
            }
        }
    }
}
