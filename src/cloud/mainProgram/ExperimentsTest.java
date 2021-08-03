package cloud.mainProgram;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cloud.algorithms.*;
import cloud.components.*;
import cloud.configurations.Parameters;

public class ExperimentsTest {
	private static final Scheduler[] SCHEDULES = { new RMWS()};//Algorithms: new RMWS(),new ROSA(),new NOSF()
	
	public static void main(String[] args)throws Exception {
		Check check = new Check();
		SimpleDateFormat cTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //Current Time
		System.out.println("The experiment start at: " + cTime.format(new Date()));
		//System.out.println();
		
		//Generate the workflow template
		WorkflowTemplate wfTemplate = new WorkflowTemplate();
		wfTemplate.generateTemplate();
		//System.out.println("Workflow Templates are generated at: " + cTime.format(new Date()));
		//System.out.println();
		
		//Generate the workflow set
		WorkflowGenerator wfGenerator = new WorkflowGenerator();
		wfGenerator.generateWorkflow(Parameters.workflowNum);
		
		int repeatNum = 10; //Repeat times for each experiment
		for(int r=0;r<repeatNum;r++) { //Repeat 30 times for each group parameters
			//System.out.println("Workflow Count: "+Parameters.workflowNum+" DeadlineBase: " + Parameters.deadlineBase + " Lamda: " + Parameters.arrivalLamda + " Variance: " + Parameters.standardDeviation + " repeat "+(r+1)+" time start at: " + cTime.format(new Date()));
		
			//System.out.println("THe " + Parameters.workflowNum +" Workflows are generated at: " + cTime.format(new Date()));
			//System.out.println();
			
			//Run each algorithm in SCHEDULES
			for(int i=0;i<SCHEDULES.length;i++){
				//System.out.println(SCHEDULES[i].getName() + " start at: " + cTime.format(new Date()));
				
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
				//System.out.println("THe " + Parameters.workflowNum +" Workflows are readed at: " + cTime.format(new Date()));
				
				//Run the schedule function in the selected algorithm
				Scheduler s = SCHEDULES[i];
				s.schedule(workflowList);
				
				//Get the results 
				
				//System.out.println(SCHEDULES[i].getName() + " finish at: " + cTime.format(new Date()));
				//System.out.println();
			}
			
			//System.out.println("Workflow Count: "+Parameters.workflowNum+" DeadlineBase: " + Parameters.deadlineBase + " Lamda: " + Parameters.arrivalLamda + " Variance: " + Parameters.standardDeviation + " repeat "+(r+1)+" time finish at: " + cTime.format(new Date()));
			System.out.println();
		}
	}

}
