package cloud.components;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import cloud.configurations.Parameters;

public class WorkflowGenerator {
	/**On the basis of the real-world workflow templates, the set of workflows are generated
	 * @return */
	public void generateWorkflow(int workflowNum) throws IOException, ClassNotFoundException {
				
		//Read the workflow template from "workflowTemplate.txt"
		List<Workflow> wtList = new ArrayList<Workflow>(); //Store the 12 workflow template
		FileInputStream fi = new FileInputStream("workflowTemplate.txt");
		ObjectInputStream si = new ObjectInputStream(fi);
		try {
			for(int i=0;i<Parameters.templateNum;i++) {
				Workflow readWorkflow = (Workflow)si.readObject(); 
				wtList.add(readWorkflow);
			}
			si.close(); 
		}
		catch(IOException e) {
			System.out.println(e.getMessage());
		}
		
		//Generate the workflow set and write it into Parameters.ByteArrayOutputStream //"generatedWorkflow.txt"
		List<Workflow> workflowList = new ArrayList<Workflow>(); //The set of generated workflows
		int arrivalTime = 0; //The arrive time of each workflow
		int workflowID = 0; //The ID used in task and workflow
		while(workflowList.size()<workflowNum) { 
			int gtNum = PoissValue(Parameters.arrivalLamda); //Get the number of workflows generated in this arrive time
			if(gtNum == 0) {
				arrivalTime++; //Go to next second
				continue;
			}
			else { //The generate number is not zero
				
				//Generate gtNum workflows by copy the tasks in template
				for(int i=0;i<gtNum;i++) {
					int wkTemplateNum = (int)(Math.random()*Parameters.templateNum); //Randomly choose a workflow template
					Workflow selectedWorkflow = wtList.get(wkTemplateNum);
					
					List<Task> tempTaskList = new ArrayList<Task>(); //Store the task list of the chosen workflow template
					List<Edge> tempEdgeList = new ArrayList<Edge>(); //Store the edge list of the chosen workflow template
					HashMap<String,Task> tempTask = new HashMap<String,Task>(); //Store the task by taskID
					
					//Copy each task, their inEdges and outEdges by create new task
					for(Task t : selectedWorkflow.getTaskList()) { //Get each task in the chosen workflow template
						
						//Copy the base parameters from t to copyTask
						Task copyTask = new Task(t.getTaskID(),workflowID,t.getBaseExecuteTime());
												
						//Calculate the real execution time
						double standardDeviation =  copyTask.getBaseExecuteTime()*Parameters.standardDeviation;
						double baseExecuteTimeWithDeviation = NormalDistribution(copyTask.getBaseExecuteTime(),standardDeviation);
						if(baseExecuteTimeWithDeviation < 0 || baseExecuteTimeWithDeviation < copyTask.getBaseExecuteTime()) {
							baseExecuteTimeWithDeviation = copyTask.getBaseExecuteTime();
						}
						copyTask.setBaseExecuteTimeWithDeviation(baseExecuteTimeWithDeviation);
						
						tempTaskList.add(copyTask);
						tempTask.put(copyTask.getTaskID(), copyTask);
					}
					
					//Copy each edge by create new edge
					for(Edge e : selectedWorkflow.getEdgeList()) {
						Task parent = tempTask.get(e.getParentTask().getTaskID()); //Get the temp task from tempTask
						Task child = tempTask.get(e.getChildTask().getTaskID()); //Get the temp task from tempTask
						long transDataSize = e.getTransDataSize(); //Get the base transfer data size
						long standardDeviationTD = (long) (transDataSize*Parameters.standardDeviationData);
						long transDataSizeWithDeviation = (long)NormalDistribution(transDataSize,standardDeviationTD); //Caculate the deviate transfer data
						if(transDataSizeWithDeviation < 0 || transDataSizeWithDeviation < transDataSize) {
							transDataSizeWithDeviation = transDataSize;
						}
						Edge copyEdge = new Edge(parent,child,transDataSize);
						copyEdge.setTransDataSizeWithDeviation(transDataSizeWithDeviation);
						tempEdgeList.add(copyEdge);
					}
					
					//Bind the copyEdge to each copyTask
					for(Task t : selectedWorkflow.getTaskList()) {
						for(Edge e : t.getInEdges()) {
							for(Edge tempE : tempEdgeList) {
								if (e.getParentTask().getTaskID().equals(tempE.getParentTask().getTaskID()) 
										&& e.getChildTask().getTaskID().equals(tempE.getChildTask().getTaskID())) {
									Task copyParent = tempTask.get(tempE.getParentTask().getTaskID());
									copyParent.addOutEdges(tempE);
									Task copyChild  = tempTask.get(tempE.getChildTask().getTaskID());
									copyChild.addInEdges(tempE);
								}
							}
						}
					}
					
					//Copy a new workflow by the chosed workflow template 
					String name = selectedWorkflow.getWorkflowName();
					double makespan = selectedWorkflow.getMakespan();
					double deadline = (double)(arrivalTime + makespan*Parameters.deadlineBase);
					Workflow tempWorkflow = new Workflow(workflowID, name, arrivalTime, makespan, deadline);
					tempWorkflow.setTaskList(tempTaskList);
					tempWorkflow.setEdgeList(tempEdgeList);
					workflowList.add(tempWorkflow);
					
					if(workflowList.size() == workflowNum) { //Stop generate workflow when reach the set number
						break;
					}
					workflowID++;
				} //End for, generate gtNum workflows
				
				if(workflowList.size() == workflowNum) { //Stop generate workflow when reach the set number
					break;
				}
				arrivalTime++; //Go to next second
				
			} //End else, generate gtNum is not zero
		} //End while, generate Parameters.workflowNum workflows
		
		//***FileOutputStream fos = new FileOutputStream("generatedWorkflow.txt");  //Write the workflow list to "generatedWorkflow.txt"
		//***ObjectOutputStream os = new ObjectOutputStream(fos);
		
		Parameters.experimentWorkflowStream  = new ByteArrayOutputStream(); //Write the workflow list to memory object, reducing I/O operation
		ObjectOutputStream os = new ObjectOutputStream(Parameters.experimentWorkflowStream );
		try	{
			for(int i=0; i<workflowList.size(); i++) {
				os.writeObject(workflowList.get(i));
			}
			os.close();
		}
		catch(IOException e){
			System.out.println(e.getMessage());
		}
        
		workflowList.clear();
	}
	
	
	/**Poisson Distribution*/
	public static int PoissValue(double Lamda) {
		int value=0;
		double b=1;
		double c=0;
		c=Math.exp(-Lamda); 
		double u=0;
		do {
			u=Math.random();
			b*=u;
			if(b>=c)
				value++;
			}while(b>=c);
		return value;
	}
	
	/**
	 * Generator for normal distribution
	 * @param average
	 * @param deviance
	 * @return 
	 */
	public static double NormalDistribution(double average,double deviance)
	{
		Random random = new Random();
		double result = average+deviance*random.nextGaussian();
		return result;
	}
	
	/**
	 * Generator for normal distribution
	 * @param average
	 * @param deviance
	 * @return 
	 */
	public static double NormalDistributionOtherWay(double average,double deviance)
	{
		double Pi=3.1415926535;
		double r1=Math.random();
		Math.random();Math.random();Math.random();Math.random();Math.random();
		Math.random();Math.random();
		double r2=Math.random();
		double u=Math.sqrt((-2)*Math.log(r1))*Math.cos(2*Pi*r2);
		double z=average+u*Math.sqrt(deviance);
		return z;
	}
}
