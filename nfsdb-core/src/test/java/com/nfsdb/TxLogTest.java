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

package com.nfsdb;

import com.nfsdb.exceptions.JournalException;
import com.nfsdb.io.sink.DelimitedCharSink;
import com.nfsdb.io.sink.StringSink;
import com.nfsdb.model.Quote;
import com.nfsdb.storage.Tx;
import com.nfsdb.storage.TxLog;
import com.nfsdb.test.tools.AbstractTest;
import com.nfsdb.test.tools.TestUtils;
import com.nfsdb.utils.Rows;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class TxLogTest extends AbstractTest {
    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    public static void main(String[] args) throws JournalException {
        TxLog server = new TxLog(new File("D:\\data\\org.nfsdb.examples.model.Price"), JournalMode.READ);
        TxLog client = new TxLog(new File("D:\\data\\price-copy"), JournalMode.READ);

        System.out.println(client.getCurrentTxn());
        System.out.println(client.getCurrentTxnPin());
        System.out.println(server.getCurrentTxn());
        System.out.println(server.getCurrentTxnPin());
    }

    @Test
    public void testTx() throws Exception {
        File dir = temp.newFolder();
        TxLog txLog = new TxLog(dir, JournalMode.APPEND);
        Assert.assertFalse(txLog.hasNext());

        Tx tx = new Tx();
        tx.prevTxAddress = 99999;
        tx.command = 0;
        tx.journalMaxRowID = 10;
        tx.lagSize = 12;
        tx.lagName = "abcrrrrrrrrrrrrrrrrrrrrrrrrrrr";
        tx.timestamp = 1000001L;
        tx.lastPartitionTimestamp = 200002L;
        tx.symbolTableSizes = new int[]{10, 12};
        tx.symbolTableIndexPointers = new long[]{2, 15, 18};
        tx.indexPointers = new long[]{36, 48};
        tx.lagIndexPointers = new long[]{55, 67};

        txLog.write(tx, true);

        Assert.assertFalse(txLog.hasNext());

        TxLog r = new TxLog(dir, JournalMode.READ);
        Assert.assertTrue(r.hasNext());

        Tx tx1 = new Tx();
        r.head(tx1);
        Assert.assertEquals(0, tx1.command);
        Assert.assertEquals(10, tx1.journalMaxRowID);
        Assert.assertEquals(12, tx1.lagSize);
        Assert.assertEquals("abcrrrrrrrrrrrrrrrrrrrrrrrrrrr", tx.lagName);
        Assert.assertEquals(99999, tx.prevTxAddress);
        Assert.assertEquals(1000001L, tx.timestamp);
        Assert.assertEquals(200002L, tx.lastPartitionTimestamp);
        Assert.assertEquals(1000001L, tx.timestamp);

        Assert.assertArrayEquals(new int[]{10, 12}, tx.symbolTableSizes);
        Assert.assertArrayEquals(new long[]{2, 15, 18}, tx.symbolTableIndexPointers);
        Assert.assertArrayEquals(new long[]{36, 48}, tx.indexPointers);
        Assert.assertArrayEquals(new long[]{55, 67}, tx.lagIndexPointers);

        Assert.assertFalse(r.hasNext());
        txLog.close();
        r.close();
    }

    @Test
    public void testTxLogIterator() throws Exception {
        JournalWriter<Quote> w = factory.writer(Quote.class);
        for (int i = 0; i < 10; i++) {
            TestUtils.generateQuoteData(w, 100, w.getMaxTimestamp());
            w.commit();
        }

        final String expected = "10\t0\t1000\n" +
                "9\t0\t900\n" +
                "8\t0\t800\n" +
                "7\t0\t700\n" +
                "6\t0\t600\n" +
                "5\t0\t500\n" +
                "4\t0\t400\n" +
                "3\t0\t300\n" +
                "2\t0\t200\n" +
                "1\t0\t100\n" +
                "0\t0\t0\n";

        DelimitedCharSink sink = new DelimitedCharSink(new StringSink(), '\t', "\n");
        for (Tx tx : w.transactions()) {
            sink.put(tx.txn);
            sink.put(tx.journalMaxRowID == -1 ? 0 : Rows.toPartitionIndex(tx.journalMaxRowID));
            sink.put(tx.journalMaxRowID == -1 ? 0 : Rows.toLocalRowID(tx.journalMaxRowID));
            sink.eol();
        }
        sink.flush();
        Assert.assertEquals(expected, sink.toString());
    }
}

