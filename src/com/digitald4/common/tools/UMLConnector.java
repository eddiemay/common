package com.digitald4.common.tools;

import org.jdom.Element;

public class UMLConnector {
	private UMLReference umlReference;
	private String attribute;
	private String refAttr;
	private String desc;
	public UMLConnector(UMLReference umlReference, String attribute, String refAttr) {
		setUmlReference(umlReference);
		setAttribute(attribute);
		setRefAttr(refAttr);
	}
	public UMLConnector(UMLReference umlReference, Element e) {
		setUmlReference(umlReference);
		setAttribute(e.getAttributeValue("attribute"));
		setRefAttr(e.getAttributeValue("refattr"));
		setDesc(e.getText());
	}

	public UMLReference getUmlReference() {
		return umlReference;
	}

	public void setUmlReference(UMLReference umlReference) {
		this.umlReference = umlReference;
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
	
	public String getAttrDBName(){
		return getAttribute().toUpperCase().replaceAll(" ", "_");
	}

	public String getRefAttr() {
		return refAttr;
	}

	public void setRefAttr(String refAttr) {
		this.refAttr = refAttr;
	}
	
	public String getRefAttrDBName(){
		return getRefAttr().toUpperCase().replaceAll(" ", "_");
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
	public String toString(){
		return getAttribute();
	}
	public Element getXMLElement() {
		Element e = new Element("CONNECTOR");
		e.setAttribute("attribute", getAttribute());
		e.setAttribute("refattr",getRefAttr());
		e.setText(getDesc());
		return e;
	}

}
