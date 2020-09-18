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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import javax.sql.DataSource;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Activator implements BundleActivator {
	
	AnnotationConfigApplicationContext appContext=null;
	
    public void start(BundleContext context) throws Exception {
    	
    	Configuration c=((ConfigurationAdmin)context.getService(
    			context.getServiceReference(ConfigurationAdmin.class.getName())))
    			.getConfiguration("site");
    	
    	Properties properties=new Properties();
    	
    	Enumeration<String> keys=c.getProperties().keys();
    	  while (keys.hasMoreElements()) {
    	    String key=(String)keys.nextElement();
    	    if (key != null && c.getProperties().get(key) != null) {
    	      properties.put(key,c.getProperties().get(key));
    	    }
    	  }
    	
    	PropertyPlaceholderConfigurer propertyPlaceholderConfigurer=new PropertyPlaceholderConfigurer();
    	propertyPlaceholderConfigurer.setProperties(properties);
    	appContext=new AnnotationConfigApplicationContext();
    	appContext.addBeanFactoryPostProcessor(propertyPlaceholderConfigurer);
    	appContext.scan("org.apromore");
    	appContext.refresh();
    	appContext.start();
    	

    	System.out.println("ok");
    	
    }
    
    public void stop(BundleContext context) throws Exception {
    	
    	appContext.stop();
    }
}
