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

package com.nfsdb.ha.bridge;

import com.lmax.disruptor.*;
import com.nfsdb.exceptions.JournalRuntimeException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings({"EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS"})
public class JournalEventProcessor {
    private final SequenceBarrier barrier;
    private final Sequence sequence;
    private final RingBuffer<JournalEvent> ringBuffer;
    private long nextSequence;

    public JournalEventProcessor(JournalEventBridge bridge) {
        this(bridge.getOutRingBuffer(), bridge.createAgentSequence(), bridge.getOutBarrier());
    }

    private JournalEventProcessor(RingBuffer<JournalEvent> ringBuffer, Sequence sequence, SequenceBarrier barrier) {
        this.ringBuffer = ringBuffer;
        this.barrier = barrier;
        this.sequence = sequence;
        this.nextSequence = sequence.get() + 1L;
    }

    public Sequence getSequence() {
        return sequence;
    }

    @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_RETURN_FALSE")
    public boolean process(JournalEventHandler handler, boolean blocking) {
        this.barrier.clearAlert();
        try {
            long availableSequence = blocking ? this.barrier.waitFor(nextSequence) : this.barrier.getCursor();
            try {
                while (nextSequence <= availableSequence) {
                    handler.handle(ringBuffer.get(nextSequence));
                    nextSequence++;
                }
                sequence.set(availableSequence);
            } catch (final Throwable e) {
                sequence.set(nextSequence);
                nextSequence++;
            }
            return true;
        } catch (InterruptedException | AlertException e) {
            throw new JournalRuntimeException(e);
        } catch (TimeoutException e) {
            return false;
        }
    }
}
