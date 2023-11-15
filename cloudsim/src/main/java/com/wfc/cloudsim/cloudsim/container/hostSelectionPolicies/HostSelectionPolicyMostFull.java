package com.wfc.cloudsim.cloudsim.container.hostSelectionPolicies;

import com.wfc.cloudsim.cloudsim.Log;
import com.wfc.cloudsim.cloudsim.container.core.ContainerHost;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;
import com.wfc.cloudsim.cloudsim.container.core.PowerContainerHostUtilizationHistory;
import com.wfc.cloudsim.cloudsim.core.CloudSim;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by sareh on 11/08/15.
 */
public class HostSelectionPolicyMostFull extends HostSelectionPolicy {

    @Override
    public ContainerHost getHost(List<ContainerHost> hostList, Object obj,Set<? extends ContainerHost> excludedHostList) {
        ContainerHost selectedHost = null;
        if(CloudSim.clock() >1.0){
        double maxUsage = Double.MIN_VALUE;
        for (ContainerHost host : hostList) {
            if (excludedHostList.contains(host)) {
                continue;
            }

            if (host instanceof PowerContainerHostUtilizationHistory) {
                double hostUtilization= ((PowerContainerHostUtilizationHistory) host).getUtilizationOfCpu();
                if (hostUtilization > maxUsage) {
                    maxUsage = hostUtilization;
                    selectedHost = host;

                }


            }
        }

        return selectedHost;
    }else {

//            At the simulation start all the VMs by leastFull algorithms.
            Log.formatLine("1111111");
            selectedHost = new HostSelectionPolicyFirstFit().getHost(hostList,obj ,excludedHostList);

            return selectedHost;
        }



    }

    @Override
    public Map<Integer, ContainerHost> getHosts(List<ContainerHost> hostList, List<ContainerPod> pods) {
        return null;
    }


}
