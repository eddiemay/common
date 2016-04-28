package com.digitald4.common.model;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Ignore;
import org.junit.Test;

import com.digitald4.common.test.DD4TestCase;

public class GeneralDataTest extends DD4TestCase{
	
	@Test
	public void testEnum() throws Exception{
		GeneralData userType = GenData.UserType.get(entityManager);
		assertNotNull(userType);
		assertNotSame(0,userType.getGeneralDatas().size());
		GeneralData admin = GenData.UserType_Admin.get(entityManager);
		assertNotNull(admin);
		assertEquals(0,admin.getGeneralDatas().size());
	}
	
	@Test
	public void findBrokenEnums(){
		for (GenData gd:GenData.values()) {
			assertNotNull(gd.get(entityManager));
		}
	}

	@Test
	@Ignore
	public void insertDefaults() throws Exception {
		GeneralData userType = new GeneralData(entityManager);
		userType.setName("User Type");
		userType.setDescription("Types of users");
		userType.setInGroupId(1);
		GeneralData admin = new GeneralData(entityManager);
		admin.setName("Admin");
		admin.setDescription("Admin User");
		admin.setInGroupId(1);
		admin.setRank(0);
		userType.addGeneralData(admin);
		GeneralData standard = new GeneralData(entityManager);
		standard.setName("Standard");
		standard.setDescription("Standard User");
		standard.setInGroupId(2);
		standard.setRank(1);
		userType.addGeneralData(standard);
		assertEquals(2,userType.getGeneralDatas().size());
		System.out.println(userType.getGeneralDatas());
		userType.insert();
		assertNotNull(userType.getId());
		assertNotNull(standard.getId());
	}
	
	@Test
	public void testLotsOfReads() {
		for (GeneralData gd : GeneralData.getAll(entityManager)) {
			gd.getGeneralDatas();
		}
	}
	
	@Test
	public void testLotsOfUpdates() throws Exception {
		for (GeneralData gd : new ArrayList<GeneralData>(GeneralData.getAll(entityManager))) {
			if (gd.getData() == null) {
				gd.setData("Not null").save();
				gd.setData(null).save();
			}
		}
	}
}
