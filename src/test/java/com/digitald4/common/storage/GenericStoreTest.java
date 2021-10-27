package com.digitald4.common.storage;

import com.digitald4.common.model.Password;
import com.digitald4.common.storage.testing.DAOTestingImpl;
import org.junit.Test;

public class GenericStoreTest {
  @Test
  public void testClassConstuctor() {
    DAOTestingImpl dao = new DAOTestingImpl();
    GenericStore<Password> passwordStore = new GenericStore<>(Password.class, () -> dao);
  }

  @Test
  public void testInstanceConstuctor() {
    DAOTestingImpl dao = new DAOTestingImpl();
    GenericStore<Password> passwordStore = new GenericStore<>(new Password(), () -> dao);
  }
}
