package com.wfc.cloudsim.cloudsim.container.hostSelectionPolicies;

import com.wfc.cloudsim.cloudsim.container.core.ContainerHost;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HostSelectionMaxMin extends HostSelectionPolicy{
    private final List<Boolean> hasChecked = new ArrayList<>();

    @Override
    public ContainerHost getHost(List<ContainerHost> hostList, Object obj, Set<? extends ContainerHost> excludedHostList) {
        return null;
    }

    @Override
    public Map<Integer, ContainerHost> getHosts(List<ContainerHost> hostList, List<ContainerPod> pods) {
        int size = pods.size();
        hasChecked.clear();
        for(int t = 0; t < size; t++) {
            hasChecked.add(false);
        }
        for(int i = 0; i < size; i++) {
            int maxIndex = 0;
            ContainerPod maxPod = null;
            for(int j = 0; j < size; j++) {
                ContainerPod pod = pods.get(j);
                if(!hasChecked.get(j)) {
                    maxPod = pod;
                    maxIndex = j;
                    break;
                }
            }
            if(maxPod == null) {
                break;
            }

            for(int j = 0; j < size; j++) {
                ContainerPod pod = pods.get(j);
                if(hasChecked.get(j)) {
                    continue;
                }
                long length = pod.getTasks().get(0).getCloudletLength();
                if(length > maxPod.getTasks().get(0).getCloudletLength()) {
                    maxPod = pod;
                    maxIndex = j;
                }
            }
            hasChecked.set(maxIndex, true);
            int hostId = -1;
            int hostSize = hostList.size();
            ContainerHost firstIdleHost = null;
            for(int j = 0; j < hostSize; j++) {
                ContainerHost host = hostList.get(j);

            }
        }
        return null;
    }
}
