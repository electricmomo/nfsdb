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

package com.nfsdb.ha.protocol.commands;

import com.nfsdb.exceptions.JournalNetworkException;
import com.nfsdb.ha.ChannelConsumer;
import com.nfsdb.utils.ByteBuffers;
import com.nfsdb.utils.Unsafe;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;

public class IntResponseConsumer implements ChannelConsumer {
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN);
    private final long address = ByteBuffers.getAddress(buffer);

    public void free() {
        ByteBuffers.release(buffer);
    }

    @Override
    public void read(ReadableByteChannel channel) throws JournalNetworkException {
        buffer.position(0);
        ByteBuffers.copy(channel, buffer);
    }

    public int getValue(ReadableByteChannel channel) throws JournalNetworkException {
        read(channel);
        return Unsafe.getUnsafe().getInt(address);
    }
}
