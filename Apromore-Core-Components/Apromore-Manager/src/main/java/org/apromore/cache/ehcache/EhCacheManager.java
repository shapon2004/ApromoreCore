package org.apromore.cache.ehcache;

import org.apromore.cache.Cache;
import org.apromore.exception.CacheException;
import org.apromore.cache.CacheManager;
import org.apromore.util.Destroyable;
import org.apromore.util.Initializable;
import org.apromore.util.ResourceUtils;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.Configuration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.apromore.cache.ehcache.EhCache;
import org.ehcache.core.Ehcache;
import org.ehcache.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static org.ehcache.config.builders.CacheManagerBuilder.newCacheManager;

/**
 * Apromore {@code CacheManager} implementation utilizing the Ehcache framework for all cache functionality.
 * <p/>
 * This class can {@link #setCacheManager(org.ehcache.CacheManager) accept} a manually configured
 * {@link org.ehcache.CacheManager org.ehcache.CacheManager} instance,
 * or an {@code ehcache.xml} path location can be specified instead and one will be constructed.
 */
public class EhCacheManager implements CacheManager, Initializable, Destroyable {

    private static final Logger log = LoggerFactory.getLogger(EhCacheManager.class);

    private volatile org.ehcache.CacheManager manager;

    private volatile String cacheManagerConfigFile = "/ehcache.xml";

    public static final String CACHE_ALIAS_XLOG = "xlog";
    public static final String CACHE_ALIAS_APMLOG = "apmlog";

    private volatile boolean cacheManagerImplicitlyCreated = false;

    private volatile XmlConfiguration cacheConfiguration = null;

    /**
     * Returns the wrapped {@link org.ehcache.CacheManager} instance
     *
     * @return the wrapped {@link org.ehcache.CacheManager} instance
     */
    public org.ehcache.CacheManager getCacheManager() {
        return manager;
    }

    /**
     * Sets the wrapped {@link org.ehcache.CacheManager} instance
     *
     * @param cacheManager the {@link org.ehcache.CacheManager} to be used
     */
    public void setCacheManager(org.ehcache.CacheManager cacheManager) {
        try {
            destroy();
        } catch (Exception e) {
            log.warn("The managed CacheManager threw an Exception while closing", e);
        }
        manager = cacheManager;
        cacheManagerImplicitlyCreated = false;
    }

    /**
     * {@inheritDoc}
     */
    public <K, V> Cache<K, V> getCache(String name) throws CacheException {
        log.trace("Acquiring Ehcache instance named [{}]", name);

        try {
            org.ehcache.Cache<Object, Object> cache = ensureCacheManager().getCache(name, Object.class, Object.class);

            if (cache == null) {
                log.info("Cache with name {} does not yet exist.  Creating now.", name);
                cache = createCache(name);
                log.info("Added Ehcache named [{}]", name);
            } else {
                log.info("Using existing Ehcache named [{}]", name);
            }

            return new org.apromore.cache.ehcache.EhCache<>(cache);
        } catch (MalformedURLException e) {
            throw new CacheException(e);
        }
    }

    private org.ehcache.Cache<Object, Object> createCache(String name) {
        try {
            XmlConfiguration xmlConfiguration = getConfiguration();
            CacheConfigurationBuilder<Object, Object> configurationBuilder = xmlConfiguration.newCacheConfigurationBuilderFromTemplate(
                    "defaultCacheConfiguration", Object.class, Object.class);
            CacheConfiguration<Object, Object> cacheConfiguration = configurationBuilder.build();
            return ensureCacheManager().createCache(name, cacheConfiguration);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | MalformedURLException e) {
            throw new CacheException(e);
        }
    }

    private org.ehcache.CacheManager ensureCacheManager() throws MalformedURLException {
        if (manager == null) {
            manager = CacheManagerBuilder.newCacheManager(getConfiguration());
            manager.init();

            cacheManagerImplicitlyCreated = true;
        }

        return manager;
    }

    private XmlConfiguration getConfiguration() {
        if (cacheConfiguration == null) {
            cacheConfiguration = new XmlConfiguration(EhCacheManager.class.getResource(cacheManagerConfigFile));
        }

        return cacheConfiguration;
    }

    public void destroy() throws Exception {
        if (cacheManagerImplicitlyCreated && manager != null) {
            manager.close();
            manager = null;
        }
    }

    /**
     * Initializes this instance.
     * <P>
     * If a {@link #setCacheManager CacheManager} has been
     * explicitly set (e.g. via Dependency Injection or programatically) prior to calling this
     * method, this method does nothing.
     * </P>
     * <P>
     * However, if no {@code CacheManager} has been set a new {@link org.ehcache.Cache} will be initialized.
     * It will use {@code ehcache.xml} configuration file at the root of the classpath.
     * </P>
     *
     * @throws org.apromore.exception.CacheException if there are any CacheExceptions thrown by EhCache.
     */
    public void init() throws CacheException {
        try {
            ensureCacheManager();
        } catch (MalformedURLException e) {
            throw new CacheException(e);
        }
    }
}
