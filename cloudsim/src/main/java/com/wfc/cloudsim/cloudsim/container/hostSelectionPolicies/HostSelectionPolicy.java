package com.wfc.cloudsim.cloudsim.container.hostSelectionPolicies;

import com.wfc.cloudsim.cloudsim.container.core.ContainerHost;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by sareh on 11/08/15.
 */
public abstract class HostSelectionPolicy {

    /**
     * Gets the host
     *
     * @param hostList the host
     * @return the destination host to migrate
     */
    public abstract ContainerHost getHost(List<ContainerHost> hostList, Object obj, Set<? extends ContainerHost> excludedHostList);

    public abstract Map<Integer ,ContainerHost> getHosts(List<ContainerHost> hostList, List<ContainerPod> pods);

}
