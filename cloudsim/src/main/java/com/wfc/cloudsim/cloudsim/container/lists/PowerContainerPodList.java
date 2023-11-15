package com.wfc.cloudsim.cloudsim.container.lists;

import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;
import com.wfc.cloudsim.cloudsim.core.CloudSim;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by sareh on 28/07/15.
 */
public class PowerContainerPodList extends ContainerPodList {

        /**
         * Sort by cpu utilization.
         *
         * @param vmList the vm list
         */
        public static <T extends ContainerPod> void sortByCpuUtilization(List<T> vmList) {
            Collections.sort(vmList, new Comparator<T>() {

                @Override
                public int compare(T a, T b) throws ClassCastException {
                    Double aUtilization = a.getTotalUtilizationOfCpuMips(CloudSim.clock());
                    Double bUtilization = b.getTotalUtilizationOfCpuMips(CloudSim.clock());
                    return bUtilization.compareTo(aUtilization);
                }
            });
        }

    }


