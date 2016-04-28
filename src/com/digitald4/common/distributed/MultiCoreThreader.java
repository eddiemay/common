package com.digitald4.common.distributed;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.FormatText;

public class MultiCoreThreader implements MultiThreader {
	
	private int runners = Runtime.getRuntime().availableProcessors();
	
	public MultiCoreThreader setRunners(int runners_) {
		runners = runners_;
		return this;
	}
	
	public <R, T>  List<R> parDo(List<T> iterate, Function<R, T> function) {
		AtomicInteger atomInt = new AtomicInteger();
		List<R> results = new ArrayList<R>(iterate.size() * 2);
		List<Runner<R, T>> runnerList = new ArrayList<Runner<R, T>>();
		for (int r = 0; r < runners && r < iterate.size(); r++) {
			Runner<R, T> runner = new Runner<R, T>(iterate, function, atomInt, results);
			runnerList.add(runner);
			runner.start();
		}
		for (Runner<R, T> runner : runnerList) {
			try {
				runner.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return results;
	}
	
	private static class Runner<R, T> extends Thread {
		private final List<T> iterate;
		private final Function<R, T> function;
		private final AtomicInteger atomInt;
		private final List<R> results;
		
		public Runner(List<T> iterate, Function<R, T> function, AtomicInteger atomInt, List<R> results) {
			this.iterate = iterate;
			this.function = function;
			this.atomInt = atomInt;
			this.results = results;
		}
		
		@Override
		public void run() {
			int index;
			int size = iterate.size();
			while ((index = atomInt.getAndIncrement()) < size) {
				T i = iterate.get(index);
				System.out.println(Calculate.round(index * 100.0 / size, 1) + "% Running for " + i);
				long sTime = System.currentTimeMillis();
				R result = function.execute(i);
				results.add(result);
				System.out.println(FormatText.formatElapshed(System.currentTimeMillis() - sTime) + " for " + i);
			}
		}
	}
	
	public static void main(String[] args) {
		List<String> names = new ArrayList<String>();
		names.add("Eddie");
		names.add("Shalonda");
		names.add("Larry");
		names.add("Steve");
		names.add("Fan Tv App");
		names.add("Kobe");
		names.add("Jos");
		List<String> results = new MultiCoreThreader().parDo(names, new Function<String, String>() {
			@Override
			public String execute(String name) {
				return "Hello " + name;
			}
		});
		
		for (String result : results) {
			System.out.println(result);
		}
	}
}
