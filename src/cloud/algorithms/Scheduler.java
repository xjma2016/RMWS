package cloud.algorithms;

import java.util.List;

import cloud.components.*;

public interface Scheduler {
	
	public void schedule(List<Workflow> workflowList); //Run the schedule for each workflow list
	public String getName(); //Get the algorithm's name
}
