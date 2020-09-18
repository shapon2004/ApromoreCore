/*-
 * #%L
 * This file is part of "Apromore Core".
 * %%
 * Copyright (C) 2018 - 2020 Apromore Pty Ltd.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
package org.apromore.persistence.activator;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;

import org.apromore.persistence.repository.FolderRepository;
import org.apromore.persistence.repository.GroupUsermetadataRepository;
import org.apromore.persistence.repository.ProcessModelVersionRepository;
import org.apromore.persistence.repository.UserRepository;
import org.apromore.persistence.repository.UsermetadataRepository;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
// 
public class Activator implements BundleActivator  {
	
	AnnotationConfigApplicationContext appContext=null;
	
    public void start(BundleContext context) throws Exception {
    	
    	Properties properties=new Properties();
    	properties.load(Activator.class.getClassLoader().getResourceAsStream("db.properties"));
    	
    	PropertyPlaceholderConfigurer propertyPlaceholderConfigurer=new PropertyPlaceholderConfigurer();
    	propertyPlaceholderConfigurer.setProperties(properties);
    	    	
    	appContext=new AnnotationConfigApplicationContext();
    	appContext.addBeanFactoryPostProcessor(propertyPlaceholderConfigurer);
    	appContext.scan("org.apromore");
    	appContext.refresh();
    	appContext.start();    	
  
    	context.registerService(EntityManagerFactory.class.getName(), appContext.getBean("entityManagerFactory"), null);
    	context.registerService(ProcessModelVersionRepository.class.getName(), appContext.getBean("processModelVersionRepository"), null);
    	context.registerService(FolderRepository.class.getName(), appContext.getBean("folderRepository"), null);
    	context.registerService(GroupUsermetadataRepository.class.getName(), appContext.getBean("groupUsermetadataRepository"), null);
    	context.registerService(UsermetadataRepository.class.getName(), appContext.getBean("usermetadataRepository"), null);    	
    	context.registerService(UserRepository.class.getName(), appContext.getBean("userRepository"), null);    
    	
    	
    	System.out.println("ok");
    	
    }
    
    public void stop(BundleContext context) throws Exception {
    	
    	appContext.stop();
    }
}
