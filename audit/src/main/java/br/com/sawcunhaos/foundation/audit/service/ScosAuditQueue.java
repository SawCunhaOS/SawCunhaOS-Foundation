/*
 *
 *  * Copyright 2026 SawCunha Open System - SawCunhaOS-Foundation
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *
 */

package br.com.sawcunhaos.foundation.audit.service;

import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditLog;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ScosAuditQueue {

    private final ConcurrentLinkedQueue<ScosAuditLog> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger size = new AtomicInteger(0);
    private final int capacity;

    public ScosAuditQueue(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Offers an event to the queue. Returns false and logs overflow if capacity exceeded.
     * Caller should route overflow to DLQ.
     */
    public boolean offer(ScosAuditLog log) {
        if (size.get() >= capacity) {
            return false;
        }
        queue.offer(log);
        size.incrementAndGet();
        return true;
    }

    /**
     * Drains up to maxElements from the queue into the provided list.
     */
    public int drainTo(List<ScosAuditLog> target, int maxElements) {
        int count = 0;
        while (count < maxElements) {
            ScosAuditLog log = queue.poll();
            if (log == null) break;
            target.add(log);
            size.decrementAndGet();
            count++;
        }
        return count;
    }

    public int size() {
        return size.get();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

}
