package cloud.components;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Edge implements Serializable
{	
	private final Task parentTask; //The parent tasks, get the value from part3 in the DAX file
	private final Task childTask; //The child tasks, get the value from part3 in the DAX file
	private long transDataSize; //The transfer data between the  tasks linked by edge, the size of uses file is got from part2 in the DAX file
	
	private long transDataSizeWithDeviation; //The deviate transfer data, only can be get at execute the childTask on a VM even if it is calculated by normal distribution at the workflow generate phase
	
	public Edge(Task parentTask, Task childTask, long transDataSize) {
		this.parentTask = parentTask;
		this.childTask = childTask;
		this.transDataSize = transDataSize;
		
		this.transDataSizeWithDeviation = -1;
	}

	//-------------------------------------getters&setters--------------------------------
	public Task getParentTask() {
		return parentTask;
	}
	
	public Task getChildTask() {
		return childTask;
	}
	
	public long getTransDataSize() {
		return transDataSize;
	}
	public void setTransDataSize(long transDataSize) {
		this.transDataSize = transDataSize;
	}
	
	public long getTransDataSizeWithDeviation() {
		return transDataSizeWithDeviation;
	}
	public void setTransDataSizeWithDeviation(long transDataSizeWithDeviation) {
		this.transDataSizeWithDeviation = transDataSizeWithDeviation;
	}

}
