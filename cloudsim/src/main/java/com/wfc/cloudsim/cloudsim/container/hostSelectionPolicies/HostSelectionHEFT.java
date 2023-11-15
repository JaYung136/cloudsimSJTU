package com.wfc.cloudsim.cloudsim.container.hostSelectionPolicies;

import com.wfc.cloudsim.cloudsim.Consts;
import com.wfc.cloudsim.cloudsim.Log;
import com.wfc.cloudsim.cloudsim.container.core.ContainerHost;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;
import com.wfc.cloudsim.workflowsim.FileItem;
import com.wfc.cloudsim.workflowsim.Task;
import com.wfc.cloudsim.workflowsim.utils.Parameters;

import java.util.*;

public class HostSelectionHEFT extends HostSelectionPolicy{
    private Map<Task, Map<ContainerHost, Double>> computationCosts;
    private Map<Task, Map<Task, Double>> transferCosts;
    private Map<Task, Double> rank;
    private Map<ContainerHost, List<Event>> schedules;
    private Map<Task, Double> earliestFinishTimes;
    private List<Task> tasks;
    private List<ContainerHost> hosts;
    private Map<Integer, Integer> task2Pod;
    private Map<Integer, ContainerPod> task2Pods;
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

    public HostSelectionHEFT() {
        computationCosts = new HashMap<>();

        transferCosts = new HashMap<>();
        rank = new HashMap<>();
        earliestFinishTimes = new HashMap<>();
        schedules = new HashMap<>();

    }

    /**
     * The main function
     */
    public void run(List<Task> tasks, List<ContainerHost> hostList, List<ContainerPod> podList) {
        Log.printLine("HEFT planner running with " + tasks.size()
                + " tasks and " + hostList.size() + "hosts");
        this.hosts = new ArrayList<>(hostList);
        this.tasks = new ArrayList<>(tasks);
        averageBandwidth = calculateAverageBandwidth();

       /* for (Object vmObject : getVmList()) {
            ContainerPod vm = (ContainerPod) vmObject;
            schedules.put(vm, new ArrayList<>());
        }*/
        for (ContainerHost h: hostList) {
                schedules.put(h, new ArrayList<>());
        }

        // Prioritization phase
        //Log.printLine("calculateComputationCosts");
        calculateComputationCosts();
        //Log.printLine("calculateTransferCosts");
        calculateTransferCosts();
        //Log.printLine("calculateRanks");
        calculateRanks();
        //Log.printLine("allocateTasks");
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
        for (ContainerHost h : this.hosts) {
            avg += h.getBw();
        }

        return avg / this.hosts.size();
    }


    /**
     * Populates the computationCosts field with the time in seconds to compute
     * a task in a vm.
     */
    private void calculateComputationCosts() {
        for (Task task : this.tasks) {

            Map<ContainerHost, Double> costsHosts = new HashMap<>();
            for (ContainerHost h : this.hosts) {
                if (h.getNumberOfPes() < task.getNumberOfPes()) {
                    costsHosts.put(h, Double.MAX_VALUE);
                } else {
                    double cost = (double)task.getCloudletTotalLength() / (double)h.getTotalMips();
                    costsHosts.put(h, cost);
                    //Log.printLine("host " + h.getId() + "'s cost is " + cost);
                }
            }
            //Log.printLine("calculate task " + task.getCloudletId() + " computation cost with " + costsHosts.size());
            computationCosts.put(task, costsHosts);
        }
    }

    /**
     * Populates the transferCosts map with the time in seconds to transfer all
     * files from each parent to each child
     */
    private void calculateTransferCosts() {
        // Initializing the matrix
        for (Task task1 : this.tasks) {
            Map<Task, Double> taskTransferCosts = new HashMap<>();
            for (Task task2 : this.tasks) {
                taskTransferCosts.put(task2, 0.0);
            }
            transferCosts.put(task1, taskTransferCosts);
        }

        // Calculating the actual values
        for (Task parent : this.tasks) {
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
        for (Task task : this.tasks) {
            //Log.printLine("calculate task " + task.getCloudletId() + "'s rank");
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
            //Log.printLine("contain");
            return rank.get(task);
        }
        //Log.printLine("calculate task " + task.getCloudletId() + "'s rank");
        double averageComputationCost = 0.0;

        for (Double cost : computationCosts.get(task).values()) {
            averageComputationCost += cost;
        }

        averageComputationCost /= computationCosts.get(task).size();

        double max = 0.0;
        for (Task child : task.getChildList()) {
            double childCost = transferCosts.get(task).get(child)
                    + calculateRank(child);
            max = Math.max(max, childCost);
        }

        rank.put(task, averageComputationCost + max);
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
        ContainerHost chosenHost = null;
        double earliestFinishTime = Double.MAX_VALUE;
        double bestReadyTime = 0.0;
        double finishTime;
        //Log.printLine("========================================================");
        //Log.printLine("allocate task: " + task.getCloudletId() + " with length of " + task.getCloudletLength());
        for (ContainerHost h : this.hosts) {

                double minReadyTime = 0.0;

                for (Task parent : task.getParentList()) {
                    double readyTime = earliestFinishTimes.get(parent);
                    if (parent.getHostId() != h.getId()) {
                        readyTime += transferCosts.get(parent).get(task);
                    }
                    minReadyTime = Math.max(minReadyTime, readyTime);
                }
                //Log.printLine("host " + h.getId() + "'s minReadyTime: " + minReadyTime);

                finishTime = findFinishTime(task, h, minReadyTime, false);
                //Log.printLine("host " + h.getId() + "'s finishTime: " + finishTime);
                if (finishTime < earliestFinishTime) {
                   /* if(chosenHost.isSuitableForContainerVm(task2Pods.get(task.getCloudletId()))) {*/
                        bestReadyTime = minReadyTime;
                        earliestFinishTime = finishTime;
                        chosenHost = h;
                    //}
                }

        }
        chosenHost.containerVmCreate(task2Pods.get(task.getCloudletId()));
        findFinishTime(task, chosenHost, bestReadyTime, true);
        earliestFinishTimes.put(task, earliestFinishTime);

        task.setHostId(chosenHost.getId());
    }


    private double findFinishTime(Task task,  ContainerHost h, double readyTime,
                                  boolean occupySlot) {
        List<Event> sched = schedules.get(h);
        double computationCost = computationCosts.get(task).get(h);
        //Log.printLine("task " + task.getCloudletId() + "'s computation cost in host " + h.getId() + " is " + computationCost);
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

    @Override
    public ContainerHost getHost(List<ContainerHost> hostList, Object obj, Set<? extends ContainerHost> excludedHostList) {
        ContainerHost choseHost = null;
        for(ContainerHost host : hostList) {
            if(excludedHostList.contains(host)){
                continue;
            }
            choseHost = host;
            break;
        }
        return choseHost;
    }

    public ContainerPod getPod(int podId, List<ContainerPod> pods) {
        for(int i = 0; i < pods.size(); i++) {
            if(pods.get(i).getId() == podId) {
                return pods.get(i);
            }
        }
        return null;
    }



    @Override
    public Map<Integer, ContainerHost> getHosts(List<ContainerHost> hostList, List<ContainerPod> pods) {
        Log.printLine("HEFT: getHosts " + hostList.size() + " pods " + pods.size());
        Map<Integer, ContainerHost> ret = new HashMap<>();
        List<Task> tasks = new ArrayList<>();
        task2Pod = new HashMap<>();
        task2Pods = new HashMap<>();
        for(int i = 0; i < pods.size(); i++) {
            Integer podId = pods.get(i).getId();
            Integer taskId = pods.get(i).getTasks().get(0).getCloudletId();
            //Log.printLine("PodId: " + podId + "    TaskId: " + taskId);
            task2Pod.put(taskId, podId);
            task2Pods.put(taskId, pods.get(i));
            tasks.addAll(pods.get(i).getTasks());
        }
        run(tasks, hostList, pods);
        //Log.printLine("tasks: " + tasks.size());
        //Log.printLine("pods: " + pods.size());
        for(int i = 0; i < this.tasks.size(); i++) {
            //Log.printLine("task's id: " + this.tasks.get(i).getCloudletId());
            ContainerPod pod = pods.get(task2Pod.get(this.tasks.get(i).getCloudletId()) - 1);
            //Log.printLine("pod's id: " + pod.getId());
            ContainerHost h = null;
            for(int j = 0; j < hostList.size(); j++) {
                if(hostList.get(j).getId() == this.tasks.get(i).getHostId()) {
                    h = hostList.get(j);
                }
            }
            ret.put(pod.getId(), h);
        }
        /*for(int i = 0; i < this.hosts.size(); i++) {
            this.hosts.get(i).containerVmDestroyAll();
        }*/
        Log.printLine("finish HEFT");
        return ret;
    }


}
