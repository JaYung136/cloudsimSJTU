/**
 * Copyright 2019-2020 ArmanRiazi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.com.wfc.cloudsim/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.wfc.cloudsim.wfc.service;

import java.io.File;
import java.lang.management.*;
import java.text.DecimalFormat;
import java.util.*;
import com.wfc.cloudsim.cloudsim.container.core.*;
import com.wfc.cloudsim.cloudsim.container.hostSelectionPolicies.*;
import com.wfc.cloudsim.cloudsim.container.resourceAllocatorMigrationEnabled.*;
import com.wfc.cloudsim.cloudsim.container.schedulers.*;
import com.wfc.cloudsim.cloudsim.container.utils.IDs;
import com.wfc.cloudsim.cloudsim.container.podSelectionPolicies.*;
import com.wfc.cloudsim.cloudsim.Cloudlet;
import com.wfc.cloudsim.cloudsim.HarddriveStorage;
import com.wfc.cloudsim.cloudsim.Log;
import com.wfc.cloudsim.cloudsim.Storage;
import com.wfc.cloudsim.cloudsim.container.resourceAllocators.*;
import com.wfc.cloudsim.cloudsim.container.containerProvisioners.*;
import com.wfc.cloudsim.cloudsim.container.containerPodProvisioners.*;
import com.wfc.cloudsim.cloudsim.core.CloudSim;
import com.wfc.cloudsim.wfc.core.*;
import com.wfc.cloudsim.workflowsim.*;
import com.wfc.cloudsim.workflowsim.utils.*;
import com.wfc.cloudsim.cloudsim.util.Conversion;
import com.wfc.cloudsim.workflowsim.failure.*;
import com.wfc.cloudsim.workflowsim.utils.DistributionGenerator;
import com.wfc.cloudsim.workflowsim.utils.Parameters.ClassType;
import org.springframework.stereotype.Service;
/*
 * @author Arman Riazi
 * @since WFC Toolkit 1.0
 * @date March 29, 2020
 */
/*
ConstantsExamples.WFC_DC_SCHEDULING_INTERVAL+ 0.1D

On Pod (
    Allocation= PowerContainerPodAllocationPolicyMigrationAbstractHostSelection
    Scheduler = ContainerPodSchedulerTimeSharedOverSubscription
    SelectionPolicy = PowerContainerPodSelectionPolicyMaximumUsage
    Pe = CotainerPeProvisionerSimple
    Overhead = 0
    ClusteringMethod.NONE
    SchedulingAlgorithm.MINMIN
    PlanningAlgorithm.INVALID
    FileSystem.LOCAL
)

On Host (
    Scheduler = ContainerPodSchedulerTimeSharedOverSubscription
    SelectionPolicy = HostSelectionPolicyFirstFit
    Pe = PeProvisionerSimple 
)

On Container (
    Allocation = PowerContainerAllocationPolicySimple
    Scheduler = ContainerCloudletSchedulerDynamicWorkload 
    UtilizationModelFull
)
*/

@Service
public class WFCService {
    
    private static String experimentName="WFCExampleStatic";
    private static  int num_user = 1;
    private static boolean trace_flag = false;  // mean trace events
    private static boolean failure_flag = true;
    private static List<Container> containerList;       
    private static List<? extends ContainerHost> hostList;
    public static List<? extends ContainerPod> vmList;
    public static List<String> ids;
    public static List<Result> results;

    HostSelectionPolicy getHostSelectionPolicy(Integer i) {
       switch (i) {
           case 1:
               return new HostSelectionPolicyLeastFull();
           case 2:
               return new HostSelectionPolicyFirstFit();
           case 3:
               return new HostSelectionPolicyK8s();
           case 4:
               return new HostSelectionRoundRobin();
           case 5:
               return new HostSelectionHEFT();
           default:
               return new HostSelectionHEFT();
       }
    }

    public List<Result> start(String hostPath, String appPath, String failPath, Integer arithmetic, boolean selection) {
        try {                                                
            IDs.reset();
            WFCConstants.CAN_PRINT_SEQ_LOG = false;
            WFCConstants.CAN_PRINT_SEQ_LOG_Just_Step = false;
            WFCConstants.ENABLE_OUTPUT = false;
            WFCConstants.FAILURE_FLAG = false;            
            WFCConstants.RUN_AS_STATIC_RESOURCE = true;     
            
            FailureParameters.FTCMonitor ftc_monitor = null;
            FailureParameters.FTCFailure ftc_failure = null;
            FailureParameters.FTCluteringAlgorithm ftc_method = null;
            DistributionGenerator[][] failureGenerators = null;
             
            Log.printLine("Starting " + experimentName + " ... ");
                        
            //String daxPath = "./config/dax/Montage_" + (WFCConstants.WFC_NUMBER_CLOUDLETS - 1) + ".xml";
            //path = daxPath;
            String daxPath = appPath;
            File daxFile = new File(appPath);
            if (!daxFile.exists()) {
                Log.printLine("Warning: Please replace daxPath with the physical path in your working environment!");
                return null;
            }
     
            if(failure_flag){
                /*
                *  Fault Tolerant Parameters
                */
               /**
                * MONITOR_JOB classifies failures based on the level of jobs;
                * MONITOR_VM classifies failures based on the vm id; MOINTOR_ALL
                * does not do any classification; MONITOR_NONE does not record any
                * failiure.
                */
                ftc_monitor = FailureParameters.FTCMonitor.MONITOR_ALL;
               /**
                * Similar to FTCMonitor, FTCFailure controls the way how we
                * generate failures.
                */
                ftc_failure = FailureParameters.FTCFailure.FAILURE_ALL;
               /**
                * In this example, we have no clustering and thus it is no need to
                * do Fault Tolerant Clustering. By default, WorkflowSim will just
                * rety all the failed task.
                */
                ftc_method = FailureParameters.FTCluteringAlgorithm.FTCLUSTERING_NOOP;
               /**
                * Task failure rate for each level
                *
                */
               failureGenerators = new DistributionGenerator[1][1];
               failureGenerators[0][0] = new DistributionGenerator(DistributionGenerator.DistributionFamily.WEIBULL,
                       100, 1.0, 30, 300, 0.78);
            }
            
            Parameters.SchedulingAlgorithm sch_method = Parameters.SchedulingAlgorithm.STATIC;//local
            Parameters.PlanningAlgorithm pln_method = Parameters.PlanningAlgorithm.INVALID;//global-stage
            WFCReplicaCatalog.FileSystem file_system = WFCReplicaCatalog.FileSystem.LOCAL;
            Parameters.setSelection(selection);
            OverheadParameters op = new OverheadParameters(0, null, null, null, null, 0);
   
            ClusteringParameters.ClusteringMethod method = ClusteringParameters.ClusteringMethod.NONE;
            ClusteringParameters cp = new ClusteringParameters(0, 0, method, null);

            if(failure_flag){
                FailureParameters.init(ftc_method, ftc_monitor, ftc_failure, failureGenerators);
            }
          
           Parameters.init(WFCConstants.WFC_NUMBER_VMS, appPath, null,
                    null, op, cp, sch_method, pln_method,
                    null, 0);
            WFCReplicaCatalog.init(file_system);

            
            if (failure_flag) {
              FailureMonitor.init();
              FailureGenerator.init();
            }
            
            WFCReplicaCatalog.init(file_system);
            
            Calendar calendar = Calendar.getInstance();            

            CloudSim.init(num_user, calendar, trace_flag);


            PowerContainerAllocationPolicy containerAllocationPolicy = new PodContainerAllocationPolicy();
            PowerContainerPodSelectionPolicy podSelectionPolicy = new PowerContainerPodSelectionPolicyMaximumUsage();
            HostSelectionPolicy hostSelectionPolicy = getHostSelectionPolicy(arithmetic);

            String logAddress = "D:/asResults";

            /* set Constant */
           // Log.printLine("daxpath: " + daxPath);
            YamlUtil parser = new YamlUtil(1);
            parser.parseHostXml(daxPath);
            parser.parseHostXml(hostPath);
            //
            //Log.printLine("parser: " + parser.getTaskList().size());
            //parser.parse();
            WFCConstants.WFC_NUMBER_CLOUDLETS = parser.getTaskList().size() + 1;
            WFCConstants.WFC_NUMBER_CONTAINER = WFCConstants.WFC_NUMBER_CLOUDLETS;
            WFCConstants.WFC_NUMBER_VMS = WFCConstants.WFC_NUMBER_CONTAINER;
            WFCConstants.WFC_NUMBER_HOSTS = 6;
            Log.printLine("number of pods: " + WFCConstants.WFC_NUMBER_VMS);
                       
            hostList = new ArrayList<ContainerHost>();
            //hostList = createHostList(WFCConstants.WFC_NUMBER_HOSTS);
            //YamlUtil parser = new YamlUtil(1);
            //parser.parseXml("./config/dax/Host_6.xml");
            hostList = parser.getHostList();
            //cloudletList = new ArrayList<ContainerCloudlet>();
            containerList= new ArrayList<Container>();
            containerList = parser.getContainerList();
            List<ContainerPod> pods = parser.getPodList();
            ArrayList peList = new ArrayList<>();
            for (int p = 0; p < WFCConstants.WFC_NUMBER_VM_PES ; p++) {
                peList.add(new ContainerPe(p, new CotainerPeProvisionerSimple((double)WFCConstants.WFC_VM_MIPS * WFCConstants.WFC_VM_RATIO)));
            }
            ContainerPod p = new PowerContainerPod(pods.size()+1, 0, WFCConstants.WFC_VM_MIPS, (float)WFCConstants.WFC_VM_RAM,
                    WFCConstants.WFC_VM_BW * 2, WFCConstants.WFC_VM_SIZE,  WFCConstants.WFC_VM_VMM,
                    new ContainerSchedulerTimeSharedOverSubscription(peList),
                    //new ContainerSchedulerTimeSharedOverSubscription(peList),
                    new ContainerRamProvisionerSimple(WFCConstants.WFC_VM_RAM),
                    new ContainerBwProvisionerSimple(WFCConstants.WFC_VM_BW * 2), peList,
                    WFCConstants.WFC_DC_SCHEDULING_INTERVAL);
            pods.add(p);
            Container c = new PowerContainer(containerList.size()+1, 0, (double) WFCConstants.WFC_CONTAINER_MIPS ,
                    WFCConstants.WFC_CONTAINER_PES_NUMBER , WFCConstants.WFC_CONTAINER_RAM,
                    WFCConstants.WFC_CONTAINER_BW, WFCConstants.WFC_CONTAINER_SIZE, WFCConstants.WFC_CONTAINER_VMM,
                    //new ContainerCloudletSchedulerTimeShared(),WFCConstants.WFC_DC_SCHEDULING_INTERVAL);
                    new ContainerCloudletSchedulerDynamicWorkload(containerList.size()+1, WFCConstants.WFC_CONTAINER_MIPS, WFCConstants.WFC_CONTAINER_PES_NUMBER),
                    WFCConstants.WFC_DC_SCHEDULING_INTERVAL);
            c.setPodId(p.getUid());
            containerList.add(c);
            //podList = new ArrayList<ContainerPod>();
            ids = new ArrayList<>();

            // hostList is in need from here
            ContainerPodAllocationPolicy vmAllocationPolicy = new
                    PowerContainerPodAllocationPolicyMigrationAbstractHostSelection(hostList, podSelectionPolicy,
                    hostSelectionPolicy, WFCConstants.WFC_CONTAINER_OVER_UTILIZATION_THRESHOLD, WFCConstants.WFC_CONTAINER_UNDER_UTILIZATION_THRESHOLD);        
            
            WFCDatacenter datacenter = (WFCDatacenter) createDatacenter("datacenter_0",
                        PowerContainerDatacenterCM.class, hostList, vmAllocationPolicy,containerList,containerAllocationPolicy,
                        getExperimentName(experimentName, String.valueOf(WFCConstants.OVERBOOKING_FACTOR)),
                        WFCConstants.WFC_DC_SCHEDULING_INTERVAL, logAddress,
                        WFCConstants.WFC_VM_STARTTUP_DELAY,
                        WFCConstants.WFC_CONTAINER_STARTTUP_DELAY);
            Integer id = datacenter.getId();
            // podList and containerList is in need from here
            WFCPlanner wfPlanner = new WFCPlanner("planner_0", 1, id, pods, containerList);
                      
            WFCEngine wfEngine = wfPlanner.getWorkflowEngine();
            //podList = createVmList(wfEngine.getSchedulerId(0), Parameters.getVmNum());
            //wfEngine.submitVmList(wfEngine.getVmList(), 0);                           
            wfEngine.bindSchedulerDatacenter(datacenter.getId(), 0);
            

            CloudSim.terminateSimulation(WFCConstants.SIMULATION_LIMIT);
            CloudSim.startSimulation();         
            CloudSim.stopSimulation();
            
            List<Job> outputList0 = wfEngine.getJobsReceivedList();
           
            printJobList(outputList0,datacenter);
            /*List<ContainerPod> podsResult = datacenter.getContainerVmList();
            YamlWriter writer = new YamlWriter();
            writer.writeYaml("./config/yaml", podsResult);*/
            Log.printLine(experimentName + "finished!");
            //outputByRunnerAbs();
            
        } catch (Exception e) {                        
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            Log.printLine(e.getMessage());
            System.exit(0);
        }
        return results;
    }

     public static WFCDatacenter createDatacenter(String name, Class<PowerContainerDatacenterCM> datacenterClass,
                                                  List<? extends ContainerHost> hostList,
                                                  ContainerPodAllocationPolicy vmAllocationPolicy,
                                                  List<Container> containerList,
                                                  ContainerAllocationPolicy containerAllocationPolicy,
                                                  String experimentName, double schedulingInterval, String logAddress, double VMStartupDelay,
                                                  double ContainerStartupDelay) throws Exception {
       
        // 4. Create a DatacenterCharacteristics object that stores the
        //    properties of a data center: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/Pe time unit).
        
        LinkedList<Storage> storageList =new LinkedList<Storage>();
        WFCDatacenter datacenter = null;

        // 5. Finally, we need to create a storage object.
        /**
         * The bandwidth within a data center in MB/s.
         */
        //int maxTransferRate = 15;// the number comes from the futuregrid site, you can specify your bw

        try {
            // Here we set the bandwidth to be 15MB/s
            HarddriveStorage s1 = new HarddriveStorage(name, 1e12);
            s1.setMaxTransferRate(WFCConstants.WFC_DC_MAX_TRANSFER_RATE);
            storageList.add(s1);           

            ContainerDatacenterCharacteristics characteristics = new              
                ContainerDatacenterCharacteristics(WFCConstants.WFC_DC_ARCH, WFCConstants.WFC_DC_OS, WFCConstants.WFC_DC_VMM,
                                                     hostList, WFCConstants.WFC_DC_TIME_ZONE, WFCConstants.WFC_DC_COST , WFCConstants.WFC_DC_COST_PER_MEM, 
                                                     WFCConstants.WFC_DC_COST_PER_STORAGE,WFCConstants.WFC_DC_COST_PER_BW);
            
            datacenter = new WFCPowerContainerDatacenter(name,
                    characteristics, 
                    vmAllocationPolicy,
                    containerAllocationPolicy, 
                    storageList, 
                    schedulingInterval, 
                    experimentName, 
                    logAddress
                    );
                
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            Log.printLine(e.getMessage());
            System.exit(0);
        }
        return datacenter;
    }
   /*
    public static List<ContainerHost> createHostList(int hostsNumber) {
        
            ArrayList<ContainerHost> hostList = new ArrayList<ContainerHost>();
        // 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
        //    create a list to store these PEs before creating
        //    a Machine.
        
         try {
            for (int i = 1; i <= WFCConstants.WFC_NUMBER_HOSTS; i++) {
                ArrayList<ContainerPodPe> peList = new ArrayList<ContainerPodPe>();
                // 3. Create PEs and add these into the list.
                //for a quad-core machine, a list of 4 PEs is required:
                for (int p = 0; p < WFCConstants.WFC_NUMBER_HOST_PES; p++) {
                  peList.add(new ContainerPodPe(p, new ContainerPodPeProvisionerSimple(WFCConstants.WFC_HOST_MIPS))); // need to store Pe id and MIPS Rating
                }

                
                 hostList.add(new PowerContainerHostUtilizationHistory(IDs.pollId(ContainerHost.class) ,
                        new ContainerPodRamProvisionerSimple(WFCConstants.WFC_HOST_RAM * 10),
                        new ContainerPodBwProvisionerSimple(WFCConstants.WFC_HOST_BW * 10), WFCConstants.WFC_HOST_STORAGE , peList,
                        new ContainerPodSchedulerTimeSharedOverSubscription(peList),
                         //new ContainerPodSchedulerTimeShared(peList),
                        WFCConstants.HOST_POWER[2]));
            }
          } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            Log.printLine(e.getMessage());
            System.exit(0);
        }
        return hostList;
    }
        */
        private static String getExperimentName(String... args) {
        StringBuilder experimentName = new StringBuilder();

        for (int i = 0; i < args.length; ++i) {
            if (!args[i].isEmpty()) {
                if (i != 0) {
                    experimentName.append("_");
                }

                experimentName.append(args[i]);
            }
        }

        return experimentName.toString();
    }

    /**
     * Gets the maximum number of GB ever used by the application's heap.
     * @return the max heap utilization in GB
     * @see <a href="https://www.oracle.com/webfolder/technetwork/tutorials/obe/java/gc01/index.html">Java Garbage Collection Basics (for information about heap space)</a>
     */
    private static double getMaxHeapUtilizationGB() {
        final double memoryBytes =
            ManagementFactory.getMemoryPoolMXBeans()
                             .stream()
                             .filter(bean -> bean.getType() == MemoryType.HEAP)
                             .filter(bean -> bean.getName().contains("Eden Space") || bean.getName().contains("Survivor Space"))
                             .map(MemoryPoolMXBean::getPeakUsage)
                             .mapToDouble(MemoryUsage::getUsed)
                             .sum();

        return Conversion.bytesToGigaBytes(memoryBytes);
    }

       /*
    public static List<Container> createContainerList(int containersNumber) {
        LinkedList<Container> list = new LinkedList<>();        
        //peList.add(new ContainerPe(0, new CotainerPeProvisionerSimple((double)mips * ratio)));         
        //create VMs
        try{
            Container[] containers = new Container[containersNumber];
            for (int i = 0; i < containersNumber; i++) {

                containers[i] = new PowerContainer(IDs.pollId(Container.class), 0, (double) WFCConstants.WFC_CONTAINER_MIPS ,
                        WFCConstants.WFC_CONTAINER_PES_NUMBER , WFCConstants.WFC_CONTAINER_RAM,
                        WFCConstants.WFC_CONTAINER_BW, WFCConstants.WFC_CONTAINER_SIZE, WFCConstants.WFC_CONTAINER_VMM,
                        //new ContainerCloudletSchedulerTimeShared(),WFCConstants.WFC_DC_SCHEDULING_INTERVAL);                    
                        new ContainerCloudletSchedulerDynamicWorkload(WFCConstants.WFC_CONTAINER_MIPS, WFCConstants.WFC_CONTAINER_PES_NUMBER),
                        WFCConstants.WFC_DC_SCHEDULING_INTERVAL);
                containers[i].setPodId(ids.get(i));
                //Log.printLine("container " + containers[i].getUid() + "is belong to " + containers[i].getPodId());
                list.add(containers[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            Log.printLine(e.getMessage());
            System.exit(0);
        }
        return list;
       }
    
    
      public static List<ContainerPod> createVmList(int containerVmsNumber) {
        //Creates a container to store VMs. This list is passed to the broker later
        LinkedList<ContainerPod> list = new LinkedList<>();
        ArrayList peList = new ArrayList();
       
        try{
            for (int p = 0; p < WFCConstants.WFC_NUMBER_VM_PES ; p++) {
              peList.add(new ContainerPe(p, new CotainerPeProvisionerSimple((double)WFCConstants.WFC_VM_MIPS * WFCConstants.WFC_VM_RATIO)));         
            }
           //create VMs
           ContainerPod[] vm = new ContainerPod[containerVmsNumber];

           for (int i = 0; i < containerVmsNumber; i++) {           
               vm[i] = new PowerContainerPod(IDs.pollId(ContainerPod.class), 0, WFCConstants.WFC_VM_MIPS, (float)WFCConstants.WFC_VM_RAM,
                        WFCConstants.WFC_VM_BW * 2, WFCConstants.WFC_VM_SIZE,  WFCConstants.WFC_VM_VMM,
                       new ContainerSchedulerTimeSharedOverSubscription(peList),
                       //new ContainerSchedulerTimeSharedOverSubscription(peList),
                       new ContainerRamProvisionerSimple(WFCConstants.WFC_VM_RAM),
                       new ContainerBwProvisionerSimple(WFCConstants.WFC_VM_BW * 2), peList,
                       WFCConstants.WFC_DC_SCHEDULING_INTERVAL);
*/
                       /*new ContainerPod(IDs.pollId(ContainerPod.class), brokerId, (double) mips, (float) ram,
                       bw, size, "Xen", new ContainerSchedulerTimeShared(peList),
                       new ContainerRamProvisionerSimple(ram),
                       new ContainerBwProvisionerSimple(bw), peList);*/

                       //new ContainerPod(i, userId, mips * ratio, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
       /*        ids.add(vm[i].getUid());
               list.add(vm[i]);
           }
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            Log.printLine(e.getMessage());
            System.exit(0);
        }
        return list;
    }*/
    /**
     * Prints the job objects
     *
     * @param list list of jobs
     */
    protected static void printJobList(List<Job> list, WFCDatacenter datacenter) {
        double maxHeapUtilizationGB = getMaxHeapUtilizationGB();
        String indent = "    ";        
        double cost = 0.0;
        double time = 0.0;
        double length= 0.0;
        int counter = 1;
        int success_counter = 0;
        int failed_counter = 0;
        //Map<String, String> c2h = new HashMap<>();
        //List<ResultEvent> events = new ArrayList<>();
        results = new ArrayList<>();
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet Column=Task=>Length,WFType,Impact # Times of Task=>Actual,Exec,Finish.");//,CloudletOutputSize
        Log.printLine();
        Log.printLine(indent+"Row"+indent + "JOB ID"  +  indent + indent + "CLOUDLET" + indent + indent 
                + "STATUS" + indent
                + "Data CENTER ID" 
                //+ indent + indent + "HOST ID" 
                + indent + "VM ID" + indent + indent+ "CONTAINER ID" + indent + indent
                + "TIME" + indent +  indent +"START TIME" + indent + indent + "FINISH TIME" + indent + "DEPTH" + indent + indent + "Cost");
        
        DecimalFormat dft0 = new DecimalFormat("###.###");
        DecimalFormat dft = new DecimalFormat("####.###");
        boolean ifA = true;
        for (Job job : list) {
            if (job.getCloudletId() == WFCConstants.WFC_NUMBER_CLOUDLETS)
                continue;
            Log.print(String.format("%6d |",counter++)+indent + job.getCloudletId() + indent + indent);
            if (job.getClassType() == ClassType.STAGE_IN.value) {
                Log.print("STAGE-IN");
            }
            for (Task task : job.getTaskList()) {                
               
              Log.print("B" + task.getCloudletId()+ " B,");
              Log.print(task.getCloudletLength()+ " ,");               
              Log.print(task.getType());                                            
              //Log.print(dft0.format(task.getImpact()));
              
              /*Log.print("\n"+"\t\t\t ("+dft0.format(task.getActualCPUTime())+ " ,");
              Log.print("\n"+"\t\t\t"+dft0.format(task.getExecStartTime())+ " ,");      
              Log.print("\n"+"\t\t\t"+dft0.format(task.getTaskFinishTime())+ " )");     */
             
            }
            Log.print(indent);
                
            cost += job.getProcessingCost();
            time += job.getActualCPUTime();
            length +=job.getCloudletLength();
            int hostId = datacenter.getVmAllocationPolicy().getHost(job.getVmId(), job.getUserId()).getId();
            Result r = new Result();
            r.name = containerList.get(job.getCloudletId()-1).getIp();
            r.host = "host" + hostId;
            r.start = dft.format(job.getExecStartTime());
            r.finish = dft.format(job.getFinishTime());
            if(ifA) {
                ifA = false;
                r.datacenter = "A";
            } else {
                ifA = true;
                r.datacenter = "B";
            }
            /*c2h.put("Container " + job.getCloudletId(), "Host " + hostId);
            ResultEvent e = new ResultEvent();
            e.id = job.getCloudletId();
            e.startTime = dft.format(job.getExecStartTime());
            e.finishTime = dft.format(job.getFinishTime());
            events.add(e);*/
            results.add(r);
            if (job.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("     SUCCESS");         
                success_counter++;
                //datacenter.getContainerAllocationPolicy().getContainerVm(job.getContainerId(), job.getUserId()).getHost().getId()
                Log.printLine(indent + indent +indent + job.getVmId()
                        //+ indent + indent  + indent + indent + datacenter.getVmAllocationPolicy().getHost(job.getVmId(), job.getUserId()).getId()
                        + indent + indent + indent + /*job.getVmId()
                        + */indent + indent + indent + "C" + job.getContainerId() + "C"
                        + indent + indent + indent + /*dft.format(job.getActualCPUTime())
                        +*/ indent + indent + indent + dft.format(job.getExecStartTime()) + indent + indent + indent
                        + dft.format(job.getFinishTime()) /*+ indent + indent + indent + job.getDepth()
                        + indent + indent + indent 
                        + dft.format(job.getProcessingCost()
                       
                        )*/);
                  //Log.printLine();                              
                  /*
                   Log.printLine(datacenter.getContainerAllocationPolicy().getContainerVm(job.getContainerId(), job.getUserId()).getAllocatedMipsForContainer(datacenter.getContainerList().get(job.getContainerId()-1)));
                  Log.printLine(datacenter.getVmAllocationPolicy().getHost(job.getVmId(), job.getUserId()).getBw());
                  Log.printLine(datacenter.getVmAllocationPolicy().getHost(job.getVmId(), job.getUserId()).getMaxAvailableMips());
                  Log.printLine(datacenter.getVmAllocationPolicy().getHost(job.getVmId(), job.getUserId()).getContainerVmBwProvisioner().getUsedBw());
                  Log.printLine(datacenter.getVmAllocationPolicy().getHost(job.getVmId(), job.getUserId()).getContainerVmRamProvisioner().getUsedVmRam());
                  Log.printLine(datacenter.getVmAllocationPolicy().getHost(job.getVmId(), job.getUserId()).getContainerVmScheduler().getTotalAllocatedMipsForContainerVm(datacenter.getVmAllocationPolicy().get(job.getVmId(), job.getUserId()).);
                  */
       
            } else if (job.getCloudletStatus() == Cloudlet.FAILED) {
                Log.print("      FAILED");                
                failed_counter++;
                Log.printLine(indent + indent + job.getResourceId() 
                        + indent + indent  + indent + indent + datacenter.getVmAllocationPolicy().getHost(job.getVmId(), job.getUserId()).getId()
                        + indent + indent + indent + job.getVmId()
                        + indent + indent + indent + job.getContainerId()
                        + indent + indent + indent + dft.format(job.getActualCPUTime())
                        + indent + indent + indent + dft.format(job.getExecStartTime()) + indent + indent + indent
                        + dft.format(job.getFinishTime()) + indent + indent + indent + job.getDepth()
                        + indent + indent + indent + dft.format(job.getProcessingCost()
                        
                        ));
            }
        }    
        Log.printLine();
        Log.printLine("MinTimeBetweenEvents is " + dft.format(CloudSim.getMinTimeBetweenEvents()));
        Log.printLine("Used MaxHeapUtilization/GB is " + dft.format(maxHeapUtilizationGB));
        Log.printLine("----------------------------------------");
        Log.printLine("The total cost is " + dft.format(cost));
        Log.printLine("The total actual cpu time is " + dft.format(time));
        Log.printLine("The length cloudlets is " + dft.format(length));    
        Log.printLine("The total failed counter is " + dft.format(failed_counter));
        Log.printLine("The total success counter is " + dft.format(success_counter));
        /*result.ContainerInHost = c2h;
        result.TotalCpuCostTime = dft.format(time);
        result.LastFinishTime = dft.format(list.get(list.size()-1).getFinishTime());
        result.events = new ArrayList<>(events);*/
    }
}




