/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.com.wfc.cloudsim/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package com.wfc.cloudsim.cloudsim.power;

import com.wfc.cloudsim.cloudsim.Host;
import com.wfc.cloudsim.cloudsim.util.MathUtil;

import java.util.List;

/**
 * A VM allocation policy that uses Local Regression Robust (LRR) to predict host utilization (load)
 * and define if a host is overloaded or not.
 * 
 * <br/>If you are using any algorithms, policies or workload included in the power package please cite
 * the following paper:<br/>
 * 
 * <ul>
 * <li><a href="http://dx.doi.com.wfc.cloudsim/10.1002/cpe.1867">Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012</a>
 * </ul>
 * 
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 3.0
 */
public class PowerPodAllocationPolicyMigrationLocalRegressionRobust extends
		PowerPodAllocationPolicyMigrationLocalRegression {

	/**
	 * Instantiates a new PowerPodAllocationPolicyMigrationLocalRegressionRobust.
	 * 
	 * @param hostList the host list
	 * @param vmSelectionPolicy the vm selection policy
	 * @param schedulingInterval the scheduling interval
	 * @param fallbackVmAllocationPolicy the fallback vm allocation policy
	 * @param utilizationThreshold the utilization threshold
	 */
	public PowerPodAllocationPolicyMigrationLocalRegressionRobust(
			List<? extends Host> hostList,
			PowerPodSelectionPolicy vmSelectionPolicy,
			double safetyParameter,
			double schedulingInterval,
			PowerPodAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy,
			double utilizationThreshold) {
		super(
				hostList,
				vmSelectionPolicy,
				safetyParameter,
				schedulingInterval,
				fallbackVmAllocationPolicy,
				utilizationThreshold);
	}

	/**
	 * Instantiates a new PowerPodAllocationPolicyMigrationLocalRegressionRobust.
	 * 
	 * @param hostList the host list
	 * @param vmSelectionPolicy the vm selection policy
	 * @param schedulingInterval the scheduling interval
	 * @param fallbackVmAllocationPolicy the fallback vm allocation policy
	 */
	public PowerPodAllocationPolicyMigrationLocalRegressionRobust(
			List<? extends Host> hostList,
			PowerPodSelectionPolicy vmSelectionPolicy,
			double safetyParameter,
			double schedulingInterval,
			PowerPodAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy) {
		super(hostList, vmSelectionPolicy, safetyParameter, schedulingInterval, fallbackVmAllocationPolicy);
	}

	/**
	 * Gets the utilization estimates.
	 * 
	 * @param utilizationHistoryReversed the utilization history reversed
	 * @return the utilization estimates
	 */
	@Override
	protected double[] getParameterEstimates(double[] utilizationHistoryReversed) {
		return MathUtil.getRobustLoessParameterEstimates(utilizationHistoryReversed);
	}

}
