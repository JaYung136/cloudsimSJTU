/**
 * Copyright 2012-2013 University Of Southern California
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sim.workflowsim.scheduling;

import org.sim.service.Constants;
import org.sim.cloudbus.cloudsim.Cloudlet;
import org.sim.cloudbus.cloudsim.Host;
import org.sim.cloudbus.cloudsim.Log;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MaxMin algorithm.
 *
 * @author Weiwei Chen
 * @since WorkflowSim Toolkit 1.0
 * @date Apr 9, 2013
 */
public class MaxMinSchedulingAlgorithm extends BaseSchedulingAlgorithm {

    /**
     * Initialize a MaxMin scheduler.
     */
    public MaxMinSchedulingAlgorithm() {
        super();
    }
    /**
     * the check point list.
     */
    private final List<Boolean> hasChecked = new ArrayList<>();

    private Map<Integer, Integer> resPes = new HashMap<>();

    @Override
    public void run() {

        for(Host h: Constants.hosts) {
            resPes.put(h.getId(), h.getNumberOfPes());
        }
        //Log.printLine("Schedulin Cycle");
        int size = getCloudletList().size();
        hasChecked.clear();
        for (int t = 0; t < size; t++) {
            hasChecked.add(false);
        }
        for (int i = 0; i < size; i++) {
            int maxIndex = 0;
            Cloudlet maxCloudlet = null;
            for (int j = 0; j < size; j++) {
                Cloudlet cloudlet = (Cloudlet) getCloudletList().get(j);
                if (!hasChecked.get(j)) {
                    maxCloudlet = cloudlet;
                    maxIndex = j;
                    break;
                }
            }
            if (maxCloudlet == null) {
                break;
            }

            for (int j = 0; j < size; j++) {
                Cloudlet cloudlet = (Cloudlet) getCloudletList().get(j);
                if (hasChecked.get(j)) {
                    continue;
                }
                long length = cloudlet.getCloudletLength();
                if (length > maxCloudlet.getCloudletLength()) {
                    maxCloudlet = cloudlet;
                    maxIndex = j;
                }
            }
            hasChecked.set(maxIndex, true);

            //int vmSize = getVmList().size();
            int hostSize = Constants.hosts.size();
            //CondorVM firstIdleVm = null;//(CondorVM)getVmList().get(0);
            Host firstIdleHost = null;
            for (int j = 0; j < hostSize; j++) {
                Host host = (Host) Constants.hosts.get(j);
                //if (Constants.hosts.get(j).getState() == WorkflowSimTags.VM_STATUS_IDLE && ) {
                if(resPes.get(host.getId()) >= maxCloudlet.getNumberOfPes()){
                    firstIdleHost = host;
                    break;
                }
                Log.printLine("Host " + j + " is not idle");
            }
            if (firstIdleHost == null) {
                break;
            }
            for (int j = 0; j < hostSize; j++) {
                Host host = (Host) getHostList().get(j);

                if ((resPes.get(host.getId()) >= maxCloudlet.getNumberOfPes()) &&
                         host.getTotalMips() > firstIdleHost.getTotalMips()) {
                    firstIdleHost = host;

                }
            }
            //firstIdleHost.setState(WorkflowSimTags.VM_STATUS_BUSY);
            maxCloudlet.setVmId(firstIdleHost.getId());
            resPes.put(firstIdleHost.getId(),resPes.get(firstIdleHost.getId()) - 1);
            getScheduledList().add(maxCloudlet);
            Log.printLine("Schedules " + maxCloudlet.getCloudletId() + " with "
                    + maxCloudlet.getCloudletLength() + " to Host " + firstIdleHost.getId()
                   );

        }
    }
}
