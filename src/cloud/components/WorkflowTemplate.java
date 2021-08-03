package cloud.components;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import cloud.configurations.Parameters;


public class WorkflowTemplate {
	private static final String[] WORKFLOWTYPE = {"CYBERSHAKE","LIGO","GENOME","SIPHT"}; //The workflow's type "CYBERSHAKE","LIGO","GENOME","SIPHT" "MONTAGE"
	private static final int[] WORKFLOWNUM = {50,200,1000}; //The workflow's number 50,200,1000
	private static final String file_location = "D:\\mxj\\Eclipse_Project\\DataSet\\dax"; //The workflow's DAX files location
	private static final String workflowTemplateFile = "workflowTemplate.txt"; //The workflow template's txt file
		
	private int workflowID = 0; //Use 1-12 indicate the 12 workflow template
	private String workflowName = ""; //Use the workflow's name and task's number indicate different workflows, e.g. "LIGO-50"
	private List<Task> taskList; // Sore all tasks in the workflow
	private List<Edge> edgeList; // Sore all edges in the workflow
	
	private HashMap<String, Task> nameTaskMapping; //Temporary store each task from a DAX file
	private HashMap<Task, List<TransferData>> transferData; //Temporary store the transData from a DAX file
	
	/**Generate workflow template into "workflowTemplate.txt"*/ 
	public  void generateTemplate() throws IOException, ClassNotFoundException {
		List<Workflow> workflowTemplateList = new ArrayList<Workflow>(); //Store the workflow templates
		String file = "";
		
		for(String workflowType : WORKFLOWTYPE) {
			for(int workflowNum : WORKFLOWNUM) {
				taskList = new ArrayList<Task>();
				edgeList = new ArrayList<Edge>();
				nameTaskMapping = new HashMap<String, Task>();
				transferData = new HashMap<Task, List<TransferData>>();
				
				file = file_location + "\\" + workflowType + "\\" + workflowType + ".n." + workflowNum + "." + 1 + ".dax";
				workflowID++;
				workflowName = workflowType + "-" + workflowNum;
							
				//read DAX file
				try {
					SAXParserFactory factory = SAXParserFactory.newInstance();
					SAXParser parser = factory.newSAXParser();
					XMLReader reader = parser.getXMLReader();
					
					System.out.println("Read DAX file: " + file);
					reader.setContentHandler(new workflowFromDAX());
					reader.parse(file);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				
				bindTransferData(); //Bind the transdata to edges between tasks
								
				//Get the max base finish time of task as the workflow's makespan
				double makespan = calculateMakespan(taskList);
				
				Workflow workflowTemplate = new Workflow(workflowID, workflowName, -1, makespan,-1); //The workflow converted from a DAX file
			    workflowTemplate.setTaskList(taskList); //Add task list to workflow template
			    workflowTemplate.setEdgeList(edgeList); //Add edge list to workflow template
				workflowTemplateList.add(workflowTemplate); //Add a workflow template to list
			}
		}
		
		Parameters.templateNum = workflowTemplateList.size();
		
		//Write the workflow templates into txt file
		FileOutputStream fos = new FileOutputStream(workflowTemplateFile);
		ObjectOutputStream os = new ObjectOutputStream(fos);
		try {
			for(int i=0; i<workflowTemplateList.size(); i++) {
				os.writeObject(workflowTemplateList.get(i));
			}
			os.close();
		}
		catch(IOException e) {
			System.out.println(e.getMessage());
		}
		workflowTemplateList.clear();
	}
	
	/**Convert a DAX file to a workflow template*/
	private class workflowFromDAX extends DefaultHandler {
		private Stack<String> tags = new Stack<String>();
		private String childID;
		private Task lastTask;
		public void startElement(String uri, String localName, String qName, Attributes attrs) { //Start parsing a node
			if(qName.equals("job")) {
				String id = attrs.getValue("id");
				if(nameTaskMapping.containsKey(id)) { //Task's ID conflicts
					throw new RuntimeException();
				}
				Task t = new Task(id, workflowID, Double.parseDouble(attrs.getValue("runtime"))); //Get a new task, include ID, workflowID, runtime
				taskList.add(t);
				nameTaskMapping.put(id, t);
				lastTask = t;
			}
			else if(qName.equals("uses") && tags.peek().equals("job")) {//After reading the element "job", the element "uses" means a trasferData (i.e., data flow)
				String filename =attrs.getValue("file");
				long fileSize = Long.parseLong(attrs.getValue("size"));
				String link =attrs.getValue("link");
				TransferData td = new TransferData(filename, fileSize, link);
				List<TransferData> tdList = transferData.get(lastTask); //The list store lasttask's all edges with different file name 
				boolean isNewEdge = true;
				if(tdList == null){
					tdList = new ArrayList<TransferData>();
				}
				else{
					for(int i=0;i<tdList.size();i++) { //Find the edge which has the same filename, size and link
						if(tdList.get(i).getName().equals(filename) && tdList.get(i).getSize() == fileSize 
								&& tdList.get(i).getLink().equals(link)) {
							isNewEdge = false;
						}
					}
				}
			
				if(isNewEdge) {
					tdList.add(td); //Add the new edge into list in transerData
				}
				transferData.put(lastTask, tdList);
			}
			else if(qName.equals("child")) {		
				childID = attrs.getValue("ref");
			}
			else if(qName.equals("parent")) { //After reading the element "child", the element "parent" means control flow
				Task child = nameTaskMapping.get(childID);
				Task parent = nameTaskMapping.get(attrs.getValue("ref"));
					
				Edge edge = new Edge(parent, child, -1); //Edge between parent and child task, set the init transfer data as -1
				edgeList.add(edge); //Add edge into the edge list
				parent.addOutEdges(edge); //Add immediate successors
				child.addInEdges(edge); //Add immediate predecessors
			}
			tags.push(qName);
		}
		public void endElement(String uri, String localName,String qName) {
			tags.pop(); //Pop the qName when end parsing a 'row'
		}
	}
	
	/**Bind transfer data to edges between tasks*/
	private void bindTransferData() {
		for(int i=0;i<edgeList.size();i++) {
			Edge edge = edgeList.get(i); //Get each edge in the workflow
			Task parentTask = edge.getParentTask();
			Task childTask = edge.getChildTask();
			List<TransferData> parentTDList = transferData.get(parentTask); //Get the transfer data list of parent task
			List<TransferData> childTDList = transferData.get(childTask); //Get the transfer data list of child task
			boolean isBinded = false; //Find the matching transfer data or not
			for(int j=0;j<parentTDList.size();j++) {
				TransferData ptd = parentTDList.get(j);
				for(int k=0;k<childTDList.size();k++) {
					TransferData ctd = childTDList.get(k);
					if(ptd.getLink().equals("output") && ctd.getLink().equals("input") && 
							ptd.getName().equals(ctd.getName()) && ptd.getSize() == ctd.getSize()) {
						edge.setTransDataSize(ptd.getSize()); //1.Bind the transfer data with same name and size
						isBinded = true;
					}
				}
			}
			if(!isBinded) { //2.Bind the transfer data with child task's first input data
				for(int j=0;j<childTDList.size();j++) {
					TransferData ctd = childTDList.get(j);
					if(ctd.getLink().equals("input") && ctd.getSize()>0) {
						edge.setTransDataSize(ctd.getSize());
						isBinded = true;
					}
				}
			}
			if(!isBinded) { //3.Bind the transfer data with parent task's first output data
				for(int j=0;j<parentTDList.size();j++) {
					TransferData ptd = parentTDList.get(j);
					if(ptd.getLink().equals("output") && ptd.getSize()>0) {
						edge.setTransDataSize(ptd.getSize());
						isBinded = true;
					}
				}
			}
		}
	}
	
	/**Calculate the makespan of a taskList, used for get the workflow's deadline so ignore the transfer data time*/
	private double calculateMakespan(List<Task> taskList) {
		int fastestVMType = Parameters.speedFactor.length - 1;
		double maxFinishTime = Double.MIN_NORMAL; //Store the max finish time
		HashMap<String,Double> calculatedTask = new HashMap<String,Double>(); //Store the calculated task's name and base finish time,like <taskIDName,baseFinishTime>
		while(calculatedTask.size() < taskList.size()) {
			for(Task t : taskList) {
				if(t.getInEdges().size() == 0) { //The entry task
					double temFinishTime = t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType];
					calculatedTask.put(t.getTaskID(), temFinishTime);
					if(maxFinishTime < temFinishTime) {
						maxFinishTime = temFinishTime;
					}
				}
				else { //The task has parents
					boolean isAllParentCalculated = true; //Check if all parent of task t have been calculated
					
					for(Edge e : t.getInEdges()) { //Find if exist the uncalculate parent of t
						if(!calculatedTask.containsKey(e.getParentTask().getTaskID())) {
							isAllParentCalculated = false;
						}
					}
					
					if(isAllParentCalculated) { //If all parent of task t have been calculated, calculate t' base finish time
						for(Edge e : t.getInEdges()) { //Caculate max start time from each parent
							Task parent = e.getParentTask();
							//double tempTransferTime = e.getTransDataSize()/Parameters.bandwidth; //Get the transfer data time
							double temStartTime = calculatedTask.get(parent.getTaskID()); //Set parent's finish time as t' start time, ignore the tranfer data time
							double temFinishTime = temStartTime + t.getBaseExecuteTime()*Parameters.speedFactor[fastestVMType];
							calculatedTask.put(t.getTaskID(),temFinishTime);
							if(maxFinishTime < temFinishTime) {
								maxFinishTime = temFinishTime;
							}
						}
					}
				} //End else
			} //End for
		} //End while
		
		return maxFinishTime;
	}
	
}
