package cloud.components;

import java.util.ArrayList;
import java.util.List;

import cloud.configurations.Parameters;

public class VM {
	private static int innerId = 0; //The VM's internal number, auto increased by 1 when generate a new VM
			
	private int vmID; //The VM's ID
	private int vmType; //The VM's Type
	private double vmPrice; //The VM's price
	private double executeTimeFactor; //The factor for execute time 
	
	private double leaseStartTime; //The lease start time 
	private double leaseFinishTime; //The lease finish time 
	private double totalLeaseCost; //The total leased cost
	
	private double availableTime; //Get the value in different status, 1.new VM: launchTime;2.Wait task: predicted finish time;3.execute task: actual finish time;4.Idle: current time
		
	private boolean status; //The on/off status
	private List<Task> taskList; //The set of assigned tasks
	private Task waitTask; //The wait task
	private Task executeTask; //The execute task
	
	private List<Task> waitTaskList; //Used in ROSA, store the wait tasks
	
	public static void resetInnerId(){	//Reset the internal number, called by the constructor of Scheduler
		innerId = 0;
	}
	
	public VM(int vmType, double startTime){
		this.vmID  = innerId++;
		this.vmType = vmType;
		this.vmPrice = Parameters.prices[vmType];
		this.executeTimeFactor = Parameters.speedFactor[vmType];
		
		this.leaseStartTime = startTime + Parameters.launchTime; //The lease start time equals the request time add the launchTime
		this.leaseFinishTime = -1;
		this.totalLeaseCost = -1;
		
		this.availableTime = -1;
			
		this.status = true;
		this.taskList = new ArrayList<Task>();
		this.waitTask = new Task("init", -1, -1);
		this.executeTask = new Task("init", -1, -1);
		
		this.waitTaskList = new ArrayList<Task>();
	}
	
	//-------------------------------------getters&setters--------------------------------
	public int getVMID() {
		return vmID;
	}
	public int getVMType() {
		return vmType;
	}
	
	public double getVMPrice() {
		return vmPrice;
	}
	
	public double getVMExecuteTimeFactor() {
		return executeTimeFactor;
	}
	
	public double getLeaseStartTime() {
		return leaseStartTime;
	}
	
	public double getLeaseFinishTime() {
		return leaseFinishTime;
	}
	public void setLeaseFinishTime(double leaseFinishTime) {
		this.leaseFinishTime = leaseFinishTime;
	}
	
	public double getTotalLeaseCost() {
		return totalLeaseCost;
	}
	public void setTotalLeaseCost(double totalLeaseCost) {
		this.totalLeaseCost = totalLeaseCost;
	}

	public double getAvailableTime() {
		return availableTime;
	}
	public void setAvailableTime(double availableTime) {
		this.availableTime = availableTime;
	}

	public boolean getVMStatus() {
		return status;
	}
	public void setVMStatus(boolean status) {
		this.status = status;
	}

	public List<Task> getTaskList() {
		return taskList;
	}
	public void setTaskList(List<Task> taskList) {
		this.taskList = taskList;
	}

	public Task getWaitTask() {
		return waitTask;
	}
	public void setWaitTask(Task waitTask) {
		this.waitTask = waitTask;
	}

	public Task getExecuteTask() {
		return executeTask;
	}
	public void setExecuteTask(Task executeTask) {
		this.executeTask = executeTask;
	}
	
	public List<Task> getWaitTaskList() {
		return waitTaskList;
	}
	public void setWaitTaskList(List<Task> waitTaskList) {
		this.waitTaskList = waitTaskList;
	}

}
