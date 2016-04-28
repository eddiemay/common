package com.digitald4.common.jdbc;

public class StatsSQLImp implements Comparable<Object>{
	private String sql;
	private int count=0;
	private long totalTime=0;
	private long maxTime=0;
	private long minTime=Integer.MAX_VALUE;
	
	public StatsSQLImp(String sql){
		this.sql = sql;
	}
	public String getSQL(){
		return sql;
	}
	public int getCount(){
		return count;
	}
	public synchronized void processStatement(StatsSQL tSQL){
		count++;
		long time = tSQL.getTotalTime();
		totalTime += time;
		if(time > maxTime)
			maxTime = time;
		if(time < minTime)
			minTime = time;
	}
	public long getTotalTime(){
		return totalTime;
	}
	public long getAvgTime(){
		return getTotalTime()/getCount();
	}
	public long getMaxTime(){
		return maxTime;
	}
	public long getMinTime(){
		return minTime;
	}
	public String getReportOutput(){
		return "count: "+getCount()+" total time: "+getTotalTime()+" avg: "+getAvgTime()+" max: "+getMaxTime()+" min: "+getMinTime()+" SQL: "+getSQL();
	}
	public String toString(){
		return getSQL();
	}
	@Override
	public int compareTo(Object o) {
		if(o instanceof StatsSQLImp){
			StatsSQLImp psc = (StatsSQLImp)o;
			if(getTotalTime() > psc.getTotalTime())
				return -1;
			if(getTotalTime() < psc.getTotalTime())
				return 1;
			if(getCount() > psc.getCount())
				return -1;
			if(getCount() < psc.getCount())
				return 1;
		}
		return toString().compareTo(o.toString());
	}
}