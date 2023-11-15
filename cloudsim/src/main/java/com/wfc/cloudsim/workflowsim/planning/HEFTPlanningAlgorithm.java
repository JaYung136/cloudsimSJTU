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
package com.wfc.cloudsim.workflowsim.planning;

import com.wfc.cloudsim.cloudsim.*;
import com.wfc.cloudsim.cloudsim.container.core.Container;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;
import com.wfc.cloudsim.workflowsim.FileItem;
import com.wfc.cloudsim.workflowsim.Task;
import com.wfc.cloudsim.workflowsim.utils.Parameters;

import java.util.*;

/**
 * The HEFT planning algorithm.
 *
 * @author Pedro Paulo Vezzá Campos
 * @date Oct 12, 2013
 */
public class HEFTPlanningAlgorithm extends BasePlanningAlgorithm {

    private Map<Task, Map<ContainerPod, Double>> computationCosts;
    private Map<Task, Map<Container, Double>> computationCostsInContainer;
    private Map<Task, Map<Task, Double>> transferCosts;
    private Map<Task, Double> rank;
    private Map<ContainerPod, List<Event>> schedules;
    private Map<Container, List<Event>> schedulersInContainer;
    private Map<Task, Double> earliestFinishTimes;
    private double averageBandwidth;

    private class Event {

        public double start;
        public double finish;

        public Event(double start, double finish) {
            this.start = start;
            this.finish = finish;
        }
    }

    private class TaskRank implements Comparable<TaskRank> {

        public Task task;
        public Double rank;

        public TaskRank(Task task, Double rank) {
            this.task = task;
            this.rank = rank;
        }

        @Override
        public int compareTo(TaskRank o) {
            return o.rank.compareTo(rank);
        }
    }

    public HEFTPlanningAlgorithm() {
        computationCosts = new HashMap<>();
        computationCostsInContainer = new HashMap<>();
        transferCosts = new HashMap<>();
        rank = new HashMap<>();
        earliestFinishTimes = new HashMap<>();
        schedules = new HashMap<>();
        schedulersInContainer = new HashMap<>();
    }

    /**
     * The main function
     */
    @Override
    public void run() {
        Log.printLine("HEFT planner running with " + getTaskList().size()
                + " tasks and " + getVmList().size() + "pods");

        averageBandwidth = calculateAverageBandwidth();

       /* for (Object vmObject : getVmList()) {
            ContainerPod vm = (ContainerPod) vmObject;
            schedules.put(vm, new ArrayList<>());
        }*/
        for (Object podObject: getVmList()) {
            Log.printLine("pod " + ((ContainerPod)podObject).getId() + "has " + ((ContainerPod)podObject).getContainerList().size());
            for(Container c: ((ContainerPod)podObject).getContainerList()) {
                schedulersInContainer.put(c, new ArrayList<>());
            }
        }

        // Prioritization phase
        calculateComputationCosts();
        Log.printLine("calculateTransferCosts");
        calculateTransferCosts();
        Log.printLine("calculateRanks");
        calculateRanks();
        Log.printLine("allocateTasks");
        // Selection phase
        allocateTasks();
    }

    /**
     * Calculates the average available bandwidth among all VMs in Mbit/s
     *
     * @return Average available bandwidth in Mbit/s
     */
    private double calculateAverageBandwidth() {
        double avg = 0.0;
        for (Object vmObject : getVmList()) {
            ContainerPod vm = (ContainerPod) vmObject;
            avg += vm.getBw();
        }
        int cSize = 0;
        for(Object pod: getVmList()) {
            cSize += ((ContainerPod)pod).getContainerList().size();
        }
        return avg / cSize;
    }

    private double calculateAverageBandwidthOfHosts() {
        double avg = 0.0;
        Integer length = 0;
        for (Object datacenter: getDatacenterList()) {
            Datacenter datacenter1 = (Datacenter) datacenter;
            for(Object host: datacenter1.getHostList()) {
                Host host1 = (Host) host;
                avg += host1.getBw();
                length ++;
            }
        }
        return avg / length;
    }

    /**
     * Populates the computationCosts field with the time in seconds to compute
     * a task in a vm.
     */
    private void calculateComputationCosts() {
        for (Task task : getTaskList()) {
            Map<ContainerPod, Double> costsVm = new HashMap<>();
            Map<Container, Double> costsContainer = new HashMap<>();
            for (Object vmObject : getVmList()) {
                ContainerPod vm = (ContainerPod) vmObject;
                for(Container c: vm.getContainerList()) {
                    if (c.getNumberOfPes() < task.getNumberOfPes()) {
                        costsContainer.put(c, Double.MAX_VALUE);
                    } else {
                        costsContainer.put(c,
                                task.getCloudletTotalLength() / c.getMips());
                    }
                }
            }
            computationCostsInContainer.put(task, costsContainer);
        }
    }

    private void calculateComputationCostsInContainer() {
        for (Task task: getTaskList()) {
            Map<Container, Double> costsContainer = new HashMap<>();
            for (Object podObject: getVmList()) {
                ContainerPod pod = (ContainerPod) podObject;
                for(Object containerObject: pod.getContainerList()) {
                    Container container = (Container) containerObject;
                    if(container.getNumberOfPes() < task.getNumberOfPes()) {
                        costsContainer.put(container, Double.MAX_VALUE);
                    } else {
                        costsContainer.put(container,
                                task.getCloudletTotalLength() / container.getMips());
                    }
                }
            }
            computationCostsInContainer.put(task, costsContainer);
        }
    }

    /**
     * Populates the transferCosts map with the time in seconds to transfer all
     * files from each parent to each child
     */
    private void calculateTransferCosts() {
        // Initializing the matrix
        for (Task task1 : getTaskList()) {
            Map<Task, Double> taskTransferCosts = new HashMap<>();
            for (Task task2 : getTaskList()) {
                taskTransferCosts.put(task2, 0.0);
            }
            transferCosts.put(task1, taskTransferCosts);
        }

        // Calculating the actual values
        for (Task parent : getTaskList()) {
            for (Task child : parent.getChildList()) {
                transferCosts.get(parent).put(child,
                        calculateTransferCost(parent, child));
            }
        }
    }

    /**
     * Accounts the time in seconds necessary to transfer all files described
     * between parent and child
     *
     * @param parent
     * @param child
     * @return Transfer cost in seconds
     */
    private double calculateTransferCost(Task parent, Task child) {
        List<FileItem> parentFiles = parent.getFileList();
        List<FileItem> childFiles = child.getFileList();

        double acc = 0.0;

        for (FileItem parentFile : parentFiles) {
            if (parentFile.getType() != Parameters.FileType.OUTPUT) {
                continue;
            }

            for (FileItem childFile : childFiles) {
                if (childFile.getType() == Parameters.FileType.INPUT
                        && childFile.getName().equals(parentFile.getName())) {
                    acc += childFile.getSize();
                    break;
                }
            }
        }

        //file Size is in Bytes, acc in MB
        acc = acc / Consts.MILLION;
        // acc in MB, averageBandwidth in Mb/s
        return acc * 8 / averageBandwidth;
    }

    /**
     * Invokes calculateRank for each task to be scheduled
     */
    private void calculateRanks() {
        Log.printLine("ranks: " + getTaskList().size());
        for (Task task : getTaskList()) {
            calculateRank(task);
        }
    }

    /**
     * Populates rank.get(task) with the rank of task as defined in the HEFT
     * paper.
     *
     * @param task The task have the rank calculates
     * @return The rank
     */
    private double calculateRank(Task task) {
        if (rank.containsKey(task)) {
            Log.printLine("contain");
            return rank.get(task);
        }

        double averageComputationCost = 0.0;

        for (Double cost : computationCostsInContainer.get(task).values()) {
            averageComputationCost += cost;
        }

        averageComputationCost /= computationCostsInContainer.get(task).size();

        double max = 0.0;
        for (Task child : task.getChildList()) {
            double childCost = transferCosts.get(task).get(child)
                    + calculateRank(child);
            max = Math.max(max, childCost);
        }

        rank.put(task, averageComputationCost + max);
        Log.printLine("cc" + rank.get(task));
        return rank.get(task);
    }

    /**
     * Allocates all tasks to be scheduled in non-ascending order of schedule.
     */
    private void allocateTasks() {
        List<TaskRank> taskRank = new ArrayList<>();
        for (Task task : rank.keySet()) {
            taskRank.add(new TaskRank(task, rank.get(task)));
        }

        // Sorting in non-ascending order of rank
        Collections.sort(taskRank);
        for (TaskRank rank : taskRank) {
            allocateTask(rank.task);
        }

    }

    /**
     * Schedules the task given in one of the VMs minimizing the earliest finish
     * time
     *
     * @param task The task to be scheduled
     * @pre All parent tasks are already scheduled
     */
    private void allocateTask(Task task) {
        ContainerPod chosenVM = null;
        Container chosenContainer = null;
        double earliestFinishTime = Double.MAX_VALUE;
        double bestReadyTime = 0.0;
        double finishTime;
        Log.printLine("allocate task: " + task.getCloudletId());
        for (Object vmObject : getVmList()) {
            ContainerPod vm = (ContainerPod) vmObject;
            Log.printLine("pod " + vm.getId() + " has " + vm.getContainerList().size() + " containers");
            for(Container c: vm.getContainerList()) {
                double minReadyTime = 0.0;

                for (Task parent : task.getParentList()) {
                    double readyTime = earliestFinishTimes.get(parent);
                    if (parent.getContainerId() != c.getId()) {
                        readyTime += transferCosts.get(parent).get(task);
                    }
                    minReadyTime = Math.max(minReadyTime, readyTime);
                }

                finishTime = findFinishTime(task, c, minReadyTime, false);

                if (finishTime < earliestFinishTime) {
                    bestReadyTime = minReadyTime;
                    earliestFinishTime = finishTime;
                    chosenContainer = c;
                    chosenVM = vm;

                }
            }
        }

        findFinishTime(task, chosenContainer, bestReadyTime, true);
        earliestFinishTimes.put(task, earliestFinishTime);

        task.setVmId(chosenVM.getId());
        task.setContainerId(chosenContainer.getId());
    }

    /**
     * Finds the best time slot available to minimize the finish time of the
     * given task in the vm with the constraint of not scheduling it before
     * readyTime. If occupySlot is true, reserves the time slot in the schedule.
     *
     * @param task The task to have the time slot reserved
     * @param c The container that will execute the task
     * @param readyTime The first moment that the task is available to be
     * scheduled
     * @param occupySlot If true, reserves the time slot in the schedule.
     * @return The minimal finish time of the task in the vmn
     */
    private double findFinishTime(Task task, Container c, double readyTime,
                                  boolean occupySlot) {
        List<Event> sched = schedulersInContainer.get(c);
        double computationCost = computationCostsInContainer.get(task).get(c);
        double start, finish;
        int pos;

        if (sched.isEmpty()) {
            if (occupySlot) {
                sched.add(new Event(readyTime, readyTime + computationCost));
            }
            return readyTime + computationCost;
        }

        if (sched.size() == 1) {
            if (readyTime >= sched.get(0).finish) {
                pos = 1;
                start = readyTime;
            } else if (readyTime + computationCost <= sched.get(0).start) {
                pos = 0;
                start = readyTime;
            } else {
                pos = 1;
                start = sched.get(0).finish;
            }

            if (occupySlot) {
                sched.add(pos, new Event(start, start + computationCost));
            }
            return start + computationCost;
        }

        // Trivial case: Start after the latest task scheduled
        start = Math.max(readyTime, sched.get(sched.size() - 1).finish);
        finish = start + computationCost;
        int i = sched.size() - 1;
        int j = sched.size() - 2;
        pos = i + 1;
        while (j >= 0) {
            Event current = sched.get(i);
            Event previous = sched.get(j);

            if (readyTime > previous.finish) {
                if (readyTime + computationCost <= current.start) {
                    start = readyTime;
                    finish = readyTime + computationCost;
                }

                break;
            }
            if (previous.finish + computationCost <= current.start) {
                start = previous.finish;
                finish = previous.finish + computationCost;
                pos = i;
            }
            i--;
            j--;
        }

        if (readyTime + computationCost <= sched.get(0).start) {
            pos = 0;
            start = readyTime;

            if (occupySlot) {
                sched.add(pos, new Event(start, start + computationCost));
            }
            return start + computationCost;
        }
        if (occupySlot) {
            sched.add(pos, new Event(start, finish));
        }
        return finish;
    }
}
