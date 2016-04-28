package com.digitald4.common.jpa;

import java.util.Collection;

public interface ChangeLog {
	public Collection<Change> getChanges();
}
