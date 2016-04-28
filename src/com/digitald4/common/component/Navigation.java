package com.digitald4.common.component;

import java.util.Collection;

public class Navigation {
	public static Navigation navigation;
	private final Collection<NavItem> navItems;
	
	public static Navigation get() {
		return navigation;
	}
	
	public static void setNavigation(Navigation _navigation) {
		navigation = _navigation;
	}
	
	public Navigation(Collection<NavItem> navItems) {
		this.navItems = navItems;
	}
	
	public Collection<NavItem> getNavItems() {
		return navItems;
	}
	
	public NavItem findNavItem(String url) {
		for (NavItem top : getNavItems()) {
			NavItem item = top.findNavItem(url);
			if (item != null) {
				return item;
			}
		}
		return null;
	}
}
