package com.digitald4.common.jpa;

import com.digitald4.common.jpa.DD4Cache.NULL_TYPES;
import com.digitald4.common.util.FormatText;
import com.digitald4.common.util.Pair;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

public class DD4TypedQueryImplV2<X> implements DD4TypedQuery<X> {
	private final DD4EntityManager em;
	private String name;
	private String query;
	private String sql;
	private Class<X> cls;
	private Map<String, Object> hints = new HashMap<String, Object>();
	private boolean complex;
	private Map<List<Object>, List<X>> cachedResults;
	
	private int firstResult;
	private FlushModeType flushMode;
	private LockModeType lockMode;
	private int maxResults;
	private Hashtable<Parameter<?>, Object> parameters = new Hashtable<Parameter<?>, Object>();
	private List<Pair<String, Expression>> props;
	
	public DD4TypedQueryImplV2(DD4EntityManager em, String name, String query, Class<X> cls) {
		this.em = em;
		this.name = name;
		this.query = query;
		this.cls = cls;
		cachedResults = new HashMap<List<Object>, List<X>>();
	}
	
	public DD4TypedQueryImplV2(DD4TypedQueryImplV2<X> orig) {
		this.em = orig.em;
		this.name = orig.getName();
		this.query = orig.getQuery();
		this.sql = orig.getSql();
		this.cls = orig.getTypeClass();
		this.hints = orig.getHints();
		this.complex = orig.isComplex();
		this.cachedResults = orig.cachedResults;
	}
	
	public String getName() {
		return name;
	}
	
	public String getQuery(){
		if (query == null) {
			query = EntityManagerHelper.getNamedQuery(getName() + "_FETCH", cls);
			if (query == null) {
				query = EntityManagerHelper.getNamedQuery(getName(), cls);
			}
		}
		return query;
	}
	
	public String getSql() {
		if (sql == null) {
			sql = EntityManagerHelper.getNamedNativeQuery(getName()+"_FETCH", cls);
			if (sql == null) {
				sql = EntityManagerHelper.convertJPQL2SQL(getTypeClass(), getQuery());
			}
		}
		return sql;
	}
	
	public Class<X> getTypeClass(){
		return cls;
	}
	
	public int executeUpdate() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getFirstResult() {
		return firstResult;
	}

	public FlushModeType getFlushMode() {
		return flushMode;
	}

	public Map<String, Object> getHints() {
		return hints;
	}

	public LockModeType getLockMode() {
		return lockMode;
	}

	public int getMaxResults() {
		return maxResults;
	}

	public Parameter<?> getParameter(String name) {
		for(Parameter<?> param:parameters.keySet())
			if(param.getName().equals(name))
				return param;
		return null;
	}

	public Parameter<?> getParameter(int position) {
		for (Parameter<?> param:parameters.keySet()) {
			if (param.getPosition()==position) {
				return param;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T> Parameter<T> getParameter(String name, Class<T> c) {
		return (Parameter<T>)getParameter(name);
	}

	@SuppressWarnings("unchecked")
	public <T> Parameter<T> getParameter(int position, Class<T> c) {
		return (Parameter<T>)getParameter(position);
	}

	@SuppressWarnings("unchecked")
	public <T> T getParameterValue(Parameter<T> param) {
		T value = (T)parameters.get(param);
		return value;
	}

	public Object getParameterValue(String name) {
		return parameters.get(getParameter(name));
	}

	public Object getParameterValue(int position) {
		return parameters.get(getParameter(position));
	}

	public Set<Parameter<?>> getParameters() {
		return new TreeSet<Parameter<?>>(parameters.keySet());
	}

	public boolean isBound(Parameter<?> param) {
		return parameters.contains(param);
	}

	public <T> T unwrap(Class<T> c) {
		try {
			return c.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public List<X> getResultList() {
		List<X> results = cachedResults.get(getValues());
		if (results == null) {
			results = fetchResults();
		}
		return results;
	}
	
	private List<X> fetchResults() {
		List<X> results;
		try {
			results = em.fetchResults(this);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		cachedResults.put(getValues(), results);
		return results;
	}

	@Override
	public X getSingleResult() {
		return getResultList().get(firstResult);
	}

	@Override
	public TypedQuery<X> setFirstResult(int firstResult) {
		this.firstResult = firstResult;
		return this;
	}

	@Override
	public TypedQuery<X> setFlushMode(FlushModeType flushMode) {
		this.flushMode = flushMode;
		return this;
	}

	@Override
	public TypedQuery<X> setHint(String hint, Object value) {
		hints.put(hint, value);
		return this;
	}

	@Override
	public TypedQuery<X> setLockMode(LockModeType lockMode) {
		this.lockMode = lockMode;
		return this;
	}

	@Override
	public TypedQuery<X> setMaxResults(int maxResults) {
		this.maxResults = maxResults;
		return this;
	}

	@Override
	public <T> TypedQuery<X> setParameter(Parameter<T> param, T value) {
		parameters.put(param,value);
		return this;
	}

	@Override
	public TypedQuery<X> setParameter(String name, Object value) {
		Parameter<?> param = getParameter(name);
		if(param == null)
			param = new DD4Parameter<Object>(name,Object.class,parameters.size()+1);
		parameters.put(param,value);
		return this;
	}

	@Override
	public TypedQuery<X> setParameter(int position, Object value) {
		Parameter<?> param = getParameter(position);
		if(param == null)
			param = new DD4Parameter<Object>(""+position,Object.class,parameters.size()+1);
		parameters.put(param,value);
		return this;
	}

	@Override
	public TypedQuery<X> setParameter(Parameter<Calendar> param, Calendar value, TemporalType tt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypedQuery<X> setParameter(Parameter<Date> param, Date value, TemporalType tt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypedQuery<X> setParameter(String name, Calendar value, TemporalType tt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypedQuery<X> setParameter(String name, Date value, TemporalType tt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypedQuery<X> setParameter(int position, Calendar value, TemporalType tt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypedQuery<X> setParameter(int position, Date value, TemporalType tt) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object[] getParameterValues() {
		Object[] values = new Object[getParameters().size()];
		int i = 0;
		for (Parameter<?> param : getParameters()) {
			values[i++] = getParameterValue(param);
		}
		return values;
	}
	
	private List<Pair<String, Expression>> getProperties() {
		if (props == null) {
			props = new ArrayList<Pair<String, Expression>>();
			String query = getQuery().toUpperCase();
			if (query.contains("WHERE")) {
				String where = query.substring(query.indexOf("WHERE"));
				StringTokenizer st = new StringTokenizer(where, ".");
				st.nextToken();
				while (st.hasMoreTokens()) {
					String elem = st.nextToken();
					if (elem.contains("<=")) {
						elem=elem.substring(0, elem.indexOf("<=")).trim();
						props.add(new Pair<String, Expression>(elem, Expression.LessThanOrEqualTo));
						complex = true;
					} else if (elem.contains(">=")) {
						elem=elem.substring(0, elem.indexOf(">=")).trim();
						props.add(new Pair<String, Expression>(elem, Expression.GreaterThanOrEqualTo));
						complex = true;
					} else if (elem.contains("=")) {
						elem=elem.substring(0, elem.indexOf("=")).trim();
						props.add(new Pair<String, Expression>(elem, Expression.Equals));
					} else if (elem.contains("<")) {
						elem=elem.substring(0, elem.indexOf("<")).trim();
						props.add(new Pair<String, Expression>(elem, Expression.LessThan));
						complex = true;
					} else if (elem.contains(">")) {
						elem=elem.substring(0, elem.indexOf(">")).trim();
						props.add(new Pair<String, Expression>(elem, Expression.GreaterThan));
						complex = true;
					} else if (elem.contains("IS NULL")) {
						elem=elem.substring(0, elem.indexOf("IS NULL")).trim();
						props.add(new Pair<String, Expression>(elem, Expression.IsNull));
					} else if (elem.contains("IS NOT NULL")) {
						elem=elem.substring(0, elem.indexOf("IS NOT NULL")).trim();
						props.add(new Pair<String, Expression>(elem, Expression.NotNull));
					}
				}
			}
		}
		return props;
	}
	
	private <T> ArrayList<Object> getValues() {
		ArrayList<Object> values = new ArrayList<Object>();
		String query = getQuery();
		query = query.toUpperCase();
		if (query.contains("WHERE")) {
			if (!query.contains(".")) { 
				throw new IllegalArgumentException("No dots. Please format attributes with \"o.\". Query: " + query);
			}
			String where = query.substring(query.indexOf("WHERE"));
			StringTokenizer st = new StringTokenizer(where, ".");
			st.nextToken();
			while (st.hasMoreTokens()) {
				String elem = st.nextToken();
				if (elem.contains("?")) {
					elem = elem.substring(elem.indexOf("?")+1);
					if (elem.contains(" ")) {
						elem = elem.substring(0, elem.indexOf(" "));
					}
					int pos = Integer.parseInt(elem);
					values.add(getParameterValue(pos));
				} else if (elem.contains(":")) {
					elem=elem.substring(elem.indexOf(":")+1);
					if (elem.contains(" ")) {
						elem = elem.substring(0, elem.indexOf(" "));
					}
					values.add(getParameterValue(elem));
				} else if (elem.contains(""+NULL_TYPES.IS_NULL)) {
					values.add(NULL_TYPES.IS_NULL);
				} else if (elem.contains(""+NULL_TYPES.IS_NOT_NULL)) {
					values.add(NULL_TYPES.IS_NOT_NULL);
				} else if (elem.contains("IS NULL")) {
					values.add(null);
				} else if (elem.contains("<=")) {
					elem=elem.substring(0, elem.indexOf("<=")).trim();
					values.add(elem);
				} else if (elem.contains(">=")) {
					elem=elem.substring(0, elem.indexOf(">=")).trim();
					values.add(elem);
				} else if (elem.contains("=")) {
					elem=elem.substring(0, elem.indexOf("=")).trim();
					values.add(elem);
				} else if (elem.contains("<")) {
					elem=elem.substring(0, elem.indexOf("<")).trim();
					values.add(elem);
				} else if (elem.contains(">")) {
					elem=elem.substring(0, elem.indexOf(">")).trim();
					values.add(elem);
				}
			}
		}
		return values;
	}

	public boolean isComplex() {
		return complex;
	}
	
	public void evict(Object o) {
		for (List<X> results : cachedResults.values()) {
			results.remove(o);
		}
	}
	
	public boolean cache(X o) {
		Class<?> c = o.getClass();
		List<Pair<String, Expression>> props = getProperties();
		List<Object> values = new ArrayList<Object>();
		for (Pair<String, Expression> key : props) {
			Object value = null;
			Method m = null;
			try {
				m = c.getMethod("get" + FormatText.toUpperCamel(key.getLeft()));
				value = m.invoke(o);
			} catch (NoSuchMethodException e) {
				try {
					m = c.getMethod("is" + FormatText.toUpperCamel(key.getLeft()));
					value = m.invoke(o);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				
			} catch (Exception e) {
				throw new RuntimeException("Error executing: " + m);
			}
			values.add(value);
		}

		if (!isComplex()) {
			List<X> collection = cachedResults.get(values);
			if (collection != null) {
				collection.add(o);
				return true;
			}
			return false;
		} else {
			boolean ret = false;
			for (List<Object> vc : cachedResults.keySet()) {
				if (meetsCriteria(vc, values)) {
					cachedResults.get(vc).add(o);
				}
			}
			return ret;
		}
	}
	
	public boolean meetsCriteria(List<Object> valueCollection, List<Object> values) {
		int i = 0;
		for (Object value : values) {
			if (!props.get(i).getRight().evaluate(value, valueCollection.get(i))) {
				return false;
			}
			i++;
		}
		return true;
	}
}
