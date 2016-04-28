package com.digitald4.common.util;

@SuppressWarnings({ "unchecked", "rawtypes" })
public interface Expression {
	
	public boolean evaluate(Object operan);
	public boolean evaluate(Object operan1, Object operan2);
	
	public static class AbstractExpression implements Expression {
		
		public boolean evaluate(Object operan) {
			throw new IllegalArgumentException("" + operan);
		}

		@Override
		public boolean evaluate(Object operan1, Object operan2) {
			return evaluate(operan1);
		}
	}
	
	public static Expression Equals = new AbstractExpression() {
		@Override
		public boolean evaluate(Object operan1, Object operan2) {
			return Calculate.isSame(operan1, operan2);
		}
	};
	
	public static Expression LessThan = new AbstractExpression() {
		public boolean evaluate(Object operan1, Object operan2) {
			if (operan1 == null || operan2 == null) return false;
			return ((Comparable)operan1).compareTo(operan2) == -1;
		}
	};
	
	public static Expression LessThanOrEqualTo = new AbstractExpression() {
		public boolean evaluate(Object operan1, Object operan2) {
			if (operan1 == null || operan2 == null) return false;
			return ((Comparable)operan1).compareTo(operan2) != 1;
		}
	};
	
	public static Expression GreaterThan = new AbstractExpression() {
		public boolean evaluate(Object operan1, Object operan2) {
			if (operan1 == null || operan2 == null) return false;
			return ((Comparable)operan1).compareTo(operan2) == 1;
		}
	};
	
	public static Expression GreaterThanOrEqualTo = new AbstractExpression() {
		public boolean evaluate(Object operan1, Object operan2) {
			if (operan1 == null || operan2 == null) return false;
			return ((Comparable)operan1).compareTo(operan2) != -1;
		}
	};
	
	public static Expression IsNull = new AbstractExpression() {
		@Override
		public boolean evaluate(Object operan) {
			return Calculate.isNull(operan);
		}
	};
	
	public static Expression NotNull = new AbstractExpression() {
		@Override
		public boolean evaluate(Object operan) {
			return !Calculate.isNull(operan);
		}
	};
}
