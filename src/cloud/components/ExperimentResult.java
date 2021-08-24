package cloud.components;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;

import cloud.configurations.Parameters;

public class ExperimentResult {
	/**Calculate the experiment result*/
	public static void calculateExperimentResult(String name, List<VM> vmList, List<Workflow> workflowList, long totalScheduleTime) {
		java.text.DecimalFormat formatD = new java.text.DecimalFormat("0.00000");
		
		double totalCost = 0; //The total cost of all leased VM, sum of each VM' lease cost
		double totalResourceUtiliazation = 0; //The resource utilization, radio of total execute time and total lease time
		double totalSuccessRate; //The success rate, radio of the successful workflow's number and all workflow's number
		double deadlineDeviation; //The average dealine deviation, radio of the total deadline deviation and all workflow's number
		double averageScheduleTimePreTask; //The average schedule time of each task, radio of total schedule time and all task's number
		
		//Calculate the total cost and resource utilization
		double totalExecuteTime = 0; //Store the total execute time of a VM
		double totalLeaseTime = 0; //Store total lease time of all VMs
		double leaseVMRU[] = new double [vmList.size()]; //Store the resource utilization of each leased VM
		double sumOfEachVMRU = 0; //Store the sum of each VM's resource utilization
		int vmIndex = 0;
		for(VM v : vmList) {
			totalCost = totalCost + v.getTotalLeaseCost(); //Add the total cost of VMs
			
			double executeTime = 0;
			for(Task t : v.getTaskList()) {
				executeTime = executeTime + t.getActualExecuteTime();
			}
			totalExecuteTime = totalExecuteTime + executeTime; //Add the total execute time of VMs
			
			double leaseTime = v.getLeaseFinishTime() - v.getLeaseStartTime();
			totalLeaseTime = totalLeaseTime + leaseTime; //Add the total lease time of VMs
			
			double vmRU = 0; //Store the resource utilization of a VM
			if(leaseTime > 0) {
				vmRU = executeTime/leaseTime;
			}
			leaseVMRU[vmIndex] = vmRU;
			sumOfEachVMRU = sumOfEachVMRU + vmRU; //Add the resource utilization of VMs
			vmIndex++;
		}
		totalResourceUtiliazation = (double) totalExecuteTime/totalLeaseTime; //Calculate the whole resource utilization of all VMs
		if(totalResourceUtiliazation > 1) {
			totalResourceUtiliazation = 0; 
		}
		
		//Calculate the success rate, time violation and average schedule time
		double totalDeadlineViolation = 0; //Store the total dealine violation
		int successWorkflowNum = 0;
		int totalTaskCount = 0; //Store the number of tasks
		for(Workflow tempWorkflow: workflowList) {
			double minActualStartTime = Integer.MAX_VALUE;
			double maxActualFinishTime = Integer.MIN_VALUE;
			boolean successFlag = false;
			for(Task task: tempWorkflow.getTaskList()) {
				if(minActualStartTime > task.getActualStartTime()) {
					minActualStartTime = task.getActualStartTime();
				}
				if(maxActualFinishTime < task.getActualFinishTime()) {
					maxActualFinishTime = task.getActualFinishTime();
				}
			}
			
			if(maxActualFinishTime <= tempWorkflow.getDeadline()) {
				successFlag = true;
			}
			
			if(successFlag) {
				successWorkflowNum++;
			}
			totalTaskCount = totalTaskCount + tempWorkflow.getTaskList().size();
			double tempDeadlineArriveTime = tempWorkflow.getDeadline()-tempWorkflow.getArrivalTime();
			double tempDeadlineDeviation = ((maxActualFinishTime - minActualStartTime) + tempWorkflow.getArrivalTime() - tempWorkflow.getDeadline())/tempDeadlineArriveTime;
			totalDeadlineViolation = totalDeadlineViolation + tempDeadlineDeviation;
		}
		
		totalSuccessRate = (double) successWorkflowNum/workflowList.size(); //The success rate
		deadlineDeviation = (double) totalDeadlineViolation/workflowList.size(); //The deadline violate
		averageScheduleTimePreTask = (double)totalScheduleTime/totalTaskCount; //The scheduling time per task
		
		//Output the results
		System.out.println(name + "--Total Cost: "+formatD.format(totalCost)+" Resource Utilization: "
		+formatD.format(totalResourceUtiliazation)+" Success Rate: "+formatD.format(totalSuccessRate)+ " Deadline Deviation: "+formatD.format(deadlineDeviation)
		+" ScheduleTime:"+formatD.format(averageScheduleTimePreTask) );
		
		//Record the results to array
		double currentParameterValue = 0;
		switch (Parameters.testParameter) {
		case "alft":
			currentParameterValue = Parameters.standardDeviation;
		    break;
		case "vita":
			currentParameterValue = Parameters.standardDeviationData;
			break;
		case "gama":
			currentParameterValue = Parameters.deadlineBase;
			break;
		case "workflowNum":
			currentParameterValue = Parameters.workflowNum;
			break;
		default:
		    break;
		}
		Parameters.costResult[Parameters.currentAlgorithm][Parameters.currentRepeatNum] = totalCost;
		Parameters.ruResult[Parameters.currentAlgorithm][Parameters.currentRepeatNum] = totalResourceUtiliazation;
		Parameters.srResult[Parameters.currentAlgorithm][Parameters.currentRepeatNum] = totalSuccessRate;
		Parameters.ddResult[Parameters.currentAlgorithm][Parameters.currentRepeatNum] = deadlineDeviation;
		Parameters.stResult[Parameters.currentAlgorithm][Parameters.currentRepeatNum] = averageScheduleTimePreTask;
			
		String resultTxt = Parameters.Result_file_location + Parameters.resultFile;
		
		//Add the title to new result txt when conducting a new parameter experiment
		if(Parameters.currentRepeatNum == 0 && Parameters.currentAlgorithm == 0 && Parameters.isFirstRepeat) {
			try	{
				FileOutputStream fos = new FileOutputStream(resultTxt);  //Write the result to txt file
				OutputStreamWriter os = new OutputStreamWriter(fos,"utf-8");
				String parameter = "Workflow Count: "+Parameters.workflowNum + 
						" alft_Task execution time : " + Parameters.standardDeviation + " vita_Data transfer time: " + Parameters.standardDeviationData +
						" gama_Deadline Factor: " + Parameters.deadlineBase;
				String title = "Repeat"+ "	"+ Parameters.testParameter+ "	"+ "Name"+ "	"+ "totalCost"+ "	"+ "               totalRU"+ "	"
				+ "totalSR"+ "	"+ "   DD"+ "	"+ "  ST";
				os.append(parameter+"\r\n");
				os.append(title+"\r\n");
				os.flush();
				os.close();
			}
			catch(IOException e){
				System.out.println(e.getMessage());
			}
		}
		
		//Add each repeat result into result txt
		String resultString = Parameters.currentRepeatNum+ "	"+currentParameterValue+ "	"+name + "	"+formatD.format(totalCost)+"	"
				+formatD.format(totalResourceUtiliazation)+"	"+formatD.format(totalSuccessRate)+ "	"+formatD.format(deadlineDeviation)
				+"	"+formatD.format(averageScheduleTimePreTask) ;
		try	{
			FileOutputStream fos = new FileOutputStream(resultTxt,true);  //Write the result into txt file
			OutputStreamWriter os = new OutputStreamWriter(fos,"utf-8");
			os.append(resultString+"\r\n");
			os.flush();
			os.close();
		}
		catch(IOException e){
			System.out.println(e.getMessage());
		}
		
		//Calculate the average result into result txt when repeat times equals the max value and run the last algorithm
		if(Parameters.currentRepeatNum == Parameters.totalRepeatNum - 1 && Parameters.currentAlgorithm == Parameters.SCHEDULES.length-1) {
			for(int i=0;i<Parameters.SCHEDULES.length;i++) {
				double totalCostValue =0;
				double totalRUValue =0;
				double totalSRValue =0;
				double totalDDValue =0;
				double totalSTValue =0;
				
				double averageCost;
				double averageRU;
				double averageSR;
				double averageDD;
				double averageST;
				
				for(int j=0;j<Parameters.totalRepeatNum;j++) {
					totalCostValue = totalCostValue + Parameters.costResult[i][j];
					totalRUValue = totalRUValue + Parameters.ruResult[i][j];
					totalSRValue = totalSRValue + Parameters.srResult[i][j];
					totalDDValue = totalDDValue + Parameters.ddResult[i][j];
					totalSTValue = totalSTValue + Parameters.stResult[i][j];
				}
				averageCost = totalCostValue/Parameters.totalRepeatNum ;
				averageRU = (double) totalRUValue/Parameters.totalRepeatNum ;
				averageSR = (double) totalSRValue/Parameters.totalRepeatNum ;
				averageDD = (double) totalDDValue/Parameters.totalRepeatNum ;
				averageST = (double) totalSTValue/Parameters.totalRepeatNum ;
				
				resultString = "Average:" + "	"+currentParameterValue+ "	" +Parameters.SCHEDULES[i].getName() + "	"+formatD.format(averageCost)+"	"
						+formatD.format(averageRU)+"	"+formatD.format(averageSR)+ "	"+formatD.format(averageDD)
						+"	"+formatD.format(averageST);
				
				try	{
					FileOutputStream fos = new FileOutputStream(resultTxt,true);  //Write the result to txt file
					OutputStreamWriter os = new OutputStreamWriter(fos,"utf-8");
					os.append(resultString+"\r\n");
					os.flush();
					os.close();
				}
				catch(IOException e){
					System.out.println(e.getMessage());
				}
			}
		}
	}

	/**Calculate total used VM number of RMWS*/
	public static void calculateActiveVMNum(String name, HashMap<Double,Integer> activeVMNumList) {
		java.text.DecimalFormat formatD = new java.text.DecimalFormat("0.00000");
		
		//Output the results
		String resultTxt = Parameters.Result_file_location + "RMWS_VMNum.txt";
		
		//Add the title to new result txt when conducting a new parameter experiment
		if(Parameters.currentAlgorithm == 2 && Parameters.testParameter == "alft" && Parameters.standardDeviation == 0.1) {
			if(Parameters.currentRepeatNum == 0) {
				try	{
					FileOutputStream fos = new FileOutputStream(resultTxt);  //Write the result to txt file
					OutputStreamWriter os = new OutputStreamWriter(fos,"utf-8");
					String parameter = "Workflow Count: "+Parameters.workflowNum + 
							" alft_Task execution time : " + Parameters.standardDeviation + " vita_Data transfer time: " + Parameters.standardDeviationData +
							" gama_Deadline Factor: " + Parameters.deadlineBase;
					String title = "Repeat"+ "	"+ Parameters.testParameter+ "	"+ "Name"+ "	"+ "currentTime"+ "	"+ "               VMNum";
					os.append(parameter+"\r\n");
					os.append(title+"\r\n");
					os.flush();
					os.close();
				}
				catch(IOException e){
					System.out.println(e.getMessage());
				}
			}
			
			//Add each repeat result into txt 
			for(double key:activeVMNumList.keySet()) {
				String resultString = Parameters.currentRepeatNum+ "	"+Parameters.standardDeviation+ "	"+name + "	"+formatD.format(key)+"	"
						+formatD.format(activeVMNumList.get(key)) ;
				try	{
					FileOutputStream fos = new FileOutputStream(resultTxt,true);  //Write the result to txt file
					OutputStreamWriter os = new OutputStreamWriter(fos,"utf-8");
					os.append(resultString+"\r\n");
					os.flush();
					os.close();
				}
				catch(IOException e){
					System.out.println(e.getMessage());
				}
			}
		}
	}
	
}
