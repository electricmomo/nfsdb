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

import com.nfsdb.ql.Record;
import com.nfsdb.ql.RecordSource;
import com.nfsdb.ql.RowSource;
import com.nfsdb.ql.ops.VirtualColumn;

public class KvIndexStrSymLambdaHeadRowSource extends KvIndexStrLambdaHeadRowSource {

    public static final LatestByLambdaRowSourceFactory FACTORY = new Factory();

    private KvIndexStrSymLambdaHeadRowSource(String column, RecordSource<? extends Record> recordSource, int recordSourceColumn, VirtualColumn filter) {
        super(column, recordSource, recordSourceColumn, filter);
    }

    @Override
    protected CharSequence getKey(Record r, int col) {
        return r.getSym(col);
    }

    public static class Factory implements LatestByLambdaRowSourceFactory {
        @Override
        public RowSource newInstance(String column, RecordSource<? extends Record> recordSource, int recordSourceColumn, VirtualColumn filter) {
            return new KvIndexStrSymLambdaHeadRowSource(column, recordSource, recordSourceColumn, filter);
        }
    }
}
