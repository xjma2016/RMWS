# Code of Real-time Multiple-Workflow Scheduling in Cloud Environments
-------------

#### Overview:
RMWS is used for the real-time scheduling of multiple workflows in Cloud, and this code is simulated based on the actual application scenarios.In the simulator, tasks of different random arrival workflows can be assigned to different VMs by the scheduler in real time. The simulator compares the performance of RMWS and the other two scheduling algorithms in terms of rental cost, resource utilization, success rate and deadline deviation under different conditions.

#### Directory structure:  
 1.cloud.algorithm: Different scheduling algorithms and the Scheduler interface;
 2.cloud.components:The components used in the workflow scheduling;
 3.cloud.configurations: Parameter configuration used to set the experiment parameters such as the workflow number, DAX files location, VM type, and so on;
 4.cloud.mainProgram: Start the simulator. 
 5.DataSet: the input dax files which can be download from "https://confluence.pegasus.isi.edu/display/pegasus/WorkflowHub" ;
 6.Results: the ouput results txt files.

### Operation steps of the simulator:
 1.Set the java compiler to 1.7 after importing the project into eclipse;
 2.Set the related parameters in cloud.configurations.Parameters.java, mainly including: file_location, workflowTemplateFile, Result_file_location;
 3.Run the simulator in cloud.mainProgram.Experiments.java, and the experimental results can be found in the result file after finishing the program.

### Others:
More details can be found in the corresponding code comments.
