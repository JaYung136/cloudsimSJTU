/**
 * Copyright 2013-2014 University Of Southern California
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
package com.wfc.cloudsim.workflowsim.scheduling;

import com.wfc.cloudsim.cloudsim.Cloudlet;
import com.wfc.cloudsim.cloudsim.container.core.Container;
import com.wfc.cloudsim.cloudsim.container.core.ContainerCloudlet;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;
import com.wfc.cloudsim.workflowsim.WorkflowSimTags;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The Round Robin algorithm.
 *
 * @author Arman Riazi
 * @since WorkflowSim Toolkit 1.0
 * @date May 12, 2014
 */
public class RoundRobinSchedulingAlgorithm extends BaseSchedulingAlgorithm {

    /**
     * The main function
     */
    @Override
    public void run() {
        int vmIndex = 0;
        
        int size = getCloudletList().size();
        Collections.sort(getCloudletList(), new CloudletListComparator());
        List vmList = getVmList();
        Collections.sort(vmList, new VmListComparator());
        for (int j = 0; j < size; j++) {
            Cloudlet cloudlet = (Cloudlet) getCloudletList().get(j);
            int vmSize = vmList.size();
            ContainerPod firstIdleVm = null;//(CondorPod)getVmList().get(0);
            Container firstIdleContainer = null;
            for (int l = 0; l < vmSize; l++) {
                ContainerPod vm = (ContainerPod) vmList.get(l);
                if (vm.getState() == WorkflowSimTags.VM_STATUS_IDLE) {
                    for(Container c: vm.getContainerList()) {
                        if(c.getState() == WorkflowSimTags.VM_STATUS_IDLE) {
                            firstIdleVm = vm;
                            firstIdleContainer = c;
                            break;
                        }
                    }
                }
            }
            if (firstIdleVm == null) {
                break;
            }
            ((Container) firstIdleContainer).setState(WorkflowSimTags.VM_STATUS_BUSY);
            cloudlet.setVmId(firstIdleVm.getId());
            ((ContainerCloudlet)cloudlet).setContainerId(firstIdleContainer.getId());
            getScheduledList().add(cloudlet);
            vmIndex = (vmIndex + 1) % vmList.size();
        }
    }
    /**
     * Sort it based on vm index
     */
    public class VmListComparator implements Comparator<ContainerPod>{
        @Override
        public int compare(ContainerPod v1, ContainerPod v2){
            return Integer.compare(v1.getId(), v2.getId());
        }
    }
    
    public class CloudletListComparator implements Comparator<Cloudlet>{
        @Override
        public int compare(Cloudlet c1, Cloudlet c2){
            return Integer.compare(c1.getCloudletId(), c2.getCloudletId());
        }
    }    
}

