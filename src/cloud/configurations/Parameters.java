package cloud.configurations;

import java.io.ByteArrayOutputStream;

import cloud.algorithms.*;
import cloud.algorithms.Scheduler;

public class Parameters {
	/**The group of test algorithms*/	
	public static Scheduler[] SCHEDULES = {new ROSA(),new NOSF(),new RMWS()};//Algorithms: new ROSA(),new NOSF(), new RMWS()

	/**The file location of the workflow dataset*/	
	public static String file_location =  "..\\RMWS\\DataSet"; 

	/**The serialize stream store the experiment workflow list*/
	public static ByteArrayOutputStream experimentWorkflowStream;
	
	/**The number of the workflow template*/	
	public static int templateNum = 12; //12

	/**The arrival rate*/
	public static double arrivalLamda = 0.2;
	
	/**The fix number of the workflows*/	
	public static int workflowNum = 100; 
	
	/**The fix deadline factor of the workflow*/
	public static double deadlineBase = 4; 
	
	/**The fix variance of task execution time*/
	public static double standardDeviation = 0.2;
	
	/**The fix variance of data transfer time*/
	public static double standardDeviationData = 0.2; 
	
	/**The theta used in calculate the probabilistic upward rank of task*/
	public static double theta = 1.5; 
	
	/**The bandwidth between VMs*/
	public static double bandwidth = 1000000;
	
	/**The interval time for calculate the lease cost*/
	public static double unitTime = 3600;
	
	/**The launch time for lease a VM*/
	public static double launchTime = 97;
	
	/**The number of VM' type*/
	public static int vmTypeNumber = 8;
	
	/**The price group of each VM type*/
	public static double[] prices = {0.023,0.0464,0.1,0.2,0.4,0.8,2.0,3.2}; 
	
	/**The weight factor F(k) of each VM type, use factor*baseExecuteTime as the task's execute time on a VM type*/
	public static double[] speedFactor = {3, 2.7, 2.5, 2.2, 1.9, 1.6, 1.3, 1}; 
	
	/**The repeat times of an algorithm for one condition*/
	public static int totalRepeatNum = 10;
	
	/**The output result files location*/
	public static String Result_file_location = "..\\RMWS\\Result\\"; 
	
	/**The result file name*/
	public static String resultFile = "alft_task execution time.txt";
	
	/**The current algorithm number, 0 ROSA, 1 NOSF, 2 RMWS*/
	public static int currentAlgorithm = 0; 
	
	/**The current repeat flag, set as true when first run a group of parameters*/
	public static boolean isFirstRepeat = false;
	
	/**The current repeat number, 0-29*/
	public static int currentRepeatNum = 0; 
	
	/**The current test parameter, alft(task execution time), vita(data transfer time), gama(deadline factor)*/
	public static String testParameter = "alft"; 
	
	/**The total cost result array*/
	public static double costResult[][]= new double [SCHEDULES.length][totalRepeatNum]; 

	/**The resource utilization result array*/
	public static double ruResult[][]= new double [SCHEDULES.length][totalRepeatNum]; 

	/**The success rate result array*/
	public static double srResult[][]= new double [SCHEDULES.length][totalRepeatNum]; 

	/**The deadline deviation result array*/
	public static double ddResult[][]= new double [SCHEDULES.length][totalRepeatNum]; 
	
	/**The schedule time result array*/
	public static double stResult[][]= new double [SCHEDULES.length][totalRepeatNum]; 
	
}
