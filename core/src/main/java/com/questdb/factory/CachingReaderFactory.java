/*******************************************************************************
 *    ___                  _   ____  ____
 *   / _ \ _   _  ___  ___| |_|  _ \| __ )
 *  | | | | | | |/ _ \/ __| __| | | |  _ \
 *  | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *   \__\_\\__,_|\___||___/\__|____/|____/
 *
 * Copyright (C) 2014-2016 Appsicle
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.questdb.factory;

import com.questdb.Journal;
import com.questdb.JournalBulkReader;
import com.questdb.JournalKey;
import com.questdb.ex.JournalDoesNotExistException;
import com.questdb.ex.JournalException;
import com.questdb.factory.configuration.JournalConfiguration;
import com.questdb.factory.configuration.JournalMetadata;
import com.questdb.log.Log;
import com.questdb.log.LogFactory;
import com.questdb.std.CharSequenceObjHashMap;
import com.questdb.std.ObjList;

import java.util.concurrent.atomic.AtomicBoolean;

public class CachingReaderFactory extends ReaderFactory implements JournalClosingListener {
    private final static Log LOG = LogFactory.getLog(CachingReaderFactory.class);
    private final CharSequenceObjHashMap<Journal> readers = new CharSequenceObjHashMap<>();
    private final CharSequenceObjHashMap<JournalBulkReader> bulkReaders = new CharSequenceObjHashMap<>();
    private final ObjList<Journal> journalList = new ObjList<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private ReaderFactoryPool pool;
    private boolean inPool = false;

    public CachingReaderFactory(String databaseHome) {
        super(databaseHome);
    }

    public CachingReaderFactory(JournalConfiguration configuration) {
        super(configuration);
    }

    CachingReaderFactory(JournalConfiguration configuration, ReaderFactoryPool pool) {
        super(configuration);
        this.pool = pool;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> JournalBulkReader<T> bulkReader(JournalKey<T> key) throws JournalException {
        String name = key.path();
        checkBlocked(name);
        JournalBulkReader<T> result = bulkReaders.get(name);
        if (result == null) {
            result = super.bulkReader(key);
            result.setCloseListener(this);
            bulkReaders.put(name, result);
            journalList.add(result);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Journal<T> reader(JournalMetadata<T> metadata, JournalKey<T> key) throws JournalException {
        String name = key.path();
        Journal result = readers.get(name);
        if (result == null) {
            if (getConfiguration().exists(name) != JournalConfiguration.EXISTS) {
                LOG.error().$("Journal does not exist: ").$(name).$();
                throw JournalDoesNotExistException.INSTANCE;
            }
            result = super.reader(metadata, key);
            result.setCloseListener(this);
            readers.put(name, result);
            journalList.add(result);
        }
        return result;
    }

    @Override
    public void close() {
        if (pool != null) {
            // To not release twice.
            if (!inPool) {
                inPool = true;
                pool.release(this);
            }
        } else {
            if (closed.compareAndSet(false, true)) {
                reset();
            }
        }
    }

    public void closeJournal(CharSequence name) {
        Journal j = readers.get(name);
        if (j != null) {
            j.setCloseListener(null);
            j.close();
            readers.remove(name);
        }

        j = bulkReaders.get(name);
        if (j != null) {
            j.setCloseListener(null);
            j.close();
            bulkReaders.remove(name);
        }
    }

    @Override
    public boolean closing(Journal journal) {
        return false;
    }

    public void refresh() {
        for (int i = 0, sz = journalList.size(); i < sz; i++) {
            journalList.getQuick(i).refresh();
        }
    }

    public void reset() {
        for (int i = 0, sz = journalList.size(); i < sz; i++) {
            Journal journal = journalList.getQuick(i);
            journal.setCloseListener(null);
            if (journal.isOpen()) {
                journal.close();
            }
        }
        readers.clear();
        bulkReaders.clear();
    }

    private void checkBlocked(String name) throws JournalException {
        if (pool != null && pool.isBlocked(name)) {
            throw new JournalException("Journal %s is being deleted", name);
        }
    }

    void clearPool() {
        this.pool = null;
    }

    void expireOpenFiles() {
        for (int i = 0, sz = journalList.size(); i < sz; i++) {
            journalList.getQuick(i).expireOpenFiles0();
        }
    }

    void setInUse() {
        inPool = false;
    }
}