package com.digitald4.common.tld;

import java.util.Collection;

import com.digitald4.common.component.NavItem;
import com.digitald4.common.component.Navigation;

public class NavTag extends DD4Tag {
	private final static String START_NAV = "<nav id=\"main-nav\"><ul class=\"container_12\">";
	private final static String MAIN_MENU_OPEN = "<li class=\"%cn\"><a href=\"%sn\" title=\"%n\">%n</a><ul>";
	private final static String MAIN_MENU_CLOSE = "</ul></li>";
	private final static String SUB_MENU = "<li%cn><a href=\"%sn\" title=\"%n\">%n</a></li>";
	private final static String END_NAV = "</ul></nav>";
	private String selected;
	private Navigation navigation;

	public void setSelected(String selected) {
		this.selected = selected;
	}
	
	public void setNavigation(Navigation navigation) {
		this.navigation = navigation;
	}
	
	public String getOutput() {
		String out = START_NAV;
		NavItem selSub = navigation.findNavItem(selected);
		NavItem selTop = selSub != null ? selSub.getParent() : null;
		while (selTop != null && selTop.getParent() != null) {
			selSub = selTop;
			selTop = selTop.getParent();
		}
		for (NavItem top : navigation.getNavItems()) {
			out += MAIN_MENU_OPEN.replaceAll("%cn", top.getUrl() + (top == selTop ? " current" : "")).replaceAll("%sn", top.getUrl()).replaceAll("%n", top.getName());
			for (NavItem sub : top.getSubItems()) {
				out += SUB_MENU.replaceAll("%cn", sub == selSub ? " class=\"current\"" : "").replaceAll("%sn", sub.getUrl()).replaceAll("%n", sub.getName());
			}
			out += MAIN_MENU_CLOSE;
		}
		out += END_NAV;
		return out;
	}

	public Collection<NavItem> getTopNavItems() {
		return navigation.getNavItems();
	}
}
