package com.digitald4.common.jpa;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

import com.digitald4.common.log.EspLogger;
import com.digitald4.common.util.Expression;
import com.digitald4.common.util.Pair;

public class DD4TypedQueryImpl<X> implements DD4TypedQuery<X> {
	private DD4Cache cache;
	private String name;
	private String query;
	private Class<X> c;
	private int firstResult;
	private FlushModeType flushMode;
	private Hashtable<String,Object> hints = new Hashtable<String,Object>();
	private LockModeType lockMode;
	private int maxResults;
	private boolean complex;
	private Hashtable<Parameter<?>,Object> parameters = new Hashtable<Parameter<?>,Object>();
	
	public DD4TypedQueryImpl(DD4Cache cache, String name, String query, Class<X> c){
		this.cache = cache;
		this.name = name;
		this.query = query;
		this.c = c;
	}
	
	public String getName(){
		return name;
	}
	
	public String getQuery(){
		return query;
	}
	
	public String getSql() {
		return null;
	}
	
	public Class<X> getTypeClass(){
		return c;
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
		for(Parameter<?> param:parameters.keySet())
			if(param.getPosition()==position)
				return param;
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

	public List<X> getResultList() {
		try {
			return ((DD4CacheImpl)cache).find(this);
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
	}

	public X getSingleResult() {
		return getResultList().get(firstResult);
	}

	public TypedQuery<X> setFirstResult(int firstResult) {
		this.firstResult = firstResult;
		return this;
	}

	public TypedQuery<X> setFlushMode(FlushModeType flushMode) {
		this.flushMode = flushMode;
		return this;
	}

	public TypedQuery<X> setHint(String hint, Object value) {
		hints.put(hint, value);
		return this;
	}

	public TypedQuery<X> setLockMode(LockModeType lockMode) {
		this.lockMode = lockMode;
		return this;
	}

	public TypedQuery<X> setMaxResults(int maxResults) {
		this.maxResults = maxResults;
		return this;
	}

	public <T> TypedQuery<X> setParameter(Parameter<T> param, T value) {
		parameters.put(param,value);
		return this;
	}

	public TypedQuery<X> setParameter(String name, Object value) {
		Parameter<?> param = getParameter(name);
		if(param == null)
			param = new DD4Parameter<Object>(name,Object.class,parameters.size()+1);
		parameters.put(param,value);
		return this;
	}

	public TypedQuery<X> setParameter(int position, Object value) {
		Parameter<?> param = getParameter(position);
		if(param == null)
			param = new DD4Parameter<Object>(""+position,Object.class,parameters.size()+1);
		parameters.put(param,value);
		return this;
	}

	public TypedQuery<X> setParameter(Parameter<Calendar> param, Calendar value, TemporalType tt) {
		// TODO Auto-generated method stub
		return null;
	}

	public TypedQuery<X> setParameter(Parameter<Date> param, Date value, TemporalType tt) {
		// TODO Auto-generated method stub
		return null;
	}

	public TypedQuery<X> setParameter(String name, Calendar value, TemporalType tt) {
		// TODO Auto-generated method stub
		return null;
	}

	public TypedQuery<X> setParameter(String name, Date value, TemporalType tt) {
		// TODO Auto-generated method stub
		return null;
	}

	public TypedQuery<X> setParameter(int position, Calendar value, TemporalType tt) {
		// TODO Auto-generated method stub
		return null;
	}

	public TypedQuery<X> setParameter(int position, Date value, TemporalType tt) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Object[] getParameterValues() {
		Object[] values = new Object[getParameters().size()];
		int i=0;
		for(Parameter<?> param:getParameters())
			values[i++] = getParameterValue(param);
		return values;
	}
	
	private PropertyCollection<X> pc;
	public PropertyCollection<X> getPropertyCollection() {
		if (pc == null) {
			List<Pair<String, Expression>> props = getProperties();
			Pair<String, Expression>[] columns = new Pair[props.size()];
			for (int x = 0; x < props.size(); x++) {
				columns[x] = props.get(x);
			}
			pc = new PropertyCollection<X>(isComplex(), columns);
		}
		return pc;
	}
	
	private ValueCollection<X> vc;
	public ValueCollection<X> getValueCollection() throws Exception{
		if(vc == null)
			vc = new ValueCollection<X>(getPropertyCollection(), getValues().toArray());
		return vc;
	}
	
	private ArrayList<Pair<String, Expression>> getProperties() {
		ArrayList<Pair<String, Expression>> props = new ArrayList<Pair<String, Expression>>();
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
		return props;
	}
	
	private <T> ArrayList<Object> getValues() throws Exception {
		ArrayList<Object> values = new ArrayList<Object>();
		String query = getQuery();
		query = query.toUpperCase();
		if (query.contains("WHERE")) {
			if(!query.contains(".")) EspLogger.error(this,"No dots. Please format attributes with \"o.\" "+query);
			String where = query.substring(query.indexOf("WHERE"));
			StringTokenizer st = new StringTokenizer(where,".");
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
				} else if (elem.contains(""+DD4Cache.NULL_TYPES.IS_NULL)) {
					values.add(DD4Cache.NULL_TYPES.IS_NULL);
				} else if (elem.contains(""+DD4Cache.NULL_TYPES.IS_NOT_NULL)) {
					values.add(DD4Cache.NULL_TYPES.IS_NOT_NULL);
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
}
