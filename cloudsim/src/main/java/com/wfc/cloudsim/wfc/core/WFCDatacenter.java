package com.wfc.cloudsim.wfc.core;

import com.wfc.cloudsim.cloudsim.container.core.*;
import com.wfc.cloudsim.cloudsim.container.resourceAllocators.ContainerAllocationPolicy;
import com.wfc.cloudsim.cloudsim.container.resourceAllocators.ContainerPodAllocationPolicy;
import com.wfc.cloudsim.cloudsim.*;
import com.wfc.cloudsim.cloudsim.core.CloudSim;
import com.wfc.cloudsim.cloudsim.core.CloudSimTags;
import com.wfc.cloudsim.cloudsim.core.SimEntity;
import com.wfc.cloudsim.cloudsim.core.SimEvent;
import com.wfc.cloudsim.workflowsim.WorkflowSimTags;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.wfc.cloudsim.cloudsim.container.schedulers.ContainerCloudletScheduler;
import com.wfc.cloudsim.workflowsim.FileItem;
import com.wfc.cloudsim.workflowsim.Job;
import com.wfc.cloudsim.workflowsim.Task;
import com.wfc.cloudsim.workflowsim.utils.Parameters;
import com.wfc.cloudsim.workflowsim.utils.Parameters.ClassType;
import com.wfc.cloudsim.workflowsim.utils.WFCReplicaCatalog;

/**
 * Created by sareh on 13/08/15.
 */
public class WFCDatacenter extends SimEntity {

    /**
     * The characteristics.
     */
    private ContainerDatacenterCharacteristics characteristics;

    /**
     * The regional cis name.
     */
    private String regionalCisName;

    /**
     * The vm provisioner.
     */
    private ContainerPodAllocationPolicy vmAllocationPolicy;
    /**
     * The container provisioner.
     */
    private ContainerAllocationPolicy containerAllocationPolicy;

    /**
     * The last process time.
     */
    private double lastProcessTime;

    /**
     * The storage list.
     */
    private List<Storage> storageList;

    /**
     * The vm list.
     */
    private List<? extends ContainerPod> containerVmList;
    /**
     * The container list.
     */
    private List<? extends Container> containerList;

    /**
     * The scheduling interval.
     */
    private double schedulingInterval;
    /**
     * The scheduling interval.
     */
    private String experimentName;
    /**
     * The log address.
     */
    private String logAddress;

    private List<Task> tasks;


    /**
     * Allocates a new PowerDatacenter object.
     * @param name
     * @param characteristics
     * @param vmAllocationPolicy
     * @param containerAllocationPolicy
     * @param storageList
     * @param schedulingInterval
     * @param experimentName
     * @param logAddress
     * @throws Exception
     */
    public WFCDatacenter(
            String name,
            ContainerDatacenterCharacteristics characteristics,
            ContainerPodAllocationPolicy vmAllocationPolicy,
            ContainerAllocationPolicy containerAllocationPolicy,
            List<Storage> storageList,
            double schedulingInterval, String experimentName, String logAddress) throws Exception {
        super(name);

        setCharacteristics(characteristics);
        setVmAllocationPolicy(vmAllocationPolicy);
        setContainerAllocationPolicy(containerAllocationPolicy);
        setLastProcessTime(0.0);
        setStorageList(storageList);
        setContainerVmList(new ArrayList<ContainerPod>());
        setContainerList(new ArrayList<Container>());
        setSchedulingInterval(schedulingInterval);
        setExperimentName(experimentName);
        setLogAddress(logAddress);
        java.io.File file = new java.io.File("D:/test_container.txt");

        for (ContainerHost host : getCharacteristics().getHostList()) {
            host.setDatacenter(this);
        }

        // If this resource doesn't have any PEs then no useful at all
        if (getCharacteristics().getNumberOfPes() == 0) {
            throw new Exception(super.getName()
                    + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
        }

        // stores id of this class
        getCharacteristics().setId(super.getId());
    }

    /**
     * Overrides this method when making a new and different type of resource. <br>
     * <b>NOTE:</b> You do not need to override {} method, if you use this method.
     *
     * @pre $none
     * @post $none
     */
    protected void registerOtherEntity() {
        // empty. This should be override by a child class
    }

    /**
     * Processes events or services that are available for this PowerDatacenter.
     *
     * @param ev a Sim_event object
     * @pre ev != null
     * @post $none
     */
    @Override
    public void processEvent(SimEvent ev) {
        
        int srcId = -1;
        if(WFCConstants.CAN_PRINT_SEQ_LOG)
        Log.printLine("ContainerDataCener=>ProccessEvent()=>ev.getTag():"+ev.getTag());
        
        switch (ev.getTag()) {
            // Resource characteristics inquiry
            case CloudSimTags.RESOURCE_CHARACTERISTICS:
                srcId = ((Integer) ev.getData()).intValue();
                sendNow(srcId, ev.getTag(), getCharacteristics());
                break;

            // Resource dynamic info inquiry
            case CloudSimTags.RESOURCE_DYNAMICS:
                srcId = ((Integer) ev.getData()).intValue();
                sendNow(srcId, ev.getTag(), 0);
                break;

            case CloudSimTags.RESOURCE_NUM_PE:
                srcId = ((Integer) ev.getData()).intValue();
                int numPE = getCharacteristics().getNumberOfPes();
                sendNow(srcId, ev.getTag(), numPE);
                break;

            case CloudSimTags.RESOURCE_NUM_FREE_PE:
                srcId = ((Integer) ev.getData()).intValue();
                int freePesNumber = getCharacteristics().getNumberOfFreePes();
                sendNow(srcId, ev.getTag(), freePesNumber);
                break;

            // New Cloudlet arrives
            case CloudSimTags.CLOUDLET_SUBMIT:
                processCloudletSubmit(ev, false);
                break;

            // New Cloudlet arrives, but the sender asks for an ack
            case CloudSimTags.CLOUDLET_SUBMIT_ACK:
                processCloudletSubmit(ev, true);
                break;

            // Cancels a previously submitted Cloudlet
            case CloudSimTags.CLOUDLET_CANCEL:
                processCloudlet(ev, CloudSimTags.CLOUDLET_CANCEL);
                break;

            // Pauses a previously submitted Cloudlet
            case CloudSimTags.CLOUDLET_PAUSE:
                processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE);
                break;

            // Pauses a previously submitted Cloudlet, but the sender
            // asks for an acknowledgement
            case CloudSimTags.CLOUDLET_PAUSE_ACK:
                processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE_ACK);
                break;

            // Resumes a previously submitted Cloudlet
            case CloudSimTags.CLOUDLET_RESUME:
                processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME);
                break;

            // Resumes a previously submitted Cloudlet, but the sender
            // asks for an acknowledgement
            case CloudSimTags.CLOUDLET_RESUME_ACK:
                processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME_ACK);
                break;

            // Moves a previously submitted Cloudlet to a different resource
            case CloudSimTags.CLOUDLET_MOVE:
                processCloudletMove((int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE);
                break;

            // Moves a previously submitted Cloudlet to a different resource
            case CloudSimTags.CLOUDLET_MOVE_ACK:
                processCloudletMove((int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE_ACK);
                break;

            // Checks the status of a Cloudlet
            case CloudSimTags.CLOUDLET_STATUS:
                processCloudletStatus(ev);
                break;

            // Ping packet
            case CloudSimTags.INFOPKT_SUBMIT:
                processPingRequest(ev);
                break;

            case CloudSimTags.VM_CREATE:
                processVmCreate(ev, false);
                break;

            case CloudSimTags.VM_CREATE_ACK:
                processVmCreate(ev, true);
                break;

            case CloudSimTags.VM_DESTROY:
                processVmDestroy(ev, false);
                break;

            case CloudSimTags.VM_DESTROY_ACK:
                processVmDestroy(ev, true);
                break;

            case CloudSimTags.VM_MIGRATE:
                processVmMigrate(ev, false);
                break;

            case CloudSimTags.VM_MIGRATE_ACK:
                processVmMigrate(ev, true);
                break;

            case CloudSimTags.VM_DATA_ADD:
                processDataAdd(ev, false);
                break;

            case CloudSimTags.VM_DATA_ADD_ACK:
                processDataAdd(ev, true);
                break;

            case CloudSimTags.VM_DATA_DEL:
                processDataDelete(ev, false);
                break;

            case CloudSimTags.VM_DATA_DEL_ACK:
                processDataDelete(ev, true);
                break;

            case CloudSimTags.VM_DATACENTER_EVENT:
                updateCloudletProcessing();
                checkCloudletCompletion();
                break;
            case containerCloudSimTags.CONTAINER_SUBMIT:
                processContainerSubmit(ev, true);
                break;

            case containerCloudSimTags.CONTAINER_MIGRATE:
                processContainerMigrate(ev, false);
                // other unknown tags are processed by this method
                break;

            case WorkflowSimTags.JOBS_GIVE:
                setTasks(ev);
            default:
                processOtherEvent(ev);
                break;
        }
    }

    private void setTasks(SimEvent ev) {
        List<Task> tasks = (List<Task>) ev.getData();
        Parameters.setTasks(tasks);
        Log.printLine("DataCenter: receive " + tasks.size() + " tasks");
    }

    public void processContainerSubmit(SimEvent ev, boolean ack) {
        List<Container> containerList = (List<Container>) ev.getData();
        Log.printLine("WFCDatacenter: processContainerSubmit: " + containerList.size() + " " + ack);
        for (Container container : containerList) {
            boolean result = getContainerAllocationPolicy().allocateVmForContainer(container, getContainerVmList());
            if (ack) {
                int[] data = new int[3];
                data[1] = container.getId();
                if (result) {
                    data[2] = CloudSimTags.TRUE;
                } else {
                    data[2] = CloudSimTags.FALSE;
                }
                if (result) {
                    ContainerPod containerPod = getContainerAllocationPolicy().getContainerVm(container);
                    data[0] = containerPod.getId();
                    if(containerPod.getId() == -1){

                        Log.printConcatLine("The ContainerVM ID is not known (-1) !");
                    }
                    
                    
                    Log.printConcatLine("Assigning the container#" + container.getUid() + "to VM #" + containerPod.getUid());
                    container.changeMips(container.getVm().getMips());
                    getContainerList().add(container);
                    if (container.isBeingInstantiated()) {
                        container.setBeingInstantiated(false);
                    }
                    List<Double> doubles = new ArrayList<>();
                    for(int i = 0; i < container.getNumberOfPes(); i++) {
                        doubles.add(WFCConstants.containerMips.get(container.getId()));
                    }
                    //container.updateContainerProcessing(CloudSim.clock(), getContainerAllocationPolicy().getContainerVm(container).getContainerScheduler().getAllocatedMipsForContainer(container));
                    container.updateContainerProcessing(CloudSim.clock(), doubles);
                } else {
                    data[0] = -1;
                    //notAssigned.add(container);
                    Log.printLine(String.format("Couldn't find a vm to host the container #%s", container.getUid()));

                }
                
                send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), containerCloudSimTags.CONTAINER_CREATE_ACK, data);

            }
        }

    }

    
    
 
    /**
     * Process data del.
     *
     * @param ev  the ev
     * @param ack the ack
     */
    protected void processDataDelete(SimEvent ev, boolean ack) {
        if (ev == null) {
            return;
        }

        Object[] data = (Object[]) ev.getData();
        if (data == null) {
            return;
        }

        String filename = (String) data[0];
        int req_source = ((Integer) data[1]).intValue();
        int tag = -1;

        // check if this file can be deleted (do not delete is right now)
        int msg = deleteFileFromStorage(filename);
        if (msg == DataCloudTags.FILE_DELETE_SUCCESSFUL) {
            tag = DataCloudTags.CTLG_DELETE_MASTER;
        } else { // if an error occured, notify user
            tag = DataCloudTags.FILE_DELETE_MASTER_RESULT;
        }

        if (ack) {
            // send back to sender
            Object pack[] = new Object[2];
            pack[0] = filename;
            pack[1] = Integer.valueOf(msg);

            sendNow(req_source, tag, pack);
        }
    }

    /**
     * Process data add.
     *
     * @param ev  the ev
     * @param ack the ack
     */
    protected void processDataAdd(SimEvent ev, boolean ack) {
        if (ev == null) {
            return;
        }

        Object[] pack = (Object[]) ev.getData();
        if (pack == null) {
            return;
        }

        File file = (File) pack[0]; // get the file
        file.setMasterCopy(true); // set the file into a master copy
        int sentFrom = ((Integer) pack[1]).intValue(); // get sender ID

        /******
         * // DEBUG Log.printLine(super.get_name() + ".addMasterFile(): " + file.getName() +
         * " from " + CloudSim.getEntityName(sentFrom));
         *******/

        Object[] data = new Object[3];
        data[0] = file.getName();

        int msg = addFile(file); // add the file

        if (ack) {
            data[1] = Integer.valueOf(-1); // no sender id
            data[2] = Integer.valueOf(msg); // the result of adding a master file
            sendNow(sentFrom, DataCloudTags.FILE_ADD_MASTER_RESULT, data);
        }
    }

    /**
     * Processes a ping request.
     *
     * @param ev a Sim_event object
     * @pre ev != null
     * @post $none
     */
    protected void processPingRequest(SimEvent ev) {
        InfoPacket pkt = (InfoPacket) ev.getData();
        pkt.setTag(CloudSimTags.INFOPKT_RETURN);
        pkt.setDestId(pkt.getSrcId());

        // sends back to the sender
        sendNow(pkt.getSrcId(), CloudSimTags.INFOPKT_RETURN, pkt);
    }

    /**
     * Process the event for an User/Broker who wants to know the status of a Cloudlet. This
     * PowerDatacenter will then send the status back to the User/Broker.
     *
     * @param ev a Sim_event object
     * @pre ev != null
     * @post $none
     */
    protected void processCloudletStatus(SimEvent ev) {
        Log.printLine("WFCDatacenter: processCloudletStatus");
        int cloudletId = 0;
        int userId = 0;
        int vmId = 0;
        int containerId = 0;
        int status = -1;

        try {
            // if a sender using cloudletXXX() methods
            int data[] = (int[]) ev.getData();
            cloudletId = data[0];
            userId = data[1];
            vmId = data[2];
            containerId = data[3];
            //Log.printLine("Data Center is processing the cloudletStatus Event ");
            status = getVmAllocationPolicy().getHost(vmId, userId).getContainerVm(vmId, userId).
                    getContainer(containerId, userId).getContainerCloudletScheduler().getCloudletStatus(cloudletId);
        }

        // if a sender using normal send() methods
        catch (ClassCastException c) {
            try {
                ContainerCloudlet cl = (ContainerCloudlet) ev.getData();
                cloudletId = cl.getCloudletId();
                userId = cl.getUserId();
                containerId = cl.getContainerId();

                status = getVmAllocationPolicy().getHost(vmId, userId).getContainerVm(vmId, userId).getContainer(containerId, userId)
                        .getContainerCloudletScheduler().getCloudletStatus(cloudletId);
            } catch (Exception e) {
                Log.printConcatLine(getName(), ": Error in processing CloudSimTags.CLOUDLET_STATUS");
                Log.printLine(e.getMessage());
                return;
            }
        } catch (Exception e) {
            Log.printConcatLine(getName(), ": Error in processing CloudSimTags.CLOUDLET_STATUS");
            Log.printLine(e.getMessage());
            return;
        }

        int[] array = new int[3];
        array[0] = getId();
        array[1] = cloudletId;
        array[2] = status;

        int tag = CloudSimTags.CLOUDLET_STATUS;
        sendNow(userId, tag, array);
    }

    /**
     * Here all the method related to VM requests will be received and forwarded to the related
     * method.
     *
     * @param ev the received event
     * @pre $none
     * @post $none
     */
    protected void processOtherEvent(SimEvent ev) {
        if (ev == null) {
            Log.printConcatLine(getName(), ".processOtherEvent(): Error - an event is null.");
        }
    }

    /**
     * Process the event for a User/Broker who wants to create a VM in this PowerDatacenter. This
     * PowerDatacenter will then send the status back to the User/Broker.
     *
     * @param ev  a Sim_event object
     * @param ack the ack
     * @pre ev != null
     * @post $none
     */
    protected void processVmCreate(SimEvent ev, boolean ack) {
        List<ContainerPod> containerPods = (List<ContainerPod>)ev.getData();
        //ContainerPod containerPod = (ContainerPod) ev.getData();
        //Log.printLine("asssssssssssssss==> " + containerPods.size());
        boolean result;
        if(Parameters.ifSelectAll()) {
            //Log.printLine("DataCenter has " + this.tasks.size() + " tasks");
            for(int i = 0; i < containerPods.size() - 1; i++) {
                List<Task> t = new ArrayList<>();
                t.add(Parameters.getTasks().get(i));
                containerPods.get(i).setTasks(t);
            }
            Task temp = new Task(containerPods.size(), 100);
            List<Task> tempT = new ArrayList<>();
            tempT.add(temp);
            containerPods.get(containerPods.size() - 1).setTasks(tempT);
           // result = getVmAllocationPolicy().allocateHostForVm(containerPods);
            result = getVmAllocationPolicy().allocateHostForPods(containerPods);
            for(int i = 0; i < containerPods.size(); i++) {
                int []data = new int[3];
                data[0] = getId();
                data[1] = containerPods.get(i).getId();
                data[2] = CloudSimTags.TRUE;
                WFCConstants.podMips.put(containerPods.get(i).getId(), WFCConstants.hostMips.get(containerPods.get(i).getHost().getId()));
                containerPods.get(i).setMips(containerPods.get(i).getHost().getContainerVmScheduler().getOneMips());
                send(containerPods.get(i).getUserId(), CloudSim.getMinTimeBetweenEvents(), CloudSimTags.VM_CREATE_ACK, data);
                getContainerVmList().add(containerPods.get(i));
                YamlWriter writer = new YamlWriter();
                List<ContainerPod> podtmp = new ArrayList<>();
                podtmp.add(containerPods.get(i));
                writer.writeYaml("./config/yaml", podtmp);
                if (containerPods.get(i).isBeingInstantiated()) {
                    containerPods.get(i).setBeingInstantiated(false);
                }

                containerPods.get(i).updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(containerPods.get(i)).getContainerVmScheduler()
                        .getAllocatedMipsForContainerVm(containerPods.get(i)));
            }

        }
        else for(int i = 0; i < containerPods.size(); i++)
        {
            ContainerPod containerPod = containerPods.get(i);
            result = getVmAllocationPolicy().allocateHostForVm(containerPod);

            if (ack) {
                int[] data = new int[3];
                data[0] = getId();
                data[1] = containerPod.getId();

                if (result) {
                    data[2] = CloudSimTags.TRUE;
                } else {
                    data[2] = CloudSimTags.FALSE;
                }
                send(containerPod.getUserId(), CloudSim.getMinTimeBetweenEvents(), CloudSimTags.VM_CREATE_ACK, data);
            }

            if (result) {
                YamlWriter writer = new YamlWriter();
                List<ContainerPod> podtmp = new ArrayList<>();
                podtmp.add(containerPods.get(i));
                writer.writeYaml("./config/yaml", podtmp);
                getContainerVmList().add(containerPod);

                if (containerPod.isBeingInstantiated()) {
                    containerPod.setBeingInstantiated(false);
                }

                containerPod.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(containerPod).getContainerVmScheduler()
                        .getAllocatedMipsForContainerVm(containerPod));
            }
        }

    }

    /**
     * Process the event for a User/Broker who wants to destroy a VM previously created in this
     * PowerDatacenter. This PowerDatacenter may send, upon request, the status back to the
     * User/Broker.
     *
     * @param ev  a Sim_event object
     * @param ack the ack
     * @pre ev != null
     * @post $none
     */
    protected void processVmDestroy(SimEvent ev, boolean ack) {
        ContainerPod containerPod = (ContainerPod) ev.getData();
        getVmAllocationPolicy().deallocateHostForVm(containerPod);

        if (ack) {
            int[] data = new int[3];
            data[0] = getId();
            data[1] = containerPod.getId();
            data[2] = CloudSimTags.TRUE;

            sendNow(containerPod.getUserId(), CloudSimTags.VM_DESTROY_ACK, data);
        }

        getContainerVmList().remove(containerPod);
    }

    /**
     * Process the event for a User/Broker who wants to migrate a VM. This PowerDatacenter will
     * then send the status back to the User/Broker.
     *
     * @param ev a Sim_event object
     * @pre ev != null
     * @post $none
     */
    protected void processVmMigrate(SimEvent ev, boolean ack) {
        Object tmp = ev.getData();
        if (!(tmp instanceof Map<?, ?>)) {
            throw new ClassCastException("The data object must be Map<String, Object>");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> migrate = (HashMap<String, Object>) tmp;

        ContainerPod containerPod = (ContainerPod) migrate.get("vm");
        ContainerHost host = (ContainerHost) migrate.get("host");

        getVmAllocationPolicy().deallocateHostForVm(containerPod);
        host.removeMigratingInContainerVm(containerPod);
        boolean result = getVmAllocationPolicy().allocateHostForVm(containerPod, host);
        if (!result) {
            Log.printLine("[Datacenter.processVmMigrate] VM allocation to the destination host failed");
            System.exit(0);
        }

        if (ack) {
            int[] data = new int[3];
            data[0] = getId();
            data[1] = containerPod.getId();

            if (result) {
                data[2] = CloudSimTags.TRUE;
            } else {
                data[2] = CloudSimTags.FALSE;
            }
            sendNow(ev.getSource(), CloudSimTags.VM_CREATE_ACK, data);
        }

        Log.formatLine(
                "%.2f: Migration of VM #%d to Host #%d is completed",
                CloudSim.clock(),
                containerPod.getId(),
                host.getId());
        containerPod.setInMigration(false);
    }

    /**
     * Process the event for a User/Broker who wants to migrate a VM. This PowerDatacenter will
     * then send the status back to the User/Broker.
     *
     * @param ev a Sim_event object
     * @pre ev != null
     * @post $none
     */
    protected void processContainerMigrate(SimEvent ev, boolean ack) {

        Object tmp = ev.getData();
        if (!(tmp instanceof Map<?, ?>)) {
            throw new ClassCastException("The data object must be Map<String, Object>");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> migrate = (HashMap<String, Object>) tmp;

        Container container = (Container) migrate.get("container");
        ContainerPod containerPod = (ContainerPod) migrate.get("vm");

        getContainerAllocationPolicy().deallocateVmForContainer(container);
        if(containerPod.getContainersMigratingIn().contains(container)){
            containerPod.removeMigratingInContainer(container);}
        boolean result = getContainerAllocationPolicy().allocateVmForContainer(container, containerPod);
        if (!result) {
            Log.printLine("[Datacenter.processContainerMigrate]Container allocation to the destination vm failed");
            System.exit(0);
        }
        if (containerPod.isInWaiting()){
            containerPod.setInWaiting(false);

        }

        if (ack) {
            int[] data = new int[3];
            data[0] = getId();
            data[1] = container.getId();

            if (result) {
                data[2] = CloudSimTags.TRUE;
            } else {
                data[2] = CloudSimTags.FALSE;
            }
            sendNow(ev.getSource(), containerCloudSimTags.CONTAINER_CREATE_ACK, data);
        }

        Log.formatLine(
                "%.2f: Migration of container #%d to Pod #%d is completed",
                CloudSim.clock(),
                container.getId(),
                container.getVm().getId());
        container.setInMigration(false);
    }

    /**
     * Processes a Cloudlet based on the event type.
     *
     * @param ev   a Sim_event object
     * @param type event type
     * @pre ev != null
     * @pre type > 0
     * @post $none
     */
    protected void processCloudlet(SimEvent ev, int type) {
        Log.printLine("WFCDatacenter: processCloudlet");
        int cloudletId = 0;
        int userId = 0;
        int vmId = 0;
        int containerId = 0;

        try { // if the sender using cloudletXXX() methods
            int data[] = (int[]) ev.getData();
            cloudletId = data[0];
            userId = data[1];
            vmId = data[2];
            containerId = data[3];
        }

        // if the sender using normal send() methods
        catch (ClassCastException c) {
            try {
                ContainerCloudlet cl = (ContainerCloudlet) ev.getData();
                cloudletId = cl.getCloudletId();
                userId = cl.getUserId();
                vmId = cl.getVmId();
                containerId = cl.getContainerId();
            } catch (Exception e) {
                Log.printConcatLine(super.getName(), ": Error in processing Cloudlet");
                Log.printLine(e.getMessage());
                return;
            }
        } catch (Exception e) {
            Log.printConcatLine(super.getName(), ": Error in processing a Cloudlet.");
            Log.printLine(e.getMessage());
            return;
        }

        // begins executing ....
        switch (type) {
            case CloudSimTags.CLOUDLET_CANCEL:
                processCloudletCancel(cloudletId, userId, vmId, containerId);
                break;

            case CloudSimTags.CLOUDLET_PAUSE:
                processCloudletPause(cloudletId, userId, vmId, containerId, false);
                break;

            case CloudSimTags.CLOUDLET_PAUSE_ACK:
                processCloudletPause(cloudletId, userId, vmId, containerId, true);
                break;

            case CloudSimTags.CLOUDLET_RESUME:
                processCloudletResume(cloudletId, userId, vmId, containerId, false);
                break;

            case CloudSimTags.CLOUDLET_RESUME_ACK:
                processCloudletResume(cloudletId, userId, vmId, containerId, true);
                break;
            default:
                break;
        }

    }

    /**
     * Process the event for a User/Broker who wants to move a Cloudlet.
     *
     * @param receivedData information about the migration
     * @param type         event tag
     * @pre receivedData != null
     * @pre type > 0
     * @post $none
     */
    protected void processCloudletMove(int[] receivedData, int type) {
        updateCloudletProcessing();

        int[] array = receivedData;
        int cloudletId = array[0];
        int userId = array[1];
        int vmId = array[2];
        int containerId = array[3];
        int vmDestId = array[4];
        int containerDestId = array[5];
        int destId = array[6];

        // get the cloudlet
        Cloudlet cl = getVmAllocationPolicy().getHost(vmId, userId).getContainerVm(vmId, userId).getContainer(containerId, userId)
                .getContainerCloudletScheduler().cloudletCancel(cloudletId);

        boolean failed = false;
        if (cl == null) {// cloudlet doesn't exist
            failed = true;
        } else {
            // has the cloudlet already finished?
            if (cl.getCloudletStatusString().equals("Success")) {// if yes, send it back to user
                int[] data = new int[3];
                data[0] = getId();
                data[1] = cloudletId;
                data[2] = 0;
                sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_SUBMIT_ACK, data);
                sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
            }

            // prepare cloudlet for migration
            cl.setVmId(vmDestId);

            // the cloudlet will migrate from one vm to another does the destination VM exist?
            if (destId == getId()) {
                ContainerPod containerPod = getVmAllocationPolicy().getHost(vmDestId, userId).getContainerVm(vmDestId, userId);
                if (containerPod == null) {
                    failed = true;
                } else {
                    // time to transfer the files
                    double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());
                    containerPod.getContainer(containerDestId, userId).getContainerCloudletScheduler().cloudletSubmit(cl, fileTransferTime);
                }
            } else {// the cloudlet will migrate from one resource to another
                int tag = ((type == CloudSimTags.CLOUDLET_MOVE_ACK) ? CloudSimTags.CLOUDLET_SUBMIT_ACK
                        : CloudSimTags.CLOUDLET_SUBMIT);
                sendNow(destId, tag, cl);
            }
        }

        if (type == CloudSimTags.CLOUDLET_MOVE_ACK) {// send ACK if requested
            int[] data = new int[3];
            data[0] = getId();
            data[1] = cloudletId;
            if (failed) {
                data[2] = 0;
            } else {
                data[2] = 1;
            }
            sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_SUBMIT_ACK, data);
        }
    }

    /**
     * Processes a Cloudlet submission.
     *
     * @param ev  a SimEvent object
     * @param ack an acknowledgement
     * @pre ev != null
     * @post $none
     */
    protected void processCloudletSubmit(SimEvent ev, boolean ack) {
        updateCloudletProcessing();

        try {            
            Job cl = (Job) ev.getData();

            // checks whether this Cloudlet has finished or not
            if (cl.isFinished()) {
                String name = CloudSim.getEntityName(cl.getUserId());
                Log.printConcatLine(getName(), ": Warning - Cloudlet #", cl.getCloudletId(), " owned by ", name,
                        " is already completed/finished.");
                /*Log.printLine("Therefore, it is not being executed again");
                Log.printLine();*/

                // NOTE: If a Cloudlet has finished, then it won't be processed.
                // So, if ack is required, this method sends back a result.
                // If ack is not required, this method don't send back a result.
                // Hence, this might cause CloudSim to be hanged since waiting
                // for this Cloudlet back.
                if (ack) {
                    int[] data = new int[3];
                    data[0] = getId();
                    data[1] = cl.getCloudletId();
                    data[2] = CloudSimTags.FALSE;

                    // unique tag = operation tag
                    int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
                    sendNow(cl.getUserId(), tag, data);
                }

                sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);

                return;
            }

            // process this Cloudlet to this CloudResource
            cl.setResourceParameter(getId(), getCharacteristics().getCostPerSecond(), getCharacteristics().getCostPerBw());

            int userId = cl.getUserId();
            int vmId = cl.getCloudletId();
            int containerId = cl.getContainerId();
            Log.printLine("job id: " + cl.getCloudletId() + "pod id is: " + vmId +"container id: " + containerId);
            if(vmId == -1) {
                checkCloudletCompletion();
                return;
            }

            ContainerHost host;
            host = getVmAllocationPolicy().getHost(vmId, userId);
            if(host == null) {
                Log.printLine("Host that contain pod " + vmId + " is null");
            }
//            Log.printLine("host id: " + host.getId());
           /* for(ContainerPod v: host.getVmList()) {
                Log.printLine(v.getId());
            }*/
            ContainerPod vm;
            vm = host.getContainerVm(vmId, userId);
            if(vm == null) {
                Log.printLine("Pod " + vmId + " is null");
            }
            Container container;
            container = vm.getContainer(containerId, userId);
            if(container == null) {
                Log.printLine("Container " + containerId + " is null");
            }
           switch (Parameters.getCostModel()) {
                case DATACENTER:
                    // process this Cloudlet to this CloudResource
                    cl.setResourceParameter(getId(), getCharacteristics().getCostPerSecond(),
                            getCharacteristics().getCostPerBw());
                    break;
                case VM:
                    cl.setResourceParameter(getId(), vm.getCost(), vm.getCostPerBW());
                    break;
                default:
                    break;
                       
            }
            if(cl.getClassType() == ClassType.STAGE_IN.value) {
                Log.printLine("WFCDatacenter: cl " + cl.getCloudletId() + "'s type is stagein");
            } else {
                Log.printLine("WFCDatacenter: cl " + cl.getCloudletId() + "'s type is not stagein");
            }
            if (cl.getClassType() == ClassType.STAGE_IN.value) {
                stageInFile2FileSystem(cl);
            }
                                    
            //double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());
            
            double fileTransferTime = 0.0;
            if (cl.getClassType() == ClassType.COMPUTE.value) {
                fileTransferTime = processDataStageInForComputeJob(cl.getFileList(), cl);
                Log.printLine("task " + cl.getCloudletId() + " 's data transfer time is: " + fileTransferTime);
            }
             //double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());
   
            //Scheduler schedulerVm = vm.getContainerScheduler();
                        
            ContainerCloudletScheduler schedulerContainer = container.getContainerCloudletScheduler();
            double estimatedFinishTime = schedulerContainer.cloudletSubmit(cl, fileTransferTime);
            Log.printLine("task " + cl.getCloudletId() + " 's finishTime: " + estimatedFinishTime);
            updateTaskExecTime(cl, host);
            
            // if this cloudlet is in the exec queue
            if (estimatedFinishTime > 0.0 && !Double.isInfinite(estimatedFinishTime)) {
                estimatedFinishTime += fileTransferTime;
                send(getId(), estimatedFinishTime, CloudSimTags.VM_DATACENTER_EVENT);
            }else {
                Log.printLine("Warning: You schedule cloudlet to a busy VM");
            }
            
            if (ack) {
                int[] data = new int[3];
                data[0] = getId();
                data[1] = cl.getCloudletId();
                data[2] = CloudSimTags.TRUE;

                // unique tag = operation tag
                int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
                sendNow(cl.getUserId(), tag, data);
            }
        } catch (ClassCastException c) {
            Log.printLine(String.format("%s.processCloudletSubmit(): ClassCastException error.", getName()));
          //by arman I commented log   c.printStackTrace();
        } catch (Exception e) {                         
              e.printStackTrace();           //by arman I commented log 
              Log.printLine(String.format("%s.processCloudletSubmit(): Exception error.", getName()));
              Log.print(e.getMessage());
        }

        checkCloudletCompletion();
    }

    /**
     * Predict file transfer time.
     *
     * @param requiredFiles the required files
     * @return the double
     */
    protected double predictFileTransferTime(List<String> requiredFiles) {
        double time = 0.0;

        for (String fileName : requiredFiles) {
            for (int i = 0; i < getStorageList().size(); i++) {
                Storage tempStorage = getStorageList().get(i);
                File tempFile = tempStorage.getFile(fileName);
                if (tempFile != null) {
                    time += tempFile.getSize() / tempStorage.getMaxTransferRate();
                    break;
                }
            }
        }
        return time;
    }

    /**
     * Processes a Cloudlet resume request.
     *
     * @param cloudletId resuming cloudlet ID
     * @param userId     ID of the cloudlet's owner
     * @param ack        $true if an ack is requested after operation
     * @param vmId       the vm id
     * @pre $none
     * @post $none
     */
    protected void processCloudletResume(int cloudletId, int userId, int vmId, int containerId, boolean ack) {
        double eventTime = getVmAllocationPolicy().getHost(vmId, userId).getContainerVm(vmId, userId).getContainer(containerId, userId)
                .getContainerCloudletScheduler().cloudletResume(cloudletId);

        boolean status = false;
        if (eventTime > 0.0) { // if this cloudlet is in the exec queue
            status = true;
            if (eventTime > CloudSim.clock()) {
                schedule(getId(), eventTime, CloudSimTags.VM_DATACENTER_EVENT);
            }
        }

        if (ack) {
            int[] data = new int[3];
            data[0] = getId();
            data[1] = cloudletId;
            if (status) {
                data[2] = CloudSimTags.TRUE;
            } else {
                data[2] = CloudSimTags.FALSE;
            }
            sendNow(userId, CloudSimTags.CLOUDLET_RESUME_ACK, data);
        }
    }

    /**
     * Processes a Cloudlet pause request.
     *
     * @param cloudletId resuming cloudlet ID
     * @param userId     ID of the cloudlet's owner
     * @param ack        $true if an ack is requested after operation
     * @param vmId       the vm id
     * @pre $none
     * @post $none
     */
    protected void processCloudletPause(int cloudletId, int userId, int vmId, int containerId, boolean ack) {
        boolean status = getVmAllocationPolicy().getHost(vmId, userId).getContainerVm(vmId, userId).getContainer(containerId, userId)
                .getContainerCloudletScheduler().cloudletPause(cloudletId);

        if (ack) {
            int[] data = new int[3];
            data[0] = getId();
            data[1] = cloudletId;
            if (status) {
                data[2] = CloudSimTags.TRUE;
            } else {
                data[2] = CloudSimTags.FALSE;
            }
            sendNow(userId, CloudSimTags.CLOUDLET_PAUSE_ACK, data);
        }
    }

    /**
     * Processes a Cloudlet cancel request.
     *
     * @param cloudletId resuming cloudlet ID
     * @param userId     ID of the cloudlet's owner
     * @param vmId       the vm id
     * @pre $none
     * @post $none
     */
    protected void processCloudletCancel(int cloudletId, int userId, int vmId, int containerId) {
        Cloudlet cl = getVmAllocationPolicy().getHost(vmId, userId).getContainerVm(vmId, userId).getContainer(containerId, userId)
                .getContainerCloudletScheduler().cloudletCancel(cloudletId);
        sendNow(userId, CloudSimTags.CLOUDLET_CANCEL, cl);
    }

    /**
     * Updates processing of each cloudlet running in this PowerDatacenter. It is necessary because
     * Hosts and VirtualMachines are simple objects, not entities. So, they don't receive events and
     * updating cloudlets inside them must be called from the outside.
     *
     * @pre $none
     * @post $none
     */
    protected void updateCloudletProcessing() {
        // if some time passed since last processing
        // R: for term is to allow loop at simulation start. Otherwise, one initial
        // simulation step is skipped and schedulers are not properly initialized
       //if (CloudSim.clock() < 0.111 || CloudSim.clock() > getLastProcessTime() + CloudSim.getMinTimeBetweenEvents()) {
                   
       if (CloudSim.clock() < 0.111 || CloudSim.clock() > getLastProcessTime() + 0.01) {
            List<? extends ContainerHost> list = getVmAllocationPolicy().getContainerHostList();
            double smallerTime = Double.MAX_VALUE;
            // for each host...
            for (int i = 0; i < list.size(); i++) {
                ContainerHost host = list.get(i);
                // inform VMs to update processing
                double time = host.updateContainerVmsProcessing(CloudSim.clock());
                // what time do we expect that the next cloudlet will finish?
                if (time < smallerTime) {
                    smallerTime = time;
                }
            }
            // gurantees a minimal interval before scheduling the event
           /* if (smallerTime < CloudSim.clock() + CloudSim.getMinTimeBetweenEvents() + 0.01) {
                smallerTime = CloudSim.clock() + CloudSim.getMinTimeBetweenEvents() + 0.01;
            }*/
            
            if (smallerTime < CloudSim.clock() + CloudSim.getMinTimeBetweenEvents() + 0.11) {
                smallerTime = CloudSim.clock() + CloudSim.getMinTimeBetweenEvents() + 0.11;
            }
            if (smallerTime != Double.MAX_VALUE) {
                schedule(getId(), (smallerTime - CloudSim.clock()), CloudSimTags.VM_DATACENTER_EVENT);                  
            }
            setLastProcessTime(CloudSim.clock());
        }
    }

    /**
     * Verifies if some cloudlet inside this PowerDatacenter already finished. If yes, send it to
     * the User/Broker
     *
     * @pre $none
     * @post $none
     */
    protected void checkCloudletCompletion() {
        Log.printLine("WFCDatacenter: checkCloudletCompletion");
        List<? extends ContainerHost> list = getVmAllocationPolicy().getContainerHostList();
        for (int i = 0; i < list.size(); i++) {
            ContainerHost host = list.get(i);
            for (ContainerPod vm : host.getVmList()) {
                for (Container container : vm.getContainerList()) {
                    while (container.getContainerCloudletScheduler().isFinishedCloudlets()) {
                        Cloudlet cl = container.getContainerCloudletScheduler().getNextFinishedCloudlet();
                        Log.printLine("nextFinished: " + cl.getCloudletId());
                        if (cl != null) {
                            sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
                            register(cl);//notice me it is important
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds a file into the resource's storage before the experiment starts. If the file is a master
     * file, then it will be registered to the RC when the experiment begins.
     *
     * @param file a DataCloud file
     * @return a tag number denoting whether this operation is a success or not
     */
    public int addFile(File file) {
        if (file == null) {
            return DataCloudTags.FILE_ADD_ERROR_EMPTY;
        }

        if (contains(file.getName())) {
            return DataCloudTags.FILE_ADD_ERROR_EXIST_READ_ONLY;
        }

        // check storage space first
        if (getStorageList().size() <= 0) {
            return DataCloudTags.FILE_ADD_ERROR_STORAGE_FULL;
        }

        Storage tempStorage = null;
        int msg = DataCloudTags.FILE_ADD_ERROR_STORAGE_FULL;

        for (int i = 0; i < getStorageList().size(); i++) {
            tempStorage = getStorageList().get(i);
            if (tempStorage.getAvailableSpace() >= file.getSize()) {
                tempStorage.addFile(file);
                msg = DataCloudTags.FILE_ADD_SUCCESSFUL;
                break;
            }
        }

        return msg;
    }

    /**
     * Checks whether the resource has the given file.
     *
     * @param file a file to be searched
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    protected boolean contains(File file) {
        if (file == null) {
            return false;
        }
        return contains(file.getName());
    }

    /**
     * Checks whether the resource has the given file.
     *
     * @param fileName a file name to be searched
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    protected boolean contains(String fileName) {
        if (fileName == null || fileName.length() == 0) {
            return false;
        }

        Iterator<Storage> it = getStorageList().iterator();
        Storage storage = null;
        boolean result = false;

        while (it.hasNext()) {
            storage = it.next();
            if (storage.contains(fileName)) {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * Deletes the file from the storage. Also, check whether it is possible to delete the file from
     * the storage.
     *
     * @param fileName the name of the file to be deleted
     * @return the error message
     */
    private int deleteFileFromStorage(String fileName) {
        Storage tempStorage = null;
        File tempFile = null;
        int msg = DataCloudTags.FILE_DELETE_ERROR;

        for (int i = 0; i < getStorageList().size(); i++) {
            tempStorage = getStorageList().get(i);
            tempFile = tempStorage.getFile(fileName);
            tempStorage.deleteFile(fileName, tempFile);
            msg = DataCloudTags.FILE_DELETE_SUCCESSFUL;
        } // end for

        return msg;
    }

    /*
     * (non-Javadoc)
     * @see cloudsim.core.SimEntity#shutdownEntity()
     */
    @Override
    public void shutdownEntity() {
        Log.printConcatLine(getName(), " is shutting down...");
    }

    /*
     * (non-Javadoc)
     * @see cloudsim.core.SimEntity#startEntity()
     */
    @Override
    public void startEntity() {
        Log.printConcatLine(getName(), " is starting...");
        // this resource should register to regional GIS.
        // However, if not specified, then register to system GIS (the
        // default CloudInformationService) entity.
        int gisID = CloudSim.getEntityId(regionalCisName);
        if (gisID == -1) {
            gisID = CloudSim.getCloudInfoServiceEntityId();
        }

        // send the registration to GIS
        sendNow(gisID, CloudSimTags.REGISTER_RESOURCE, getId());
        // Below method is for a child class to override
        registerOtherEntity();
    }

    /**
     * Gets the host list.
     *
     * @return the host list
     */
    @SuppressWarnings("unchecked")
    public <T extends ContainerHost> List<T> getHostList() {
        return (List<T>) getCharacteristics().getHostList();
    }

    /**
     * Gets the characteristics.
     *
     * @return the characteristics
     */
    protected ContainerDatacenterCharacteristics getCharacteristics() {
        return characteristics;
    }

    /**
     * Sets the characteristics.
     *
     * @param characteristics the new characteristics
     */
    protected void setCharacteristics(ContainerDatacenterCharacteristics characteristics) {
        this.characteristics = characteristics;
    }

    /**
     * Gets the regional cis name.
     *
     * @return the regional cis name
     */
    protected String getRegionalCisName() {
        return regionalCisName;
    }

    /**
     * Sets the regional cis name.
     *
     * @param regionalCisName the new regional cis name
     */
    protected void setRegionalCisName(String regionalCisName) {
        this.regionalCisName = regionalCisName;
    }

    /**
     * Gets the vm allocation policy.
     *
     * @return the vm allocation policy
     */
    public ContainerPodAllocationPolicy getVmAllocationPolicy() {
        return vmAllocationPolicy;
    }

    /**
     * Sets the vm allocation policy.
     *
     * @param vmAllocationPolicy the new vm allocation policy
     */
    protected void setVmAllocationPolicy(ContainerPodAllocationPolicy vmAllocationPolicy) {
        this.vmAllocationPolicy = vmAllocationPolicy;
    }

    /**
     * Gets the last process time.
     *
     * @return the last process time
     */
    protected double getLastProcessTime() {
        return lastProcessTime;
    }

    /**
     * Sets the last process time.
     *
     * @param lastProcessTime the new last process time
     */
    protected void setLastProcessTime(double lastProcessTime) {
        this.lastProcessTime = lastProcessTime;
    }

    /**
     * Gets the storage list.
     *
     * @return the storage list
     */
    protected List<Storage> getStorageList() {
        return storageList;
    }

    /**
     * Sets the storage list.
     *
     * @param storageList the new storage list
     */
    protected void setStorageList(List<Storage> storageList) {
        this.storageList = storageList;
    }

    /**
     * Gets the vm list.
     *
     * @return the vm list
     */
    @SuppressWarnings("unchecked")
    public <T extends ContainerPod> List<T> getContainerVmList() {
        return (List<T>) containerVmList;
    }

    /**
     * Sets the vm list.
     *
     * @param containerVmList the new vm list
     */
    protected <T extends ContainerPod> void setContainerVmList(List<T> containerVmList) {
        this.containerVmList = containerVmList;
    }

    /**
     * Gets the scheduling interval.
     *
     * @return the scheduling interval
     */
    protected double getSchedulingInterval() {
        return schedulingInterval;
    }

    /**
     * Sets the scheduling interval.
     *
     * @param schedulingInterval the new scheduling interval
     */
    protected void setSchedulingInterval(double schedulingInterval) {
        this.schedulingInterval = schedulingInterval;
    }


    public ContainerAllocationPolicy getContainerAllocationPolicy() {
        return containerAllocationPolicy;
    }

    public void setContainerAllocationPolicy(ContainerAllocationPolicy containerAllocationPolicy) {
        this.containerAllocationPolicy = containerAllocationPolicy;
    }


    public <T extends Container> List<T> getContainerList() {
        return (List<T>) containerList;
    }

    public void setContainerList(List<? extends Container> containerList) {
        this.containerList = containerList;
    }


    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public String getLogAddress() {
        return logAddress;
    }

    public void setLogAddress(String logAddress) {
        this.logAddress = logAddress;
    }
    
    
    /**
     * Update the submission time/exec time of a job
     *
     * @param job
     * @param host
     */
    private void updateTaskExecTime(Job job, ContainerHost host) {
        double start_time = job.getExecStartTime();
        Log.printLine("job's length: " + job.getTaskList().size());
        for (Task task : job.getTaskList()) {
            task.setExecStartTime(start_time);

            double task_runtime =  (double) task.getCloudletLength() / (double)host.getTotalMips();
            Log.printLine("task id: " + task.getCloudletId() + " start time is: " + start_time + " run time is: " + task_runtime);
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
     * @param
     * @pre $none
     * @post $none
     */
    private void stageInFile2FileSystem(Job job) {
        List<FileItem> fList = job.getFileList();
        Log.printLine("**WFCDatacenter=>stageInFile2FileSystem** Job: " + job.getCloudletId());
        Log.print(job.getDepth());
        for (FileItem file : fList) {
                  Log.printLine("          file : ");
                  Log.printLine(file);
                  Log.printLine("          getDepth : ");
                  Log.printLine(job.getDepth());
            switch (WFCReplicaCatalog.getFileSystem()) {
                /**
                 * For local file system, add it to local storage (data center
                 * name)
                 */
                case LOCAL:
                    
                    WFCReplicaCatalog.addFileToStorage(file.getName(), this.getName());
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
                    WFCReplicaCatalog.addFileToStorage(file.getName(), this.getName());
                    
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
         Log.printLine("**WFCDatacenter=>processDataStageInForComputeJob**");
        
         double time = 0.0;
               for (FileItem file : requiredFiles) {
                    /* Log.printLine("          file : ");
                     Log.printLine(file);
                     Log.printLine("          getDepth : ");
                     Log.printLine(job.getDepth());*/
            //The input file is not an output File 
            if (file.isRealInputFile(requiredFiles)) {
                double maxBwth = 0.0;
                Log.printLine("file: " + file.getName());
                List siteList = WFCReplicaCatalog.getStorageList(file.getName());
                  /*Log.printLine("          siteList : ");
                  Log.printLine(siteList);*/
                if(siteList == null) {
                    continue;
                }
                if (siteList.isEmpty()) {
                    throw new Exception(file.getName() + " does not exist");
                }
                switch (WFCReplicaCatalog.getFileSystem()) {
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
                        ContainerHost host = getVmAllocationPolicy().getHost(vmId, userId);
                        ContainerPod vm = host.getContainerVm(vmId, userId);

                        boolean requiredFileStagein = true;
                        int destinationId = vmId;
                        int sourceId = vmId;
                        for (Iterator it = siteList.iterator(); it.hasNext();) {
                            //site is where one replica of this data is located at
                            String site = (String) it.next();
                            //Log.printLine("site: " + site);
                            if (site.equals(this.getName())) {
                                //Log.printLine("continue");
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
                            int destId;
                            if (site.equals(Parameters.SOURCE)) {
                                //transfers from the source to the VM is limited to the VM bw only
                                bwth = vm.getBw();
                                //bwth = dcStorage.getBaseBandwidth();
                            } else {
                                //transfers between two VMs is limited to both VMs
                                bwth = Math.min(vm.getBw(), getVmAllocationPolicy().getHost(Integer.parseInt(site), userId).getContainerVm(Integer.parseInt(site), userId).getBw());
                                //bwth = dcStorage.getBandwidth(Integer.parseInt(site), vmId);
                            }
                            if (bwth > maxBwth) {
                                //Log.printLine(site);
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
                        WFCReplicaCatalog.addFileToStorage(file.getName(), Integer.toString(vmId));
                        break;
                }
            }
        }
        return time;
    }
    
    
    
    private void register(Cloudlet cl) {
        
        Log.printLine("**WFCDatacenter=>register** + cl: " + cl.getCloudletId());
        
        Task tl = (Task) cl;
        List<FileItem> fList = tl.getFileList();
        for (FileItem file : fList) {
            Log.printLine("          file : ");
            Log.printLine(file);
            Log.printLine("          task-getDepth : ");
            Log.printLine(tl.getDepth());
            if (file.getType() == Parameters.FileType.OUTPUT)//output file
            {
                switch (WFCReplicaCatalog.getFileSystem()) {
                    case SHARED:
                        WFCReplicaCatalog.addFileToStorage(file.getName(), this.getName());
                        break;
                    case LOCAL:
                        int vmId = cl.getVmId();
                        int userId = cl.getUserId();
                        ContainerHost host = getVmAllocationPolicy().getHost(vmId, userId);
                        /**
                         * Left here for future work
                         */
                        ContainerPod vm = (ContainerPod) host.getContainerVm(vmId, userId);
                        WFCReplicaCatalog.addFileToStorage(file.getName(), Integer.toString(vmId));
                        break;
                }
            }
        }
    }

    protected double updateCloudletProcessingWithoutSchedulingFutureEventsForce() {
        Log.printLine("WFCDatacenter: updateCloudletProcessingWithoutSchedulingFutureEventsForce");
        double currentTime = CloudSim.clock();
        double minTime = Double.MAX_VALUE;
        double timeDiff = currentTime - getLastProcessTime();
        double timeFrameDatacenterEnergy = 0.0;
        for(PowerContainerHost host: this.<PowerContainerHost>getHostList()) {
            double time = host.updateContainerVmsProcessing(currentTime);
            if(time < minTime) {
                minTime = time;
            }
            Log.formatLine(
                    "%.2f: [Host #%d] utilization is %.2f%%",
                    currentTime,
                    host.getId(),
                    host.getUtilizationOfCpu() * 100);
        }
        if(timeDiff > 0) {
            for(PowerContainerHost host: this.<PowerContainerHost>getHostList()) {
                double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
                double utilizationOfCpu = host.getUtilizationOfCpu();
                double timeFrameHostEnergy = host.getEnergyLinearInterpolation(
                        previousUtilizationOfCpu,
                        utilizationOfCpu,
                        timeDiff
                );
            }
        }
        return 0.0;
    }
}


