package cloud.components;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Check {
	public static SimpleDateFormat cTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //Current Time
	
	/**Check the task and edge in a workflow*/
	public void checkWorkflow(Workflow workflow) {
		List<Task> taskList = workflow.getTaskList();
		for(int i=0;i<taskList.size();i++) {
			Task task = taskList.get(i);
			if(task.getTaskWorkflowID() == -1 || task.getBaseExecuteTime() == -1 || task.getBaseExecuteTime() == -1 || task.getBaseExecuteTimeWithDeviation() == -1) {
				throw new IllegalArgumentException("There are some mistake at task:" + task.getTaskID() + "in workflow : " + task.getTaskWorkflowID());
			}
		}
		
		List<Edge> edgeList = workflow.getEdgeList();
		for(int i=0;i<edgeList.size();i++) {
			Edge edge = edgeList.get(i);
			if(edge.getParentTask() == null || edge.getChildTask() == null || 
					edge.getTransDataSize() <= 0 || edge.getTransDataSizeWithDeviation() <= 0 ) {
				throw new IllegalArgumentException("There are some mistake at edge:" + edge.getParentTask().getTaskID() + " to " + edge.getChildTask().getTaskID());
			}
		}
	}
	
	/**Check if there has task parameters not calculated*/
	public void checkTaskParameter(List<Task> taskList) {
		int a = 0;
		List<Task> tlist = new ArrayList<Task>();
		for(int j=0;j<taskList.size();j++) {
			Task t = taskList.get(j);
			if(t.getEarliestStartTime() == -1) {
				throw new IllegalArgumentException("Task's EarliestStartTime is not calculate!");
			}
			if(t.getEarliestFinishTime() == -1) {
				throw new IllegalArgumentException("Task's EarliestFinishTime is not calculate!");
			}
			if(t.getLatestFinishTime() == -1) {
				throw new IllegalArgumentException("Task's LatestFinishTime is not calculate!");
			}
			if(t.getSubDeadline() == -1) {
				a++;
				tlist.add(t);
			}
		}
		if(a>0) {
			throw new IllegalArgumentException("Task's SubDeadline is not calculate!" + a);
		}
	}
	
	/**Check if there has unfinished task or VM*/
	public void checkUnfinishTaskAndVM(List<Workflow> workflowList, List<VM> vmList)  {
		for(int m=0;m<workflowList.size();m++) {
			Workflow w = workflowList.get(m);
			List<Task> tt = w.getTaskList();
			
			for(int k=0;k<tt.size();k++) {
				Task t = tt.get(k);
				if(!t.getIsFinished() || !t.getIsAssigned()){
					System.out.println("unSchedule task: " + t.getTaskID()+"---"+t.getTaskWorkflowID()  );
					throw new IllegalArgumentException("There exist a unfinished task in task pool!");
				}
				if(t.getActualStartTime() == -1 || t.getActualExecuteTime() == -1 || t.getActualFinishTime() == -1){
					System.out.println("unSchedule task: " + t.getTaskID()+"---"+t.getTaskWorkflowID()  );
					throw new IllegalArgumentException("There exist a unfinished task in task pool!");
				}
			}
		}
		
		for(VM v: vmList) {
			if(v.getVMStatus()) {
				throw new IllegalArgumentException("There exist a unfinished VM!");
			}
			if(!v.getExecuteTask().getTaskID().equals("init") || !v.getWaitTask().getTaskID().equals("init")) {
				throw new IllegalArgumentException("There exist a execute or wait task on VM!");
			}
			if(v.getLeaseStartTime() == -1 || v.getLeaseFinishTime() == -1) {
				throw new IllegalArgumentException("The lease start/finish time is -1 on VM!");
			}
		}
	}
		
	/**Print the schedule task from all lease VM list*/
	public void printScheduleTask(List<VM> vmList)  {
		for(int k = 0;k<vmList.size();k++){
			VM v = vmList.get(k);
			for(int m = 0;m<v.getTaskList().size();m++){
				Task t = v.getTaskList().get(m);
				System.out.println("VM:  "+v.getVMID()+" type:  "+v.getVMType()+" status:  "+v.getVMStatus()+"  Schedule task: " + t.getTaskID()+"---"+t.getTaskWorkflowID()  );
			}
		}
	}
	
	/**Print text*/
	public void printText(String text)  {
		System.out.println(text + cTime.format(new Date()) );
	}
}
