package gov.pnnl.goss.gridappsd.app;

import java.util.Timer;
import java.util.TimerTask;

public class ApplicationTimeoutTask extends TimerTask {

	private int seconds;
	private String appId;
	private HeartbeatTimeout timeoutCallback;

	public ApplicationTimeoutTask(int seconds, String appId, HeartbeatTimeout timeoutCallback) {
		this.seconds = seconds;
		this.appId = appId;
		this.timeoutCallback = timeoutCallback;
		// timer = new Timer(true);
		// reset();
	}

	public void reset() {

		Timer timer = new Timer(true);
		try {
			timer.scheduleAtFixedRate(this, 1000, this.seconds*1000);
		}
		catch (IllegalStateException e) {
			this.cancel();
			try {
				timer.scheduleAtFixedRate(this, 1000, this.seconds*1000);
			}
			catch (IllegalStateException e1) {

			}
		}
		//		try {
//			timer.cancel();
//		}
//		catch (java.lang.IllegalStateException e) {
//			// Ignored because canceling a non scheduled task throws this.
//
//		}

	}

	@Override
	public void run() {
		System.out.println("Timeout not reset  in time for " + this.appId);
		this.timeoutCallback.timeout(this.appId);

	}


}
