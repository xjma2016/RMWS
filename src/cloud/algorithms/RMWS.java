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
	private List<Task> taskPool; //The unassigned tasks list
	private List<Task> readyTaskList; //The ready task list 
	private List<Task> scheduledTaskList ; //The scheduled task after assigning
	
	private List<VM> activeVMList; //The active VM list
	private List<VM> allLeaseVMList; //All used VM list that store the VM after leased
	private HashMap<Double, Integer> activeVMNumList;//Record the active VM count at each current time, <Double:currentTime, Integer:vmNum>
	
	private double currentTime; //The current time in the scheduling process
	private Check check; //Check the schedule result
	
	public void schedule(List<Workflow> workflowList) {
		check = new Check();
		
		//Initialize
		init(workflowList);
		
		//check.printText("Init at: ");	  		
		
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
					i++; //Update i to the next workflow that has different arrive time
				}
				else {
					break; //Break 'for' after get all workflows that have the same arrive time
				}
			}
			
			long startTime01 = System.currentTimeMillis();
			
			//Calculate the latest start/finish time, subdeadline of tasks in the workflowWithSameArrivetime
			for(int w=0;w<workflowWithSameArrivetime.size();w++) {
				Workflow workflow = workflowWithSameArrivetime.get(w); //Get the w-th workflow
				List<Task> taskList = workflow.getTaskList();//Get the task list in w-th workflow
				calculateTaskParameter(taskList,workflow.getDeadline());	
			} 
			
			//Get the ready tasks from the workflows with the same arrive time
			readyTaskList = getReadyTask(workflowWithSameArrivetime);
			Collections.sort(readyTaskList, new compareTaskByLatestStartTime()); //Sort the ready tasks by the latest start time
			
			//Schedule the ready tasks to active VMs or new VMs
			scheduleTaskToVM(readyTaskList, activeVMList);
			
			activeVMNumList.put(currentTime, activeVMList.size());
			
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
			
			//Get the next arrival time
			double nextArrivalTime = Integer.MAX_VALUE;
			if(i != experimentWorkflow.size()-1) {
				nextArrivalTime = experimentWorkflow.get(i).getArrivalTime();
			}
			
			//Initialize the next finish VM and its finish time, the next released VM and its release time
			VM nextFinishVM = null; //The VM on which the execute task has the min actual finish time
			double nextFinishTime = Integer.MAX_VALUE; //The min actual finish time of execute tasks
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
					if((currentTime-vm.getLeaseStartTime())%Parameters.unitTime==0) { 
						tempNextReleaseTime = currentTime;
					}
					else {
						int unitTimes = (int)Math.ceil((currentTime - vm.getLeaseStartTime())/Parameters.unitTime);
						tempNextReleaseTime = vm.getLeaseStartTime() + unitTimes*Parameters.unitTime;
					}
					if(nextReleaseTime>tempNextReleaseTime) {
						nextReleaseTime = tempNextReleaseTime;
						nextReleaseVM = vm;
					}
				}
			} //End for, get the four parameters
			
			//Update the schedule status before next workflow arrives
			while(nextArrivalTime>=nextFinishTime || nextArrivalTime>nextReleaseTime) {
				//Update the VM and its execute task, which have the min finish time
				if(nextFinishTime<=nextReleaseTime) {
					//Set the execute task as finished
					Task nextFinishTask = nextFinishVM.getExecuteTask();
					nextFinishTask.setIsFinished(true);
					nextFinishVM.setExecuteTask(new Task("init", -1, -1));
					currentTime = nextFinishTask.getActualFinishTime(); //Set the current time as the next finish time
					
					if(!nextFinishVM.getWaitTask().getTaskID().equals("init")) { //The next finish VM has a wait task, then execute it
						Task nextExecuteTask = nextFinishVM.getWaitTask();
						nextFinishVM.setWaitTask(new Task("init", -1, -1)); //Set the wait task as init
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
					
					long startTime02 = System.currentTimeMillis();
					
					//Get the ready children of the next finish task
					List<Task> readySucessorList = getReadySucessor(nextFinishTask);
					
					//Get the deadline of the workflow that contain nextFinishTask
					double  deadline = -1;
					List<Task> workflowTaskList = null;
					for(int w=0;w<workflowList.size();w++) {
						if(workflowList.get(w).getWorkflowId()==nextFinishTask.getTaskWorkflowID()) {
							deadline = workflowList.get(w).getDeadline();
							workflowTaskList = workflowList.get(w).getTaskList();
						}
					}
					
					//Calculate the parameters of the corresponding taskList
					if(deadline!=-1) {
						calculateSuccessorParameter(workflowTaskList,deadline);
					}
					else {
						 throw new IllegalArgumentException("Cannot get the deadline from the workflow with a finished task!");
					}
					
					//Schedule the ready tasks to active VMs or New VMs, and move them from task pool
					scheduleTaskToVM(readySucessorList, activeVMList);
					
					//Remove the assign tasks from task pool
					taskPool.removeAll(readySucessorList);
					
					long endTime02 = System.currentTimeMillis();
					totalScheduleTime = totalScheduleTime + (endTime02 - startTime02);
					
					activeVMNumList.put(currentTime, activeVMList.size());
				}
				
				//Release VM
				if(nextReleaseTime<nextFinishTime) {
					currentTime = nextReleaseTime; //Set the current time as the next release time
					double vmLeaseTime = nextReleaseTime - nextReleaseVM.getLeaseStartTime();
					int unitTimes = (int)Math.ceil(vmLeaseTime/Parameters.unitTime);
					double cost = unitTimes*nextReleaseVM.getVMPrice(); 
					
					nextReleaseVM.setLeaseFinishTime(nextReleaseTime);
					nextReleaseVM.setTotalLeaseCost(cost);
					nextReleaseVM.setVMStatus(false);
					
					activeVMList.remove(nextReleaseVM);
					allLeaseVMList.add(nextReleaseVM);
					activeVMNumList.put(currentTime, activeVMList.size());
				}
				
				//Update the next finish VM and release VM, which has the min time
				nextFinishVM = null; //The VM on which the executing task has the min actual finish time
				nextFinishTime = Integer.MAX_VALUE; //The min actual finish time
				nextReleaseVM = null; //The VM that will be released firstly
				nextReleaseTime = Integer.MAX_VALUE; //The firstly released time
				
				//Re-update the above four parameters in the active VM list
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
						if((currentTime-vm.getLeaseStartTime())%Parameters.unitTime == 0) { 
							tempNextReleaseTime = currentTime;
						}
						else { 
							int unitTimes = (int)Math.ceil((currentTime - vm.getLeaseStartTime())/Parameters.unitTime);
							tempNextReleaseTime = vm.getLeaseStartTime() + unitTimes*Parameters.unitTime;
						}
						if(nextReleaseTime>tempNextReleaseTime) {
							nextReleaseTime = tempNextReleaseTime;
							nextReleaseVM = vm;
						}
					}
				} //End for, re-update the four parameters
				
				//End while if can not find a next finish/release VM before nextArrivalTime, 
				if(nextArrivalTime==Integer.MAX_VALUE && nextFinishTime==Integer.MAX_VALUE && nextReleaseTime==Integer.MAX_VALUE) {
					break;
				}
				
			} //End while, update the schedule status before next workflow arrives
			
		} //End for, schedule each workflow in workflowList
		
		//Check the tasks and VMs status
		check.checkUnfinishTaskAndVM(experimentWorkflow,allLeaseVMList);
		
		//Calculate the experimental results
		ExperimentResult.calculateExperimentResult("RMWS", allLeaseVMList, experimentWorkflow, totalScheduleTime);
		
		//Calculate the active VM number
		//ExperimentResult.calculateActiveVMNum("RMWS", activeVMNumList);
				
		//Clear the lists
		experimentWorkflow.clear();
		taskPool.clear();
	    readyTaskList.clear();
		activeVMList.clear();
		allLeaseVMList.clear();
		activeVMNumList.clear();
		VM.resetInnerId(); //Reset the VM'id as 0 when finish the experiment of the algorithm
		
	}
	
    /**Init the lists*/
	public void init(List<Workflow> workflowList) {
		this.experimentWorkflow = workflowList;
		taskPool = new ArrayList<Task>(); 
		readyTaskList = new ArrayList<Task>(); 
		activeVMList = new ArrayList<VM>(); 
		allLeaseVMList = new ArrayList<VM>(); 
	    scheduledTaskList  =new ArrayList<Task>();
	  	activeVMNumList = new HashMap<Double,Integer>();
	}
	
	/**Calculate the latest start/finish time, subdeadline of tasks in the task list*/
	public void calculateTaskParameter(List<Task> taskList, double workflowDeadline) {
		int fastestVMType = Parameters.speedFactor.length - 1;
		int calculateNum = 0; //Store the number of calculate tasks
		while(calculateNum < taskList.size()) {
			for(int j=taskList.size()-1;j>=0;j--) { //Calculate task's latest start/finish time/PUR from the exit task to the entry task
				Task t = taskList.get(j);
				if(t.getOutEdges().size() == 0) { //The exit task
					//Calculate task's latest start/finish time
					t.setLatestFinishTime(workflowDeadline); //Set the exit task's latest finish time as the workflow's deadline
					double fastestExecuteTime =  t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType];
					t.setLatestStartTime(t.getLatestFinishTime() - fastestExecuteTime);
					
					//Set task's probabilistic upward rank as the fastest execute time
					t.setPUpwardRank(fastestExecuteTime);
					
					calculateNum++;
				}
				else { //The task has child
					double minLatestFinishTime = Integer.MAX_VALUE; //Store the min start time of task t
					boolean isAllChildrenCalculated = true; //Check if all children of task t have been calculated
					double maxPURAndTT = -1; //Store the max sum of pUpwardRand and transfer data time between t and t's child
					double fastestExecuteTime = t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType];
					double randomNum = Math.random(); //Get a random number between 0 and 1
					
					for(Edge e : t.getOutEdges()) { //Find the calculated child of t
						if(e.getChildTask().getLatestStartTime() == -1) {
							isAllChildrenCalculated = false;
						}
					}
					
					//If all children of task t have been calculated, calculated t' latest start/finish time, pUpwardRank
					if(isAllChildrenCalculated) { 
						for(Edge e : t.getOutEdges()) { //Calculate min start time from each child
							double baseTransferDateTime =e.getTransDataSize()/Parameters.bandwidth;
							double tempLatestFinishTime = e.getChildTask().getLatestStartTime() - baseTransferDateTime; 
							if(minLatestFinishTime > tempLatestFinishTime) {
								minLatestFinishTime = tempLatestFinishTime;
							}
							
							double ccr = -(fastestExecuteTime/baseTransferDateTime); //Store the ratio of computation to communication ratio
							int eta = 1;
							double tempEta = Math.pow(Parameters.theta, ccr);
							if(tempEta < randomNum) {
								eta = 0;
							}
							double tempPURAndTT = e.getChildTask().getPUpwardRank() + eta*baseTransferDateTime;
							if(maxPURAndTT < tempPURAndTT) {
								maxPURAndTT = tempPURAndTT;
							}
						}
						
						//Set the lft/lst/PUR of the task
						t.setLatestFinishTime(minLatestFinishTime); 
						t.setLatestStartTime(minLatestFinishTime - fastestExecuteTime);
						t.setPUpwardRank(maxPURAndTT + fastestExecuteTime);
						
						calculateNum++;
					} //End if
				} //End else
			} //End for
		} //End while
	
		//Calculate the task's subdeadline
		double maxEntryTaskPUR = -1; //Store the max entry task's PUR
		
		//Get the max PUR of entry task if there has more than one entry task
		for(int k=0;k<taskList.size();k++) {
			Task entryTask = taskList.get(k);
			if(entryTask.getInEdges().size() == 0) { //The entry task has no parent tasks
				double tempPUK = entryTask.getPUpwardRank();
				if(maxEntryTaskPUR < tempPUK) {
					maxEntryTaskPUR = tempPUK;
				}
			}
		}
		
		//Calculate the subdeadline of each task
		for(int k=0;k<taskList.size();k++) {
			Task t = taskList.get(k);
			double fastestExecuteTime = t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType];
			double taskPUR = t.getPUpwardRank();
			if(taskPUR == -1) {
				throw new IllegalArgumentException("Task's PUR is not calculate!");
			}
			double taskSubDeadline = workflowDeadline*((maxEntryTaskPUR - taskPUR + fastestExecuteTime)/maxEntryTaskPUR); 
			t.setSubDeadline(taskSubDeadline);
		}
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
					double fastestExecuteTime = t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType];
					t.setLatestStartTime(t.getLatestFinishTime() - fastestExecuteTime);
					
					//Set task's probabilistic upward rank as the fastest execute time
					t.setPUpwardRank(fastestExecuteTime);
					
					calculateNum++;
				}
				else { //The task has child
					double minLatestFinishTime = Integer.MAX_VALUE; //Store the min start time of task t
					double fastestExecuteTime = -1; //Store the execute time based whether the task is finished or not
					boolean isAllChildrenCalculated = true; //Check if all children of task t have been calculated
					
					double maxPURAndTT = -1; //Store the max sum of pUpwardRand and transfer data time between t and t's child
					if(t.getIsFinished()) {
						fastestExecuteTime = t.getActualExecuteTime();
					}
					else {
						fastestExecuteTime = t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType]; 
					}
					
					double randomNum = Math.random();
					
					for(Edge e : t.getOutEdges()) { //Find the calculated child of t
						if(e.getChildTask().getLatestStartTime() == -1) {
							isAllChildrenCalculated = false;
						}
					}
					
					if(isAllChildrenCalculated) { //If all children of task t have been calculated, calculated t' latest start/finish time, pUpwardRank
						for(Edge e : t.getOutEdges()) { //Calculate min start time from each child
							double baseTransferDateTime = -1; //Store the data transfer time based whether the task is finished or not
							if(t.getIsFinished()) {
								baseTransferDateTime = e.getTransDataSizeWithDeviation()/Parameters.bandwidth;
							}
							else {
								baseTransferDateTime = e.getTransDataSize()/Parameters.bandwidth; 
							}
							double tempLatestFinishTime = e.getChildTask().getLatestStartTime() - baseTransferDateTime; 
							if(minLatestFinishTime > tempLatestFinishTime) {
								minLatestFinishTime = tempLatestFinishTime;
							}
							
							double ccr = -(fastestExecuteTime/baseTransferDateTime); //Store the ratio of computation to communication ratio
							double eta = 1;
							double tempEta = Math.pow(Parameters.theta, ccr);
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
			double fastestExecuteTime = t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType];; //The fastest execute time of t
			double taskPUK = t.getPUpwardRank();
			if(taskPUK == -1) {
				throw new IllegalArgumentException("Task's PUK is not calculate!");
			}
			double taskSubDeadline = workflowDeadline*((maxEntryTaskPUK - taskPUK + fastestExecuteTime)/maxEntryTaskPUK); //Calculate the task's subdeadline
			if(t.getLatestFinishTime()<taskSubDeadline) {
				t.setSubDeadline(t.getLatestFinishTime());
			}
			else {
				t.setSubDeadline(taskSubDeadline);
			}
		}
	}
	
	/**Get the ready tasks from a workflow list*/
	public List<Task> getReadyTask(List<Workflow> workflowList) {
		List<Task> readyTaskList = new ArrayList<Task>();
		for(Workflow w : workflowList) {
			for(Task t : w.getTaskList()) { 
				if(t.getInEdges().size() == 0) {//The entry task
					readyTaskList.add(t);
				}
			}
		}
		return readyTaskList;
	}
	
	/**Sort tasks in a list by task's latest start time*/
	public class compareTaskByLatestStartTime implements Comparator<Task>{
		/**Compare two task by their latest start time*/
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
			Task scheduleTask = taskList.get(i); //The selected task
			double minCost = Double.MAX_VALUE; //Store the min cost while select a VM
			double minFinishTime = Double.MAX_VALUE; //Store the min finish time while select a VM
			VM selectVM = null; //The selected VM
			int selectVMType = -1; //The selected VM type 
			
			//Calculate the task's predicated finish time and cost on a new VM
			double newVMStartTime = calculatePredicateStartTime(scheduleTask,null);
			for(int j=0;j<Parameters.vmTypeNumber-1;j++) {
				double newVMExecuteTime = (1+Parameters.standardDeviation)*scheduleTask.getBaseExecuteTime()*Parameters.speedFactor[j];
				double newVMFinishTime = Parameters.launchTime + newVMStartTime + newVMExecuteTime;
				int preUnitTimes = (int)Math.ceil(newVMExecuteTime/Parameters.unitTime);
				double newVMCost = preUnitTimes*Parameters.prices[j];
				//Select the VM that has the min cost under the subdeadline
				if(newVMFinishTime<=scheduleTask.getSubDeadline()) {
					if(minCost>=newVMCost){
						minCost = newVMCost;
						selectVMType = j;
					}
				}
			}
			
			//Calculate the task's predicated finish time and cost on active VMs
			for(VM vm : activeVMList) {
				if(vm.getWaitTask().getTaskID().equals("init")) { //The VM has no wait task
					double activeVMStartTime = calculatePredicateStartTime(scheduleTask,vm); //Get the predicate start time
					double minCostOnActiveVM = Integer.MAX_VALUE;
						
					//Get the available time of VM
					double tempAvialableTime = currentTime;
					if(!vm.getExecuteTask().getTaskID().equals("init")) { //The VM has an execute task
						tempAvialableTime = vm.getExecuteTask().getActualFinishTime();
					}
					
					//Calculate the predicted increase cost if assign scheduleTask on the VM 
					double perExecuteTime = (1+Parameters.standardDeviation)*scheduleTask.getBaseExecuteTime()*vm.getVMExecuteTimeFactor(); 
					double preFinishTime = activeVMStartTime + perExecuteTime;
					
					double preLeaseTime = preFinishTime - vm.getLeaseStartTime(); //The VM's predicted lease time
					int preUnitTimes = (int)Math.ceil(preLeaseTime/Parameters.unitTime); //The total predicted lease round of VM
					double originalTime = tempAvialableTime - vm.getLeaseStartTime();
					int originalUnitTimes = (int)Math.ceil(originalTime/Parameters.unitTime);
					double increaseCost = (preUnitTimes - originalUnitTimes)*vm.getVMPrice();
					
					//Select the VM which has the min cost and finish time under the subDeadline
					if(preFinishTime<=scheduleTask.getSubDeadline()) { 
						if(minCostOnActiveVM>=increaseCost) {
							if(minFinishTime>preFinishTime) {
								minCostOnActiveVM = increaseCost;
								minFinishTime = preFinishTime;
								selectVM = vm;
							}
						}
					}
				} //End if, find the VM has no wait task
			} //End for, find each active VM
			
			//Set the selectVMType as the fastest type when cannot find a suitable VM from active VMs or new VMs
			if(selectVMType == -1 && selectVM == null) {
				selectVMType = Parameters.vmTypeNumber-1;
			}
			
			//Lease a new VM with selectVMType when the select active VM is null
			if(selectVM == null) {
				selectVM = new VM(selectVMType, currentTime);
				activeVMList.add(selectVM);
				//System.out.println("Assigned on a new VM" );
			}
			
			//Assign the scheduleTaks on the selected VM
			if(selectVM.getExecuteTask().getTaskID().equals("init")) { //The selected VM has no execute task
				//Update the parameters of selected VM and scheduleTask 
				double actualStartTime = calculateActualStartTime(scheduleTask, selectVM);
				double actualExecuteTime = scheduleTask.getBaseExecuteTimeWithDeviation()*selectVM.getVMExecuteTimeFactor();
				double actualFinishTime = actualStartTime + actualExecuteTime;
				scheduleTask.setActualStartTime(actualStartTime);
				scheduleTask.setActualExecuteTime(actualExecuteTime);
				scheduleTask.setActualFinishTime(actualFinishTime);
				
				selectVM.setExecuteTask(scheduleTask);
			}
			else { //Set the scheduleTask as the wait task
				if(!selectVM.getWaitTask().getTaskID().equals("init")) {
					throw new IllegalArgumentException("The select VM has a wait Task!");
				}
				double predicteStartTime = calculatePredicateStartTime(scheduleTask, selectVM);
				double predicteExecuteTime = (1+Parameters.standardDeviation)*scheduleTask.getBaseExecuteTime()*selectVM.getVMExecuteTimeFactor(); 
				double predicteFinishTime = predicteStartTime + predicteExecuteTime;
				scheduleTask.setPredictStartTime(predicteStartTime);
				scheduleTask.setPredictFinishTime(predicteFinishTime);
				
				selectVM.setWaitTask(scheduleTask);
			}
			
			selectVM.getTaskList().add(scheduleTask);
			scheduleTask.setAssignedVM(selectVM);
			scheduleTask.setIsAssigned(true);
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
			
			//Whether consider the transfer data time based on if scheduleTask and its parent are on the the same VM 
			if(parentTaskVM.equals(vm)) { 
				maxParentTransTime = parent.getActualFinishTime();
			}
			else { 
				maxParentTransTime = parent.getActualFinishTime() + ((1+Parameters.standardDeviationData)*inEdge.getTransDataSize())/Parameters.bandwidth;
			}
			
			if(predictStartTime < maxParentTransTime ) { //Set the max transfer data time of scheduleTask's parent as the predict start time
				predictStartTime = maxParentTransTime; 
			}
		}
		
		//Calculate the available time of VM
		if(vm !=null) {
			if(!vm.getExecuteTask().getTaskID().equals("init")) { //The VM has an execute task
				if(vm.getWaitTask().getTaskID().equals("init")) { //The VM can be choose only has no wait task
					if(predictStartTime < vm.getExecuteTask().getActualFinishTime()) { //An execute task must has actual start/execute/finish time
						predictStartTime = vm.getExecuteTask().getActualFinishTime();
					}
				}
				else {
					throw new IllegalArgumentException("A assigned VM has a wait task!");
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
	
	/**Calculate the actual start time for a task on a VM, need consider the transfer data whether assigned on the same VM*/
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
			
			if(parentTaskVM.equals(vm)) { //Ignore the transfer data time if scheduleTask and its parent are on the same VM 
				maxParentTransTime = parent.getActualFinishTime();
			}
			else { //Consider the transfer data time if not on the same VM 
				maxParentTransTime = parent.getActualFinishTime() + inEdge.getTransDataSizeWithDeviation()/Parameters.bandwidth;
			}
			
			if(actualStartTime<maxParentTransTime ) { //Set the max transfer data time of scheduleTask's parent as the min start time
				actualStartTime = maxParentTransTime; 
			}
		}
		
		//Calculate the available time of VM
		if(vm !=null) {
			if(!vm.getExecuteTask().getTaskID().equals("init")) { //This cannot happen when calculate the actual start time of task if the VM has an execute task
				//throw new IllegalArgumentException("A assigned VM has a execute task!");
				if(vm.getWaitTask().getTaskID().equals("init")) { //Only the vm has not a wait task, the vm can be choose
					if(actualStartTime < vm.getExecuteTask().getActualFinishTime()) { //An execute taks must has actual start/execute/finish time
						actualStartTime = vm.getExecuteTask().getActualFinishTime();
					}
				}
				else {
					throw new IllegalArgumentException("A assigned VM has a wait task!");
				}
			}
			else { //The VM is idle
				if(actualStartTime<vm.getLeaseStartTime()) {
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
