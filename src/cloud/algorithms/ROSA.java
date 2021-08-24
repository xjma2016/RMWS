package cloud.algorithms;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cloud.components.*;
import cloud.configurations.Parameters;

/**Programming according to article 
Uncertainty-Aware Online Scheduling for Real-Time Workflows in Cloud Service Environment
DOI: 10.1109/TSC.2018.2866421*/

public class ROSA implements Scheduler {
	private List<Workflow> experimentWorkflow; //The workflow list;
	private List<Task> taskPool; //The unprepared tasks list
	private List<Task> readyTaskList; //The ready task list 
	
	private List<VM> activeVMList; //The active VM list
	private List<VM> allLeaseVMList; //All used VM list store the VM after leased
	private double currentTime; //The current time in the scheduling process
	
	List<Task> scheduledTaskList ;
	Check check = new Check();
	
	SimpleDateFormat cTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //Current Time	
	public void schedule(List<Workflow> workflowList) {
		//Initialize
		init(workflowList);
		
		//Record the runtime of scheduling algorithm
		long totalScheduleTime = 0;
		
		//Schedule each workflow in workflowList
		for(int i=0;i<experimentWorkflow.size();i++) {
			//Set the current time as the i-th workflow's arrival time 
			currentTime = experimentWorkflow.get(i).getArrivalTime();
			
			//Get the workflows that has the same arrive time
			List<Workflow> workflowWithSameArrivetime = new ArrayList<Workflow>();
			workflowWithSameArrivetime.add(experimentWorkflow.get(i)); //Add the i-th workflow
			for(int j=i+1;j<experimentWorkflow.size();j++) { //Because workflows are ascending by arrival time, just compare the workflows after i
				if(experimentWorkflow.get(j).getArrivalTime() == experimentWorkflow.get(i).getArrivalTime()) {
					workflowWithSameArrivetime.add(experimentWorkflow.get(j));
					i++; //Update i to the next workflow that has different arriveTime
				}
				else {
					break; //Break 'for' after get all workflows that have the same arriveTime
				}
			}
			
			long startTime01 = System.currentTimeMillis();
		    
			//Calculate the latest start/finish time, subdeadline of tasks in the workflow list
			for(int w=0;w<workflowWithSameArrivetime.size();w++) {
				Workflow workflow = workflowWithSameArrivetime.get(w); //Get the w-th workflow
				//List<Task> taskList = workflow.getTaskList();//Get the task list in w-th workflow
				//calculateTaskParameter(taskList,workflow.getDeadline());	
				CaculateLeastFinishTimeForWTask(workflow);
				
			} 
			
			//Get the ready tasks from workflowsWithSameArrivetime
			readyTaskList = getReadyTask(workflowWithSameArrivetime);
			Collections.sort(readyTaskList, new compareTaskByPredictLatestStartTime()); //Sort the ready tasks by the predict latest start time
			
			//Schedule the ready tasks to active VMs or New VMs
			scheduleTaskToVM(readyTaskList, activeVMList);
			
			//Add all unready tasks into the task pool
			for(Workflow w : workflowWithSameArrivetime) {
				for(Task t : w.getTaskList()) {
					if(!t.getIsAssigned()) {
						taskPool.add(t);
					}
				}
			}
			
			long endTime01 = System.currentTimeMillis();
			totalScheduleTime = totalScheduleTime + (endTime01 - startTime01);		
			
			//Get the next arrival time,
			double nextArrivalTime = Integer.MAX_VALUE;
			if(i != experimentWorkflow.size()-1) {
				nextArrivalTime = experimentWorkflow.get(i).getArrivalTime();
			}
			
			//Initialize the next finish VM and its finish time, the next released VM and its release time
			VM nextFinishVM = null; //The VM on which the execute task has the min actual finish time
			double nextFinishTime = Integer.MAX_VALUE; //The min actual finish time
			VM nextReleaseVM = null; //The VM that will be released firstly
			double nextReleaseTime = Integer.MAX_VALUE; //The firstly released time
			
			//Get the above four parameters in the active VM list
			for(VM vm : activeVMList) { //VM's status can be classified as: wait task/execute task/idle
				double actualFinishTime = vm.getExecuteTask().getActualFinishTime(); //The execute task's actual finish time
				
				if(actualFinishTime != -1) { //The VM has an execute task
					if(nextFinishTime > actualFinishTime) {
						nextFinishTime = actualFinishTime;
						nextFinishVM = vm;
					}
				}
				else { //The VM is idle
					//Set the release time when the whole lease time is integer multiple of unitTime
					double tempNextReleaseTime = Integer.MAX_VALUE;
					if((currentTime - vm.getLeaseStartTime())%Parameters.unitTime == 0) { 
						tempNextReleaseTime = currentTime;
					}
					else { //Set the next integer multiple of unit time as the next release time
						int unitTimes = (int)Math.ceil((currentTime - vm.getLeaseStartTime())/Parameters.unitTime);
						tempNextReleaseTime = vm.getLeaseStartTime() + unitTimes*Parameters.unitTime;
					}
					if(nextReleaseTime > tempNextReleaseTime) {
						nextReleaseTime = tempNextReleaseTime;
						nextReleaseVM = vm;
					}
				}
			} //End for, get the four parameters
			
			//Update the schedule status before next workflow arrives
			while(nextArrivalTime >= nextFinishTime || nextArrivalTime > nextReleaseTime) {
				//update the VM and its execute task, which have the min finish time
				if(nextFinishTime <= nextReleaseTime) {
					//Set the execute task as finished
					Task nextFinishTask = nextFinishVM.getExecuteTask();
					nextFinishTask.setIsFinished(true);
					nextFinishVM.setExecuteTask(new Task("init", -1, -1));
					currentTime = nextFinishTask.getActualFinishTime(); //Set the current time as the next finish time
					
					if(nextFinishVM.getWaitTaskList().size() > 0) { //The next finish VM has wait tasks, then execute it
						Task nextExecuteTask = nextFinishVM.getWaitTaskList().get(0); //Execute the first wait task
						double nextStartTime = calculateActualStartTime(nextExecuteTask, nextFinishVM); //Get the actual start time of next execute task
						double nextExecuteTime = nextExecuteTask.getBaseExecuteTimeWithDeviation()*nextFinishVM.getVMExecuteTimeFactor();
						nextExecuteTask.setActualStartTime(nextStartTime);
						nextExecuteTask.setActualExecuteTime(nextExecuteTime);
						nextExecuteTask.setActualFinishTime(nextStartTime+nextExecuteTime);
						nextFinishVM.setExecuteTask(nextExecuteTask);
						nextFinishVM.getWaitTaskList().remove(nextExecuteTask);
						
					}
					else { //The next finish VM has no wait task
						nextFinishVM.setExecuteTask(new Task("init", -1, -1));
					}
					
					long startTime02 = System.currentTimeMillis();
					
					//Get the ready children of the next finish task
					List<Task> readySucessorList = getReadySucessor(nextFinishTask);
					
					//Schedule the ready tasks to active VMs or New VMs, and move them from task pool
					scheduleTaskToVM(readySucessorList, activeVMList);
					
					taskPool.removeAll(readySucessorList);
					
					long endTime02 = System.currentTimeMillis();
					totalScheduleTime = totalScheduleTime + (endTime02 - startTime02);
				}
				
				//Release VM
				if(nextReleaseTime < nextFinishTime) {
					currentTime = nextReleaseTime; //Set the current time as the next release time
					double vmLeaseTime = nextReleaseTime - nextReleaseVM.getLeaseStartTime();
					int unitTimes = (int)Math.ceil(vmLeaseTime/Parameters.unitTime);
					double cost = unitTimes*nextReleaseVM.getVMPrice(); 
					
					nextReleaseVM.setLeaseFinishTime(nextReleaseTime);
					nextReleaseVM.setTotalLeaseCost(cost);
					nextReleaseVM.setVMStatus(false);
					
					activeVMList.remove(nextReleaseVM);
					allLeaseVMList.add(nextReleaseVM);
					//System.out.println("An idle VM#: "+nextReleaseVM.getVMID()+"is removed from active VM List");
				}
				
				//Update the next finish VM and release VM, which has the min time
				nextFinishVM = null; //The VM on which the executing task has the min actual finish time
				nextFinishTime = Integer.MAX_VALUE; //The min actual finish time
				nextReleaseVM = null; //The VM that will be released firstly
				nextReleaseTime = Integer.MAX_VALUE; //The firstly released time
				
				//Re-update the above four parameters in the active VM list
				for(VM vm : activeVMList) { //VM's status can be classified as: wait task/execute task/idle
					double actualFinishTime = vm.getExecuteTask().getActualFinishTime(); //The execute task's actual finish time
					
					if(actualFinishTime != -1) { //The vm has an execute task, no need to consider the wait task which will be execute once the execute task finished
						if(nextFinishTime > actualFinishTime) {
							nextFinishTime = actualFinishTime;
							nextFinishVM = vm;
						}
					}
					else { //The VM is idle
						//Set the release time when the whole lease time is integer multiple of unitTime
						double tempNextReleaseTime = Integer.MAX_VALUE;
						if((currentTime - vm.getLeaseStartTime())%Parameters.unitTime == 0) { 
							tempNextReleaseTime = currentTime;
						}
						else { //Set the next integer multiple of unit time as the next release time
							int unitTimes = (int)Math.ceil((currentTime - vm.getLeaseStartTime())/Parameters.unitTime);
							tempNextReleaseTime = vm.getLeaseStartTime() + unitTimes*Parameters.unitTime;
						}
						if(nextReleaseTime > tempNextReleaseTime) {
							nextReleaseTime = tempNextReleaseTime;
							nextReleaseVM = vm;
						}
					}
				} //End for, Re-update the four parameters
				
				//End while if can not find a next finish/release VM before nextArrivalTime, 
				if(nextArrivalTime==Integer.MAX_VALUE && nextFinishTime==Integer.MAX_VALUE && nextReleaseTime==Integer.MAX_VALUE) {
					break;
				}
				
			} //End while, update the schedule status before next workflow arrives
			
		} //End for, schedule each workflow in workflowList
		
		//Check the tasks and VMs status
		check.checkUnfinishTaskAndVM(experimentWorkflow,allLeaseVMList);
		
		//Calculate the experimental results
		ExperimentResult.calculateExperimentResult("ROSA", allLeaseVMList, experimentWorkflow, totalScheduleTime);
		
		//Clear the lists
		experimentWorkflow.clear();
		taskPool.clear();
	    readyTaskList.clear();
		activeVMList.clear();
		allLeaseVMList.clear();
		VM.resetInnerId(); //Reset the VM'id as 0 when finish the experiment of the algorithm
		
	}
	
	/**Calculate the latest start/finish time, subdeadline of tasks in workflow list*/
	
    /**Init the lists*/
	public void init(List<Workflow> workflowList) {
		this.experimentWorkflow = workflowList;
		taskPool = new ArrayList<Task>(); 
		readyTaskList = new ArrayList<Task>(); 
		activeVMList = new ArrayList<VM>(); 
		allLeaseVMList = new ArrayList<VM>(); 
	    scheduledTaskList  =new ArrayList<Task>();
	}
	
	/**Calculate the least finish time for each workflow task, used from the ROSA code*/
	public void CaculateLeastFinishTimeForWTask(Workflow tempWorkflow)
	{
		
		//Test from the type of service instance with lower capacity
		for(int f=0; f<Parameters.speedFactor.length; f++)
		{
			double factor = Parameters.speedFactor[f];
			
			//Calculate the least start and finish time of each task
			CalculateTaskLeastStartAndFinishTime(tempWorkflow, factor);
			
			double leastFinishTime = 0; //The least finish time of the workflow
			for(Task cTask: tempWorkflow.getTaskList())
			{
				if(cTask.getPredictLatestFinishTime() > leastFinishTime)
				{
					leastFinishTime = cTask.getPredictLatestFinishTime();
				}
			}
			
			//If the least finish time of the workflow is less than its deadline
			if(leastFinishTime <= tempWorkflow.getDeadline())
			{
				//The laxity time between the deadline and the least finish time
				double laxity = tempWorkflow.getDeadline() - leastFinishTime;
				for(Task task: tempWorkflow.getTaskList())
				{
					double tempExecutionTime = task.getPredictLatestFinishTime() - task.getPredictLatestStartTime();
					tempExecutionTime = tempExecutionTime + laxity*((factor*task.getBaseExecuteTime()*Parameters.standardDeviation)/(leastFinishTime-currentTime));
					task.setAllowExecutionTime(tempExecutionTime);
					
				}
				
				
				List<Task> calculatedTaskList = new ArrayList<Task>();
				while(true)
				{
					for(Task task: tempWorkflow.getTaskList())// Check all the tasks
					{
						if(task.getSubDeadline() > -1) 
						{
							continue;
						}
						
						double executionTime = task.getAllowExecutionTime(); 	
						if(task.getInEdges().size() == 0)
						{// The task without predecessor
							task.setSubDeadline(currentTime+executionTime);
							calculatedTaskList.add(task);
						}
						else 
						{// The task has predecessors
							double maxDataSize = 0; // The size of data that needs to be transfered
							for(Edge e : task.getInEdges())
							{
								if(e.getTransDataSize()*Parameters.standardDeviationData > maxDataSize)
								{
									maxDataSize = e.getTransDataSize()*Parameters.standardDeviationData;							
								}
							}
							//Data transfer time
							double commDelay = maxDataSize/Parameters.bandwidth;
							
							double maxStartTime = -1; //Least start time
							boolean unCalculatedParent = false; // Whether all the predecessors have been calculated
							for(Edge e : task.getInEdges())
							{//If all the predecessors have been calculated, calculate the start time
								if(e.getParentTask().getSubDeadline() > -1)
								{
									double startTime = e.getParentTask().getSubDeadline() + commDelay;
									if(startTime > maxStartTime)
									{
										maxStartTime = startTime;
									}
								}
								else
								{//There exist predecessors that are not calculated
									unCalculatedParent = true;
									break;
								}						
							}
							
							if(unCalculatedParent == false)
							{								
								task.setSubDeadline(maxStartTime + executionTime);
								calculatedTaskList.add(task);
							}																															
						}//end else
					}//end for(Task task: cWorkflow.getTaskList()) 
					
					if(calculatedTaskList.size() == tempWorkflow.getTaskList().size())
					{
						break;
					}
				}//end while
				
				break;
			}
			else // If the least finish time of the workflow is larger than its deadline
			{
				// Try next type of service instance with higher capacity
				if(f == Parameters.speedFactor.length-1)
				{// If this type of service instance has the highest capacity
					for(Task task: tempWorkflow.getTaskList())
					{
						task.setSubDeadline(task.getPredictFinishTime());
					}
				}
				else
				{//Clear the least start/finish time
					for(Task task: tempWorkflow.getTaskList())//Check all the tasks
					{
						task.setPredictLatestStartTime(-1);
						task.setPredictLatestFinishTime(-1);
					}
				}	
			}	
		}
		
		for(Task task: tempWorkflow.getTaskList()) // Set the least finish time for each task
		{
			task.setPredictLatestFinishTime(task.getSubDeadline());
		}		
	} 
	
	
	/**Calculate the least finish and start time for each workflow task*/
	public void CalculateTaskLeastStartAndFinishTime(Workflow cWorkflow, double factor)
	{
		List<Task> calculatedTaskList = new ArrayList<Task>();
		while(true)
		{
			for(Task task: cWorkflow.getTaskList())//Check each task
			{
				if(task.getPredictLatestFinishTime()> -1) // If a task has been calculated, then ignores it
				{
					continue;
				}
				
				double executionTime = factor*task.getBaseExecuteTime()*Parameters.standardDeviation; //The \alpha-quantile of the execution time
				if(task.getInEdges().size() == 0)
				{// For a task without predecessor
					task.setPredictLatestStartTime(currentTime); //Its start time is set as current time
					task.setPredictLatestFinishTime(currentTime+executionTime);
					calculatedTaskList.add(task);
				}
				else //For a task having predecessors
				{	
					//Determine the size of data that needs to be transfered
					double maxDataSize = 0;
					for(Edge e : task.getInEdges())
					{
						if(e.getTransDataSize()*Parameters.standardDeviationData > maxDataSize)
						{
							maxDataSize = e.getTransDataSize()*Parameters.standardDeviationData;							
						}
					}
					//The data transfer time
					double commDelay = maxDataSize/Parameters.bandwidth;
					
					double maxStartTime = -1; //The least start time
					boolean unCalculatedParent = false; //Whether exists un-calculated predecessors
					for(Edge e : task.getInEdges())
					{//Check whether all the predecessors of this task have been calculated
						if(e.getParentTask().getPredictLatestFinishTime()>-1)
						{
							double startTime = e.getParentTask().getPredictLatestFinishTime() + commDelay;
							if(startTime > maxStartTime)
							{
								maxStartTime = startTime;
							}
						}
						else
						{//If there exist un-calculated predecessors
							unCalculatedParent = true;
							break;
						}						
					}
					
					if(unCalculatedParent == false)
					{//If all the predecessors of this task have been calculated, then calculate the start/finish time for this task
						task.setPredictLatestStartTime(maxStartTime);
						task.setPredictLatestFinishTime(maxStartTime+executionTime);
						calculatedTaskList.add(task);
					}																															
				}//end else
			}//end for(Task task: cWorkflow.getTaskList()) 
			
			if(calculatedTaskList.size() == cWorkflow.getTaskList().size())
			{// If all the tasks in this workflow have been calculated, then stop
				break;
			}
		}//end while
		
	}	
	
	/**Get the ready tasks from a workflow list*/
	public List<Task> getReadyTask(List<Workflow> workflowList) {
		List<Task> readyTaskList = new ArrayList<Task>();
		for(Workflow w : workflowList) {
			for(Task t : w.getTaskList()) { 
				if(t.getInEdges().size() == 0) {
					readyTaskList.add(t);
				}
			}
		}
		return readyTaskList;
	}
	
	/**Compare two task by their predicted latest start time*/
	public class compareTaskByPredictLatestStartTime implements Comparator<Task>{
		public int compare(Task t1, Task t2) {
			if(t1.getPredictLatestStartTime() > t2.getPredictLatestStartTime())
			{
				return 1;
			}
			else if(t1.getPredictLatestStartTime() < t2.getPredictLatestStartTime())
			{
				return -1;
			}
			else
			{
				return 0;
			}
		}
	}
	
	/**Schedule tasks in a list to active VMs or new VMs, which has the min lease cost under the predicte latest finish time*/
	public void scheduleTaskToVM(List<Task> taskList, List<VM> activeVMList) {
		for(int i=0;i<taskList.size();i++) {
			Task scheduleTask = taskList.get(i); //Selected the task
			double minCost = Double.MAX_VALUE; //Store the min cost while select a VM
			VM selectVM = null; //The selected VM
			int selectVMType = -1; //The selected VM type 
			
			//Calculate the task's predicated finish time and cost on active VM
			for(VM vm : activeVMList) {
				double activeVMStartTime = calculatePredicateStartTime(scheduleTask,vm); //Get the predicate start time on VM
					
				//Get the available time of VM
				double tempAvialableTime = currentTime;
				if(!vm.getExecuteTask().getTaskID().equals("init")) { //The VM has an execute task
					if(vm.getWaitTaskList().size() == 0) {
						tempAvialableTime = vm.getExecuteTask().getActualFinishTime();
					}
					else {
						int waitTaskNum = vm.getWaitTaskList().size();
						tempAvialableTime = vm.getWaitTaskList().get(waitTaskNum-1).getPredictFinishTime();
					}
				}
				
				//Calculate the predicted lease cost if assign scheduleTask on VM 
				double executeTimeWithDeviation =  scheduleTask.getBaseExecuteTime() +  scheduleTask.getBaseExecuteTime()*Parameters.standardDeviation;
				double perExecuteTime = executeTimeWithDeviation*vm.getVMExecuteTimeFactor(); //The predicted execute time of scheduleTask on VM
				double preFinishTime = activeVMStartTime + perExecuteTime;
				
				double preLeaseTime = preFinishTime - tempAvialableTime; //The scheduleTask's predicted lease time on VM
				double preCost = vm.getVMPrice()*preLeaseTime;
				
				//Select the VM when has the less cost than a new VM and meet the schedlueTask's subDeadline
				if(preFinishTime <= scheduleTask.getPredictLatestFinishTime()) {
					if(minCost >= preCost) {
						minCost = preCost;
						selectVM = vm;
					}
				}
			} //End for, find each active VM
			
			//Calculate the task's predicated finish time and cost on a new VM
			for(int j=0;j<Parameters.vmTypeNumber-1;j++) {
				double predicteStartTime = calculatePredicateStartTime(scheduleTask, null);
				double executeTimeWithDeviation =  scheduleTask.getBaseExecuteTime() + scheduleTask.getBaseExecuteTime()*Parameters.standardDeviation;
				double perNewExecuteTime = executeTimeWithDeviation*Parameters.speedFactor[j]; 
				double perNewkFinishTime = Parameters.launchTime + predicteStartTime + perNewExecuteTime;
				double preNewCost = Parameters.speedFactor[j]*perNewExecuteTime;
				if(perNewkFinishTime<=scheduleTask.getPredictLatestFinishTime()) {
					if(minCost>preNewCost){ //If find a new VM that has less cost under predicate finish time
						minCost = preNewCost;
						selectVMType = j;
						//selectVM = null; //Maybe a mistake in Alg.3 line 12 of ROSA, if set selectVM as null, the tasks will always assign on a new VM 
					}
				}
			}
			
			//Set the selectVMType as the fastest type when can find a suitable VM from active VMs or new VMs, not consider in ROSA
			if(selectVMType == -1 && selectVM == null) {
				selectVMType = Parameters.vmTypeNumber-1;
			}
			
			//Lease a new VM with selectVMType when the select active VM is null
			if(selectVM == null) {
				selectVM = new VM(selectVMType, currentTime);
				activeVMList.add(selectVM);
				//System.out.println("Assigned on a new VM" );
			}
			else {
				//System.out.println("Assigned on a active VM" );
			}
			
			//Assign task on the selected VM and update the parameters
			if(selectVM.getExecuteTask().getTaskID().equals("init")) { //The VM has no execute task
				double actualStartTime = calculateActualStartTime(scheduleTask, selectVM);
				double actualExecuteTime = scheduleTask.getBaseExecuteTimeWithDeviation()*selectVM.getVMExecuteTimeFactor();
				double actualFinishTime = actualStartTime + actualExecuteTime;
				scheduleTask.setActualStartTime(actualStartTime);
				scheduleTask.setActualExecuteTime(actualExecuteTime);
				scheduleTask.setActualFinishTime(actualFinishTime);
				
				selectVM.setExecuteTask(scheduleTask);
			}
			else {
				double predicteStartTime = calculatePredicateStartTime(scheduleTask, selectVM);
				double executeTimeWithDeviation =  scheduleTask.getBaseExecuteTime() +  scheduleTask.getBaseExecuteTime()*Parameters.standardDeviation;
				double predicteExecuteTime = executeTimeWithDeviation*selectVM.getVMExecuteTimeFactor(); 
				double predicteFinishTime = predicteStartTime + predicteExecuteTime;
				scheduleTask.setPredictStartTime(predicteStartTime);
				scheduleTask.setPredictFinishTime(predicteFinishTime);
				
				selectVM.getWaitTaskList().add(scheduleTask);
			}
			selectVM.getTaskList().add(scheduleTask);
			scheduleTask.setAssignedVM(selectVM);
			scheduleTask.setIsAssigned(true);
			
			//System.out.println("Task#: "+scheduleTask.getTaskID()+" is assigned on VM#: " +selectVM.getVMID());
		}
		scheduledTaskList.addAll(taskList);
	}
	
	/**Calculate the predicate start time for a task on a VM*/
	public double calculatePredicateStartTime(Task task, VM vm) {
		double predictStartTime = currentTime;
		
		//Calculate the predicated start time by scheduleTask' parent
		for(Edge inEdge : task.getInEdges()) {
			Task parent = inEdge.getParentTask();
			if(!parent.getIsFinished()) { //Check the parent is finished or not, because a task can be scheduled only all its parent had finished
				throw new IllegalArgumentException("A parent of the schedule Task is not finished!");
			}
			VM parentTaskVM = parent.getAssignedVM();
			double maxParentTransTime = Double.MIN_VALUE; //Store the max transfer data time between scheduleTask and its parent
			
			if(parentTaskVM.equals(vm)) { //ScheduleTask and its parent are on the same VM, then ignore the transfer data time
				maxParentTransTime = parent.getActualFinishTime();
			}
			else { //If not the same VM, need consider the transfer data time
				maxParentTransTime = parent.getActualFinishTime() + ((1+Parameters.standardDeviationData)*inEdge.getTransDataSize())/Parameters.bandwidth;
			}
			
			if(predictStartTime < maxParentTransTime ) { //Set the max transfer data time of scheduleTask's parent as the min start time
				predictStartTime = maxParentTransTime; 
			}
		}
		
		//Calculate the available time of VM
		if(vm !=null) {
			if(!vm.getExecuteTask().getTaskID().equals("init")) { //The VM has an execute task
				if(vm.getWaitTaskList().size() == 0) { //The VM has no wait task
					if(predictStartTime < vm.getExecuteTask().getActualFinishTime()) { //An execute task must has actual start/execute/finish time
						predictStartTime = vm.getExecuteTask().getActualFinishTime();
					}
				}
				else {
					int waitTaskNum = vm.getWaitTaskList().size();
					predictStartTime =  vm.getWaitTaskList().get(waitTaskNum-1).getPredictFinishTime();
				}
			}
			else { //The VM is idle
				if(predictStartTime < vm.getLeaseStartTime()) {
					predictStartTime = vm.getLeaseStartTime();
				}
			}
		}
				
		return predictStartTime;
	}
	
	/**Calculate the actual start time for a task on a VM*/
	public double calculateActualStartTime(Task task, VM vm) {
		double actualStartTime = currentTime;
		
		//Calculate the actual start time by scheduleTask' parent
		for(Edge inEdge : task.getInEdges()) {
			Task parent = inEdge.getParentTask();
			if(!parent.getIsFinished()) { //Check the parent is finished or not, because a task can be scheduled only all its parent had finished
				throw new IllegalArgumentException("A parent of the schedule Task is not finished!");
			}
			VM parentTaskVM = parent.getAssignedVM();
			double maxParentTransTime = Double.MIN_VALUE; //Store the max transfer data time between scheduleTask and its parent
			
			if(parentTaskVM.equals(vm)) { //ScheduleTask and its parent are on the same VM, then ignore the transfer data time
				maxParentTransTime = parent.getActualFinishTime();
			}
			else { //If not the same VM, need consider the transfer data time
				maxParentTransTime = parent.getActualFinishTime() + inEdge.getTransDataSizeWithDeviation()/Parameters.bandwidth;
			}
			
			if(actualStartTime < maxParentTransTime ) { //Set the max transfer data time of scheduleTask's parent as the min start time
				actualStartTime = maxParentTransTime; 
			}
		}
		
		//Calculate the available time of VM
		if(vm !=null) {
			if(!vm.getExecuteTask().getTaskID().equals("init")) { 
				throw new IllegalArgumentException("A assigned VM has a execute task!");
			}
			else { //The VM is idle
				if(actualStartTime < vm.getLeaseStartTime()) {
					actualStartTime = vm.getLeaseStartTime();
				}
			}
		}
				
		return actualStartTime;
	}
	 
	/**Get the ready children of a task in task pool*/
	public List<Task> getReadySucessor(Task task) {
		List<Task> readySucessorList = new ArrayList<Task>();
		for(Edge outEdge : task.getOutEdges()) { //Check each child of task
			Task child = outEdge.getChildTask();
			boolean ready = true;
			for(Edge inEdge : child.getInEdges()) { //If child's all parent were finished, then add the child into readySucessorList
				Task parent = inEdge.getParentTask();
				if(!parent.getIsFinished()) { //If child exist a unfinished parent, then break and find next child
					ready = false;
					break;
				}
			}
			if(ready) { //Can not find a unfinished parent of the child
				readySucessorList.add(child);
			}
		}
		return readySucessorList;
	}
	
	/**Get the algorithm's name*/
	public String getName(){
		return "ROSA";
	}

	
	
}
