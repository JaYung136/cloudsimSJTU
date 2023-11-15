package com.wfc.cloudsim.cloudsim.container.hostSelectionPolicies;

import com.wfc.cloudsim.cloudsim.container.core.ContainerHost;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class HostSelectionRoundRobin extends HostSelectionPolicy{
    int lastSelectId = -1;

    @Override
    public ContainerHost getHost(List<ContainerHost> hostList, Object obj, Set<? extends ContainerHost> excludedHostList) {
        ContainerHost selectHost = null;
        int size = hostList.size();
        int nowSelectId = lastSelectId + 1;
        if(nowSelectId >= size) nowSelectId = 0;
        selectHost = hostList.get(nowSelectId);
        int changeTimes = 0;
        while(excludedHostList.contains(selectHost)) {
            changeTimes ++;
            if(changeTimes == size) return null;
            nowSelectId ++;
            if(nowSelectId >= size) nowSelectId = 0;
            selectHost = hostList.get(nowSelectId);
        }
        lastSelectId = nowSelectId;
        return selectHost;
    }

    @Override
    public Map<Integer, ContainerHost> getHosts(List<ContainerHost> hostList, List<ContainerPod> pods) {
        return null;
    }
}
