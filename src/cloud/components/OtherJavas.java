package cloud.components;

public class OtherJavas {
	
   /*Date staD = new Date();
	SimpleDateFormat sta = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	System.out.println("Generate template start at : " + sta.format(staD));
	throw new IllegalArgumentException("Task base execution time is less than zero!");
	*/
	
	/*HashMap<Integer,String> workflowName = new HashMap<Integer,String>();
	for(int k=0;k<workflowWithSameArrivetime.size();k++) {
		workflowName.put(workflowList.get(k).getWorkflowId(), workflowList.get(k).getWorkflowName());
		if(workflowList.get(k).getWorkflowName().equals("CYBERSHAKE-50") ) {
			workflowName.put(workflowList.get(k).getWorkflowId(), workflowList.get(k).getWorkflowName());
			workflowNamea = workflowList.get(k).getWorkflowName();
		}
	}*/
	
/*	for(int k=0;k<readyTaskList.size();k++) {
		Task t = readyTaskList.get(k);
		//System.out.println("Schedule task: " + t.getTaskID()+"---"+t.getTaskWorkflowID() + "---"+ workflowName.get(t.getTaskWorkflowID()) );
	}*/

	//Setup a new VM with each type when the active vm list is empty
	/**if(activeVMList.size() == 0) {
		for(int j=0;j<Parameters.vmTypeNumber-1;j++) {
			VM newVM = new VM(j,-1); 
			activeVMList.add(newVM);
		}
	}*/
	
/*	for(Edge e : scheduleTask.getInEdges()) {
		Task parent = e.getParentTask();
		double parentActualFinishTime = 
		VM parentVM = parent.getAssignedVM();
		if(!parentVM.equals(selectVM)) {
			if(tempMaxTransferData < e.getTransDataSize()) {
				tempMaxTransferData = e.getTransDataSize();
			}
		}
	}*/

	
	//Choose a new VM
	/*if(minCost > 0 || selectVM == null) {
		double startTime = currentTime; //The scheduleTask's start time
		
		//Calculate the predicated start time by scheduleTask' parent
		for(Edge inEdge : scheduleTask.getInEdges()) {
			Task parent = inEdge.getParentTask();
			double maxParentTransTime; //Store the max transfer data time between scheduleTask and its parent
			if(!parent.getIsFinished()) { //Check the parent is finished or not, because a task can be scheduled only all its parent had finished
				throw new IllegalArgumentException("A parent of the schedule Task is not finished!");
			}
			if(parent.getActualFinishTime() > currentTime) {
				maxParentTransTime = parent.getActualFinishTime() + inEdge.getTransDataSizeWithDeviation()/Parameters.bandwidth;
			}
			else {
				maxParentTransTime = currentTime + inEdge.getTransDataSizeWithDeviation()/Parameters.bandwidth;
			}
			
			if(maxParentTransTime > startTime) { //Set the max transfer data time of scheduleTask's parent as the min start time
				startTime = maxParentTransTime; 
			}
		}
		
		
		//Get the vm that has min cost under the subdeadline constraints
		double tempCost = Double.MAX_VALUE;
		int tempType = 0;
		、、double executeTime = scheduleTask.getBaseExecuteTimeWithDeviation();
		double subDeadline = scheduleTask.getSubDeadline();
	
		for(int type=0;type<Parameters.vmTypeNumber-1;type++) {
			double vmFactor = Parameters.speedFactor[type];
			int unitTimes = (int)Math.ceil((executeTime*vmFactor)/Parameters.unitTime);
			if((startTime + executeTime*vmFactor) <= subDeadline) {
				tempType = type;
				tempCost = unitTimes*Parameters.prices[type];
			}
		}
		
		//Lease a new VM with tempType
		if(minCost > tempCost || selectVM == null) {
			selectVM = new VM(tempType, startTime);
			activeVMList.add(selectVM);
			startTimeWithDeviation = startTime;
		}
		
		if(taskList.size()<55 && workflow.getWorkflowName().equals("LIGO-50")) {
				int a =0;
			}
			
		if(temp.getTaskID().equals("ID00000") && temp.getSubDeadline() == -1 ) {
								System.out.println("entry task: " + temp.getTaskID());
			}
		}*/
			   
	    //Unneed--Generate the task's deviate base execution time and deviate transfer data on edge
	   /* for(int i=0;i<experimentWorkflow.size();i++) {
	    	Workflow w = experimentWorkflow.get(i);
	    	List<Task> tList = w.getTaskList();
	    	List<Edge> eList = w.getEdgeList();
	    	for(int j=0;j<tList.size();j++) { //Calculate baseExecuteTimeWithDeviation
	    		Task task = tList.get(j);
	    		double baseExecuteTime = task.getBaseExecuteTime(); //Get the base execute time
	    		double standardDeviation = baseExecuteTime*Parameters.standardDeviation;
	    		double baseExecuteTimeWithDeviation = WorkflowGenerator.NormalDistribution(baseExecuteTime,standardDeviation); //Caculate the deviate execute time
				if(baseExecuteTimeWithDeviation < 0) {
					baseExecuteTimeWithDeviation = baseExecuteTime;
				}
				task.setBaseExecuteTimeWithDeviation(baseExecuteTimeWithDeviation);
	    	}
	    	for(int j=0;j<eList.size();j++) { //Calculate transDataSizeWithDeviation
	    		Edge edge = eList.get(j);
	    		long transDataSize = edge.getTransDataSize(); //Get the base transfer data size
	    		double standardDeviationTD = transDataSize*Parameters.standardDeviationData;
				long transDataSizeWithDeviation = (long)WorkflowGenerator.NormalDistribution(transDataSize,standardDeviationTD); //Caculate the deviate transfer data
				if(transDataSizeWithDeviation < 0) {
					transDataSizeWithDeviation = transDataSize;
				}
				edge.setTransDataSizeWithDeviation(transDataSizeWithDeviation);
	    	}
	    }*/
	
	/**Calculate the experiment result*/
	/*public void calculateExperimentResult(List<VM> vmList, List<Workflow> workflowList, long totalScheduleTime) {
		java.text.DecimalFormat formatD = new java.text.DecimalFormat("0.00000");
		
		double totalCost = 0; //The total cost of all leased VM, sum of each VM' lease cost
		double totalRU = 0; //The resource utiliazation, radio of total execute time and total lease time
		double totalSuccessRate; //The success rate, radio of the successful workflow's number and all workflow's number
		
		double workflowDeviation;
		double fairness;
		double averageScheduleTimePreTask; //The average schedule time of each task, radio of total schedule time and all task's number
		
		//Calculate the resource utilization
		double totalExecuteTime = 0; //Store the total execute time of a vm
		double totalLeaseTime = 0; //Store total lease time of all VMs
		double leaseVMRU[] = new double [vmList.size()]; //Store the resource utilization of each leased VM
		double sumOfEachVMRU = 0; //Store the sum of each VM's resource utilization
		int vmIndex = 0;
		for(VM v : vmList) {
			totalCost = totalCost + v.getTotalLeaseCost(); //Store the total cost of a vm
			
			double executeTime = 0;
			for(Task t : v.getTaskList()) {
				executeTime = t.getActualExecuteTime();
			}
			totalExecuteTime = totalExecuteTime + executeTime; 
			
			double leaseTime = v.getLeaseFinishTime() - v.getLeaseStartTime();
			totalLeaseTime = totalLeaseTime + leaseTime;
			
			double vmRU = 0; //Store the resource utilization of a vm
			if(leaseTime > 0) {
				vmRU = executeTime/leaseTime;
			}
			leaseVMRU[vmIndex] = vmRU;
			sumOfEachVMRU = sumOfEachVMRU + vmRU;
			vmIndex++;
		}
		totalRU = (double) totalExecuteTime/totalLeaseTime; //Calculate the whole resource utilization of all VMs
		
		//Calculate the fairness
		double averageRU = sumOfEachVMRU/vmList.size(); //Average resource utilization
		double squareSum = 0;
		for(double tempRU: leaseVMRU) {
			squareSum = squareSum +  Math.sqrt((tempRU-averageRU)*(tempRU-averageRU));
		}
		double RUStandardDevition = squareSum/(vmList.size() - 1); //Variance of resource utilization
		fairness = (double) 1/RUStandardDevition; //fairness
		
		//Calculate time violation
		int successWorkflowNum = 0;
		workflowDeviation = 0;
		int totalTaskCount = 0; //Record the number of tasks
		for(Workflow tempWorkflow: workflowList) {
			double maxActualFinishTime = 0;
			double maxLatestFinishTime = 0;
			double minActualStartTime = Integer.MAX_VALUE;
			double minLatestStartTime = Integer.MAX_VALUE;
			boolean successFlag = false;
			String workflowName = tempWorkflow.getWorkflowName();
			String taskID = "init";
			for(Task task: tempWorkflow.getTaskList()) {
				if(task.getActualFinishTime()>task.getLatestFinishTime()) {
					taskID = task.getTaskID();
				}
				if(minActualStartTime > task.getActualStartTime()) {
					minActualStartTime = task.getActualStartTime();
				}
				if(minLatestStartTime > task.getLatestStartTime()) {
					minLatestStartTime = task.getLatestStartTime();
				}
				if(maxActualFinishTime < task.getActualFinishTime()) {
					maxActualFinishTime = task.getActualFinishTime();
				}
				if(maxLatestFinishTime < task.getLatestFinishTime()) {
					maxLatestFinishTime = task.getLatestFinishTime();
				}
				
			}
			
			if(maxActualFinishTime <= tempWorkflow.getDeadline()) {
				successFlag = true;
			}
			
			if(successFlag) {
				successWorkflowNum++;
			}
			else {
				double i =0;
			}
			
			workflowDeviation = workflowDeviation + (double)Math.abs((maxLatestFinishTime - maxActualFinishTime))/tempWorkflow.getMakespan();	
			
			totalTaskCount = totalTaskCount + tempWorkflow.getTaskList().size();
		}
		workflowDeviation = (double) workflowDeviation/workflowList.size();
		
		//The success rate
		totalSuccessRate = (double) successWorkflowNum/workflowList.size();
		
		//The scheduling time per task
		averageScheduleTimePreTask = (double)totalScheduleTime/totalTaskCount;
		
		//Record the results	
		ExperimentResult.totalCost = totalCost;
		ExperimentResult.resourceUtilization = totalRU;
		ExperimentResult.successRate = totalSuccessRate;
		
		ExperimentResult.deviation = workflowDeviation;
		ExperimentResult.fairness = fairness;
		ExperimentResult.averageTimePerTask = averageScheduleTimePreTask;
				
		//Output the results
		System.out.println(getName() + "--Total Cost: "+formatD.format(totalCost)+" Resource Utilization: "
		+formatD.format(totalRU)+" Success Rate: "+formatD.format(totalSuccessRate)+ " Deviation: "+formatD.format(workflowDeviation)
		+" Fairness:"+formatD.format(fairness) +" ScheduleTime:"+formatD.format(averageScheduleTimePreTask) );
		
//		System.out.println(getName() + "--Total leaseVM: "+vmList.size() + " Success Rate: " + totalSuccessRate );
			
	}*/
	
	/*
	*//**Record the total cost for on algorithm in one run*//*
	public static double totalCost;
	
	*//**Record the resource utilization for on algorithm in one run*//*
	public static double resourceUtilization;
	
	*//**Record the success rate in one run*//*
	public static double successRate;
	
	*//**Record the deadline deviation for on algorithm in one run*//*
	public static double deadlineDeviation;
	
	*//**Record the fairness for on algorithm in one run*//*
	public static double fairness;
	
	*//**Record the schedule time per workflow task for on algorithm in one run*//*
	public static double averageTimePerTask;*/
	
//	//Ingore the vm when idle time is more than the set value
//	if((activeVMStartTime - tempAvialableTime) > Parameters.maxIdleTime) {
//		continue;
//	}
}
