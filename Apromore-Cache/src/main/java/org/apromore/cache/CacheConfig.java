package org.apromore.cache;

import org.apromore.cache.ehcache.CacheRepository;
import org.apromore.cache.ehcache.CacheRepositoryImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class CacheConfig {

	@Value("${manager.ehcache.config.url}")
	Resource managerCacheUrl;
	
	@Bean
	public CacheRepository cacheRepo()
	{
		CacheRepositoryImpl cacheRepositoryImpl=new CacheRepositoryImpl();
		cacheRepositoryImpl.setCacheName("xlog");	
		cacheRepositoryImpl.setEhCacheCacheManager(ehCacheCacheManager());
		return cacheRepositoryImpl;
	}
	
	@Bean
	public EhCacheCacheManager ehCacheCacheManager()
	{
		EhCacheCacheManager ehCacheCacheManager=new EhCacheCacheManager();
		ehCacheCacheManager.setCacheManager(ehCacheManagerFactoryBean().getObject());
		return ehCacheCacheManager;
	}
	
	@Bean
	public EhCacheManagerFactoryBean ehCacheManagerFactoryBean()
	{
		EhCacheManagerFactoryBean ehCacheManagerFactoryBean=new EhCacheManagerFactoryBean();
		ehCacheManagerFactoryBean.setConfigLocation(managerCacheUrl);	
		return ehCacheManagerFactoryBean;
	}
	
	
}
