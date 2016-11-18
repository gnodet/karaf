/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.features.internal.service;

import org.apache.karaf.features.internal.resolver.ResolverUtil;
import org.apache.karaf.features.internal.resolver.ResourceUtils;
import org.osgi.framework.Version;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.osgi.framework.namespace.IdentityNamespace.TYPE_BUNDLE;
import static org.osgi.framework.namespace.IdentityNamespace.TYPE_FRAGMENT;

public class ResourceComparator implements Comparator<Resource> {

    private final Map<Resource, List<Wire>> wiring;
    private final Map<Resource, Set<Resource>> depends = new HashMap<>();

    public ResourceComparator(Map<Resource, List<Wire>> wiring) {
        this.wiring = wiring;
    }

    @Override
    public int compare(Resource o1, Resource o2) {
        boolean d1 = depends.computeIfAbsent(o1, this::getDepends).contains(o2);
        boolean d2 = depends.computeIfAbsent(o2, this::getDepends).contains(o1);
        if (d1 && !d2) {
            return 1;
        }
        if (d2 && !d1) {
            return -1;
        }
        String bsn1 = ResolverUtil.getSymbolicName(o1);
        String bsn2 = ResolverUtil.getSymbolicName(o2);
        int c = bsn1.compareTo(bsn2);
        if (c == 0) {
            Version v1 = ResolverUtil.getVersion(o1);
            Version v2 = ResolverUtil.getVersion(o2);
            c = v1.compareTo(v2);
            if (c == 0) {
                c = o1.hashCode() - o2.hashCode();
            }
        }
        return c;
    }

    private Set<Resource> getDepends(Resource root) {
        Set<Resource> depends = new HashSet<>();
        LinkedList<Resource> remaining = new LinkedList<>(Collections.singletonList(root));
        Resource r;
        while ((r = remaining.poll()) != null) {
            if (depends.add(r)) {
                wiring.get(r).stream()
                        .map(Wire::getProvider)
                        .filter(p -> {
                            String t = ResourceUtils.getType(p);
                            return TYPE_BUNDLE.equals(t) || TYPE_FRAGMENT.equals(t);
                        })
                        .forEach(remaining::add);
            }
        }
        return depends;
    }

}
