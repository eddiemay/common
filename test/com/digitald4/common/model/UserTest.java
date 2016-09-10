package com.digitald4.common.model;

import static org.junit.Assert.*;

import org.junit.Test;

import com.digitald4.common.test.DD4TestCase;

public class UserTest extends DD4TestCase {

	@Test
	public void setEmail() throws Exception {
		User user = new User(entityManager);
		user.setEmail("eddiemay@gmail.com");
		assertEquals("eddiemay", user.getUserName());
	}

	@Test
	public void createNew() throws Exception {
		User user  = new User(entityManager)
				.setType(GenData.UserType_Standard.get(entityManager))
				.setEmail("eddiemay@gmail.com")
				.setFirstName("Eddie")
				.setLastName("Mayfield")
				.setPassword("testpass");
		assertEquals("Eddie", user.getFirstName());
		assertEquals(User.encodePassword("testpass"), user.getPassword());
		assertNotNull(user);
	}

	@Test
	public void findByEmailPassword() throws Exception {
		User user = User.get(entityManager, "eddiemay@gmail.com", "vxae11");
		assertNotNull(user);
		user = User.get(entityManager, "eddiemay@gmail.com", "hjlf");
		assertNull(user);
	}

	@Test
	public void findByUserNamePassword() throws Exception {
		User user = User.get(entityManager, "eddiemay", "vxae11");
		assertNotNull(user);
		user = User.get(entityManager, "eddiemay", "hjlf");
		assertNull(user);
	}
}
