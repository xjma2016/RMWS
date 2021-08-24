package cloud.algorithms;

import java.util.List;

import cloud.components.*;

public interface Scheduler {
	
	/**Run the schedule on the workflow list */
	public void schedule(List<Workflow> workflowList); 
	
	/**Get the algorithm's name */
	public String getName(); 
	
}
