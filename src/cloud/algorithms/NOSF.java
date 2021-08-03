package cloud.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import cloud.components.Check;
import cloud.components.Edge;
import cloud.components.ExperimentResult;
import cloud.components.Task;
import cloud.components.VM;
import cloud.components.Workflow;
import cloud.components.WorkflowGenerator;
import cloud.configurations.Parameters;

/**Programming according to article 
Online Multi-Workflow Scheduling under Uncertain Task Execution Time in IaaS Clouds
DOI: 10.1109/TCC.2019.2906300*/

public class NOSF implements Scheduler {


	private List<Workflow> experimentWorkflow; //The workflow list;
	private List<Task> taskPool; //The unprepared tasks list
	private List<Task> readyTaskList; //The ready task list 
	
	private List<VM> activeVMList; //The active VM list
	private List<VM> allLeaseVMList; //All used VM list store the VM after leased
	private double currentTime; //The current time in the scheduling process
	
	List<Task> scheduledTaskList ;
		
	public void schedule(List<Workflow> workflowList) {
		//Initialize
		init(workflowList);
		//Check.printText("init at: ");	  		
		
		//Record the runtime of scheduling algorithm
		long totalScheduleTime = 0;
		long startTime01 = System.currentTimeMillis();
			    
		//Calculate the earliest start/completion time, latest completion time, subDeadline of tasks in the workflow list
		calculateTaskParameter(experimentWorkflow);	
		//Check.printText("calculateTaskParameter at: ");	  		
		
	/*	for(int m=0;m<experimentWorkflow.size();m++) {
			Workflow wk = experimentWorkflow.get(m);
			List<Task> taskList = wk.getTaskList();
			if(taskList.size()<55 && wk.getWorkflowName().equals("LIGO-50")) {
				System.out.print("NOSF:  ");
				for(int n=0;n<taskList.size();n++) {
					System.out.print( taskList.get(n).getTaskID() + "--" + taskList.get(n).getSubDeadline() + "/");
				}
			}
		}*/
		
		long endTime01 = System.currentTimeMillis();
		totalScheduleTime = totalScheduleTime + (endTime01 - startTime01);
		
		//Schedule each workflow in workflowList
		for(int i=0;i<experimentWorkflow.size();i++) {
			//Set the current time as the ith workflow's arrival time 
			currentTime = experimentWorkflow.get(i).getArrivalTime();
			
			//Get the workflows that has the same arrivetime
			List<Workflow> workflowWithSameArrivetime = new ArrayList<Workflow>();
			workflowWithSameArrivetime.add(experimentWorkflow.get(i)); //Add the ith workflow
			for(int j=i+1;j<experimentWorkflow.size();j++) { //Because workflows are ascending by arrivaltime, just compare the workflows after i
				if(experimentWorkflow.get(j).getArrivalTime() == experimentWorkflow.get(i).getArrivalTime()) {
					workflowWithSameArrivetime.add(experimentWorkflow.get(j));
					i++; //Update i to the next workflow that has different arriveTime
				}
				else {
					break; //Break 'for' after get all workflows that have the same arriveTime
				}
			}
			
			//Get the ready tasks from workflowsWithSameArrivetime
			readyTaskList = getReadyTask(workflowWithSameArrivetime);
			Collections.sort(readyTaskList, new compareTaskByEarliestStartTime()); //Sort the ready tasks by the earliest start time
			//Check.printText("readyTaskList at: ");	  
			
			long startTime02 = System.currentTimeMillis();
			
			//Schedule the ready tasks to active VMs or New VMs
			scheduleTaskToVM(readyTaskList, activeVMList);
			//Check.printText("scheduleTaskToVM at: ");	
			
			long endTime02 = System.currentTimeMillis();
			totalScheduleTime = totalScheduleTime + (endTime02 - startTime02);		
			
			//Add all unready tasks into the task pool
			for(Workflow w : workflowWithSameArrivetime) {
				for(Task t : w.getTaskList()) {
					if(!t.getIsAssigned()) {
						taskPool.add(t);
					}
				}
			}
			
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
				
				if(actualFinishTime != -1) { //The vm has an execute task, no need to consider the wait task which will be execute once the execute task finished
					if(nextFinishTime > actualFinishTime) {
						nextFinishTime = actualFinishTime;
						nextFinishVM = vm;
					}
				}
				else { //The vm is idle
					double tempNextReleaseTime = Integer.MAX_VALUE;
					if((currentTime - vm.getLeaseStartTime())%Parameters.unitTime == 0) { //The vm will be released when the whole lease time is integer multiple of unitTime
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
					Task nextFinishTask = nextFinishVM.getExecuteTask();
					nextFinishTask.setIsFinished(true);
					currentTime = nextFinishTask.getActualFinishTime(); //Set the current time as the next finish time
					
					if(!nextFinishVM.getWaitTask().getTaskID().equals("init")) { //The next finish VM has a wait task, then execute it
						Task nextExecuteTask = nextFinishVM.getWaitTask();
						nextFinishVM.setWaitTask(new Task("init", -1, -1)); //Init a new wait task after the wait task is execute
						double nextStartTime = calculateActualStartTime(nextExecuteTask, nextFinishVM); //Get the actual start time of next execute task
						double nextExecuteTime = nextExecuteTask.getBaseExecuteTimeWithDeviation()*nextFinishVM.getVMExecuteTimeFactor();
						nextExecuteTask.setActualStartTime(nextStartTime);
						nextExecuteTask.setActualExecuteTime(nextExecuteTime);
						nextExecuteTask.setActualFinishTime(nextStartTime+nextExecuteTime);
						nextFinishVM.setExecuteTask(nextExecuteTask);
					}
					else { //The next finish VM has no wait task
						nextFinishVM.setExecuteTask(new Task("init", -1, -1));
					}
					
					//Get the ready children of the next finish task
					List<Task> readySucessorList = getReadySucessor(nextFinishTask);
					calculateSuccessorParameter(readySucessorList);
					
					long startTime03 = System.currentTimeMillis();
					
					//Schedule the ready tasks to active VMs or New VMs, and move them from task pool
					scheduleTaskToVM(readySucessorList, activeVMList);
					//Check.printText("scheduleSucessorToVM at: ");	
					
					long endTime03 = System.currentTimeMillis();
					totalScheduleTime = totalScheduleTime + (endTime03 - startTime03);
					
					taskPool.removeAll(readySucessorList);
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
				}
				
				//Update the next finish VM and release VM, which has the min time
				nextFinishVM = null; //The VM on which the executing task has the min actual finish time
				nextFinishTime = Integer.MAX_VALUE; //The min actual finish time
				nextReleaseVM = null; //The VM that will be released firstly
				nextReleaseTime = Integer.MAX_VALUE; //The firstly released time
				
				//Get the above four parameters in the active VM list
				for(VM vm : activeVMList) { //VM's status can be classified as: wait task/execute task/idle
					double actualFinishTime = vm.getExecuteTask().getActualFinishTime(); //The execute task's actual finish time
					
					if(actualFinishTime != -1) { //The vm has an execute task, no need to consider the wait task which will be execute once the execute task finished
						if(nextFinishTime > actualFinishTime) {
							nextFinishTime = actualFinishTime;
							nextFinishVM = vm;
						}
					}
					else { //The vm is idle
						double tempNextReleaseTime = Integer.MAX_VALUE;
						if((currentTime - vm.getLeaseStartTime())%Parameters.unitTime == 0) { //The vm will be released when the whole lease time is integer multiple of unitTime
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
				
				//End while if can not find a next finish/release VM before nextArrivalTime, 
				if(nextArrivalTime==Integer.MAX_VALUE && nextFinishTime==Integer.MAX_VALUE && nextReleaseTime==Integer.MAX_VALUE) {
					break;
				}
				
			} //End while, update the schedule status before next workflow arrives
			
		} //End for, schedule each workflow in workflowList
		
		//Check the tasks and VMs status
		Check.checkUnfinishTaskAndVM(experimentWorkflow,allLeaseVMList);
		
		//Calculate the experimental results
		ExperimentResult.calculateExperimentResult("NOSF", allLeaseVMList, experimentWorkflow, totalScheduleTime);
		
		//Clear the lists
		experimentWorkflow.clear();
		taskPool.clear();
	    readyTaskList.clear();
		activeVMList.clear();
		allLeaseVMList.clear();
		VM.resetInnerId(); //Reset the VM'id as 0 when finish the experiment of the algorithm
		
	}
	
    /**Init the global parameter, generate deviation values of task base execution time and base transfer data*/
	public void init(List<Workflow> workflowList) {
		this.experimentWorkflow = workflowList;
		taskPool = new ArrayList<Task>(); 
		readyTaskList = new ArrayList<Task>(); 
		activeVMList = new ArrayList<VM>(); 
		allLeaseVMList = new ArrayList<VM>(); 
	    scheduledTaskList  =new ArrayList<Task>();
	}
	
	/**Calculate the earliest start/completion time, latest completion time, subDeadline of tasks in the workflow list*/
	public void calculateTaskParameter(List<Workflow> workflowList) {
		int fastestVMType = Parameters.speedFactor.length - 1;
		for(int i=0;i<workflowList.size();i++) { 
			Workflow workflow = workflowList.get(i); //Get the ith workflow
			List<Task> taskList = workflow.getTaskList();//Get the task list in ith workflow
			
			int calculateNum = 0; //Store the number of calculate tasks
			double workflowArriveTime = workflow.getArrivalTime();
			double workflowDeadline = workflow.getDeadline();
			
			//Calculate task's earliest start/completion time from the entry task to the exit task
			while(calculateNum < taskList.size()) {
				for(int j=0;j<taskList.size();j++) {
					Task t = taskList.get(j);
					if(t.getInEdges().size() == 0 && t.getEarliestFinishTime() == -1) { //The uncalculate entry task
						t.setEarliestStartTime(workflowArriveTime);
						//double executeTimeWithDeviation = t.getBaseExecuteTime() + t.getBaseExecuteTime()*Parameters.standardDeviation;
						double perExecuteTime = t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType]; //The predicted execute time of scheduleTask on the fastest vm
						double earliestFinishTime = t.getEarliestStartTime() + perExecuteTime;
						t.setEarliestFinishTime(earliestFinishTime);
						calculateNum++;
					}
					else { //The task has parent
						double maxEarliestStartTime = Integer.MIN_VALUE; //Store the max start time of task t, calculate by the sum of earliest finish time and transfer data time between t and t's parent
						boolean isAllParentCalculated = true; //Check if all parent of task t have been calculated
						for(Edge e : t.getInEdges()) { //Find the calculated parent of t
							if(e.getParentTask().getEarliestStartTime() == -1) {
								isAllParentCalculated = false;
							}
						}
						
						if(isAllParentCalculated && t.getEarliestFinishTime() == -1) { //If all parent of task t have been calculated, calculated t' earliest start/finish time
							for(Edge e : t.getInEdges()) { //Caculate max start time from each parent
								double baseTransferDateTime = e.getTransDataSize()/Parameters.bandwidth;
								double tempEarlistStartTime = e.getParentTask().getEarliestFinishTime() + baseTransferDateTime; 
								if(maxEarliestStartTime < tempEarlistStartTime) {
									maxEarliestStartTime = tempEarlistStartTime;
								}
							}
							
							t.setEarliestStartTime(maxEarliestStartTime); //Set t's earliest start time
							//double executeTimeWithDeviation =  t.getBaseExecuteTime() + t.getBaseExecuteTime()*Parameters.standardDeviation;
							double perExecuteTime = t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType]; //The predicted execute time of scheduleTask on the fastest vm
							t.setEarliestFinishTime(maxEarliestStartTime + perExecuteTime);
							calculateNum++;
						} //End if
					} //End else
				} //End for, calculate task's earliest start/completion time from the entry task to the exit task 
			} //End while
			
			//Calculate latest completion time of tasks in the workflow list
			calculateNum = 0; //Store the number of calculate tasks
			while(calculateNum < taskList.size()) {
				for(int j=taskList.size()-1;j>=0;j--) { 
					Task t = taskList.get(j);
					if(t.getOutEdges().size() == 0 && t.getLatestFinishTime() == -1) { //The uncalculate exit task
						//Calculate task's latest completion time
						t.setLatestFinishTime(workflowDeadline); //Set the workflow's deadline as the exit task's completion finish time
						calculateNum++;
					}
					else { //The task has child
						double minLatestFinishTime = Integer.MAX_VALUE; //Store the min start time of task t
						boolean isAllChildrenCalculated = true; //Check if all children of task t have been calculated
						
						for(Edge e : t.getOutEdges()) { //Find uncalculated child of t
							if(e.getChildTask().getLatestFinishTime() == -1) {
								isAllChildrenCalculated = false;
							}
						}
						
						if(isAllChildrenCalculated && t.getLatestFinishTime() == -1) { //If all children of task t have been calculated, calculated t' latest completion time
							for(Edge e : t.getOutEdges()) { //Caculate min start time from each child
								double baseTransferDateTime = e.getTransDataSize()/Parameters.bandwidth;
								//double executeTimeWithDeviation =  e.getChildTask().getBaseExecuteTime() + e.getChildTask().getBaseExecuteTime()*Parameters.standardDeviation;
								double perExecuteTime = e.getChildTask().getBaseExecuteTime()*Parameters.speedFactor[fastestVMType]; //The predicted execute time of scheduleTask on the fastest vm
								double tempLatestFinishTime = e.getChildTask().getLatestFinishTime() - perExecuteTime - baseTransferDateTime; 
								if(minLatestFinishTime > tempLatestFinishTime) {
									minLatestFinishTime = tempLatestFinishTime;
								}
							}
							
							t.setLatestFinishTime(minLatestFinishTime); //Set the task's latest completion time as the min latest finishTime
							calculateNum++;
						} //End if
					} //End else
				} //End for,calculate latest completion time of tasks in the workflow list
			} //End while
			
			//Calculate the task's subdeadline, 1.get PCP from an exit task 2.get sub PCP from each task in the PCP 3.get PCP from other exit tasks
			ArrayList<Task> exitTaskList = new ArrayList<Task>(); //Store the exit task
			for(int j=taskList.size()-1;j>=0;j--){
				Task t = taskList.get(j);
				if(t.getOutEdges().size() == 0) { //Get each exit task
					exitTaskList.add(t);
				}
			}
			
			if(taskList.size()<55 && workflow.getWorkflowName().equals("LIGO-50")) {
				Check.checkTaskParameter(taskList);
			}
			
			for(int j=0;j<exitTaskList.size();j++){
				Task task = exitTaskList.get(j);
				ArrayList<Task> pcpList = getPCP(task); //Get the whole PCP of an exit task
				pcpList.add(0,task);//Add the exit task into first, otherwise its subDeadline and other PCP cannot get
				calculateSubDeadline(pcpList); //Calculate the subDeadline of each task in the whole PCP
				Stack<Task> pcpStack = new Stack<Task>(); //Store the tasks on the each PCP
				for(int k=0;k<pcpList.size();k++) { //Add all tasks of the whole PCP, then the tasks will be get in positive order
					Task t = pcpList.get(k);
					pcpStack.push(t);
				}
				
				while(pcpStack.size()>0) {
					Task t = pcpStack.pop();
					for(Edge e : t.getInEdges()) {//If t has unCalculate parent, should add it into pcpStack again, otherwise will miss tasks on t's subPCP
						if(!pcpStack.contains(t) && e.getParentTask().getSubDeadline() == -1) {
							pcpStack.push(t);
						}
					} 
					ArrayList<Task> tempPCPList = getPCP(t);
					if(tempPCPList.size()>0) {
						calculateSubDeadline(tempPCPList);
						for(int k=0;k<tempPCPList.size();k++) {
							Task temp = tempPCPList.get(k);
							pcpStack.push(temp);
						}
					}
				}
			}
			
			if(taskList.size()<55 && workflow.getWorkflowName().equals("LIGO-50")) {
				Check.checkTaskParameter(taskList);
			}
			
		} //End for, each workflow in the list
	}
	
	/**Get PCP list of a task*/
	public ArrayList<Task> getPCP(Task task) {
		Task currentTask = task;
		ArrayList<Task> pcpList = new ArrayList<Task>();
		boolean existUnAssignParent = true;
		
		while(existUnAssignParent) {
			Task criticalParent = null;
			Task parent = null;
			double maxEFTAndTT = Integer.MIN_VALUE; //Store the max earliest finish time and transfer data time between t and t's parent
			for(Edge e : currentTask.getInEdges()) { //Get the critical parent task
				if(e.getParentTask().getSubDeadline() == -1) { //Get the unCalculate parent
					parent = e.getParentTask();
				}
				if(parent != null) {
					double tempEFTAndTT = parent.getEarliestFinishTime() + e.getTransDataSize()/Parameters.bandwidth;
					if(maxEFTAndTT < tempEFTAndTT){
						maxEFTAndTT = tempEFTAndTT;
						criticalParent = parent;
					}
				}
			}
			if(criticalParent != null) {
				pcpList.add(criticalParent); //Add the parent task into end, then the pcp list has a reverse order of tasks
				currentTask = criticalParent;
			}
			else{
				existUnAssignParent = false;
			}
		}
		
		return pcpList;
	}
	
	/**Calculate subdeadline for each task in PCP, notice the pcpList has reverse order*/
	public void calculateSubDeadline(List<Task> pcpList) {
		int lastNum = pcpList.size()-1;
		double firstEST = pcpList.get(lastNum).getEarliestStartTime(); //The first task' earlist start time, notice the last task in list is the first
		double lastEFT = pcpList.get(0).getEarliestFinishTime(); //The last task' earlist completion time, notice the first task in list is the last
		double psd = pcpList.get(0).getLatestFinishTime() - pcpList.get(lastNum).getEarliestStartTime(); //The path subdeadline
		for(int k=0;k<pcpList.size();k++) {
			Task t = pcpList.get(k);
			double currentEFT = t.getEarliestFinishTime(); //The current task's earlist completion time
			double subDeadline = firstEST + ((currentEFT - firstEST)/(lastEFT - firstEST)) * psd;
			t.setSubDeadline(subDeadline);
			t.setThelta(subDeadline-t.getEarliestStartTime()); //Calculate the difference between subDeadline and earlist start time
		}
	}
	
	/**Calculate the latest start/finish time, subdeadline of Successor for a finished taskin the workflow list*/
	public void calculateSuccessorParameter(List<Task> successorList) {
		int fastestVMType = Parameters.speedFactor.length - 1;
		int calculateNum = 0; //Store the number of calculate tasks

		//Calculate task's earliest start/completion time from the entry task to the exit task
		while(calculateNum < successorList.size()) {
			for(int j=0;j<successorList.size();j++){
				Task t = successorList.get(j);
				double maxEarliestStartTime = Integer.MIN_VALUE; //Store the max start time of task t, calculate by the sum of earliest finish time and transfer data time between t and t's parent
				
				for(Edge e : t.getInEdges()) { //Caculate max start time from each parent
					double baseTransferDateTime = e.getTransDataSize()/Parameters.bandwidth;
					double tempEarlistStartTime = e.getParentTask().getActualFinishTime() + baseTransferDateTime; 
					if(maxEarliestStartTime < tempEarlistStartTime) {
						maxEarliestStartTime = tempEarlistStartTime;
					}
				}
				
				t.setEarliestStartTime(maxEarliestStartTime); //Set the max start time as task's earliest start time
				//double executeTimeWithDeviation =  t.getBaseExecuteTime() + t.getBaseExecuteTime()*Parameters.standardDeviation;
				double perExecuteTime = t.getBaseExecuteTime() * Parameters.speedFactor[fastestVMType]; //The predicted execute time of scheduleTask on the fastest vm
				t.setEarliestFinishTime(maxEarliestStartTime + perExecuteTime);
				calculateNum++;
			} //End for, calculate task's earliest start/completion time from the entry task to the exit task 
		} //End while
	
		//Calculate the task's subdeadline
		for(int j=0;j<successorList.size();j++) {
			Task t = successorList.get(j);
			double tempSub = t.getEarliestStartTime() + t.getThelta();
			if(tempSub < t.getLatestFinishTime()) {
				t.setSubDeadline(tempSub);
			}
			else {
				t.setSubDeadline(t.getLatestFinishTime());
			}
		}
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
	
	/**Compare two task by their earliest start time*/
	public class compareTaskByEarliestStartTime implements Comparator<Task>{
		public int compare(Task t1, Task t2) {
			if(t1.getEarliestStartTime() > t2.getEarliestStartTime())
			{
				return 1;
			}
			else if(t1.getEarliestStartTime() < t2.getEarliestStartTime())
			{
				return -1;
			}
			else
			{
				return 0;
			}
		}
	}
	
	/**Schedule tasks in a list to active VMs or new VMs, which has the min lease cost under the subdeadline*/
	public void scheduleTaskToVM(List<Task> taskList, List<VM> activeVMList) {
		for(int i=0;i<taskList.size();i++) {
			Task scheduleTask = taskList.get(i); //The selected the task
			double growthCost = Double.MAX_VALUE; //Store the min cost while select a VM
			List<VM> suitableVMList = new ArrayList<VM>(); //Store the suitable VM from activeVM List
			
			VM selectVM = null; //The selected VM
			int selectVMType = -1; //The selected VM type 
			
			//Calculate the task's predicated finish time and cost on active VM
			for(VM vm : activeVMList) {
				if(vm.getWaitTask().getTaskID().equals("init")) {//Only check the VM has no wait task}
					double activeVMStartTime = calculatePredicateStartTime(scheduleTask,vm); //Get the predicate start time on vm
					
					//Calculate the predicted completion time and cost if assign scheduleTask on vm 
					double executeTimeWithDeviation =  scheduleTask.getBaseExecuteTime() + scheduleTask.getBaseExecuteTime()*Parameters.standardDeviation;
					double perExecuteTime = executeTimeWithDeviation*vm.getVMExecuteTimeFactor(); //The predicted execute time of scheduleTask on vm
					double preFinishTime = activeVMStartTime + perExecuteTime;
					double preCost = perExecuteTime * vm.getVMPrice();
					
					if(preFinishTime<=scheduleTask.getSubDeadline() && preCost<growthCost) {
						suitableVMList.add(vm);
						growthCost = preCost;
					}
				} 
			} //End for, find each active VM
			
			//Get the min idle time VM
			if(suitableVMList.size()>0) {
				double minIdleTime = Double.MAX_VALUE;
				for(VM vm : suitableVMList) {
					double tempIdleTime = calculateIdleTime(scheduleTask,vm);
					if(minIdleTime>tempIdleTime) {
						minIdleTime = tempIdleTime;
						selectVM = vm;
					}
				}
			}
		
			//Calculate the task's predicated finish time and cost on a new VM
			double minCost = Double.MAX_VALUE;
			for(int j=0;j<Parameters.vmTypeNumber-1;j++) {
				double predicteStartTime = calculatePredicateStartTime(scheduleTask, null);
				double executeTimeWithDeviation =  scheduleTask.getBaseExecuteTime() + scheduleTask.getBaseExecuteTime()*Parameters.standardDeviation;
				double perExecuteTime = executeTimeWithDeviation*Parameters.speedFactor[j]; //The predicted execute time of scheduleTask on vm
				double preFinishTime = Parameters.launchTime + predicteStartTime + perExecuteTime;
				double preCost = perExecuteTime * Parameters.prices[j];
				if(preFinishTime<=scheduleTask.getSubDeadline()) {
					if(minCost>preCost){
						minCost = preCost;
						selectVMType = j;
					}
				}
			}
				
			//Set the selectVMType as the fastest type when can find a suitable VM from active VMs or new VMs
			if(selectVMType == -1 && selectVM == null) {
				selectVMType = Parameters.vmTypeNumber-1;
			}
			
			//Lease a new VM with selectVMType when the select active VM is null
			if(selectVM == null) {
				selectVM = new VM(selectVMType, currentTime);
				activeVMList.add(selectVM);
			}
			
			if(selectVM.getExecuteTask().getTaskID().equals("init")) { //The vm has no execute task
				//Update vm's and scheduleTask's parameters
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
				double executeTimeWithDeviation =  scheduleTask.getBaseExecuteTime() + scheduleTask.getBaseExecuteTime()*Parameters.standardDeviation;
				double predicteExecuteTime = executeTimeWithDeviation*selectVM.getVMExecuteTimeFactor(); //Use base execute time to calculate predicte execute time
				double predicteFinishTime = predicteStartTime + predicteExecuteTime;
				scheduleTask.setPredictStartTime(predicteStartTime);
				scheduleTask.setPredictFinishTime(predicteFinishTime);
				
				selectVM.setWaitTask(scheduleTask);
			}
			selectVM.getTaskList().add(scheduleTask);
			scheduleTask.setAssignedVM(selectVM);
			scheduleTask.setIsAssigned(true);
		}
		scheduledTaskList.addAll(scheduledTaskList);
	}
	
	/**Calculate the predicate start time for a task on a VM, need consider the transfer data whether assigned on the same VM*/
	public double calculatePredicateStartTime(Task task, VM vm) {
		double actualStartTime = currentTime;
		
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
			
			if(actualStartTime < maxParentTransTime ) { //Set the max transfer data time of scheduleTask's parent as the min start time
				actualStartTime = maxParentTransTime; 
			}
		}
		
		//Calculate the available time of vm
		if(vm !=null) {
			if(!vm.getExecuteTask().getTaskID().equals("init")) { //The vm has an execute task
				if(vm.getWaitTask().getTaskID().equals("init")) { //The vm has no wait task
					if(actualStartTime < vm.getExecuteTask().getActualFinishTime()) { //An execute taks must has actual start/execute/finish time
						actualStartTime = vm.getExecuteTask().getActualFinishTime();
					}
				}
				else {
					actualStartTime =  vm.getWaitTask().getPredictFinishTime();
				}
			}
			else { //The vm is idle
				if(actualStartTime < vm.getLeaseStartTime()) {
					actualStartTime = vm.getLeaseStartTime();
				}
			}
		}
				
		return actualStartTime;
	}
	
	/**Calculate the actual start time for a task on a VM, need consider the transfer data whether assigned on the same VM*/
	public double calculateActualStartTime(Task task, VM vm) {
		double actualStartTime = currentTime;
		
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
				maxParentTransTime = parent.getActualFinishTime() + inEdge.getTransDataSizeWithDeviation()/Parameters.bandwidth;
			}
			
			if(actualStartTime < maxParentTransTime ) { //Set the max transfer data time of scheduleTask's parent as the min start time
				actualStartTime = maxParentTransTime; 
			}
		}
		
		//Calculate the available time of vm
		if(vm !=null) {
			if(!vm.getExecuteTask().getTaskID().equals("init")) { //The vm has an execute task
				if(vm.getWaitTask().getTaskID().equals("init")) { //The vm has no wait task
					if(actualStartTime < vm.getExecuteTask().getActualFinishTime()) { //An execute taks must has actual start/execute/finish time
						actualStartTime = vm.getExecuteTask().getActualFinishTime();
					}
				}
				else {
					actualStartTime =  vm.getWaitTask().getPredictFinishTime();
				}
			}
			else { //The vm is idle
				if(actualStartTime < vm.getLeaseStartTime()) {
					actualStartTime = vm.getLeaseStartTime();
				}
			}
		}
				
		return actualStartTime;
	}
	 
	/**Calculate the idle time for a task on a VM, need consider the transfer data whether assigned on the same VM*/
	public double calculateIdleTime(Task task, VM vm) {
		double taskStartTime = Double.MAX_VALUE;
		double vmAvailableTime = Double.MAX_VALUE;
		
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
				maxParentTransTime = parent.getActualFinishTime() + inEdge.getTransDataSizeWithDeviation()/Parameters.bandwidth;
			}
			
			if(taskStartTime < maxParentTransTime ) { //Set the max transfer data time of scheduleTask's parent as the min start time
				taskStartTime = maxParentTransTime; 
			}
		}
		
		//Calculate the available time of vm
		if(vm !=null) {
			if(!vm.getExecuteTask().getTaskID().equals("init")) { //The vm has an execute task
				if(vm.getWaitTask().getTaskID().equals("init")) { //The vm has no wait task
					if(vmAvailableTime < vm.getExecuteTask().getActualFinishTime()) { //An execute taks must has actual start/execute/finish time
						vmAvailableTime = vm.getExecuteTask().getActualFinishTime();
					}
				}
				else {
					vmAvailableTime =  vm.getWaitTask().getPredictFinishTime();
				}
			}
			else { //The vm is idle
				if(vmAvailableTime < vm.getLeaseStartTime()) {
					vmAvailableTime = vm.getLeaseStartTime();
				}
			}
		}
		
		if(taskStartTime > vmAvailableTime) {
			return taskStartTime - vmAvailableTime;
		}
		else {
			return 0;
		}
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
		return "NOSF";
	}

}
