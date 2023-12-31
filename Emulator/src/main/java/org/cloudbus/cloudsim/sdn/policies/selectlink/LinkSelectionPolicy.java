/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn.policies.selectlink;

import org.cloudbus.cloudsim.sdn.physicalcomponents.Link;
import org.cloudbus.cloudsim.sdn.physicalcomponents.Node;

import java.util.List;

public interface LinkSelectionPolicy {
	// This function decides which link to select among links collection.
	public abstract Link selectLink(List<Link> links, int flowId, Node srcHost, Node destHost, Node prevNode);

	public abstract boolean isDynamicRoutingEnabled();
}
