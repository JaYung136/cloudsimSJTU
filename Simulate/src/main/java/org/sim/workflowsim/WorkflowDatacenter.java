/**
 * Copyright 2012-2013 University Of Southern California
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sim.workflowsim;

import org.sim.cloudbus.cloudsim.*;
import org.sim.cloudbus.cloudsim.*;
import org.sim.service.Constants;
import org.sim.workflowsim.utils.ReplicaCatalog;
import org.sim.cloudbus.cloudsim.*;
import org.sim.cloudbus.cloudsim.core.CloudSim;
import org.sim.cloudbus.cloudsim.core.CloudSimTags;
import org.sim.cloudbus.cloudsim.core.SimEvent;

import org.sim.cloudbus.cloudsim.power.PowerHost;
import org.sim.workflowsim.utils.Parameters;
import org.sim.workflowsim.utils.Parameters.ClassType;
import org.sim.workflowsim.utils.Parameters.FileType;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * WorkflowDatacenter extends Datacenter so as we can use CondorVM and other
 * components
 *
 * @author Weiwei Chen
 * @since WorkflowSim Toolkit 1.0
 * @date Apr 9, 2013
 */
public class WorkflowDatacenter extends Datacenter {

    private double cloudletSubmitted = -1;

    public WorkflowDatacenter(String name,
            DatacenterCharacteristics characteristics,
            VmAllocationPolicy vmAllocationPolicy,
            List<Storage> storageList,
            double schedulingInterval) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
    }

    /**
     * Processes a Cloudlet submission. The cloudlet is actually a job which can
     * be cast to Job
     *
     * @param ev a SimEvent object
     * @param ack an acknowledgement
     * @pre ev != null
     * @post $none
     */
    @Override
    protected void processCloudletSubmit(SimEvent ev, boolean ack) {
        updateCloudletProcessing();
        this.cloudletSubmitted = CloudSim.clock();
        try {
            /**
             * cl is actually a job but it is not necessary to cast it to a job
             */
            Job job = (Job) ev.getData();

            if (job.isFinished()) {
                String name = CloudSim.getEntityName(job.getUserId());
                Log.printLine(getName() + ": Warning - Cloudlet #" + job.getCloudletId() + " owned by " + name
                        + " is already completed/finished.");
                Log.printLine("Therefore, it is not being executed again");
                Log.printLine();

                // NOTE: If a Cloudlet has finished, then it won't be processed.
                // So, if ack is required, this method sends back a result.
                // If ack is not required, this method don't send back a result.
                // Hence, this might cause CloudSim to be hanged since waiting
                // for this Cloudlet back.
                if (ack) {
                    int[] data = new int[3];
                    data[0] = getId();
                    data[1] = job.getCloudletId();
                    data[2] = CloudSimTags.FALSE;

                    // unique tag = operation tag
                    int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
                    sendNow(job.getUserId(), tag, data);
                }

                sendNow(job.getUserId(), CloudSimTags.CLOUDLET_RETURN, job);

                return;
            }

            int userId = job.getUserId();
            int jobId = job.getCloudletId();
            int vmId = job.getVmId();
            //Host host = getVmAllocationPolicy().getHost(vmId, userId);
            Host host = null;
            for(Host h: Constants.hosts) {
                if(h.getId() == vmId) {
                    host = h;
                }
            }
            if(host == null) {
                Log.printLine("WorkflowDatacenter: host is null when find job " + job.getCloudletId() + "'s host");
                return;
            }
            //CondorVM vm = (CondorVM) host.getVm(vmId, userId);

            switch (Parameters.getCostModel()) {
                case VM:
                case DATACENTER:
                    // process this Cloudlet to this CloudResource
                    job.setResourceParameter(getId(), getCharacteristics().getCostPerSecond(),
                            getCharacteristics().getCostPerBw());
                    break;
                /*case VM:
                    job.setResourceParameter(getId(), vm.getCost(), vm.getCostPerBW());
                    break;*/
                default:
                    break;
            }

            /**
             * Stage-in file && Shared based on the file.system
             */
            if (job.getClassType() == ClassType.STAGE_IN.value) {
                stageInFile2FileSystem(job);
            }

            /**
             * Add data transfer time (communication cost
             */
            double fileTransferTime = 0.0;
            if (job.getClassType() == ClassType.COMPUTE.value) {
                fileTransferTime = processDataStageInForComputeJob(job.getFileList(), job);
            }

            CloudletScheduler scheduler = host.getCloudletScheduler();
           // Log.printLine("asasasa" + fileTransferTime);
            /*if(job.getTaskList().size() >= 1) {
                Log.printLine("asaas::::::::::::::: " + job.getTaskList().get(0).getRam());
            }*/

            double estimatedFinishTime = scheduler.cloudletSubmit(job, fileTransferTime);
           // Log.printLine("est: " + estimatedFinishTime);
            if(estimatedFinishTime == 0.0) {
                //host.setState(WorkflowSimTags.VM_STATUS_BUSY);
                for(Host h: Constants.hosts) {
                    if(h.getId() == vmId) {
                        h.setState(WorkflowSimTags.VM_STATUS_BUSY);
                    }
                }
            }
            updateTaskExecTime(job, host);
            //Log.printLine("asasa");

            // if this cloudlet is in the exec queue
            if (estimatedFinishTime > 0.0 && !Double.isInfinite(estimatedFinishTime)) {
                send(getId(), estimatedFinishTime, CloudSimTags.VM_DATACENTER_EVENT);
            } else {
                schedule(getId(), 0, WorkflowSimTags.HOST_IS_BUSY, job);
                Log.printLine("Warning: You schedule cloudlet to a busy Host");
            }

            if (ack) {
                // it seem not be used
                //Log.printLine("asasassssss");
                int[] data = new int[3];
                data[0] = getId();
                data[1] = job.getCloudletId();
                data[2] = CloudSimTags.TRUE;

                int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
                sendNow(job.getUserId(), tag, data); //scheduler id
            }
        } catch (ClassCastException c) {
            Log.printLine(getName() + ".processCloudletSubmit(): " + "ClassCastException error.");
        } catch (Exception e) {
            Log.printLine(getName() + ".processCloudletSubmit(): " + "Exception error.");
            e.printStackTrace();
        }
        checkCloudletCompletion();

    }

    /**
     * Update the submission time/exec time of a job
     *
     * @param job
     * @param host
     */
    private void updateTaskExecTime(Job job, Host host) {
        double start_time = job.getExecStartTime();
        for (Task task : job.getTaskList()) {
            Double pause = 0.0;
            if(Constants.pause.containsKey(task.getCloudletId())) {
                pause = Constants.pause.get(task.getCloudletId()).getValue();
            }
            task.setExecStartTime(start_time);
            double mips = host.getPeList().get(0).getPeProvisioner().getMips();
            mips = mips * task.getNumOfPes();
            double task_runtime = task.getCloudletLength() / mips + pause;
            start_time += task_runtime;
            //Because CloudSim would not let us update end time here
            task.setTaskFinishTime(start_time);
        }
    }

    /**
     * Stage in files for a stage-in job. For a local file system (such as
     * condor-io) add files to the local storage; For a shared file system (such
     * as NFS) add files to the shared storage
     *
     * @param job, the job
     * @pre $none
     * @post $none
     */
    private void stageInFile2FileSystem(Job job) {
        List<FileItem> fList = job.getFileList();

        for (FileItem file : fList) {
            switch (ReplicaCatalog.getFileSystem()) {
                /**
                 * For local file system, add it to local storage (data center
                 * name)
                 */
                case LOCAL:
                    ReplicaCatalog.addFileToStorage(file.getName(), this.getName());
                    /**
                     * Is it not really needed currently but it is left for
                     * future usage
                     */
                    //ClusterStorage storage = (ClusterStorage) getStorageList().get(0);
                    //storage.addFile(file);
                    break;
                /**
                 * For shared file system, add it to the shared storage
                 */
                case SHARED:
                    ReplicaCatalog.addFileToStorage(file.getName(), this.getName());
                    break;
                default:
                    break;
            }
        }
    }

    /*
     * Stage in for a single job (both stage-in job and compute job)
     * @param requiredFiles, all files to be stage-in
     * @param job, the job to be processed
     * @pre  $none
     * @post $none
     */
    protected double processDataStageInForComputeJob(List<FileItem> requiredFiles, Job job) throws Exception {
        double time = 0.0;
        for (FileItem file : requiredFiles) {
            //The input file is not an output File 
            if (file.isRealInputFile(requiredFiles)) {
                double maxBwth = 0.0;
                List siteList = ReplicaCatalog.getStorageList(file.getName());
                if (siteList.isEmpty()) {
                    throw new Exception(file.getName() + " does not exist");
                }
                switch (ReplicaCatalog.getFileSystem()) {
                    case SHARED:
                        //stage-in job
                        /**
                         * Picks up the site that is closest
                         */
                        double maxRate = Double.MIN_VALUE;
                        for (Storage storage : getStorageList()) {
                            double rate = storage.getMaxTransferRate();
                            if (rate > maxRate) {
                                maxRate = rate;
                            }
                        }
                        //Storage storage = getStorageList().get(0);
                        time += file.getSize() / (double) Consts.MILLION / maxRate;
                        break;
                    case LOCAL:
                        int vmId = job.getVmId();
                        int userId = job.getUserId();
                        Host host = getVmAllocationPolicy().getHost(vmId, userId);
                        Vm vm = host.getVm(vmId, userId);

                        boolean requiredFileStagein = true;
                        for (Iterator it = siteList.iterator(); it.hasNext();) {
                            //site is where one replica of this data is located at
                            String site = (String) it.next();
                            if (site.equals(this.getName())) {
                                continue;
                            }
                            /**
                             * This file is already in the local vm and thus it
                             * is no need to transfer
                             */
                            if (site.equals(Integer.toString(vmId))) {
                                requiredFileStagein = false;
                                break;
                            }
                            double bwth;
                            if (site.equals(Parameters.SOURCE)) {
                                //transfers from the source to the VM is limited to the VM bw only
                                bwth = vm.getBw();
                                //bwth = dcStorage.getBaseBandwidth();
                            } else {
                                //transfers between two VMs is limited to both VMs
                                bwth = Math.min(vm.getBw(), getVmAllocationPolicy().getHost(Integer.parseInt(site), userId).getVm(Integer.parseInt(site), userId).getBw());
                                //bwth = dcStorage.getBandwidth(Integer.parseInt(site), vmId);
                            }
                            if (bwth > maxBwth) {
                                maxBwth = bwth;
                            }
                        }
                        if (requiredFileStagein && maxBwth > 0.0) {
                            time += file.getSize() / (double) Consts.MILLION / maxBwth;
                        }

                        /**
                         * For the case when storage is too small it is not
                         * handled here
                         */
                        //We should add but since CondorVm has a small capability it often fails
                        //We currently don't use this storage to do anything meaningful. It is left for future. 
                        //condorVm.addLocalFile(file);
                        ReplicaCatalog.addFileToStorage(file.getName(), Integer.toString(vmId));
                        break;
                }
            }
        }
        return time;
    }

    @Override
    protected void updateCloudletProcessing() {
        // if some time passed since last processing
        // R: for term is to allow loop at simulation start. Otherwise, one initial
        // simulation step is skipped and schedulers are not properly initialized
        //this is a bug of CloudSim if the runtime is smaller than 0.1 (now is 0.01) it doesn't work at all
        /*if (CloudSim.clock() < 0.111 || CloudSim.clock() > getLastProcessTime() + 0.01) {
            List<? extends Host> list = getVmAllocationPolicy().getHostList();
            double smallerTime = Double.MAX_VALUE;
            // for each host...
            for (Host host : list) {
                // inform VMs to update processing
                double time = host.updateVmsProcessing(CloudSim.clock());
                //Log.printLine(time);
                // what time do we expect that the next cloudlet will finish?
                if (time < smallerTime) {
                    smallerTime = time;
                }
            }
            // gurantees a minimal interval before scheduling the event
            if (smallerTime < CloudSim.clock() + 0.11) {
                smallerTime = CloudSim.clock() + 0.11;
            }
            if (smallerTime != Double.MAX_VALUE) {
                //Log.printLine("sssssssssssss");
                schedule(getId(), (smallerTime - CloudSim.clock()), CloudSimTags.VM_DATACENTER_EVENT);
            }
            setLastProcessTime(CloudSim.clock());

        }*/
        /*if(this.cloudletSubmitted == -1 || this.cloudletSubmitted == CloudSim.clock())
        {
            CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
            schedule(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
        }*/
        if (CloudSim.clock() >= 0.111 && CloudSim.clock() <= getLastProcessTime() + 0.01) {
            return;
        }
        double currentTime = CloudSim.clock();
        if(super.isIfInMigrate()) {
            setLastProcessTime(currentTime);
            return;
        }
        double time = 0;
        //Log.printLine("current time: " + currentTime);
        //updateCloudletProcessingWithoutSchedulingFutureEventsForce();
        if(currentTime > getLastProcessTime()) {
            double minTime = updateCloudletProcessingWithoutSchedulingFutureEventsForce();
            if(minTime < CloudSim.clock() + 0.11) {
                minTime = CloudSim.clock() + 0.11;
            }
            if(minTime == Double.MAX_VALUE) {
                setLastProcessTime(CloudSim.clock());
                //CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
                //send(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
                //Log.printLine("no more");
                return;
            }
            List<Map<String, Object>> migrationMap = getVmAllocationPolicy().optimizeAllocation(getVmList());

            Log.printLine("start to migrate");
            if(migrationMap != null) {
                for(Map<String, Object> migrate: migrationMap) {
                    super.setInMigrate();
                    ResCloudlet rcl = (ResCloudlet) migrate.get("container");
                    Host targetHost = (Host) migrate.get("targetHost");
                    Host oldHost = (Host) migrate.get("oldHost");
                    int[] data = new int[5];
                    data[0] = rcl.getCloudlet().getCloudletId();
                    data[1] = 0;
                    data[2] = oldHost.getId();
                    data[3] = targetHost.getId();
                    data[4] = getId();
                    send(getId(), ((double) targetHost.getBw() / (2 * 8000)), CloudSimTags.CLOUDLET_MOVE, data);
                    super.addMigrateNum();
                    time += (double) targetHost.getBw() / (2 * 8000);
                    Log.printLine("move from " + oldHost.getId() + " to " + " target " + targetHost.getId());
                    //break;
                }
            }

            setLastProcessTime(CloudSim.clock());
        }
        //CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
        schedule(getId(), getSchedulingInterval() + 0.01, CloudSimTags.VM_DATACENTER_EVENT);
    }

    private double updateCloudletProcessingWithoutSchedulingFutureEventsForce() {
        double currentTime = CloudSim.clock();
        double minTime = Double.MAX_VALUE;
        double timeDiff = currentTime - getLastProcessTime();
        double timeFrameDatacenterEnergy = 0.0;

        Log.printLine("\n\n--------------------------------------------------------------\n\n");
        Log.formatLine("New resource usage for the time frame starting at %.2f:", currentTime);

        for (Host host : this.<PowerHost> getHostList()) {
            Log.printLine();

            double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing
            if (time < minTime) {
                minTime = time;
            }

            /*Log.formatLine(
                    "%.2f: [Host #%d] utilization is %.2f%%",
                    currentTime,
                    host.getId(),
                    host.getUtilizationOfCpu() * 100);*/
        }

        if (timeDiff > 0) {
            /*Log.formatLine(
                    "\nEnergy consumption for the last time frame from %.2f to %.2f:",
                    getLastProcessTime(),
                    currentTime);*/

            /*for (PowerHost host : this.<PowerHost> getHostList()) {
                double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
                double utilizationOfCpu = host.getUtilizationOfCpu();
                double timeFrameHostEnergy = host.getEnergyLinearInterpolation(
                        previousUtilizationOfCpu,
                        utilizationOfCpu,
                        timeDiff);
                timeFrameDatacenterEnergy += timeFrameHostEnergy;

                Log.printLine();
                Log.formatLine(
                        "%.2f: [Host #%d] utilization at %.2f was %.2f%%, now is %.2f%%",
                        currentTime,
                        host.getId(),
                        getLastProcessTime(),
                        previousUtilizationOfCpu * 100,
                        utilizationOfCpu * 100);
                Log.formatLine(
                        "%.2f: [Host #%d] energy is %.2f W*sec",
                        currentTime,
                        host.getId(),
                        timeFrameHostEnergy);
            }*/

            /*Log.formatLine(
                    "\n%.2f: Data center's energy is %.2f W*sec\n",
                    currentTime,
                    timeFrameDatacenterEnergy);*/
        }

        checkCloudletCompletion();

        /** Remove completed VMs **/
       /* for (PowerHost host : this.<PowerHost> getHostList()) {
            for (Vm vm : host.getCompletedVms()) {
                getVmAllocationPolicy().deallocateHostForVm(vm);
                getVmList().remove(vm);
                Log.printLine("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
            }
        }*/

        Log.printLine();

        setLastProcessTime(currentTime);
        return minTime;
    }


    /**
     * Verifies if some cloudlet inside this PowerDatacenter already finished.
     * If yes, send it to the User/Broker
     *
     * @pre $none
     * @post $none
     */
    @Override
    protected void checkCloudletCompletion() {
        List<? extends Host> list = getVmAllocationPolicy().getHostList();
        for (Host host : list) {
            //for (Vm vm : host.getVmList()) {
                while (host.getCloudletScheduler().isFinishedCloudlets()) {
                    Cloudlet cl = host.getCloudletScheduler().getNextFinishedCloudlet();
                    if(cl != null) {
                        //Log.printLine("is not null");
                    }
                    if (cl != null) {

                        sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
                        register(cl);
                    }
                }
            //}
        }
    }
    /*
     * Register a file to the storage if it is an output file
     * @param requiredFiles, all files to be stage-in
     * @param job, the job to be processed
     * @pre  $none
     * @post $none
     */

    private void register(Cloudlet cl) {
        Task tl = (Task) cl;
        List<FileItem> fList = tl.getFileList();
        for (FileItem file : fList) {
            if (file.getType() == FileType.OUTPUT)//output file
            {
                switch (ReplicaCatalog.getFileSystem()) {
                    case SHARED:
                        ReplicaCatalog.addFileToStorage(file.getName(), this.getName());
                        break;
                    case LOCAL:
                        int vmId = cl.getVmId();
                        int userId = cl.getUserId();
                        //Host host = getVmAllocationPolicy().getHost(vmId, userId);
                        Host host = null;
                        for(Host h: getHostList()) {
                            if(host.getId() == vmId) {
                                host = h;
                            }
                        }
                        if(host == null) {
                            return;
                        }
                        /**
                         * Left here for future work
                         */
                        ReplicaCatalog.addFileToStorage(file.getName(), Integer.toString(vmId));
                        break;
                }
            }
        }
    }
}
