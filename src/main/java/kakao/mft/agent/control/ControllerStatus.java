package kakao.mft.agent.control;

public class ControllerStatus {
	private boolean run;
	
	ControllerStatus() {
		run = true;
	}
	
	public synchronized void stop() {
		this.run = false;
	}
	
	public boolean isRun() {
		return run;
	}
}
