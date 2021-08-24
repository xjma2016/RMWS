package cloud.components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class Workflow implements Serializable, Cloneable
{
	private int workflowId; //Workflow's ID, used to distinguish different workflows
	private final String workflowName; //Workflow's name, the format is 'type-tasknumber',e.g.LIGO-50
	private double arrivalTime; //Workflow's arrival time, start at 0 second and add by 1 second
	private double makespan; //Workflow's makespan based on the base execution time of all its tasks
	private double deadline; //Workflows's deadline, the finish time if assigned all tasks on the fastest VM and ignored the data transfer time
	
	private List<Task> taskList; //The list set to store all tasks in workflow
	private List<Edge> edgeList; //The list set to store all edges in workflow
	private boolean startedFlag; //Whether this workflow start to be allocated
	private double finishTime;   //Workflow's finish time
	private boolean successFlag; //Whether this workflow is finished before deadline
	
	public Workflow(int workFlowId, String name, double arrivalTime, double makespan, double deadline)
	{
		this.workflowId = workFlowId;
		this.workflowName = name;
		this.arrivalTime = arrivalTime;
		this.makespan = makespan;
		this.deadline = deadline;
			
		this.taskList = new ArrayList<Task>(); 
		this.edgeList = new ArrayList<Edge>(); 
		this.startedFlag = false;
		this.finishTime = -1;
		this.successFlag = false;
	}
	
	//-------------------------------------getters&setters--------------------------------
	public int getWorkflowId()
	{
		return workflowId;
	}
	public void setWorkflowId(int workflowId)
	{
		this.workflowId = workflowId;
	}
	
	public String getWorkflowName()
	{
		return workflowName;
	}
	
	public double getArrivalTime()
	{
		return arrivalTime;
	}
	public void setArrivalTime(double arrivalTime)
	{
		this.arrivalTime = arrivalTime;
	}
	
	public double getMakespan()
	{
		return makespan;
	}
	public void setMakespan(double makespan)
	{
		this.makespan = makespan;
	}
	
	public double getDeadline()
	{
		return deadline;
	}
	public void setDeadline(double deadline)
	{
		this.deadline = deadline;
	}
	
	public List<Task> getTaskList()
	{
		return taskList;
	}
	public void setTaskList(List<Task> tasklist)
	{
		this.taskList = tasklist;
	}
	
	public List<Edge> getEdgeList()
	{
		return edgeList;
	}
	public void setEdgeList(List<Edge> edgelist)
	{
		this.edgeList = edgelist;
	}
	
	public boolean getStartedFlag()
	{
		return startedFlag;
	}
	public void setStartedFlag(boolean startedFlag)
	{
		this.startedFlag = startedFlag;
	}
	
	public double getFinishTime()
	{
		return finishTime;
	}
	public void setFinishTime(double finishTime)
	{
		this.finishTime = finishTime;
	}
		
	public boolean getSuccessFlag()
	{
		return successFlag;
	}
	public void setSuccessFlag(boolean successFlag)
	{
		this.successFlag = successFlag;
	}
	
}