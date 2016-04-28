package com.digitald4.common.tld;

import java.util.Collection;

import com.digitald4.common.component.NavItem;
import com.digitald4.common.component.Navigation;

public class BreadCrumbTag extends DD4Tag {
	private final static String START = "<ul id=\"breadcrumb\">";
	private final static String CRUMB = "<li><a href=\"%url\" title=\"%n\">%n</a></li>";
	private final static String END = "</ul>";
	private String selected;
	private Navigation navigation;

	public void setSelected(String selected) {
		this.selected = selected;
	}
	
	public void setNavigation(Navigation navigation) {
		this.navigation = navigation;
	}
	
	public String getOutput() {
		String out = "";
		NavItem navItem = navigation.findNavItem(selected);
		while (navItem != null) {
			if (navItem.isNavigatible()) {
				out = CRUMB.replaceAll("%url", navItem.getUrl()).replaceAll("%n", navItem.getName()) + out;
			} else {
				out = "<li> " + navItem.getName() + "</li>" + out;
			}
			navItem = navItem.getParent();
		}
		return START + out + END;
	}

	public Collection<NavItem> getTopNavItems() {
		return navigation.getNavItems();
	}
}
