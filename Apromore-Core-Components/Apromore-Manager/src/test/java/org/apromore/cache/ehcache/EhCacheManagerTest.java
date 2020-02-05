package org.apromore.cache.ehcache;

import org.apromore.cache.Cache;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XLogImpl;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.spi.service.StatisticsService;
import org.ehcache.core.statistics.DefaultStatisticsService;
import org.ehcache.impl.config.persistence.CacheManagerPersistenceConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class EhCacheManagerTest {

    @Test
    public void testGetCache() throws Exception {

        final String PERSISTENCE_PATH = "/Users/frank/terracotta";

        CacheConfiguration<Long, XLog> cacheConfig =
                CacheConfigurationBuilder.newCacheConfigurationBuilder(Long.class, XLog.class,
                        ResourcePoolsBuilder.heap(1).disk(1000, MemoryUnit.MB, true)).withValueSerializer(EhcacheSerializer.class).build();

        StatisticsService statisticsService = new DefaultStatisticsService();


        CacheManager cacheManager =
                CacheManagerBuilder.newCacheManagerBuilder().with(new CacheManagerPersistenceConfiguration(new File(PERSISTENCE_PATH))).withCache("xLogCache", cacheConfig).using(statisticsService).build(true);
        org.ehcache.Cache<Long, XLog> xLogCache = cacheManager.getCache("xLogCache", Long.class,
                XLog.class);


//        EhCacheManager cacheManager = new EhCacheManager();

        try {
//            Cache<String, XLog> someCache = cacheManager.getCache("xlog");
            Assert.assertNotNull(xLogCache);

            final Long key = 1L;
//            final String value = "value";
            final XLog value = new XLogImpl(new XAttributeMapImpl());
            xLogCache.put(key, value);
//            Assert.assertNull(xLogCache.put(key, value));
            Assert.assertEquals(value, xLogCache.get(key));
        } finally {
            cacheManager.close();

        }
    }
}