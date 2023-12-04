package org.sim.service;

import org.sim.cloudbus.cloudsim.*;
import org.sim.cloudbus.cloudsim.*;
import org.sim.cloudbus.cloudsim.core.CloudSim;
import org.sim.controller.Result;
import org.sim.workflowsim.*;
import org.sim.workflowsim.failure.FailureGenerator;
import org.sim.workflowsim.failure.FailureMonitor;
import org.sim.workflowsim.failure.FailureParameters;
import org.sim.workflowsim.utils.*;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.stereotype.Service;
import org.sim.workflowsim.*;
import org.sim.workflowsim.utils.*;
import org.sim.cloudbus.cloudsim.*;
import org.sim.workflowsim.*;
import org.sim.workflowsim.utils.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

@Service
public class service {
    public List<Host> hostList;

    protected List<CondorVM> createVM(int userId, int vms) {
        //Creates a container to store VMs. This list is passed to the broker later
        LinkedList<CondorVM> list = new LinkedList<>();

        //VM Parameters
        long size = 10000; //image size (MB)
        int ram = 512; //vm memory (MB)
        int mips = 1000;
        long bw = 1000;
        int pesNumber = 1; //number of cpus
        String vmm = "Xen"; //VMM name

        //create VMs
        CondorVM[] vm = new CondorVM[vms];
        for (int i = 0; i < vms; i++) {
            double ratio = 1.0;
            vm[i] = new CondorVM(i, userId, 0, 0, 0, 0, 0, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm[i]);
        }
        return list;
    }

    ////////////////////////// STATIC METHODS ///////////////////////
    /**
     * Creates main() to run this example This example has only one datacenter
     * and one storage
     */
    public void simulate(String hostPath, String appPath, String faultPath, Integer arithmetic) {
        DistributionGenerator.DistributionFamily f = DistributionGenerator.DistributionFamily.WEIBULL;
        Double s = 100.0;
        Double shape = 1.0;
        try {
            // First step: Initialize the WorkflowSim package.
            /**
             * However, the exact number of vms may not necessarily be vmNum If
             * the data center or the host doesn't have sufficient resources the
             * exact vmNum would be smaller than that. Take care.
             */
            XmlUtil util = new XmlUtil(6);
            //util.parseHostXml("D:/WorkflowSim-1.0/config/tmp/Host_8.xml");
            if(!faultPath.equals("")) {
                util.parseHostXml(faultPath);
                f = util.distributionFamily;
                s = util.scale;
                shape = util.shape;
                Log.printLine("FaultInject: \n"  + "type: " + f.name() + "  scale: " + s + " shape: " + shape);
            }
            util.parseHostXml(hostPath);
            hostList = util.getHostList();
            int vmNum = 1;//number of vms;
            FailureParameters.FTCMonitor ftc_monitor = FailureParameters.FTCMonitor.MONITOR_ALL;
            /**
             * Similar to FTCMonitor, FTCFailure controls the way how we
             * generate failures.
             */
            FailureParameters.FTCFailure ftc_failure = FailureParameters.FTCFailure.FAILURE_ALL;
            /**
             * In this example, we have no clustering and thus it is no need to
             * do Fault Tolerant Clustering. By default, WorkflowSim will just
             * rety all the failed task.
             */
            FailureParameters.FTCluteringAlgorithm ftc_method = FailureParameters.FTCluteringAlgorithm.FTCLUSTERING_NOOP;
            /**
             * Task failure rate for each level
             *
             */
            DistributionGenerator[][] failureGenerators = new DistributionGenerator[1][1];
            failureGenerators[0][0] = new DistributionGenerator(f,
                    s, shape, 30, 300, 0.78);


            Parameters.SchedulingAlgorithm sch_method = Parameters.SchedulingAlgorithm.STATIC;
            Parameters.PlanningAlgorithm pln_method = Parameters.PlanningAlgorithm.HEFT;
            ReplicaCatalog.FileSystem file_system = ReplicaCatalog.FileSystem.SHARED;

            switch (arithmetic) {
                case 1:
                    sch_method = Parameters.SchedulingAlgorithm.ROUNDROBIN;
                    pln_method = Parameters.PlanningAlgorithm.INVALID;
                    break;
                case 2:
                    sch_method = Parameters.SchedulingAlgorithm.MAXMIN;
                    pln_method = Parameters.PlanningAlgorithm.INVALID;
                    break;
                case 3:
                    sch_method = Parameters.SchedulingAlgorithm.FCFS;
                    pln_method = Parameters.PlanningAlgorithm.INVALID;
                    break;
                case 4:
                    sch_method = Parameters.SchedulingAlgorithm.MCT;
                    pln_method = Parameters.PlanningAlgorithm.INVALID;
                    break;
                case 6:
                    sch_method = Parameters.SchedulingAlgorithm.MIGRATE;
                    pln_method = Parameters.PlanningAlgorithm.INVALID;
                    break;
                case 7:
                    sch_method = Parameters.SchedulingAlgorithm.K8S;
                    pln_method = Parameters.PlanningAlgorithm.INVALID;
                    break;
            }


            FailureMonitor.init();
            FailureGenerator.init();


            /**
             * No overheads
             */
            OverheadParameters op = new OverheadParameters(0, null, null, null, null, 0);

            /**
             * No Clustering
             */
            ClusteringParameters.ClusteringMethod method = ClusteringParameters.ClusteringMethod.NONE;
            ClusteringParameters cp = new ClusteringParameters(0, 0, method, null);
            FailureParameters.init(ftc_method, ftc_monitor, ftc_failure, failureGenerators);
            Parameters.init(vmNum, "", null,
                    null, op, cp, sch_method, pln_method,
                    null, 0);
            ReplicaCatalog.init(file_system);

            /**
             * Initialize static parameters
             */

            // before creating any entities.
            int num_user = 1;   // number of grid users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // mean trace events

            // Initialize the CloudSim library
            CloudSim.init(num_user, calendar, trace_flag);

            WorkflowDatacenter datacenter0 = createDatacenter("Datacenter_0");

            /**
             * Create a WorkflowPlanner with one schedulers.
             */
            WorkflowPlanner wfPlanner = new WorkflowPlanner("planner_0", 1);
            //wfPlanner.setAppPath("D:/WorkflowSim-1.0/config/tmp/app10.xml");
            wfPlanner.setAppPath(appPath);
            /**
             * Create a WorkflowEngine.
             */
            WorkflowEngine wfEngine = wfPlanner.getWorkflowEngine();
            /**
             * Create a list of VMs.The userId of a vm is basically the id of
             * the scheduler that controls this vm.
             */
            List<CondorVM> vmlist0 = createVM(wfEngine.getSchedulerId(0), 1);
            Constants.Scheduler_Id = wfEngine.getSchedulerId(0);
            Constants.LOG_PATH = "D:/WorkflowSim-1.0/config/result/log2.xml";
            /**
             * Submits this list of vms to this WorkflowEngine.
             */
            wfEngine.submitVmList(vmlist0, 0);
            //wfEngine.submitVmList(new ArrayList<>(), 0);

            wfEngine.submitHostList(hostList, 0);
            Constants.hosts = new ArrayList<>(hostList);

            /**
             * Binds the data centers with the scheduler.
             */
            wfEngine.bindSchedulerDatacenter(datacenter0.getId(), 0);
            CloudSim.startSimulation();
            List<Job> outputList0 = wfEngine.getJobsReceivedList();
            Constants.resultPods = new ArrayList<>(outputList0);
            CloudSim.stopSimulation();
            printJobList(outputList0);
        } catch (Exception e) {
            Log.printLine("The simulation has been terminated due to an unexpected error" );
            e.printStackTrace();
        }
    }

    protected WorkflowDatacenter createDatacenter(String name) {
        String arch = "x86";      // system architecture
        String os = "Linux";          // operating system
        String vmm = "Xen";
        double time_zone = 10.0;         // time zone this resource located
        double cost = 3.0;              // the cost of using processing in this resource
        double costPerMem = 0.05;		// the cost of using memory in this resource
        double costPerStorage = 0.1;	// the cost of using storage in this resource
        double costPerBw = 0.1;			// the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<>();	//we are not adding SAN devices by now
        WorkflowDatacenter datacenter = null;

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        // 5. Finally, we need to create a storage object.
        /**
         * The bandwidth within a data center in MB/s.
         */
        int maxTransferRate = 15;// the number comes from the futuregrid site, you can specify your bw

        try {
            // Here we set the bandwidth to be 15MB/s
            HarddriveStorage s1 = new HarddriveStorage(name, 1e12);
            s1.setMaxTransferRate(maxTransferRate);
            storageList.add(s1);
            VmAllocationPolicy v = new VmAllocationPolicySimple(hostList);
            datacenter = new WorkflowDatacenter(name, characteristics, v, storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return datacenter;
    }


    /**
     * Prints the job objects
     *
     * @param list list of jobs
     */
    protected void printJobList(List<Job> list) {
        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Job ID" + indent + "Task ID" + indent + "STATUS" + indent
                + "Data center ID" + indent + "VM ID" + indent + indent
                + "Time" + indent + "Start Time" + indent + "Finish Time" + indent + "Depth");
        DecimalFormat dft = new DecimalFormat("###.##");
        for (Job job : list) {
            Log.print(indent + job.getCloudletId() + indent + indent);
            if (job.getClassType() == Parameters.ClassType.STAGE_IN.value) {
                Log.print("Stage-in");
            }
            for (Task task : job.getTaskList()) {
                Log.print(task.getCloudletId() + ",");
            }
            Log.print(indent);

            if (job.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");
                Log.printLine(indent + indent + job.getResourceId() + indent + indent + indent + job.getVmId()
                        + indent + indent + indent + dft.format(job.getActualCPUTime())
                        + indent + indent + dft.format(job.getExecStartTime()) + indent + indent + indent
                        + dft.format(job.getFinishTime()) + indent + indent + indent + job.getDepth());
                if(job.getTaskList().size() >= 1) {
                    Result r = new Result();
                    double t1 = job.getFinishTime();
                    r.finish = dft.format(t1);
                    r.start = dft.format(job.getExecStartTime());
                    r.host = "host" + job.getVmId();
                    r.name = job.getTaskList().get(0).getType();
                    Constants.results.add(r);
                }
            } else if (job.getCloudletStatus() == Cloudlet.FAILED) {
                Log.print("FAILED");
                Log.printLine(indent + indent + job.getResourceId() + indent + indent + indent + job.getVmId()
                        + indent + indent + indent + dft.format(job.getActualCPUTime())
                        + indent + indent + dft.format(job.getExecStartTime()) + indent + indent + indent
                        + dft.format(job.getFinishTime()) + indent + indent + indent + job.getDepth());
            }
        }
        try {
            File file = new File(Constants.LOG_PATH);
            if(!file.exists()) {
                file.getParentFile().mkdir();
            }
            Element root = new Element("root");
            Document doc = new Document(root);
            int hostSize = hostList.size();
            Element r = null;
            for(int i = 0; i < Constants.logs.size(); i++) {
                if(i % hostSize == 0) {
                    r = new Element("Utilization");
                    r.setAttribute("time", Constants.logs.get(i).time+"");
                }
                Element t = new Element("Host");
                t.setAttribute("id", String.valueOf(i % hostSize));
                t.setAttribute("cpuUtilization", Constants.logs.get(i).cpuUtilization + "");
                t.setAttribute("ramUtilization", Constants.logs.get(i).ramUtilization + "");
                r.addContent(t);
                if(i % hostSize == hostSize - 1)
                {
                    doc.getRootElement().addContent(r);
                }

            }

            XMLOutputter xmlOutput = new XMLOutputter();
            Format f = Format.getRawFormat();
            f.setIndent("  "); // 文本缩进
            f.setTextMode(Format.TextMode.TRIM_FULL_WHITE);
            xmlOutput.setFormat(f);

            // 把xml文件输出到指定的位置
            xmlOutput.output(doc, new FileOutputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
