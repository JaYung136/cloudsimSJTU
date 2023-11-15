/**
 * Copyright 2019-2020 University Of Southern California
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
package com.wfc.cloudsim.workflowsim;



import com.wfc.cloudsim.cloudsim.Log;
import com.wfc.cloudsim.cloudsim.container.containerPodProvisioners.ContainerPodBwProvisionerSimple;
import com.wfc.cloudsim.cloudsim.container.containerPodProvisioners.ContainerPodPe;
import com.wfc.cloudsim.cloudsim.container.containerPodProvisioners.ContainerPodPeProvisionerSimple;
import com.wfc.cloudsim.cloudsim.container.containerPodProvisioners.ContainerPodRamProvisionerSimple;
import com.wfc.cloudsim.cloudsim.container.containerProvisioners.ContainerBwProvisionerSimple;
import com.wfc.cloudsim.cloudsim.container.containerProvisioners.ContainerPe;
import com.wfc.cloudsim.cloudsim.container.containerProvisioners.ContainerRamProvisionerSimple;
import com.wfc.cloudsim.cloudsim.container.containerProvisioners.CotainerPeProvisionerSimple;
import com.wfc.cloudsim.cloudsim.container.core.*;
import com.wfc.cloudsim.cloudsim.container.schedulers.ContainerCloudletSchedulerDynamicWorkload;
import com.wfc.cloudsim.cloudsim.container.schedulers.ContainerPodSchedulerTimeSharedOverSubscription;
import com.wfc.cloudsim.cloudsim.container.schedulers.ContainerSchedulerTimeSharedOverSubscription;
import com.wfc.cloudsim.cloudsim.container.utils.IDs;
import com.wfc.cloudsim.wfc.core.WFCConstants;
import com.wfc.cloudsim.workflowsim.failure.FailureParameters;
import com.wfc.cloudsim.workflowsim.utils.DistributionGenerator;
import com.wfc.cloudsim.workflowsim.utils.Parameters;
import com.wfc.cloudsim.workflowsim.utils.WFCReplicaCatalog;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * YamlUtil parse a DAX into tasks so that WorkflowSim can manage them
 *
 * @author Arman Riazi
 * @since WorkflowSim Toolkit 1.0
 * @date Aug 23, 2013
 * @date Nov 9, 2014
 */
public final class YamlUtil {

    /**
     * The path to DAX file.
     */
    private final String daxPath;
    /**
     * The path to DAX files.
     */
    private final List<String> daxPaths;
    /**
     * All tasks.
     */
    private List<Task> taskList;

    private List<PowerContainerHost> hostList;

    private List<ContainerPod> podList;

    private List<Container> containerList;
    /**
     * User id. used to create a new task.
     */
    private final int userId;

    /**
     * current job id. In case multiple workflow submission
     */
    private int jobIdStartsFrom;

    private DistributionGenerator.DistributionFamily distributionFamily;

    public Integer scale;
    public Integer shape;
    public List<PowerContainerHost> getHostList() {
        return hostList;
    }

    public void setHostList(List<PowerContainerHost> hostList) {
        this.hostList = hostList;
    }

    /**
     * Gets the task list
     *
     * @return the task list
     */
    @SuppressWarnings("unchecked")
    public List<Task> getTaskList() {
        return taskList;
    }

    public List<ContainerPod> getPodList() {return podList;}

    public List<Container> getContainerList() {return containerList;}

    /**
     * Sets the task list
     *
     * @param taskList the task list
     */
    protected void setTaskList(List<Task> taskList) {
        this.taskList = taskList;
    }
    /**
     * Map from task name to task.
     */
    protected Map<String, Task> mName2Task;

    /**
     * Initialize a WorkflowParser
     *
     * @param userId the user id. Currently we have just checked single user
     * mode
     */
    public YamlUtil(int userId) {
        this.userId = userId;
        this.mName2Task = new HashMap<>();
        this.daxPath = Parameters.getDaxPath();
        this.daxPaths = Parameters.getDAXPaths();
        this.jobIdStartsFrom = 1;
        this.hostList = new ArrayList<>();
        this.podList = new ArrayList<>();
        this.containerList = new ArrayList<>();
        setTaskList(new ArrayList<>());
    }

    /**
     * Start to parse a workflow which is a xml file(s).
     */
    public void parse() {
        if (this.daxPath != null) {
            parseXmlFile(this.daxPath);
        } else if (this.daxPaths != null) {
            for (String path : this.daxPaths) {
                parseXmlFile(path);
            }
        }
    }

    public void parseHostXml(String path) {
        parseXmlFile(path);
    }

    /**
     * Sets the depth of a task
     *
     * @param task the task
     * @param depth the depth
     */
    private void setDepth(Task task, int depth) {
        if (depth  > task.getDepth()) {
            task.setDepth(depth);
        }
        for (Task cTask : task.getChildList()) {
            setDepth(cTask, task.getDepth()+1 );
        }
    }

    /**
     * Parse a DAX file with jdom
     */
    private void parseXmlFile(String path) {

        try {

            SAXBuilder builder = new SAXBuilder();
            //parse using builder to get DOM representation of the XML file
            Document dom = builder.build(new File(path));
            Element root = dom.getRootElement();
            List<Element> list = root.getChildren();
            for (Element node : list) {
                Log.printLine("node: " + node.getName().toLowerCase());
                switch (node.getName().toLowerCase()) {
                    case "node":
                        String name = node.getAttributeValue("name");
                        String memory = node.getAttributeValue("memory");
                        String storage = node.getAttributeValue("storage");
                        Log.printLine(storage);
                        String cores = node.getAttributeValue("cores");
                        String mips = node.getAttributeValue("mips");
                        String bandwidth = node.getAttributeValue("bandwidth");
                        Integer memory_MB = Integer.parseInt(memory);
                        Long storage_MB = Long.parseLong(storage);
                        Integer pes_size = Integer.parseInt(cores);
                        Double pe_mips = Double.parseDouble(mips);
                        Long bandwidth_gbps = Long.parseLong(bandwidth);
                        List<ContainerPodPe> pes = new ArrayList<>();
                        for(int i = 0; i < pes_size; i++) {
                            pes.add(new ContainerPodPe(i,new ContainerPodPeProvisionerSimple(pe_mips)));
                        }

                        PowerContainerHostUtilizationHistory host = new PowerContainerHostUtilizationHistory(IDs.pollId(ContainerHost.class),
                                new ContainerPodRamProvisionerSimple(memory_MB),
                                new ContainerPodBwProvisionerSimple(bandwidth_gbps), storage_MB , pes,
                                new ContainerPodSchedulerTimeSharedOverSubscription(pes),
                                //new ContainerPodSchedulerTimeShared(peList),
                                WFCConstants.HOST_POWER[2]);
                        host.setName(name);
                        this.hostList.add(host);
                        WFCConstants.hostMips.put(host.getId(), pe_mips);
                        break;
                    case "job":
                        long length = 0;
                        String nodeName = node.getAttributeValue("id");
                        String nodeType = node.getAttributeValue("name");
                        /**
                         * capture runtime. If not exist, by default the runtime
                         * is 0.1. Otherwise CloudSim would ignore this task.
                         * BUG/#11
                         */
                        double runtime;
                        if (node.getAttributeValue("runtime") != null) {
                            String nodeTime = node.getAttributeValue("runtime");
                            runtime = 1000 * Double.parseDouble(nodeTime);
                            if (runtime < 100) {
                                runtime = 100;
                            }
                            length = (long) runtime;
                        } else {
                            Log.printLine("Cannot find runtime for " + nodeName + ",set it to be 0");
                        }   //multiple the scale, by default it is 1.0
                        length *= Parameters.getRuntimeScale();
                        List<Element> fileList = node.getChildren();
                        List<FileItem> mFileList = new ArrayList<>();
                        for (Element file : fileList) {
                            if (file.getName().toLowerCase().equals("uses")) {
                                String fileName = file.getAttributeValue("name");//DAX version 3.3
                                if (fileName == null) {
                                    fileName = file.getAttributeValue("file");//DAX version 3.0
                                }
                                if (fileName == null) {
                                    Log.print("Error in parsing xml");
                                }

                                String inout = file.getAttributeValue("link");
                                double size = 0.0;

                                String fileSize = file.getAttributeValue("size");
                                if (fileSize != null) {
                                    size = Double.parseDouble(fileSize) /*/ 1024*/;
                                } else {
                                    Log.printLine("File Size not found for " + fileName);
                                }

                                /**
                                 * a bug of cloudsim, size 0 causes a problem. 1
                                 * is ok.
                                 */
                                if (size == 0) {
                                    size++;
                                }
                                /**
                                 * Sets the file type 1 is input 2 is output
                                 */
                                Parameters.FileType type = Parameters.FileType.NONE;
                                switch (inout) {
                                    case "input":
                                        type = Parameters.FileType.INPUT;
                                        break;
                                    case "output":
                                        type = Parameters.FileType.OUTPUT;
                                        break;
                                    default:
                                        Log.printLine("Parsing Error");
                                        break;
                                }
                                FileItem tFile;
                                /*
                                 * Already exists an input file (fcom.wfc.cloudsimet output file)
                                 */
                                if (size < 0) {
                                    /*
                                     * Assuming it is a parsing error
                                     */
                                    size = 0 - size;
                                    Log.printLine("Size is negative, I assume it is a parser error");
                                }
                                /*
                                 * Note that CloudSim use size as MB, in this case we use it as Byte
                                 */
                                if (type == Parameters.FileType.OUTPUT) {
                                    /**
                                     * It is good that CloudSim does tell
                                     * whether a size is zero
                                     */
                                    tFile = new FileItem(fileName, size);
                                } else if (WFCReplicaCatalog.containsFile(fileName)) {
                                    tFile = WFCReplicaCatalog.getFile(fileName);
                                } else {

                                    tFile = new FileItem(fileName, size);
                                    WFCReplicaCatalog.setFile(fileName, tFile);
                                }

                                tFile.setType(type);
                                mFileList.add(tFile);

                            }
                        }
                        Task task;
                        //In case of multiple workflow submission. Make sure the jobIdStartsFrom is consistent.
                        synchronized (this) {
                            task = new Task(this.jobIdStartsFrom, length);
                            this.jobIdStartsFrom++;
                        }
                        task.setType(nodeType);
                        task.setUserId(userId);
                        mName2Task.put(nodeName, task);
                        for (FileItem file : mFileList) {
                            task.addRequiredFile(file.getName());
                        }
                        task.setFileList(mFileList);
                        this.getTaskList().add(task);

                        /**
                         * Add dependencies info.
                         */
                        break;
                    case "child":
                        List<Element> pList = node.getChildren();
                        String childName = node.getAttributeValue("ref");
                        if (mName2Task.containsKey(childName)) {

                            Task childTask = (Task) mName2Task.get(childName);

                            for (Element parent : pList) {
                                String parentName = parent.getAttributeValue("ref");
                                if (mName2Task.containsKey(parentName)) {
                                    Task parentTask = (Task) mName2Task.get(parentName);
                                    parentTask.addChild(childTask);
                                    childTask.addParent(parentTask);
                                }
                            }
                        }
                        break;

                    case "faultgenerator":
                        String fType = node.getAttributeValue("type");
                        if(fType.equals("LogNormal")) this.distributionFamily = DistributionGenerator.DistributionFamily.LOGNORMAL;
                        if(fType.equals("Weibull")) this.distributionFamily = DistributionGenerator.DistributionFamily.WEIBULL;
                        if(fType.equals("Gamma")) this.distributionFamily = DistributionGenerator.DistributionFamily.GAMMA;
                        if(fType.equals("Normal")) this.distributionFamily = DistributionGenerator.DistributionFamily.NORMAL;
                        for(Element element: node.getChildren()) {
                            if(element.getName().equals("scale")) this.scale = Integer.parseInt(element.getText());
                            if(element.getName().equals("shape")) this.shape = Integer.parseInt(element.getText());
                        }


                    case "application":
                        String aName = node.getAttributeValue("Name");
                        String aMemBss = node.getAttributeValue("MemoryBssSize");
                        String aMemeData = node.getAttributeValue("MemoryDataSize");
                        String aMemPersistBss = node.getAttributeValue("MemoryPersistentBssSize");
                        String aMemPersistData = node.getAttributeValue("MemoryPersistentDataSize");
                        String aMemText = node.getAttributeValue("MemoryTextSize");
                        String requiredMem = node.getAttributeValue("RequiredMemorySize");
                        String upBandwidth = node.getAttributeValue("UpBandwidth");
                        String downBandwidth = node.getAttributeValue("DownBandwidth");
                        String computeTime = node.getAttributeValue("ComputeTime");
                        String ip = node.getAttributeValue("IpAddress");
                        Integer memBss = Integer.parseInt(aMemBss);
                        Integer memData = Integer.parseInt(aMemeData);
                        Integer memPersistBss = Integer.parseInt(aMemPersistBss);
                        Integer memPersistData = Integer.parseInt(aMemPersistData);
                        Integer memText = Integer.parseInt(aMemText);
                        Integer reqMem = Integer.parseInt(requiredMem);
                        Integer upB = Integer.parseInt(upBandwidth);
                        Integer downB = Integer.parseInt(downBandwidth);
                        Double computeT = Double.parseDouble(computeTime);
                        long runtimeT = (long) (1000 * computeT);
                        if (runtimeT < 100) {
                            runtimeT = 100;
                        }
                        int size = this.podList.size();
                        int sizeId = size + 1;
                        ArrayList peList = new ArrayList();
                        for (int p = 0; p < WFCConstants.WFC_NUMBER_VM_PES ; p++) {
                            peList.add(new ContainerPe(p, new CotainerPeProvisionerSimple((double)WFCConstants.WFC_VM_MIPS * WFCConstants.WFC_VM_RATIO)));
                        }
                        ContainerPod cp = new PowerContainerPod(sizeId, 0, WFCConstants.WFC_VM_MIPS, (float)reqMem,
                                (upB+downB) / 2, memBss+memData+memPersistBss+memPersistData+memText,  WFCConstants.WFC_VM_VMM,
                                new ContainerSchedulerTimeSharedOverSubscription(peList),
                                //new ContainerSchedulerTimeSharedOverSubscription(peList),
                                new ContainerRamProvisionerSimple(reqMem),
                                new ContainerBwProvisionerSimple(upB+downB), peList,
                                WFCConstants.WFC_DC_SCHEDULING_INTERVAL);
                       this.podList.add(cp);
                        Container c = new PowerContainer(sizeId, 0, (double) WFCConstants.WFC_CONTAINER_MIPS ,
                                WFCConstants.WFC_CONTAINER_PES_NUMBER , reqMem,
                                (upB+downB) /2, memBss+memData+memPersistBss+memPersistData+memText, WFCConstants.WFC_CONTAINER_VMM,
                                //new ContainerCloudletSchedulerTimeShared(),WFCConstants.WFC_DC_SCHEDULING_INTERVAL);
                                new ContainerCloudletSchedulerDynamicWorkload(sizeId, WFCConstants.WFC_CONTAINER_MIPS, WFCConstants.WFC_CONTAINER_PES_NUMBER),
                                WFCConstants.WFC_DC_SCHEDULING_INTERVAL);
                        c.setPodId(cp.getUid());
                        c.setIp(ip);
                        this.containerList.add(c);
                        runtimeT *= Parameters.getRuntimeScale();
                        List<Element> fileListT = node.getChildren();
                        List<FileItem> mFileListT = new ArrayList<>();
                        for (Element file : fileListT) {
                            if (file.getName().toLowerCase().equals("uses")) {
                                String fileName = file.getAttributeValue("name");//DAX version 3.3
                                if (fileName == null) {
                                    fileName = file.getAttributeValue("file");//DAX version 3.0
                                }
                                if (fileName == null) {
                                    Log.print("Error in parsing xml");
                                }

                                String inout = file.getAttributeValue("link");
                                double sizeT = 0.0;

                                String fileSizeT = file.getAttributeValue("size");
                                if (fileSizeT != null) {
                                    sizeT = Double.parseDouble(fileSizeT) /*/ 1024*/;
                                } else {
                                    Log.printLine("File Size not found for " + fileName);
                                }

                                /**
                                 * a bug of cloudsim, size 0 causes a problem. 1
                                 * is ok.
                                 */
                                if (sizeT == 0) {
                                    sizeT++;
                                }
                                /**
                                 * Sets the file type 1 is input 2 is output
                                 */
                                Parameters.FileType type = Parameters.FileType.NONE;
                                switch (inout) {
                                    case "input":
                                        type = Parameters.FileType.INPUT;
                                        break;
                                    case "output":
                                        type = Parameters.FileType.OUTPUT;
                                        break;
                                    default:
                                        Log.printLine("Parsing Error");
                                        break;
                                }
                                FileItem tFile;
                                /*
                                 * Already exists an input file (fcom.wfc.cloudsimet output file)
                                 */
                                if (size < 0) {
                                    /*
                                     * Assuming it is a parsing error
                                     */
                                    size = 0 - size;
                                    Log.printLine("Size is negative, I assume it is a parser error");
                                }
                                /*
                                 * Note that CloudSim use size as MB, in this case we use it as Byte
                                 */
                                if (type == Parameters.FileType.OUTPUT) {
                                    /**
                                     * It is good that CloudSim does tell
                                     * whether a size is zero
                                     */
                                    tFile = new FileItem(fileName, size);
                                } else if (WFCReplicaCatalog.containsFile(fileName)) {
                                    tFile = WFCReplicaCatalog.getFile(fileName);
                                } else {

                                    tFile = new FileItem(fileName, size);
                                    WFCReplicaCatalog.setFile(fileName, tFile);
                                }

                                tFile.setType(type);
                                mFileListT.add(tFile);

                            }
                        }
                        Task taskT;
                        //In case of multiple workflow submission. Make sure the jobIdStartsFrom is consistent.
                        synchronized (this) {
                            taskT = new Task(this.jobIdStartsFrom, runtimeT);
                            this.jobIdStartsFrom++;
                        }
                        taskT.setType(aName);
                        taskT.setUserId(userId);
                        mName2Task.put(aName, taskT);
                        for (FileItem file : mFileListT) {
                            taskT.addRequiredFile(file.getName());
                        }
                        taskT.setFileList(mFileListT);
                        this.getTaskList().add(taskT);
                    case "":
                }
            }
            /**
             * If a task has no parent, then it is root task.
             */
            ArrayList roots = new ArrayList<>();
            for (Task task : mName2Task.values()) {
                task.setDepth(0);
                if (task.getParentList().isEmpty()) {
                    roots.add(task);
                }
            }

            /**
             * Add depth from top to bottom.
             */
            for (Iterator it = roots.iterator(); it.hasNext();) {
                Task task = (Task) it.next();
                setDepth(task, 1);
            }
            /**
             * Clean them so as to save memory. Parsing workflow may take much
             * memory
             */
            this.mName2Task.clear();

        } catch (JDOMException jde) {
            Log.printLine("JDOM Exception;Please make sure your dax file is valid");

        } catch (IOException ioe) {
            Log.printLine("IO Exception;Please make sure dax.path is correctly set in your config file");

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Parsing Exception");
        }
    }
}
