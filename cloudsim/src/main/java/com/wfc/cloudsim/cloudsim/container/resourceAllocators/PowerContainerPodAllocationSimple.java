package com.wfc.cloudsim.cloudsim.container.resourceAllocators;

import com.wfc.cloudsim.cloudsim.container.core.ContainerHost;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;
import com.wfc.cloudsim.wfc.core.WFCDatacenter;

import java.util.List;
import java.util.Map;

/**
 * Created by sareh on 14/07/15.
 */
public class PowerContainerPodAllocationSimple extends PowerContainerPodAllocationAbstract {

    public PowerContainerPodAllocationSimple(List<? extends ContainerHost> list) {
        super(list);
    }

    @Override

    public List<Map<String, Object>> optimizeAllocation(List<? extends ContainerPod> vmList) {
        return null;
    }

    @Override
    public void setDatacenter(WFCDatacenter datacenter) {

    }
}
