package com.wfc.cloudsim.cloudsim.container.hostSelectionPolicies;

import com.wfc.cloudsim.cloudsim.container.core.ContainerHost;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class HostSelectionPolicyK8s extends HostSelectionPolicy{

    private double leastRequestedPriority(ContainerHost host) {
        int free_pes = host.getNumberOfFreePes();
        int total_pes = host.getNumberOfPes();
        double cpu_score = 10 * free_pes / total_pes;
        //Log.printLine("cpu_score: " + cpu_score);
        float free_ram = host.getContainerVmRamProvisioner().getAvailableRam();
        float total_ram = host.getContainerVmRamProvisioner().getRam();
        double ram_score = 10 * free_ram / total_ram;
        //Log.printLine("ram_score: " + ram_score);
        return (cpu_score + ram_score) / 2;
    }

    private double balancedResourceAllocation(ContainerHost host) {
        double cpu_fraction = (host.getNumberOfPes() - host.getNumberOfFreePes()) / host.getNumberOfPes();
        //Log.printLine("cpu_: " + cpu_fraction);
        double ram_fraction = (host.getRam() - host.getContainerVmRamProvisioner().getAvailableRam()) / host.getRam();
        //Log.printLine("ram: " + ram_fraction);
        double storage_fraction = (host.getTotal_storage() - host.getStorage()) / host.getTotal_storage();
        //Log.printLine("storage: " + storage_fraction);
        double mean = (cpu_fraction + ram_fraction + storage_fraction) / 3;
        //Log.printLine("mean: " + mean);
        double variance = ((cpu_fraction - mean)*(cpu_fraction - mean)
                + (ram_fraction - mean)*(ram_fraction - mean)
                + (storage_fraction - mean)*(storage_fraction - mean)) / 3;
        //Log.printLine("variance: " + variance);
        return 10 - variance * 10;
    }

    private double getScore(ContainerHost host) {
        return (balancedResourceAllocation(host) + leastRequestedPriority(host)) / 2;
    }


    @Override
    public ContainerHost getHost(List<ContainerHost> hostList, Object obj, Set<? extends ContainerHost> excludedHostList) {
        double maxScore = Double.MIN_VALUE;
        ContainerHost selectedHost = null;
        for (ContainerHost host: hostList) {
            double score;
            if (excludedHostList.contains(host)) {
               // Log.printLine("asasasssss");
                continue;
            }
            score = getScore(host);
            //Log.printLine("host " + host.getId() + " 's score: " + score);
            if(score > maxScore) {
                maxScore = score;
                selectedHost = host;
            }
        }
        return selectedHost;
    }

    @Override
    public Map<Integer, ContainerHost> getHosts(List<ContainerHost> hostList, List<ContainerPod> pods) {
        return null;
    }
}
