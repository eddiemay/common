package com.digitald4.common.storage;

import com.digitald4.common.model.Password;
import com.digitald4.common.storage.testing.DAOTestingImpl;
import org.junit.Test;

public class GenericStoreTest {
  @Test
  public void testClassConstructor() {
    DAOTestingImpl dao = new DAOTestingImpl(new ChangeTracker(null, null, null, null, null));
    GenericStore<Password, Long> passwordStore = new GenericStore<>(Password.class, () -> dao);
  }
}
