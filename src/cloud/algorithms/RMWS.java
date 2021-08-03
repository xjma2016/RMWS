package cloud.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import cloud.components.*;
import cloud.configurations.Parameters;

public class RMWS implements Scheduler {
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
			    
		//Calculate the latest start/finish time, subdeadline of tasks in the workflow list
		calculateTaskParameter(experimentWorkflow);	
		
	/*	for(int m=0;m<experimentWorkflow.size();m++) {
			Workflow wk = experimentWorkflow.get(m);
			List<Task> taskList = wk.getTaskList();
			if(taskList.size()<55 && wk.getWorkflowName().equals("LIGO-50")) {
				System.out.print("RMWS:  ");
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
			Collections.sort(readyTaskList, new compareTaskByLatestStartTime()); //Sort the ready tasks by the Latest start time
			
			long startTime02 = System.currentTimeMillis();
			
			//Schedule the ready tasks to active VMs or New VMs
			scheduleTaskToVM(readyTaskList, activeVMList);
			
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
					
					long startTime03 = System.currentTimeMillis();
					
					//Schedule the ready tasks to active VMs or New VMs, and move them from task pool
					scheduleTaskToVM(readySucessorList, activeVMList);
					
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
		ExperimentResult.calculateExperimentResult("RMWS", allLeaseVMList, experimentWorkflow, totalScheduleTime);
		
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
	
	/**Calculate the latest start/finish time, subdeadline of tasks in the workflow list*/
	public void calculateTaskParameter(List<Workflow> workflowList) {
		int fastestVMType = Parameters.speedFactor.length - 1;
		for(int i=0;i<workflowList.size();i++) { 
			Workflow workflow = workflowList.get(i); //Get the ith workflow
			List<Task> taskList = workflow.getTaskList();//Get the task list in ith workflow
			
			int calculateNum = 0; //Store the number of calculate tasks
			while(calculateNum < taskList.size()) {
				for(int j=taskList.size()-1;j>=0;j--) { //Calculate task's latest start/finish time/PUR from the exit task to the entry task
					Task t = taskList.get(j);
					if(t.getOutEdges().size() == 0) { //The exit task
						//Calculate task's latest start/finish time
						t.setLatestFinishTime(workflow.getDeadline()); //Set the exit task's latest finish time as the workflow's deadline
						//double fastestExecuteTime =  (1+Parameters.standardDeviation)*t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType];
						double fastestExecuteTime =  t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType];
						if(t.getLatestFinishTime() <= fastestExecuteTime) {
							//throw new IllegalArgumentException("Task's latest finish time is no more than the fastest execute time!");
						}
						t.setLatestStartTime(t.getLatestFinishTime() - fastestExecuteTime);
						
						//Set task's probabilistic upward rank as the fastest execute time
						t.setPUpwardRank(fastestExecuteTime);
						
						calculateNum++;
					}
					else { //The task has child
						double minLatestFinishTime = Integer.MAX_VALUE; //Store the min start time of task t
						boolean isAllChildrenCalculated = true; //Check if all children of task t have been calculated
						
						double maxPURAndTT = -1; //Store the max sum of pUpwardRand and transfer data time between t and t's child
						//double fastestExecuteTime = (1+Parameters.standardDeviation)*t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType];
						double fastestExecuteTime = t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType];
						double randomNum = Math.random();
						
						for(Edge e : t.getOutEdges()) { //Find the calculated child of t
							if(e.getChildTask().getLatestStartTime() == -1) {
								isAllChildrenCalculated = false;
							}
						}
						
						if(isAllChildrenCalculated) { //If all children of task t have been calculated, calculated t' latest start/finish time, pUpwardRank
							for(Edge e : t.getOutEdges()) { //Caculate min start time from each child
								//double baseTransferDateTime = ((1+Parameters.standardDeviationData)*e.getTransDataSize())/Parameters.bandwidth;
								double baseTransferDateTime =e.getTransDataSize()/Parameters.bandwidth;
								double tempLatestFinishTime = e.getChildTask().getLatestStartTime() - baseTransferDateTime; 
								if(minLatestFinishTime > tempLatestFinishTime) {
									minLatestFinishTime = tempLatestFinishTime;
								}
								
								double ccr = -(fastestExecuteTime/baseTransferDateTime); //Store the ratio of computation to communication ratio
								double eta = Parameters.theta;
								double tempEta = 1 - Math.pow(Parameters.theta, ccr);
								if(tempEta < randomNum) {
									eta = 0;
								}
								double tempPURAndTT = e.getChildTask().getPUpwardRank() + eta*baseTransferDateTime;
								if(maxPURAndTT < tempPURAndTT) {
									maxPURAndTT = tempPURAndTT;
								}
							}
							
							t.setLatestFinishTime(minLatestFinishTime); //Set the task's latest finish time as the min latest finishTime
							if(t.getLatestFinishTime() <= fastestExecuteTime) {
								//double test = -1;
								//throw new IllegalArgumentException("Task's latest finish time is no more than the fastest execute time!");
							}
							t.setLatestStartTime(minLatestFinishTime - fastestExecuteTime);
							
							//Set task's probabilistic upward rank as the fastest execute time add maxPURAndTT
							t.setPUpwardRank(maxPURAndTT + fastestExecuteTime);
							
							calculateNum++;
						} //End if
					} //End else
				} //End for
			} //End while
		
			//Calculate the task's subdeadline
			double maxEntryTaskPUK = -1; //Store the max entry task's PUK
			double workflowDeadline = workflow.getDeadline();
			
			for(int k=0;k<taskList.size();k++) {
				Task entryTask = taskList.get(k);
				if(entryTask.getInEdges().size() == 0) { //The entry task
					double tempPUK = entryTask.getPUpwardRank();
					if(maxEntryTaskPUK < tempPUK) {
						maxEntryTaskPUK = tempPUK;
					}
				}
			}
			
			for(int k=0;k<taskList.size();k++) {
				Task t = taskList.get(k);
				//double fastestExecuteTime = (1+Parameters.standardDeviation)*t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType];
				double fastestExecuteTime = t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType];
				double taskPUK = t.getPUpwardRank();
				if(taskPUK == -1) {
					throw new IllegalArgumentException("Task's PUK is not calculate!");
				}
				double taskSubDeadline = workflowDeadline*((maxEntryTaskPUK - taskPUK + fastestExecuteTime)/maxEntryTaskPUK); //Calculate the task's subdeadline
				t.setSubDeadline(taskSubDeadline);
			}
		} //End for, each workflow in the list
	}
	
	/**Calculate the latest start/finish time, subdeadline of Successor for a finished taskin the workflow list*/
	public void calculateSuccessorParameter(List<Task> successorList, double workflowDeadline) {
		int fastestVMType = Parameters.speedFactor.length - 1;
		int calculateNum = 0; //Store the number of calculate tasks
		while(calculateNum < successorList.size()) {
			for(int j=successorList.size()-1;j>=0;j--) { //Calculate task's latest start/finish time/PUR from the exit task to the entry task
				Task t = successorList.get(j);
				if(t.getOutEdges().size() == 0) { //The exit task
					//Calculate task's latest start/finish time
					t.setLatestFinishTime(workflowDeadline); //Set the exit task's latest finish time as the workflow's deadline
					//double fastestExecuteTime = (1+Parameters.standardDeviation)*t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType];
					double fastestExecuteTime = t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType];
					if(t.getLatestFinishTime() <= fastestExecuteTime) {
						//throw new IllegalArgumentException("Task's latest finish time is no more than the fastest execute time!");
					}
					t.setLatestStartTime(t.getLatestFinishTime() - fastestExecuteTime);
					
					//Set task's probabilistic upward rank as the fastest execute time
					t.setPUpwardRank(fastestExecuteTime);
					
					calculateNum++;
				}
				else { //The task has child
					double minLatestFinishTime = Integer.MAX_VALUE; //Store the min start time of task t
					boolean isAllChildrenCalculated = true; //Check if all children of task t have been calculated
					
					double maxPURAndTT = -1; //Store the max sum of pUpwardRand and transfer data time between t and t's child
					//double fastestExecuteTime = (1+Parameters.standardDeviation)*t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType]; //The fastest execute time of t
					double fastestExecuteTime = t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType]; //The fastest execute time of t
					double randomNum = Math.random();
					
					for(Edge e : t.getOutEdges()) { //Find the calculated child of t
						if(e.getChildTask().getLatestStartTime() == -1) {
							isAllChildrenCalculated = false;
						}
					}
					
					if(isAllChildrenCalculated) { //If all children of task t have been calculated, calculated t' latest start/finish time, pUpwardRank
						for(Edge e : t.getOutEdges()) { //Caculate min start time from each child
							//double baseTransferDateTime = (1+Parameters.standardDeviationData)*e.getTransDataSize()/Parameters.bandwidth;
							double baseTransferDateTime = e.getTransDataSize()/Parameters.bandwidth;
							double tempLatestFinishTime = e.getChildTask().getLatestStartTime() - baseTransferDateTime; 
							if(minLatestFinishTime > tempLatestFinishTime) {
								minLatestFinishTime = tempLatestFinishTime;
							}
							
							double ccr = -(fastestExecuteTime/baseTransferDateTime); //Store the ratio of computation to communication ratio
							double eta = Parameters.theta;
							double tempEta = 1 - Math.pow(Parameters.theta, ccr);
							if(tempEta < randomNum) {
								eta = 0;
							}
							double tempPURAndTT = e.getChildTask().getPUpwardRank() + eta*baseTransferDateTime;
							if(maxPURAndTT < tempPURAndTT) {
								maxPURAndTT = tempPURAndTT;
							}
						}
						
						t.setLatestFinishTime(minLatestFinishTime); //Set the task's latest finish time as the min latest finishTime
						if(t.getLatestFinishTime() <= fastestExecuteTime) {
							//double test = -1;
							//throw new IllegalArgumentException("Task's latest finish time is no more than the fastest execute time!");
						}
						t.setLatestStartTime(minLatestFinishTime - fastestExecuteTime);
						
						//Set task's probabilistic upward rank as the fastest execute time add maxPURAndTT
						t.setPUpwardRank(maxPURAndTT + fastestExecuteTime);
						
						calculateNum++;
					} //End if
				} //End else
			} //End for
		} //End while
	
		//Calculate the task's subdeadline
		double maxEntryTaskPUK = -1; //Store the max entry task's PUK
		
		for(int k=0;k<successorList.size();k++) {
			Task entryTask = successorList.get(k);
			if(entryTask.getInEdges().size() == 0) { //The entry task
				double tempPUK = entryTask.getPUpwardRank();
				if(maxEntryTaskPUK < tempPUK) {
					maxEntryTaskPUK = tempPUK;
				}
			}
		}
		
		for(int k=0;k<successorList.size();k++) {
			Task t = successorList.get(k);
			//double fastestExecuteTime = (1+Parameters.standardDeviation)*t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType];; //The fastest execute time of t
			double fastestExecuteTime = t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType];; //The fastest execute time of t
			double taskPUK = t.getPUpwardRank();
			if(taskPUK == -1) {
				throw new IllegalArgumentException("Task's PUK is not calculate!");
			}
			double taskSubDeadline = workflowDeadline*((maxEntryTaskPUK - taskPUK + fastestExecuteTime)/maxEntryTaskPUK); //Calculate the task's subdeadline
			t.setSubDeadline(taskSubDeadline);
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
	
	/**Compare two task by their latest start time*/
	
	/**Sort tasks in a list by task's latest start time*/
	public class compareTaskByLatestStartTime implements Comparator<Task>{
		public int compare(Task t1, Task t2) {
			if(t1.getLatestStartTime() > t2.getLatestStartTime())
			{
				return 1;
			}
			else if(t1.getLatestStartTime() < t2.getLatestStartTime())
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
			double minCost = Double.MAX_VALUE; //Store the min cost while select a VM
			VM selectVM = null; //The selected VM
			int selectVMType = -1; //The selected VM type 
			
			//Calculate the task's predicated finish time and cost on a new VM
			double newVMStartTime = calculatePredicateStartTime(scheduleTask,null);
			
			for(int j=0;j<Parameters.vmTypeNumber-1;j++) {
				double newVMExecuteTime = (1+Parameters.standardDeviation)*scheduleTask.getBaseExecuteTime()*Parameters.speedFactor[j];
				double newVMFinishTime = Parameters.launchTime + newVMStartTime + newVMExecuteTime; //The transfer data only can begin after the new VM launched 
				int preUnitTimes = (int)Math.ceil(newVMExecuteTime/Parameters.unitTime);
				double newVMCost = preUnitTimes*Parameters.prices[j];
				if(newVMFinishTime<=scheduleTask.getSubDeadline()) { //if(newVMFinishTime <= scheduleTask.getSubDeadline()) {
					if(minCost>newVMCost){
						minCost = newVMCost;
						selectVMType = j;
					}
				}
			}
			
			//Calculate the task's predicated finish time and cost on active VM
			for(VM vm : activeVMList) {
				if(vm.getWaitTask().getTaskID().equals("init")) { //Only check the VM has no wait task
					double activeVMStartTime = calculatePredicateStartTime(scheduleTask,vm); //Get the predicate start time on vm
					double minCostOnActiveVM = Integer.MAX_VALUE;
					double minIdleTime = Integer.MAX_VALUE;
						
					//Get the available time of vm
					double tempAvialableTime = currentTime;
					if(!vm.getExecuteTask().getTaskID().equals("init")) { //The vm has an execute task
						tempAvialableTime = vm.getExecuteTask().getActualFinishTime();
					}
					
					//Ingore the vm when idle time is more than the set value, the paper unused this
				/*	if((activeVMStartTime - tempAvialableTime) > Parameters.maxIdleTime) {
						continue;
					}*/
					
					//Calculate the predicted increase cost if assign scheduleTask on vm 
					double perExecuteTime = (1+Parameters.standardDeviation)*scheduleTask.getBaseExecuteTime()*vm.getVMExecuteTimeFactor(); //The predicted execute time of scheduleTask on vm
					double preFinishTime = activeVMStartTime + perExecuteTime;
					
					double preLeaseTime = preFinishTime - vm.getLeaseStartTime(); //The vm's predicted lease time
					int preUnitTimes = (int)Math.ceil(preLeaseTime/Parameters.unitTime); //The total predicted lease round of vm
					double originalTime = tempAvialableTime - vm.getLeaseStartTime();
					int originalUnitTimes = (int)Math.ceil(originalTime/Parameters.unitTime);
					double increaseCost = (preUnitTimes - originalUnitTimes)*vm.getVMPrice();
					//double increaseCost = perExecuteTime*vm.getVMPrice();
					
					//Select the vm which has the least cost under the schedlueTask's subDeadline
					if(preFinishTime <= scheduleTask.getSubDeadline()) { 
						if(minCostOnActiveVM >= increaseCost) {
							minCostOnActiveVM = increaseCost;
							selectVM = vm;
						}
					}
				} //End if, find the VM has no wait task
				
			} //End for, find each active VM
			
			//Set the selectVMType as the fastest type when can find a suitable VM from active VMs or new VMs
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
				if(!selectVM.getWaitTask().getTaskID().equals("init")) {
					throw new IllegalArgumentException("The select VM has a wait Task!");
				}
				double predicteStartTime = calculatePredicateStartTime(scheduleTask, selectVM);
				double predicteExecuteTime = (1+Parameters.standardDeviation)*scheduleTask.getBaseExecuteTime()*selectVM.getVMExecuteTimeFactor(); //Use base execute time to calculate predicte execute time
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
				if(vm.getWaitTask().getTaskID().equals("init")) { //Only the vm has not a wait task, the vm can be choose
					if(actualStartTime < vm.getExecuteTask().getActualFinishTime()) { //An execute taks must has actual start/execute/finish time
						actualStartTime = vm.getExecuteTask().getActualFinishTime();
					}
				}
				else {
					throw new IllegalArgumentException("A assigned VM has a wait task!");
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
				if(vm.getWaitTask().getTaskID().equals("init")) { //Only the vm has not a wait task, the vm can be choose
					if(actualStartTime < vm.getExecuteTask().getActualFinishTime()) { //An execute taks must has actual start/execute/finish time
						actualStartTime = vm.getExecuteTask().getActualFinishTime();
					}
				}
				else {
					throw new IllegalArgumentException("A assigned VM has a wait task!");
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
		return "RMWS";
	}

}
