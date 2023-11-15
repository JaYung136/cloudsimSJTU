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
package com.wfc.cloudsim.workflowsim.scheduling;

import com.wfc.cloudsim.cloudsim.Cloudlet;
import com.wfc.cloudsim.cloudsim.Log;
import com.wfc.cloudsim.cloudsim.container.core.Container;
import com.wfc.cloudsim.cloudsim.container.core.ContainerCloudlet;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;
import com.wfc.cloudsim.workflowsim.WorkflowSimTags;

/**
 * MCT algorithm
 *
 * @author Arman Riazi
 * @since WorkflowSim Toolkit 1.0
 * @date March 29, 2020
 */
public class MCTSchedulingAlgorithm extends BaseSchedulingAlgorithm {

    public MCTSchedulingAlgorithm() {
        super();
    }

    @Override
    public void run() {


        int size = getCloudletList().size();

        for (int i = 0; i < size; i++) {
            Cloudlet cloudlet = (Cloudlet) getCloudletList().get(i);
            int vmSize = getVmList().size();
            ContainerPod firstIdleVm = null;
            Container firstIdleContainer = null;
            for (int j = 0; j < vmSize; j++) {
                ContainerPod vm = (ContainerPod) getVmList().get(j);
                if (vm.getState() == WorkflowSimTags.VM_STATUS_IDLE) {
                    for(Container c: vm.getContainerList()) {
                        firstIdleVm = vm;
                        firstIdleContainer = c;
                        break;
                    }
                }
            }
            if (firstIdleVm == null) {
                break;
            }

            for (int j = 0; j < vmSize; j++) {
                ContainerPod vm = (ContainerPod) getVmList().get(j);
                if ((vm.getState() == WorkflowSimTags.VM_STATUS_IDLE)){
                    for(Container c: vm.getContainerList()) {
                        if (c.getState() == WorkflowSimTags.VM_STATUS_IDLE && (c.getCurrentRequestedTotalMips() > firstIdleContainer.getCurrentRequestedTotalMips())) {
                            firstIdleVm = vm;
                            firstIdleContainer = c;
                        }
                    }
                }
            }
            firstIdleContainer.setState(WorkflowSimTags.VM_STATUS_BUSY);
            cloudlet.setVmId(firstIdleVm.getId());
            ((ContainerCloudlet)cloudlet).setContainerId(firstIdleContainer.getId());
            getScheduledList().add(cloudlet);
            Log.printLine("Schedules " + cloudlet.getCloudletId() + " with "
                    + cloudlet.getCloudletLength() + " to VM " + firstIdleVm.getId()
                    + " with " + firstIdleVm.getCurrentRequestedTotalMips());
        }
    }
}
