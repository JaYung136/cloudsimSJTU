package org.sim.workflowsim.scheduling;

import org.sim.cloudbus.cloudsim.Cloudlet;
import org.sim.cloudbus.cloudsim.Host;
import org.sim.cloudbus.cloudsim.Log;
import org.sim.service.Constants;
import org.sim.workflowsim.Job;

import java.util.HashMap;
import java.util.Map;

public class K8sSchedulingAlgorithm extends BaseSchedulingAlgorithm{
    private Map<Integer, Double> cpus;
    private Map<Integer, Double> rams;
    //private Map<Integer, Double> storages;

    private double leastRequestedPriority(Host host) {
        double cpu_score = (double) (host.getNumberOfPes() - cpus.get(host.getId())) / (double) host.getNumberOfPes();
        //Log.printLine("cpu_score: " + cpu_score);
        double ram_score = (double) (host.getRam() - rams.get(host.getId())) / (double) host.getRam();
        //Log.printLine("ram_score: " + ram_score);
        return 10 * (cpu_score + ram_score) / 2;
    }

    private double balancedResourceAllocation(Host host) {
        double cpu_fraction = cpus.get(host.getId()) / (double) host.getNumberOfPes();
        //Log.printLine("cpu_: " + cpu_fraction);
        double ram_fraction = rams.get(host.getId()) / (double)host.getRam();
        //Log.printLine("ram: " + ram_fraction);
        //double storage_fraction = (host.getTotal_storage() - host.getStorage()) / host.getTotal_storage();
        //Log.printLine("storage: " + storage_fraction);
        double mean = (cpu_fraction + ram_fraction) / 2;
        //Log.printLine("mean: " + mean);
        double variance = ((cpu_fraction - mean)*(cpu_fraction - mean)
                + (ram_fraction - mean)*(ram_fraction - mean)
                ) / 2;
        //Log.printLine("variance: " + variance);
        return 10 - variance * 10;
    }

    private double getScore(Host host) {
        return (balancedResourceAllocation(host) + leastRequestedPriority(host)) / 2;
    }

    @Override
    public void run() throws Exception {
        cpus = new HashMap<>();
        rams = new HashMap<>();
        for(Host h: Constants.hosts) {
            cpus.put(h.getId(), (double)h.getUsedPe());
            rams.put(h.getId(), 0.0);
        }
        int size = getCloudletList().size();
        for(int i = 0; i < size; i++) {
            double maxScore = -1;
            Integer hostId = -1;
            for(Host h: Constants.hosts) {
                /*if(((Job)getCloudletList().get(i)).getTaskList().size() >= 1) {
                    //Log.printLine("asasas");
                    Log.printLine("cpu: " + cpus.get(h.getId()));
                    Log.printLine("request: " + ((Job)getCloudletList().get(i)).getNumberOfPes());
                    Log.printLine("ram: " + rams.get(h.getId()));
                    Log.printLine("request: " + ((Job)getCloudletList().get(i)).getTaskList().get(0).getRam());
                    if(h.getNumberOfPes() < cpus.get(h.getId()) +  ((Job)getCloudletList().get(i)).getNumberOfPes()) {
                        continue;
                    }
                    if(h.getRam() < rams.get(h.getId()) + ((Job)getCloudletList().get(i)).getTaskList().get(0).getRam()) {
                        continue;
                    }
                }*/
                if(getScore(h) > maxScore) {
                    hostId = h.getId();
                }
            }
            if(hostId == -1) {
                Log.printLine("node is not enough");
                throw new Exception("node is not enough");
            }
            if(((Job)getCloudletList().get(i)).getTaskList().size() >= 1) {
                Double cpu = cpus.get(hostId);
                Double ram = rams.get(hostId);
                cpu += 1;
                ram += ((Job)getCloudletList().get(i)).getTaskList().get(0).getRam();
                Log.printLine(cpu);
                Log.printLine(ram);
                cpus.put(hostId, cpu);
                rams.put(hostId, ram);
            }
            Cloudlet c = (Cloudlet) getCloudletList().get(i);
            c.setVmId(hostId);
            Log.printLine("schedule container " + c.getCloudletId() + " to host " + hostId);
            getScheduledList().add(c);
        }
    }
}
