package cloud.configurations;

import java.io.ByteArrayOutputStream;

import cloud.algorithms.NOSF;
import cloud.algorithms.RMWS;
import cloud.algorithms.ROSA;
import cloud.algorithms.Scheduler;

public class Parameters {
	/**The group of algorithms*/	
	public static Scheduler[] SCHEDULES = {new ROSA(),new NOSF(),new RMWS()};//Algorithms: new ROSA(),new NOSF(), new RMWS()
	
	/**The number of the workflow template*/	
	public static int templateNum = 12; //12
	
	/**The number of the workflows*/	
	public static int workflowNum = 100; //800
	
	/**The serialize stream store the experiment workflow list*/
	public static ByteArrayOutputStream experimentWorkflowStream;

	/**The arrival rate*/
	public static double arrivalLamda = 0.2;
	
	/**The deadline factor of the workflow*/
	public static double deadlineBase = 4; //4.0
	
	/**The variance of task execution time*/
	public static double standardDeviation = 0.2;
	
	/**The variance of data transfer time*/
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
	
	/**The price of each VM type*/
	public static double[] prices = {0.023,0.0464,0.1,0.2,0.4,0.8,2.0,3.2}; //The price group of all VM's type
	
	/**The speed factor of each VM type, use factor*baseExecuteTime as the task's execute time on a VM type*/
	public static double[] speedFactor = {3, 2.7, 2.5, 2.2, 1.9, 1.6, 1.3, 1}; //The speed factor group of all VM's type
	
/*	*//**The maximal allowable idle time for VM*//*
	public static int maxIdleTime = 2200;
	*/
	
	/**The repeat number of an algorithm for one condition*/
	public static int totalRepeatNum = 10; //Repeat times for each experiment
	
	/**The result files location*/
	public static String Result_file_location = "D:\\mxj\\Eclipse_Project\\OutPut\\RMWS_Result\\"; 
	
	/**The result file name*/
	public static String resultFile = "alft_task execution time.txt";
	
	/**The current algorithm number*/
	public static int currentAlgorithm = 0; //0 ROSA, 1 NOSF, 2 RMWS
	
	/**The current repeat flag*/
	public static boolean isFirstRepeat = false; //Set as true when first run a group of parameters

	/**The current repeat number*/
	public static int currentRepeatNum = 0; //0-29
	
	/**The current test parameter*/
	public static String testParameter = "alft"; //alft(task execution time), vita(data transfer time), gama(deadline factor)
	
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
