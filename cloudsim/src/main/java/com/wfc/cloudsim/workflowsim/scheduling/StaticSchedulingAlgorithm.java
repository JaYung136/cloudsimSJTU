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
import com.wfc.cloudsim.cloudsim.container.core.ContainerCloudlet;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;
import com.wfc.cloudsim.wfc.core.WFCConstants;
import com.wfc.cloudsim.workflowsim.WorkflowSimTags;

import java.util.HashMap;
import java.util.Map;

/**
 * Static algorithm. Do not schedule at all and reply on Workflow Planner to set
 * the mapping relationship. But StaticSchedulingAlgorithm would check whether a
 * job has been assigned a VM in this stage (in case your implementation of
 * planning algorithm fcom.wfc.cloudsimets it)
 *
 * @author Arman Riazi
 * @since WorkflowSim Toolkit 1.0
 * @date Jun 17, 2013
 */
public class StaticSchedulingAlgorithm extends BaseSchedulingAlgorithm {

    public StaticSchedulingAlgorithm() {
        super();
    }

    @Override
    public void run() throws Exception {

        Map<Integer, ContainerPod> mId2Vm = new HashMap<>();

        for (int i = 0; i < getVmList().size(); i++) {
            ContainerPod vm = (ContainerPod) getVmList().get(i);
            if (vm != null) {
                mId2Vm.put(vm.getId(), vm);
            }
        }

        int size = getCloudletList().size();

        for (int i = 0; i < size; i++) {
            Cloudlet cloudlet = (Cloudlet) getCloudletList().get(i);
            /**
             * Make sure cloudlet is matched to a VM. It should be done in the
             * Workflow Planner. If not, throws an exception because
             * StaticSchedulingAlgorithm itself does not do the mapping.
             */
            if(cloudlet.getCloudletId() == WFCConstants.WFC_NUMBER_CLOUDLETS) {
                cloudlet.setVmId(cloudlet.getCloudletId());
                ((ContainerCloudlet)cloudlet).setContainerId(cloudlet.getCloudletId());
                getScheduledList().add(cloudlet);
                continue;
            }
            /*if (cloudlet.getVmId() < 0 || !mId2Vm.containsKey(cloudlet.getCloudletId())) {
                Log.printLine("Cloudlet " + cloudlet.getCloudletId() + " is not matched."
                        + "It is possible a stage-in job");
                cloudlet.setVmId(1);
                //((Job)cloudlet).setContainerId(1);
            } else {*/
            cloudlet.setVmId(cloudlet.getCloudletId());
            //}
            ContainerPod vm = mId2Vm.get(cloudlet.getVmId());
            cloudlet.setVmId(vm.getId());
            ((ContainerCloudlet)cloudlet).setContainerId(vm.getContainerList().get(0).getId());
            if (vm.getContainerList().get(0).getState() == WorkflowSimTags.VM_STATUS_IDLE) {
                vm.getContainerList().get(0).setState(WorkflowSimTags.VM_STATUS_BUSY);
                getScheduledList().add(cloudlet);
                Log.printLine("Schedules " + cloudlet.getCloudletId() + " with "
                        + cloudlet.getCloudletLength() + " to VM " + cloudlet.getVmId());
            }
        }
    }
}
