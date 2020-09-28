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
import org.apromore.persistence.repository.GroupFolderRepository;
import org.apromore.persistence.repository.GroupLogRepository;
import org.apromore.persistence.repository.GroupProcessRepository;
import org.apromore.persistence.repository.GroupRepository;
import org.apromore.persistence.repository.GroupUsermetadataRepository;
import org.apromore.persistence.repository.HistoryEventRepository;
import org.apromore.persistence.repository.LogRepository;
import org.apromore.persistence.repository.MembershipRepository;
import org.apromore.persistence.repository.NativeRepository;
import org.apromore.persistence.repository.NativeTypeRepository;
import org.apromore.persistence.repository.PermissionRepository;
import org.apromore.persistence.repository.ProcessBranchRepository;
import org.apromore.persistence.repository.ProcessModelAttributeRepository;
import org.apromore.persistence.repository.ProcessModelVersionRepository;
import org.apromore.persistence.repository.ProcessRepository;
import org.apromore.persistence.repository.RoleRepository;
import org.apromore.persistence.repository.SearchHistoryRepository;
import org.apromore.persistence.repository.UserRepository;
import org.apromore.persistence.repository.UsermetadataLogRepository;
import org.apromore.persistence.repository.UsermetadataRepository;
import org.apromore.persistence.repository.UsermetadataTypeRepository;
import org.apromore.persistence.repository.WorkspaceRepository;
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
  
  
    	context.registerService(ProcessModelVersionRepository.class.getName(), appContext.getBean("processModelVersionRepository"), null);
    	context.registerService(FolderRepository.class.getName(), appContext.getBean("folderRepository"), null);
    	context.registerService(GroupUsermetadataRepository.class.getName(), appContext.getBean("groupUsermetadataRepository"), null);
    	context.registerService(UsermetadataRepository.class.getName(), appContext.getBean("usermetadataRepository"), null);    	
    	context.registerService(UserRepository.class.getName(), appContext.getBean("userRepository"), null);  
    	
    	context.registerService(GroupFolderRepository.class.getName(), appContext.getBean("groupFolderRepository"), null);  
    	context.registerService(GroupLogRepository.class.getName(), appContext.getBean("groupLogRepository"), null);  
    	context.registerService(GroupProcessRepository.class.getName(), appContext.getBean("groupProcessRepository"), null);  
    	context.registerService(GroupRepository.class.getName(), appContext.getBean("groupRepository"), null);
    	
    	
    	context.registerService(HistoryEventRepository.class.getName(), appContext.getBean("historyEventRepository"), null);  
    	context.registerService(LogRepository.class.getName(), appContext.getBean("logRepository"), null); 
    	
    	context.registerService(MembershipRepository.class.getName(), appContext.getBean("membershipRepository"), null);  
    	context.registerService(NativeRepository.class.getName(), appContext.getBean("nativeRepository"), null); 
    	 
    	
    	context.registerService(NativeTypeRepository.class.getName(), appContext.getBean("nativeTypeRepository"), null);  
    	context.registerService(PermissionRepository.class.getName(), appContext.getBean("permissionRepository"), null); 
    	 
    	context.registerService(ProcessBranchRepository.class.getName(), appContext.getBean("processBranchRepository"), null);  
    	context.registerService(ProcessModelAttributeRepository.class.getName(), appContext.getBean("processModelAttributeRepository"), null); 
    	context.registerService(ProcessRepository.class.getName(), appContext.getBean("processRepository"), null); 
    	
    	
    	context.registerService(RoleRepository.class.getName(), appContext.getBean("roleRepository"), null);  
    	context.registerService(SearchHistoryRepository.class.getName(), appContext.getBean("searchHistoryRepository"), null); 
    	context.registerService(UsermetadataLogRepository.class.getName(), appContext.getBean("usermetadataLogRepository"), null); 
    	context.registerService(UsermetadataTypeRepository.class.getName(), appContext.getBean("usermetadataTypeRepository"), null); 
    	context.registerService(WorkspaceRepository.class.getName(), appContext.getBean("workspaceRepository"), null); 
    	
    	
    	System.out.println("ok");
    	
    }
    
    public void stop(BundleContext context) throws Exception {
    	
    	appContext.stop();
    }
}
