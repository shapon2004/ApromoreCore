package com.apromore.persistence;

import static org.assertj.core.api.Assertions.*;

import org.apromore.persistence.entity.Group;
import org.apromore.persistence.entity.Role;
import org.apromore.persistence.entity.User;
import org.apromore.persistence.repository.GroupRepository;
import org.apromore.persistence.repository.RoleRepository;
import org.apromore.persistence.repository.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.apromore.persistence.model.builder.UserManagementBuilder;

public class UserManagementTest extends BaseTestClass {

	@Autowired
	GroupRepository groupRepository;
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	RoleRepository roleRepository;


	UserManagementBuilder builder;

	@Before
	public void Setup() {
		builder = new UserManagementBuilder();

	}

	@Test
	public void testSaveGroup() {
		// given
		Group userGroup = builder.withGroup("testGroup", "USER").buildGroup();

		// when
		Group savedUSerGroup = groupRepository.saveAndFlush(userGroup);
		// then
		assertThat(savedUSerGroup.getId()).isNotNull();
		assertThat(savedUSerGroup.getName()).isEqualTo(userGroup.getName());
		assertThat(savedUSerGroup.getType()).isEqualTo(userGroup.getType());
	}
	
	@Test
	public void testSaveUser() {
		// given
		Group group=groupRepository.saveAndFlush(builder.withGroup("testGroup", "USER").buildGroup());
		Role role=roleRepository.saveAndFlush(builder.withRole("testRole").buildRole());
		User user = builder.withGroup(group)
				.withRole(role)
				.withMembership("n@t.com")
				.withUser("TestUser", "first", "last", "org")
				.buildUser();
		// when
		User savedUSer = userRepository.saveAndFlush(user);
		// then
		assertThat(savedUSer.getId()).isNotNull();
		assertThat(savedUSer.getMembership().getEmail()).isEqualTo(user.getMembership().getEmail());
	}

}
