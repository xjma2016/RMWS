package cloud.mainProgram;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cloud.algorithms.*;
import cloud.components.*;
import cloud.configurations.Parameters;

public class Experiments {
	private static int workflowNum; //The workflow's number, 200-2200 by 300
	private static double alft; //The variance of task execution time, 0.1-0.5 by 0.05
	private static double vita; //The variance of data transfer time, 0.1-0.5 by 0.05
	private static double gama; //The deadline factor, 1-6 by 0.5
	
	public static void main(String[] args)throws Exception {
		Check check = new Check();
		SimpleDateFormat cTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //Current Time
		System.out.println("The experiment start at: " + cTime.format(new Date()));
		
		//Generate the workflow template
		WorkflowTemplate wfTemplate = new WorkflowTemplate();
		wfTemplate.generateTemplate();
		System.out.println();
		
		//Test the impact of variance of task execution time, when other parameters is default value
		for(alft=0.1; alft<=0.51; alft=alft+0.05) { //Test the impact of variance of task execution time
			Parameters.standardDeviation = alft;
			Parameters.testParameter = "alft";
			
			//Set the first run flag
			if(alft==0.1) {
				Parameters.isFirstRepeat = true;
			}
			else {
				Parameters.isFirstRepeat = false;
			}
			
			for(int r=0;r<Parameters.totalRepeatNum;r++) { //Repeat 30 times for each group parameters
				//Generate the workflow set
				Parameters.resultFile = "alft_task execution time.txt";
				Parameters.currentRepeatNum = r;
				
				WorkflowGenerator wfGenerator = new WorkflowGenerator();
				wfGenerator.generateWorkflow(Parameters.workflowNum);
				
				System.out.println("Workflow Count: "+Parameters.workflowNum + 
						" alft: " + Parameters.standardDeviation + " vita: " + Parameters.standardDeviationData +
						" Deadline Factor: " + Parameters.deadlineBase + " repeat "+(r+1)+" time start at: " + cTime.format(new Date()));
				
				for(int i=0;i<Parameters.SCHEDULES.length;i++) { //Run each algorithm in SCHEDULES
					Parameters.currentAlgorithm = i;
					
					//Get the workflow list
					List<Workflow> workflowList = new ArrayList<Workflow>(); //Store the workflows used in experiments
					//***FileInputStream fi = new FileInputStream("generatedWorkflow.txt"); //Get experimentWorkflow from txt
					ByteArrayInputStream fi = new ByteArrayInputStream(Parameters.experimentWorkflowStream.toByteArray()); //Get experimentWorkflow from stream
					ObjectInputStream si = new ObjectInputStream(fi);
					try {
						while(workflowList.size()<Parameters.workflowNum) {
							Workflow readWorkflow = (Workflow)si.readObject(); 
							workflowList.add(readWorkflow);
							check.checkWorkflow(readWorkflow);
						}
						si.close(); 
					}
					catch(IOException e) {
						System.out.println(e.getMessage());
					}
					
					//Run the schedule function in the selected algorithm
					Scheduler s = Parameters.SCHEDULES[i];
					s.schedule(workflowList);
					
				} //End run each algorithm in SCHEDULES
				
				System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------");
			} //End repeat
		} //End test the impact of variance of task execution time
		Parameters.standardDeviation = 0.2; //Set the alft as default value
		
		//Test the impact of variance of data transfer time, when other parameters is default value
		for(vita=0.1; vita<=0.51; vita=vita+0.05) { //Test the impact of variance of data transfer time
			Parameters.standardDeviationData = vita;
			Parameters.testParameter = "vita";
			
			//Set the first run flag
			if(vita==0.1) {
				Parameters.isFirstRepeat = true;
			}
			else {
				Parameters.isFirstRepeat = false;
			}
			
			for(int r=0;r<Parameters.totalRepeatNum;r++) { //Repeat 30 times for each group parameters
				//Generate the workflow set
				Parameters.resultFile = "vita_data transfer time.txt";
				Parameters.currentRepeatNum = r;
				
				WorkflowGenerator wfGenerator = new WorkflowGenerator();
				wfGenerator.generateWorkflow(Parameters.workflowNum);
				
				System.out.println("Workflow Count: "+Parameters.workflowNum + 
						" alft: " + Parameters.standardDeviation + " vita: " + Parameters.standardDeviationData +
						" Deadline Factor: " + Parameters.deadlineBase + " repeat "+(r+1)+" time start at: " + cTime.format(new Date()));
				
				for(int i=0;i<Parameters.SCHEDULES.length;i++) { //Run each algorithm in SCHEDULES
					Parameters.currentAlgorithm = i;
					
					//Get the workflow list
					List<Workflow> workflowList = new ArrayList<Workflow>(); //Store the workflows used in experiments
					//***FileInputStream fi = new FileInputStream("generatedWorkflow.txt"); //Get experimentWorkflow from txt
					ByteArrayInputStream fi = new ByteArrayInputStream(Parameters.experimentWorkflowStream.toByteArray()); //Get experimentWorkflow from stream
					ObjectInputStream si = new ObjectInputStream(fi);
					try {
						while(workflowList.size()<Parameters.workflowNum) {
							Workflow readWorkflow = (Workflow)si.readObject(); 
							workflowList.add(readWorkflow);
							check.checkWorkflow(readWorkflow);
						}
						si.close(); 
					}
					catch(IOException e) {
						System.out.println(e.getMessage());
					}
					
					//Run the schedule function in the selected algorithm
					Scheduler s = Parameters.SCHEDULES[i];
					s.schedule(workflowList);
					
				} //End run each algorithm in SCHEDULES
				
				System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------");
			} //End repeat
		} //End test the impact of variance of data trandfer time
		Parameters.standardDeviationData = 0.2; //Set the vita as default value
		
		//Test the impact of variance of deadline factor, when other parameters is default value
		for(gama=1; gama<=8.01; gama=gama+1) { //Test the impact of variance of deadline factor
			Parameters.deadlineBase = gama;
			Parameters.testParameter = "gama";
			
			//Set the first run flag
			if(gama==1) {
				Parameters.isFirstRepeat = true;
			}
			else {
				Parameters.isFirstRepeat = false;
			}
			
			for(int r=0;r<Parameters.totalRepeatNum;r++) { //Repeat 30 times for each group parameters
				//Generate the workflow set
				Parameters.resultFile = "gama_deadline factor.txt";
				Parameters.currentRepeatNum = r;
				
				WorkflowGenerator wfGenerator = new WorkflowGenerator();
				wfGenerator.generateWorkflow(Parameters.workflowNum);
				
				System.out.println("Workflow Count: "+Parameters.workflowNum + 
						" alft: " + Parameters.standardDeviation + " vita: " + Parameters.standardDeviationData +
						" Deadline Factor: " + Parameters.deadlineBase + " repeat "+(r+1)+" time start at: " + cTime.format(new Date()));
				
				for(int i=0;i<Parameters.SCHEDULES.length;i++) { //Run each algorithm in SCHEDULES
					Parameters.currentAlgorithm = i;
					
					//Get the workflow list
					List<Workflow> workflowList = new ArrayList<Workflow>(); //Store the workflows used in experiments
					//***FileInputStream fi = new FileInputStream("generatedWorkflow.txt"); //Get experimentWorkflow from txt
					ByteArrayInputStream fi = new ByteArrayInputStream(Parameters.experimentWorkflowStream.toByteArray()); //Get experimentWorkflow from stream
					ObjectInputStream si = new ObjectInputStream(fi);
					try {
						while(workflowList.size()<Parameters.workflowNum) {
							Workflow readWorkflow = (Workflow)si.readObject(); 
							workflowList.add(readWorkflow);
							check.checkWorkflow(readWorkflow);
						}
						si.close(); 
					}
					catch(IOException e) {
						System.out.println(e.getMessage());
					}
					
					//Run the schedule function in the selected algorithm
					Scheduler s = Parameters.SCHEDULES[i];
					s.schedule(workflowList);
					
				} //End run each algorithm in SCHEDULES
				
				System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------");
			} //End repeat
		} //End test the impact of variance of deadline factor
		Parameters.deadlineBase = 4; //Set the vita as default value		
		
		//Test the impact of variance of workflow count, when other parameters is default value
		int[] wfNumGroup = {30,50,100,200,500}; //The workflow count group30,50,100,200,500,
		for(int workflowNum : wfNumGroup) { //Test the impact of variance of workflow count
			Parameters.workflowNum = workflowNum;
			Parameters.testParameter = "workflowNum";
			
			//Set the first run flag
			if(workflowNum==30) {
				Parameters.isFirstRepeat = true;
			}
			else {
				Parameters.isFirstRepeat = false;
			}
			
			for(int r=0;r<Parameters.totalRepeatNum;r++) { //Repeat 30 times for each group parameters
				//Generate the workflow set
				Parameters.resultFile = "workflowNum_workflow number.txt";
				Parameters.currentRepeatNum = r;
				
				WorkflowGenerator wfGenerator = new WorkflowGenerator();
				wfGenerator.generateWorkflow(Parameters.workflowNum);
				
				System.out.println("Workflow Count: "+Parameters.workflowNum + 
						" alft: " + Parameters.standardDeviation + " vita: " + Parameters.standardDeviationData +
						" Deadline Factor: " + Parameters.deadlineBase + " repeat "+(r+1)+" time start at: " + cTime.format(new Date()));
				
				for(int i=0;i<Parameters.SCHEDULES.length;i++) { //Run each algorithm in SCHEDULES
					Parameters.currentAlgorithm = i;
					
					//Get the workflow list
					List<Workflow> workflowList = new ArrayList<Workflow>(); //Store the workflows used in experiments
					//***FileInputStream fi = new FileInputStream("generatedWorkflow.txt"); //Get experimentWorkflow from txt
					ByteArrayInputStream fi = new ByteArrayInputStream(Parameters.experimentWorkflowStream.toByteArray()); //Get experimentWorkflow from stream
					ObjectInputStream si = new ObjectInputStream(fi);
					try {
						while(workflowList.size()<Parameters.workflowNum) {
							Workflow readWorkflow = (Workflow)si.readObject(); 
							workflowList.add(readWorkflow);
							check.checkWorkflow(readWorkflow);
						}
						si.close(); 
					}
					catch(IOException e) {
						System.out.println(e.getMessage());
					}
					
					//Run the schedule function in the selected algorithm
					Scheduler s = Parameters.SCHEDULES[i];
					s.schedule(workflowList);
					
				} //End run each algorithm in SCHEDULES
				
				System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------");
			} //End repeat
		} //End test the impact of variance of workflow count	

    } //End main

}
