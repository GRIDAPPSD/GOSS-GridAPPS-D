package gov.pnnl.goss.gridappsd;

import java.util.Stack;

import org.junit.Test;

import gov.pnnl.goss.gridappsd.testmanager.TimeInterval;


public class TestRunningWindow {
	@Test
	public void test60seconds(){

		long lastTimeStamp=0;
		long simulationTimestamp=0;
//		int duration = 60;
		int interval = 10;
		Stack<TimeInterval> timeIntervals = TimeInterval.getTimeIntervals(lastTimeStamp, simulationTimestamp, interval);
		for (TimeInterval timeInterval : timeIntervals) {
			System.out.println(timeInterval.toString());
		}
		System.out.println();
		simulationTimestamp=14;
		timeIntervals = TimeInterval.getTimeIntervals(lastTimeStamp, simulationTimestamp, interval);
		for (TimeInterval timeInterval : timeIntervals) {
			System.out.println(timeInterval.toString());
		}
		System.out.println();
		lastTimeStamp=simulationTimestamp;
		simulationTimestamp=29;
		timeIntervals = TimeInterval.getTimeIntervals(lastTimeStamp, simulationTimestamp, interval);
		for (TimeInterval timeInterval : timeIntervals) {
			System.out.println(timeInterval.toString());
		}
		System.out.println();
		lastTimeStamp=simulationTimestamp;
	
	}


}
