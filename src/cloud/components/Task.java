package cloud.components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class Task implements Serializable {
	private final String taskID; //The task's ID
	private int taskWorkflowID; //The workflow's ID which the task belongs to
	
	private double baseExecuteTime; //The task's base execution time, get from the DAX file
	private double baseExecuteTimeWithDeviation; //The deviate base execution time of the task, only can be get when execute on a VM even if it is calculated by normal distribution at the schedule init phase
		
	private double latestStartTime; //The task's latest start time, calculate by the workflow's deadline and the fastest VM
	private double latestFinishTime; //The task's latest finish time, calculate by the workflow's deadline and the fastest VM
	private double pUpwardRank; //The task's probabilistic upward rank
	private double subDeadline; //The task's subdeadline
	
	private double predictStartTime; //The task's predict start time when as a wait task assigned on a VM
	private double predictFinishTime; //The task's predict finish time when as a wait task assigned on a VM
	
	private double actualStartTime; //The task's actual start time, only can be get after the task is execute on a VM
	private double actualExecuteTime; //The task's actual execute time, calculate by baseExecutionTimeWithDeviation*VMFactor
	private double actualFinishTime; //The task's actual finish time, only can be get after the task is execute on a VM
	
	private boolean isAssigned; //Whether this task has been assigned
	private VM assignedVM; //The assigned VM 
	private boolean isFinished; //Whether this task has been finished
	
	private List<Edge> inEdges; //The in edges set, includes all the immediate predecessors
	private List<Edge> outEdges; //The out edges set, includes all the immediate successors
	
	/** Only used in ROSA*/
	private double predictLatestStartTime; //The task's predict latest start time
	private double predictLatestFinishTime; //The task's predict latest finish time, not used 
	private double allowExecutionTime; //The task's allow execute time
	
	/** Only used in NOSF*/
	private double earliestStartTime; //The task's earliest start time, only can be get after the task is execute on a VM
	private double earliestFinishTime; //The task's earliest finish time, calculate by baseExecutionTimeWithDeviation*VMFactor
	private double thelta; //The difference between subdeadline and earliestStartTime
	
	public Task(String taskID, int taskWorkflowID,  double baseExecuteTime) {
		this.taskID  = taskID;
		this.taskWorkflowID = taskWorkflowID;
		
		this.baseExecuteTime = baseExecuteTime;
		this.baseExecuteTimeWithDeviation = -1;
		
		this.latestStartTime = -1;
		this.latestFinishTime = -1;
		this.pUpwardRank = -1;
		this.subDeadline = -1;
		
		this.predictStartTime = -1;
		this.predictFinishTime = -1;
		
		this.actualStartTime = -1;
		this.actualExecuteTime = -1;
		this.actualFinishTime = -1;
		
		this.isAssigned = false;
		this.assignedVM = null;
		this.isFinished = false;
		
		inEdges = new ArrayList<Edge>();
		outEdges = new ArrayList<Edge>();
		
		this.predictLatestStartTime = -1;
		this.predictLatestFinishTime = -1;
		this.allowExecutionTime = -1;
		
		this.earliestStartTime = -1;
		this.earliestFinishTime = -1;
		this.thelta = -1;
	}
	
	//-------------------------------------getters&setters--------------------------------
	public String getTaskID() {
		return taskID;
	}

	public int getTaskWorkflowID() {
		return taskWorkflowID;
	}
	
	public double getBaseExecuteTime() {
		return baseExecuteTime;
	}
			
	public double getBaseExecuteTimeWithDeviation() {
		return baseExecuteTimeWithDeviation;
	}
	public double setBaseExecuteTimeWithDeviation(double baseExecuteTimeWithDeviation) {
		return this.baseExecuteTimeWithDeviation = baseExecuteTimeWithDeviation;
	}
	
	public double getLatestStartTime() {
		return latestStartTime;
	}
	public double setLatestStartTime(double latestStartTime) {
		return this.latestStartTime = latestStartTime;
	}
	
	public double getLatestFinishTime() {
		return latestFinishTime;
	}
	public double setLatestFinishTime(double latestFinishTime) {
		return this.latestFinishTime = latestFinishTime;
	}

	public double getPUpwardRank() {
		return pUpwardRank;
	}
	public double setPUpwardRank(double pUpwardRank) {
		return this.pUpwardRank = pUpwardRank;
	}

	public double getSubDeadline() {
		return subDeadline;
	}
	public double setSubDeadline(double subDeadline) {
		return this.subDeadline = subDeadline;
	}
	
	public double getPredictStartTime() {
		return predictStartTime;
	}
	public double setPredictStartTime(double predictStartTime) {
		return this.predictStartTime = predictStartTime;
	}
	
	public double getPredictFinishTime() {
		return predictFinishTime;
	}
	public double setPredictFinishTime(double predictFinishTime) {
		return this.predictFinishTime = predictFinishTime;
	}
	
	public double getActualStartTime() {
		return actualStartTime;
	}
	public double setActualStartTime(double actualStartTime) {
		return this.actualStartTime = actualStartTime;
	}
	
	public double getActualExecuteTime() {
		return actualExecuteTime;
	}
	public double setActualExecuteTime(double actualExecuteTime) {
		return this.actualExecuteTime = actualExecuteTime;
	}

	public double getActualFinishTime() {
		return actualFinishTime;
	}
	public double setActualFinishTime(double actualFinishTime) {
		return this.actualFinishTime = actualFinishTime;
	}
	
	public boolean getIsAssigned() {
		return isAssigned;
	}
	public boolean setIsAssigned(boolean isAssigned) {
		return this.isAssigned = isAssigned;
	}
	
	public VM getAssignedVM() {
		return assignedVM;
	}
	public VM setAssignedVM(VM assignedVM) {
		return this.assignedVM = assignedVM;
	}

	public boolean getIsFinished() {
		return isFinished;
	}
	public boolean setIsFinished(boolean isFinished) {
		return this.isFinished = isFinished;
	}
	
	public List<Edge> getInEdges() {
		return inEdges;
	}	
	public void addInEdges(Edge inEdge) {
		this.inEdges.add(inEdge);
	}
	
	public List<Edge> getOutEdges() {
		return outEdges;
	}	
	public void addOutEdges(Edge outEdge) {
		this.outEdges.add(outEdge);
	}
	
	public double getPredictLatestStartTime() {
		return predictLatestStartTime;
	}
	public double setPredictLatestStartTime(double predictLatestStartTime) {
		return this.predictLatestStartTime = predictLatestStartTime;
	}
	
	public double getPredictLatestFinishTime() {
		return predictLatestFinishTime;
	}
	public double setPredictLatestFinishTime(double predictLatestFinishTime) {
		return this.predictLatestFinishTime = predictLatestFinishTime;
	}
	
	public double getAllowExecutionTime() {
		return allowExecutionTime;
	}
	public double setAllowExecutionTime(double allowExecutionTime) {
		return this.allowExecutionTime = allowExecutionTime;
	}
	
	public double getEarliestStartTime() {
		return earliestStartTime;
	}
	public double setEarliestStartTime(double earliestStartTime) {
		return this.earliestStartTime = earliestStartTime;
	}

	public double getEarliestFinishTime() {
		return earliestFinishTime;
	}
	public double setEarliestFinishTime(double earliestFinishTime) {
		return this.earliestFinishTime = earliestFinishTime;
	}
	
	public double getThelta() {
		return thelta;
	}
	public double setThelta(double thelta) {
		return this.thelta = thelta;
	}
	
}
