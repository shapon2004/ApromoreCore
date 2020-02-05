package org.apromore.cache.ehcache;

import org.apromore.cache.ehcache.EhCache;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.*;

public class EhCacheTest extends BaseEhcacheTest {


    @Test(expected = IllegalArgumentException.class)
    public void testNullEhCache() {
        new EhCache<Long, String>(null);
    }


    private EhCache<Long, String> cache;

    @Before
    public void setUp() {
        super.setUp();
        cache = new EhCache<Long, String>(basicCache);
    }

    @Test
    public void testPutAndGet() {
        final Long key = Long.valueOf(1);
        final String value = "some string value";

        Assert.assertNull(cache.get(key));
        Assert.assertNull(cache.put(key, value));
        Assert.assertEquals(value, cache.get(key));
        Assert.assertEquals(value, cache.put(key, "another value"));
    }

    @Test
    public void testSize() {
        putElementsAndAssertSize();
    }

    @Test
    public void testClear() {
        putElementsAndAssertSize();
        cache.clear();
        Assert.assertEquals(0, cache.size());
    }

    private void putElementsAndAssertSize() {
        int count = 10;
        for (int i = 0; i < count; i++) {
            cache.put(Long.valueOf(i), "prefix-" + i);
        }

        Assert.assertEquals(10, cache.size());
    }

    @Test
    public void testKeys() {
        putElementsAndAssertSize();
        assertEquals(cache.keys());
    }

    @Test
    public void testEmptyKeys() {
        assertEmptyCollection(cache.keys());
    }

    @Test
    public void testEmptyValues() {
        assertEmptyCollection(cache.values());
    }

    private <T> void assertEmptyCollection(Collection<T> collection) {
        Assert.assertTrue(collection.isEmpty());
        Assert.assertEquals(0, collection.size());
    }

    @Test
    public void testValues() {
        putElementsAndAssertSize();
        assertEquals(cache.values());
    }

    private <T> void assertEquals(Collection<T> toInspect) {
        Assert.assertEquals(toInspect.size(), cache.size());
    }

    @Test
    public void testRemove() {
        final Long key = Long.valueOf(1);
        final String value = "yet another value";
        Assert.assertNull(cache.put(key, value));
        Assert.assertEquals(value, cache.remove(key));
    }

}