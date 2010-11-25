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
package org.apache.karaf.testing.container;

import org.ops4j.io.FileUtils;
import org.ops4j.pax.exam.CompositeCustomizer;
import org.ops4j.pax.exam.Customizer;
import org.ops4j.pax.exam.Info;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.TestContainerException;
import org.ops4j.pax.exam.TimeoutException;
import org.ops4j.pax.exam.options.*;
import org.ops4j.pax.url.maven.commons.MavenSettingsImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static org.ops4j.pax.exam.Constants.START_LEVEL_SYSTEM_BUNDLES;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.*;

public class KarafContainer implements TestContainer {

    private static Logger LOG = LoggerFactory.getLogger(KarafContainer.class);
    private final Option[] options;
    private final List<String> m_bundles = new ArrayList<String>();
    private final Map<String, Boolean> m_start = new HashMap<String, Boolean>();
    private final Map<String, String> m_properties = new HashMap<String, String>();

    private Framework m_framework;
    private Stack<Long> m_installed;

    public KarafContainer(Option[] options) {

        this.options = options;

        System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );

        options = expand( combine( localOptions(), options ) );

        ProvisionOption[] bundleOptions = filter( ProvisionOption.class, options );
        for( ProvisionOption opt : bundleOptions )
        {
            m_bundles.add( opt.getURL() );
            m_start.put( opt.getURL(), opt.shouldStart() );
        }
        SystemPropertyOption[] systemOptions = filter( SystemPropertyOption.class, options );
        for( SystemPropertyOption opt : systemOptions )
        {
            m_properties.put(opt.getKey(), opt.getValue());
        }
        SystemPackageOption[] packageOptions = filter( SystemPackageOption.class, options );
        for( SystemPackageOption opt : packageOptions )
        {
            String p = m_properties.get( "org.osgi.framework.system.packages" );
            if( p == null || p.length() == 0 )
            {
                p = opt.getPackage();
            }
            else
            {
                p = p + "," + opt.getPackage();
            }
            m_properties.put( "org.osgi.framework.system.packages", p );
        }
        BootDelegationOption[] bootDelegationOptions = filter( BootDelegationOption.class, options );
        for( BootDelegationOption opt : bootDelegationOptions )
        {
            String p = m_properties.get( "org.osgi.framework.bootdelegation" );
            if( p == null || p.length() == 0 )
            {
                p = opt.getPackage();
            }
            else
            {
                p = p + "," + opt.getPackage();
            }
            m_properties.put( "org.osgi.framework.bootdelegation", p );
        }
        FrameworkStartLevelOption[] startLevelOptions = filter( FrameworkStartLevelOption.class, options );
        for( FrameworkStartLevelOption opt : startLevelOptions )
        {
            m_properties.put( "org.osgi.framework.startlevel.beginning", Integer.toString( opt.getStartLevel() ) );
        }
    }

    public <T> T getService( Class<T> serviceType, String filter, long timeout )
        throws TestContainerException
    {
        assert m_framework != null : "Framework should be up";
        assert serviceType != null : "serviceType not be null";

        long start = System.currentTimeMillis();

        LOG.info( "Aquiring Service " + serviceType.getName() + " " + ( filter != null ? filter : "" ) );

        do
        {
            try
            {
                ServiceReference[] reference = m_framework.getBundleContext().getServiceReferences( serviceType.getName(), filter );
                if( reference != null )
                {

                    for( ServiceReference ref : reference )
                    {
                        return ( (T) m_framework.getBundleContext().getService( ref ) );
                    }
                }

                Thread.sleep( 200 );
            } catch( Exception e )
            {
                LOG.error( "Some problem during looking up service from framework: " + m_framework, e );
            }
            // wait a bit
        } while( ( System.currentTimeMillis() ) < start + timeout );
        printAvailableAlternatives( serviceType );

        throw new TestContainerException( "Not found a matching Service " + serviceType.getName() + " for Filter:" + ( filter != null ? filter : "" ) );

    }

    private <T> void printAvailableAlternatives( Class<T> serviceType )
    {
        try
        {
            ServiceReference[] reference = m_framework.getBundleContext().getServiceReferences( serviceType.getName(), null );
            if( reference != null )
            {
                LOG.warn( "Test Endpoints: " + reference.length );

                for( ServiceReference ref : reference )
                {
                    LOG.warn( "Endpoint: " + ref );
                }
            }

        } catch( Exception e )
        {
            LOG.error( "Some problem during looking up alternative service. ", e );
        }
    }

    public long install( InputStream stream )
    {

        try
        {
            Customizer[] customizers = OptionUtils.filter( Customizer.class, options );
            CompositeCustomizer composite = new CompositeCustomizer( customizers );

            if( m_installed == null )
            {
                m_installed = new Stack<Long>();
            }
            Bundle b = m_framework.getBundleContext().installBundle( "local", composite.customizeTestProbe( stream ) );
            m_installed.push( b.getBundleId() );
            LOG.debug( "Installed bundle " + b.getSymbolicName() + " as Bundle ID " + b.getBundleId() );

            // stream.close();
            b.start();
            return b.getBundleId();
        } catch( Exception e )
        {
            e.printStackTrace();
        }
        return -1;
    }

    public void cleanup()
    {
        if( m_installed != null )
        {
            while( ( !m_installed.isEmpty() ) )
            {
                try
                {
                    Long id = m_installed.pop();
                    Bundle bundle = m_framework.getBundleContext().getBundle( id );
                    bundle.uninstall();
                    LOG.debug( "Uninstalled bundle " + id );
                } catch( BundleException e )
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setBundleStartLevel( long bundleId, int startLevel )
        throws TestContainerException
    {
        try
        {
            m_framework.getBundleContext().getBundle( bundleId ).start( startLevel );
        } catch( BundleException e )
        {
            e.printStackTrace();
        }
    }

    public TestContainer stop()
        throws TimeoutException
    {
//        if (1==1) throw new RuntimeException( "stop has been called." );
        if( m_framework != null )
        {

            try
            {
                LOG.debug( "Framework goes down.." );
                m_framework.stop();
                m_framework.waitForStop( 1000 );
                m_framework = null;

            } catch( BundleException e )
            {
                LOG.warn( "Problem during stopping fw.", e );
            } catch( InterruptedException e )
            {
                LOG.warn( "InterruptedException during stopping fw.", e );
            }
        }
        else
        {
            throw new IllegalStateException( "Framework does not exist. Called start() before ? " );
        }
        return this;
    }

    public void waitForState( long bundleId, int state, long timeoutInMillis )
        throws TimeoutException
    {
        // look for a certain state in fw
    }

    public TestContainer start()
        throws TimeoutException
    {
        ClassLoader parent = null;
        try
        {
            for (String key : m_properties.keySet()) {
                System.setProperty( key, m_properties.get(key));
            }

            final Map<String, String> p = new HashMap<String, String>(m_properties);
            String folder = p.get("karaf.data") + "/cache";
            LOG.debug( "Cache folder set to " + folder );
            FileUtils.delete(new File(folder));
            p.put( "org.osgi.framework.storage", folder );

            String extra = p.get( "org.osgi.framework.system.packages.extra" );
            if( extra != null && extra.length() > 0 ) {
                extra += ",";
            } else {
                extra = "";
            }
            extra += "org.ops4j.pax.exam.raw.extender;version=" + skipSnapshotFlag( Info.getPaxExamVersion() );
            p.put( "org.osgi.framework.system.packages.extra", extra );

            BootClasspathLibraryOption[] classpath = OptionUtils.filter( BootClasspathLibraryOption.class, options );
            FrameworkOption[] frameworkOptions = OptionUtils.filter(FrameworkOption.class, options);
            List<String> urlStrs = new ArrayList<String>();
            for( FrameworkOption opt : frameworkOptions )
            {
                UrlReference urlRef;
                if( opt instanceof FelixFrameworkOption) {
                    urlRef = mavenBundle("org.apache.felix", "org.apache.felix.framework").versionAsInProject();
                } else if( opt instanceof EquinoxFrameworkOption ) {
                    urlRef = mavenBundle("org.eclipse", "osgi").versionAsInProject();
                } else {
                    throw new UnsupportedOperationException( "Unsupported framework: " +  opt.getName() );
                }
                urlStrs.add( urlRef.getURL() );
            }
            for( BootClasspathLibraryOption opt : classpath )
            {
                UrlReference urlRef = opt.getLibraryUrl();
                if( opt.isAfterFramework() ) {
                    urlStrs.add( urlRef.getURL() );
                } else {
                    urlStrs.add( 0, urlRef.getURL() );
                }
            }

            String localRepo = new MavenSettingsImpl(null).getLocalRepository();
            List<URL> urls = new ArrayList<URL>();
            for (String url : urlStrs) {
                url = getFileUrlForMvn(localRepo, url);
                urls.add(new URL(url));
            }


            // TODO fix ContextClassLoaderUtils.doWithClassLoader() and replace logic with it.
            //ClassLoader classLoader = new GuardClassLoader( urls.toArray( new URL[ urls.size() ]), p );
            ClassLoader classLoader = new IsolatingClassLoader( urls.toArray( new URL[ urls.size() ]) );

//            parent = Thread.currentThread().getContextClassLoader();
//            Thread.currentThread().setContextClassLoader( null );

            InputStream is = classLoader.getResourceAsStream("META-INF/services/" + FrameworkFactory.class.getName());
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String factoryClass = br.readLine();
            br.close();
            FrameworkFactory factory = (FrameworkFactory) classLoader.loadClass(factoryClass).newInstance();
            m_framework = factory.newFramework( p );

            m_framework.init();

            BundleContext context = m_framework.getBundleContext();
            List<Bundle> toStart = new ArrayList<Bundle>();
            for( String bundle : m_bundles )
            {
                Bundle b = context.installBundle( getFileUrlForMvn( localRepo, bundle ) );
                if( m_start.get( bundle ) ) {
                    toStart.add( b );
                }
                LOG.debug( "Installed bundle " + b.getSymbolicName() + " as Bundle ID " + b.getBundleId() );

            }
            m_framework.start();
            for( Bundle b : toStart )
            {
                b.start();
                LOG.debug( "Started: " + b.getSymbolicName() );
            }

        } catch( Exception e )
        {
            e.printStackTrace();
        } finally
        {
            if( parent != null )
            {
                Thread.currentThread().setContextClassLoader( parent );

            }
        }
        return this;
    }

    private String getFileUrlForMvn(String localRepo, String url) {
        if (url.startsWith("mvn:")) {
            String[] parts = url.substring(4).split("/");
            if (parts.length == 3) {
                url = "file://" + localRepo + "/" + parts[0].replace('.', '/') + "/" + parts[1] + "/" + parts[2]
                        + "/" + parts[1] + "-" + parts[2] + ".jar";
            } else if (parts.length == 4) {
                url = "file://" + localRepo + "/" + parts[0].replace('.', '/') + "/" + parts[1] + "/" + parts[2]
                        + "/" + parts[1] + "-" + parts[2] + "." + parts[3];
            } else {
                throw new IllegalStateException("Unsupported urls: " + url);
            }
        }
        return url;
    }

    private String skipSnapshotFlag( String version )
    {
        int idx = version.indexOf( "-" );
        if( idx >= 0 )
        {
            return version.substring( 0, idx );
        }
        else
        {
            return version;
        }
    }


    private Option[] localOptions()
    {
        return new Option[]{
//            bootDelegationPackage( "sun.*" ),
//            mavenBundle()
//                .groupId( "org.ops4j.pax.logging" )
//                .artifactId( "pax-logging-api" )
//                .version( "1.5.0" )
//                .startLevel( START_LEVEL_SYSTEM_BUNDLES ),
//            mavenBundle()
//                .groupId( "org.osgi" )
//                .artifactId( "org.osgi.compendium" )
//                .version( "4.2.0" )
//                .startLevel( START_LEVEL_SYSTEM_BUNDLES ),
            mavenBundle()
                .groupId( "org.ops4j.pax.exam" )
                .artifactId( "pax-exam-extender-service" )
                .version( Info.getPaxExamVersion() )
                .update( Info.isPaxExamSnapshotVersion() )
                .startLevel( START_LEVEL_SYSTEM_BUNDLES )
        };
    }

    public class IsolatingClassLoader extends URLClassLoader {
        private final URL[] urls;
        public IsolatingClassLoader(URL[] urls) {
            super(urls, ClassLoader.getSystemClassLoader().getParent());
            this.urls = urls;
        }
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("org.osgi.framework.")
                    || name.startsWith("org.ops4j.pax.exam.raw.extender.")) {
                try {
                    Class cl = IsolatingClassLoader.class.getClassLoader().loadClass(name);
                    return cl;
                } catch (ClassNotFoundException e) {
                }
            }
            return super.loadClass(name, resolve);
        }
        public String toString() {
            return "IsolatedClassLoader" + Arrays.toString(urls);
        }
    }

    public class GuardClassLoader extends URLClassLoader {

        private Set<String> bootDelegationPackages = new HashSet<String>();
        private Set<String> packages = new HashSet<String>();
        private List<ClassLoader> parents = new ArrayList<ClassLoader>();

        public GuardClassLoader(URL[] urls, Map<String,String> props) throws MalformedURLException {
            super(urls, KarafContainer.class.getClassLoader());
            String prop = props.get("org.osgi.framework.system.packages");
            String[] ps = prop.split(",");
            for (String p : ps) {
                String[] spack = p.split(";");
                for (String sp : spack) {
                    sp = sp.trim();
                    if (!sp.startsWith("version")) {
                        packages.add(sp);
                    }
                }
            }
//            if (additionalPackages != null) {
//                packages.addAll(additionalPackages);
//            }
            prop = props.get("org.osgi.framework.bootdelegation");
            ps = prop.split(",");
            for (String p : ps) {
                p = p.trim();
                if (p.endsWith("*")) {
                    p = p.substring(0, p.length() - 1);
                }
                bootDelegationPackages.add(p);
            }
            ClassLoader cl = getParent();
            while (cl != null) {
                parents.add(0, cl);
                cl = cl.getParent();
            }
            //System.err.println("Boot packages: " + packages);
        }

        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            //System.err.println("Loading class: " + name);
            Class c = findLoadedClass(name);
            if (c == null) {
                String pkg = name.substring(0, name.lastIndexOf('.'));
                boolean match = name.startsWith("java.") || packages.contains(pkg);
                if (!match) {
                    for (String p : bootDelegationPackages) {
                        if (pkg.startsWith(p)) {
                            match = true;
                            break;
                        }
                    }
                }
                if (match) {
                    for (ClassLoader cl : parents) {
                        try {
                            c = cl.loadClass(name);
                            //System.err.println("Class loaded from: " + cl.getResource(name.replace('.', '/') + ".class"));
                            break;
                        } catch (ClassNotFoundException e) {
                        }
                    }
                    if (c == null) {
                        throw new ClassNotFoundException(name);
                    }
                    //c = getParent().loadClass(name);
                } else  {
                    c = findClass(name);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }

        public URL getResource(String name) {
            //System.err.println("GetResource: " + name);
            URL url = getParent().getResource(name);
            if (url != null && url.toString().startsWith("file:")) {
                return url;
            }
            url = findResource(name);
            System.err.println("Resource " + name + " found at " + url);
            return url;
            /*
            URL u = getParent().getResource(name);
            if (u != null) {
                String path = u.toString();
                int idx = path.indexOf('!');
                if (idx > 0) {
                    path = path.substring(0, idx);
                    if (!jars.contains(path)) {
                        return null;
                    }
                } else {
                    idx = 0;
                }
            }
            return u;
            */
        }

        public Enumeration<URL> getResources(final String name) throws IOException {
            //System.err.println("GetResources: " + name);
            Enumeration[] tmp = new Enumeration[2];
            final Enumeration<URL> e = getParent().getResources(name);
            tmp[0] = new Enumeration<URL>() {
                URL prev = null;
                URL next = null;
                public boolean hasMoreElements() {
                    prev = null;
                    while (next == null && e.hasMoreElements()) {
                        next = e.nextElement();
                        String path = next.toString();
                        if (!path.startsWith("file:")) {
                            next = null;
                        }
                    }
                    return next != null;
                }
                public URL nextElement() {
                    if (prev == null) {
                        prev = next;
                        next = null;
                    }
                    if (prev == null) {
                        throw new NoSuchElementException();
                    }
                    return prev;
                }
            };
            tmp[1] = findResources(name);
            return new CompoundEnumeration(tmp) {
                public Object nextElement() {
                    Object next = super.nextElement();
                    System.err.println("Resources " + name + " found at " + next);
                    return next;
                }
            };
        }
    }

}
