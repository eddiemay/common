package com.digitald4.common.test;

import java.util.ArrayList;

import org.junit.Test;


import junit.framework.TestCase;

public class InOrderTest extends TestCase {
	public void testDependable(){
		Dependable d1 = new Dependable("1");
		Dependable d2 = new Dependable("2");
		Dependable d3 = new Dependable("3");
		Dependable d4 = new Dependable("4");
		Dependable d5 = new Dependable("5");
		Dependable d6 = new Dependable("6");
		d2.addPred(d1);
		d3.addPred(d1);
		d4.addPred(d1);
		d4.addPred(d5);
		d5.addPred(d2);
		d6.addPred(d3);
		d4.output();
	}
	private class Dependable{
		private ArrayList<Dependable> preds = new ArrayList<Dependable>();
		private ArrayList<Dependable> succs = new ArrayList<Dependable>();
		private String name;
		private boolean outted=false;
		public Dependable(String name){
			this.name = name;
		}
		public void addPred(Dependable pred){
			preds.add(pred);
			pred.succs.add(this);
		}
		public boolean isOutted(){
			return outted;
		}
		public void output(){
			for(Dependable dep:preds)
				if(!dep.isOutted()){
					dep.output();
					return;
				}
			System.out.println(name);
			outted=true;
			for(Dependable suc:succs)
				if(!suc.isOutted())
					suc.output();
		}
	}
	
	@Test
	public void testSuperSuper() {
		assertEquals(15, new A().foo());
		assertEquals(16, new B().foo());
		assertEquals(15, new C().foo());
	}
	
	private class A {
		public int foo() {
			return 15;
		}
	}
	
	private class B extends A {
		public int foo() {
			return 16;
		}
	}
	
	private class C extends B {
		public int foo() {
			return new A().foo();
		}
	}
	
	public int getValue(String word) {
		int value = 0;
		for (char c : word.toCharArray()) {
			value += c - 'A' + 1;
		}
		System.out.println(word + " = " + value);
		return value;
	}
	
	@Test
	public void testWordValues() {
		getValue("HARDWORK");
		getValue("ATTITUDE");
		getValue("HUSTLING");
		getValue("ICAN");
		getValue("IDID");
		getValue("IWILL");
	}
}
