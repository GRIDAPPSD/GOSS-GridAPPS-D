package gov.pnnl.goss.gridappsd.testmanager;

import java.util.Stack;

public class TimeInterval{
	public long start=0;
	public long end=0;
	
	public TimeInterval(long start, long end) {
		this.start = start;
		this.end = end;
	}

	public String toString(){
		return "start time " + start + " end time " + end;
	}
	
	// 
	public static Stack<TimeInterval> getTimeIntervals(long lastTimeStamp, long simulationTimestamp, int interval) {
		Stack<TimeInterval> timeIntervals = new Stack<TimeInterval>();
		// Just for the first time
		if(lastTimeStamp==simulationTimestamp){
			TimeInterval timeInterval = new TimeInterval(simulationTimestamp , simulationTimestamp);
			timeIntervals.add(timeInterval);		
			return timeIntervals;
		}
		
		for (long window_Start_time = lastTimeStamp; window_Start_time < Long.valueOf(simulationTimestamp); window_Start_time += interval) {
			long window_end_time = window_Start_time+interval;
			if(window_end_time>simulationTimestamp){
				window_end_time=simulationTimestamp;
			}
			TimeInterval timeInterval = new TimeInterval(window_Start_time+1 , window_end_time);
			timeIntervals.add(timeInterval);
//			System.out.println(timeInterval.toString());
		}
		return timeIntervals;
	}
}