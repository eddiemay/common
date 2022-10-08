package com.digitald4.common.storage;

import com.digitald4.common.model.Password;
import com.digitald4.common.storage.testing.DAOTestingImpl;
import org.junit.Test;

public class ModelObjectStoreTest {
  @Test
  public void testInstanceConstuctor() {
    DAOTestingImpl dao = new DAOTestingImpl();
    ModelObjectStore<Long, Password> passwordStore = new ModelObjectStore<>(new Password(), () -> dao);
  }

  @Test
  public void testClassConstuctor() {
    DAOTestingImpl dao = new DAOTestingImpl();
    ModelObjectStore<Long, Password> passwordStore = new ModelObjectStore<>(Password.class, () -> dao);
  }
}
