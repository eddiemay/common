package com.digitald4.common.storage;

import com.digitald4.common.model.PasswordInfo;
import com.digitald4.common.storage.testing.DAOTestingImpl;
import org.junit.Test;

public class GenericStoreTest {
  @Test
  public void testClassConstuctor() {
    DAOTestingImpl dao = new DAOTestingImpl();
    GenericStore<PasswordInfo> passwordStore = new GenericStore<>(PasswordInfo.class, () -> dao);
  }

  @Test
  public void testInstanceConstuctor() {
    DAOTestingImpl dao = new DAOTestingImpl();
    GenericStore<PasswordInfo> passwordStore = new GenericStore<>(new PasswordInfo(), () -> dao);
  }
}
