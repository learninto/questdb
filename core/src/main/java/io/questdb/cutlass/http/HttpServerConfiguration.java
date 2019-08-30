/*******************************************************************************
 *    ___                  _   ____  ____
 *   / _ \ _   _  ___  ___| |_|  _ \| __ )
 *  | | | | | | |/ _ \/ __| __| | | |  _ \
 *  | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *   \__\_\\__,_|\___||___/\__|____/|____/
 *
 * Copyright (C) 2014-2019 Appsicle
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

package io.questdb.cutlass.http;

import io.questdb.cutlass.http.processors.JsonQueryProcessorConfiguration;
import io.questdb.cutlass.http.processors.StaticContentProcessorConfiguration;
import io.questdb.cutlass.http.processors.TextImportProcessorConfiguration;
import io.questdb.network.IODispatcherConfiguration;
import io.questdb.std.time.MillisecondClock;

public interface HttpServerConfiguration {
    String DEFAULT_PROCESSOR_URL = "*";

    int getConnectionPoolInitialCapacity();

    int getConnectionStringPoolCapacity();

    int getMultipartHeaderBufferSize();

    long getMultipartIdleSpinCount();

    int getRecvBufferSize();

    int getRequestHeaderBufferSize();

    int getResponseHeaderBufferSize();

    MillisecondClock getClock();

    IODispatcherConfiguration getDispatcherConfiguration();

    StaticContentProcessorConfiguration getStaticContentProcessorConfiguration();

    TextImportProcessorConfiguration getTextImportProcessorConfiguration();

    JsonQueryProcessorConfiguration getJsonQueryProcessorConfiguration();

    int getWorkerCount();

    boolean workerHaltOnError();

    int[] getWorkerAffinity();

    int getSendBufferSize();

    boolean isEnabled();

    boolean getDumpNetworkTraffic();
}