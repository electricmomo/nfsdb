/*******************************************************************************
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
 ******************************************************************************/

package com.nfsdb.ql.impl;

import com.nfsdb.collections.AbstractImmutableIterator;
import com.nfsdb.exceptions.JournalException;
import com.nfsdb.factory.JournalReaderFactory;
import com.nfsdb.factory.configuration.RecordMetadata;
import com.nfsdb.ql.Record;
import com.nfsdb.ql.RecordCursor;
import com.nfsdb.ql.RecordSource;
import com.nfsdb.ql.StorageFacade;

public class CrossJoinRecordSource extends AbstractImmutableIterator<Record> implements RecordSource<Record>, RecordCursor<Record> {
    private final RecordSource<? extends Record> masterSource;
    private final RecordSource<? extends Record> slaveSource;
    private final SplitRecordMetadata metadata;
    private final SplitRecord record;
    private RecordCursor<? extends Record> masterCursor;
    private RecordCursor<? extends Record> slaveCursor;
    private boolean nextSlave = false;

    public CrossJoinRecordSource(RecordSource<? extends Record> masterSource, RecordSource<? extends Record> slaveSource) {
        this.masterSource = masterSource;
        this.slaveSource = slaveSource;
        this.metadata = new SplitRecordMetadata(masterSource.getMetadata(), slaveSource.getMetadata());
        this.record = new SplitRecord(metadata, masterSource.getMetadata().getColumnCount());
    }

    @Override
    public Record getByRowId(long rowId) {
        return null;
    }

    @Override
    public StorageFacade getSymFacade() {
        return null;
    }

    @Override
    public RecordMetadata getMetadata() {
        return metadata;
    }

    @Override
    public RecordCursor<Record> prepareCursor(JournalReaderFactory factory) throws JournalException {
        masterCursor = masterSource.prepareCursor(factory);
        slaveCursor = slaveSource.prepareCursor(factory);
        return this;
    }

    @Override
    public void reset() {
        masterSource.reset();
        slaveSource.reset();
        nextSlave = false;
    }

    @Override
    public boolean supportsRowIdAccess() {
        return false;
    }

    @Override
    public boolean hasNext() {
        return nextSlave || masterCursor.hasNext();
    }

    @Override
    public Record next() {
        if (!nextSlave) {
            record.setA(masterCursor.next());
            slaveSource.reset();
        }

        if (nextSlave || slaveCursor.hasNext()) {
            record.setB(slaveCursor.next());
            nextSlave = slaveCursor.hasNext();
        } else {
            record.setB(null);
            nextSlave = false;
        }
        return record;
    }
}
