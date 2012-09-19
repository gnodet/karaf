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
package org.apache.karaf.management.mbeans.bundles.internal;

import org.apache.karaf.management.mbeans.bundles.BundlesMBean;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * Bundles MBean implementation.
 */
public class BundlesMBeanImpl extends StandardMBean implements BundlesMBean {

    private BundleContext bundleContext;

    public BundlesMBeanImpl() throws NotCompliantMBeanException {
        super(BundlesMBean.class);
    }

    public TabularData getBundles() throws Exception {
        ServiceReference startLevelReference = bundleContext.getServiceReference(StartLevel.class.getName());
        StartLevel startLevel = null;
        if (startLevelReference != null) {
            startLevel = (StartLevel) bundleContext.getService(startLevelReference);
        }

        CompositeType bundleType = new CompositeType("Bundle", "OSGi Bundle",
                new String[]{"ID", "Name", "Version", "Start Level", "State"},
                new String[]{"ID of the Bundle", "Name of the Bundle", "Version of the Bundle", "Start Level of the Bundle", "Current State of the Bundle"},
                new OpenType[]{SimpleType.LONG, SimpleType.STRING, SimpleType.STRING, SimpleType.INTEGER, SimpleType.STRING});
        TabularType tableType = new TabularType("Bundles", "Tables of all Bundles", bundleType, new String[]{"ID"});
        TabularData table = new TabularDataSupport(tableType);

        Bundle[] bundles = bundleContext.getBundles();

        for (int i = 0; i < bundles.length; i++) {
            try {
                int bundleStartLevel = 1;
                if (startLevel != null) {
                    bundleStartLevel = startLevel.getBundleStartLevel(bundles[i]);
                }
                int bundleState = bundles[i].getState();
                String bundleStateString;
                if (bundleState == Bundle.ACTIVE)
                    bundleStateString = "ACTIVE";
                else if (bundleState == Bundle.INSTALLED)
                    bundleStateString = "INSTALLED";
                else if (bundleState == Bundle.RESOLVED)
                    bundleStateString = "RESOLVED";
                else if (bundleState == Bundle.STARTING)
                    bundleStateString = "STARTING";
                else if (bundleState == Bundle.STOPPING)
                    bundleStateString = "STOPPING";
                else bundleStateString = "UNKNOWN";
                CompositeData data = new CompositeDataSupport(bundleType,
                        new String[]{"ID", "Name", "Version", "Start Level", "State"},
                        new Object[]{bundles[i].getBundleId(), bundles[i].getSymbolicName(), bundles[i].getVersion().toString(), bundleStartLevel, bundleStateString});
                table.put(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        bundleContext.ungetService(startLevelReference);

        return table;
    }

    /**
     * @deprecated use getBundles() instead.
     */
    public TabularData list() throws Exception {
        return getBundles();
    }

    public int getStartLevel(String bundleId) throws Exception {
        ServiceReference startLevelReference = bundleContext.getServiceReference(StartLevel.class.getName());
        if (startLevelReference == null) {
            throw new IllegalStateException("StartLevel service is not available");
        }
        StartLevel startLevel = (StartLevel) bundleContext.getService(startLevelReference);
        if (startLevel == null) {
            throw new IllegalStateException("StartLevel service is not available");
        }

        BundlesSelector selector = new BundlesSelector(bundleContext);
        List<Bundle> bundles = selector.selectBundles(bundleId);

        if (bundles.size() != 1) {
            throw new IllegalArgumentException("Provided bundle Id doesn't return any bundle or more than one bundle selected");
        }

        int bundleStartLevel = startLevel.getBundleStartLevel(bundles.get(0));
        bundleContext.ungetService(startLevelReference);
        return bundleStartLevel;
    }

    public void setStartLevel(String bundleId, int bundleStartLevel) throws Exception {
        ServiceReference startLevelReference = bundleContext.getServiceReference(StartLevel.class.getName());
        if (startLevelReference == null) {
            throw new IllegalStateException("StartLevel service is not available");
        }
        StartLevel startLevel = (StartLevel) bundleContext.getService(startLevelReference);
        if (startLevel == null) {
            throw new IllegalStateException("StartLevel service is not available");
        }

        BundlesSelector selector = new BundlesSelector(bundleContext);
        List<Bundle> bundles = selector.selectBundles(bundleId);

        for (Bundle bundle : bundles) {
            startLevel.setBundleStartLevel(bundle, bundleStartLevel);
        }

        bundleContext.ungetService(startLevelReference);
    }

    public void refresh() throws Exception {
        ServiceReference packageAdminReference = getBundleContext().getServiceReference(PackageAdmin.class.getName());
        if (packageAdminReference == null) {
            throw new IllegalStateException("PackageAdmin service is not available");
        }
        PackageAdmin packageAdmin = (PackageAdmin) bundleContext.getService(packageAdminReference);
        if (packageAdmin == null) {
            throw new IllegalStateException("PackageAdmin service is not available");
        }
        packageAdmin.refreshPackages(null);
        getBundleContext().ungetService(packageAdminReference);
    }

    public void refresh(String bundleId) throws Exception {
        ServiceReference packageAdminReference = getBundleContext().getServiceReference(PackageAdmin.class.getName());
        if (packageAdminReference == null) {
            throw new IllegalStateException("PackageAdmin service is not available");
        }
        PackageAdmin packageAdmin = (PackageAdmin) bundleContext.getService(packageAdminReference);
        if (packageAdmin == null) {
            throw new IllegalStateException("PackageAdmin service is not available");
        }

        BundlesSelector selector = new BundlesSelector(bundleContext);
        List<Bundle> bundles = selector.selectBundles(bundleId);

        Bundle[] bundlesArray = new Bundle[bundles.size()];
        packageAdmin.refreshPackages(bundles.toArray(bundlesArray));

        getBundleContext().ungetService(packageAdminReference);
    }

    public void update(String bundleId) throws Exception {
        update(bundleId, null);
    }

    public void update(String bundleId, String location) throws Exception {
        BundlesSelector selector = new BundlesSelector(bundleContext);
        List<Bundle> bundles = selector.selectBundles(bundleId);

        if (location == null) {
            for (Bundle bundle : bundles) {
                bundle.update();
            }
            return;
        }

        if (bundles.size() != 1) {
            throw new IllegalArgumentException("Provided bundle Id doesn't return any bundle or more than one bundle selected");
        }

        InputStream is = new URL(location).openStream();
        bundles.get(0).update(is);
    }

    public void resolve() throws Exception {
        ServiceReference packageAdminReference = getBundleContext().getServiceReference(PackageAdmin.class.getName());
        if (packageAdminReference == null) {
            throw new IllegalStateException("PackageAdmin service is not available");
        }
        PackageAdmin packageAdmin = (PackageAdmin) bundleContext.getService(packageAdminReference);
        if (packageAdmin == null) {
            throw new IllegalStateException("PackageAdmin service is not available");
        }
        packageAdmin.resolveBundles(null);
        getBundleContext().ungetService(packageAdminReference);
    }

    public void resolve(String bundleId) throws Exception {
        ServiceReference packageAdminReference = getBundleContext().getServiceReference(PackageAdmin.class.getName());
        if (packageAdminReference == null) {
            throw new IllegalStateException("PackageAdmin service is not available");
        }
        PackageAdmin packageAdmin = (PackageAdmin) bundleContext.getService(packageAdminReference);
        if (packageAdmin == null) {
            throw new IllegalStateException("PackageAdmin service is not available");
        }

        BundlesSelector selector = new BundlesSelector(bundleContext);
        List<Bundle> bundles = selector.selectBundles(bundleId);

        Bundle[] bundlesArray = new Bundle[bundles.size()];
        packageAdmin.resolveBundles(bundles.toArray(bundlesArray));

        getBundleContext().ungetService(packageAdminReference);
    }

    public void restart(String bundleId) throws Exception {
        BundlesSelector selector = new BundlesSelector(bundleContext);
        List<Bundle> bundles = selector.selectBundles(bundleId);

        for (Bundle bundle : bundles) {
            bundle.stop();
            bundle.start();
        }
        ;
    }

    public long install(String url) throws Exception {
        return install(url, false);
    }

    public long install(String url, boolean start) throws Exception {
        Bundle bundle = bundleContext.installBundle(url, null);
        if (start) {
            bundle.start();
        }
        return bundle.getBundleId();
    }

    public void start(String bundleId) throws Exception {
        BundlesSelector selector = new BundlesSelector(bundleContext);
        List<Bundle> bundles = selector.selectBundles(bundleId);

        for (Bundle bundle : bundles) {
            bundle.start();
        }
    }

    public void stop(String bundleId) throws Exception {
        BundlesSelector selector = new BundlesSelector(bundleContext);
        List<Bundle> bundles = selector.selectBundles(bundleId);

        for (Bundle bundle : bundles) {
            bundle.stop();
        }
    }

    public void uninstall(String bundleId) throws Exception {
        BundlesSelector selector = new BundlesSelector(bundleContext);
        List<Bundle> bundles = selector.selectBundles(bundleId);

        for (Bundle bundle : bundles) {
            bundle.uninstall();
        }
    }

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
