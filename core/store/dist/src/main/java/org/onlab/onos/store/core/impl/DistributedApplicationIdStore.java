/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.store.core.impl;

import com.google.common.collect.ImmutableSet;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.MapEvent;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.core.ApplicationIdStore;
import org.onlab.onos.core.DefaultApplicationId;
import org.onlab.onos.store.hz.AbstractHazelcastStore;
import org.onlab.onos.store.hz.SMap;
import org.onlab.onos.store.serializers.KryoNamespaces;
import org.onlab.onos.store.serializers.KryoSerializer;
import org.onlab.util.KryoNamespace;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple implementation of the application ID registry using in-memory
 * structures.
 */
@Component(immediate = true)
@Service
public class DistributedApplicationIdStore
        extends AbstractHazelcastStore<AppIdEvent, AppIdStoreDelegate>
        implements ApplicationIdStore {

    protected IAtomicLong lastAppId;
    protected SMap<String, DefaultApplicationId> appIdsByName;

    protected Map<Short, DefaultApplicationId> appIds = new ConcurrentHashMap<>();


    @Override
    @Activate
    public void activate() {
        super.activate();

        this.serializer = new KryoSerializer() {
            @Override
            protected void setupKryoPool() {
                serializerPool = KryoNamespace.newBuilder()
                        .register(KryoNamespaces.API)
                        .build()
                        .populate(1);
            }
        };

        lastAppId = theInstance.getAtomicLong("applicationId");

        appIdsByName = new SMap<>(theInstance.<byte[], byte[]>getMap("appIdsByName"), this.serializer);
        appIdsByName.addEntryListener((new RemoteAppIdEventHandler()), true);

        primeAppIds();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Set<ApplicationId> getAppIds() {
        return ImmutableSet.<ApplicationId>copyOf(appIds.values());
    }

    @Override
    public ApplicationId getAppId(Short id) {
        ApplicationId appId = appIds.get(id);
        if (appId == null) {
            primeAppIds();
        }
        return appId;
    }

    private synchronized void primeAppIds() {
        for (DefaultApplicationId appId : appIdsByName.values()) {
            appIds.put(appId.id(), appId);
        }
    }

    @Override
    public synchronized ApplicationId registerApplication(String name) {
        DefaultApplicationId appId = appIdsByName.get(name);
        if (appId == null) {
            short id = (short) lastAppId.getAndIncrement();
            appId = new DefaultApplicationId(id, name);
            appIds.put(id, appId);
            appIdsByName.put(name, appId);
        }
        return appId;
    }

    private class RemoteAppIdEventHandler implements EntryListener<String, DefaultApplicationId> {
        @Override
        public void entryAdded(EntryEvent<String, DefaultApplicationId> event) {
            DefaultApplicationId appId = event.getValue();
            appIds.put(appId.id(), appId);
        }

        @Override
        public void entryRemoved(EntryEvent<String, DefaultApplicationId> event) {
        }

        @Override
        public void entryUpdated(EntryEvent<String, DefaultApplicationId> event) {
            entryAdded(event);
        }

        @Override
        public void entryEvicted(EntryEvent<String, DefaultApplicationId> event) {
        }

        @Override
        public void mapEvicted(MapEvent event) {
        }

        @Override
        public void mapCleared(MapEvent event) {
        }
    }
}