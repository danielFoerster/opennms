//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2003 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// 2004 Jan 06: Added support for SUSPEND_POLLING_SERVICE_EVENT_UEI and
// 		RESUME_POLLING_SERVICE_EVENT_UEI
// 2003 Nov 11: Merged changes from Rackspace project
// 2003 Jan 31: Cleaned up some unused imports.
//
// Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//
// Tab Size = 8
//

package org.opennms.netmgt.poller;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Category;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.EventConstants;
import org.opennms.netmgt.config.PollerConfig;
import org.opennms.netmgt.eventd.EventIpcManager;
import org.opennms.netmgt.eventd.EventListener;
import org.opennms.netmgt.poller.pollables.PollableInterface;
import org.opennms.netmgt.poller.pollables.PollableNetwork;
import org.opennms.netmgt.poller.pollables.PollableNode;
import org.opennms.netmgt.poller.pollables.PollableService;
import org.opennms.netmgt.utils.XmlrpcUtil;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Parm;
import org.opennms.netmgt.xml.event.Parms;
import org.opennms.netmgt.xml.event.Value;

/**
 * 
 * @author <a href="mailto:jamesz@opennms.com">James Zuo </a>
 * @author <a href="mailto:weave@oculan.com">Brian Weaver </a>
 * @author <a href="http://www.opennms.org/">OpenNMS </a>
 */
final class PollerEventProcessor implements EventListener {

    private Poller m_poller;

    /**
     * Create message selector to set to the subscription
     */
    private void createMessageSelectorAndSubscribe() {
        // Create the selector for the ueis this service is interested in
        //
        List ueiList = new ArrayList();

        // nodeGainedService
        ueiList.add(EventConstants.NODE_GAINED_SERVICE_EVENT_UEI);

        // serviceDeleted
        // deleteService
        /*
         * NOTE: deleteService is only generated by the PollableService itself.
         * Therefore, we ignore it. If future implementations allow other
         * subsystems to generate this event, we may have to listen for it as
         * well. 'serviceDeleted' is the response event that the outage manager
         * generates. We ignore this as well, since the PollableService has
         * already taken action at the time it generated 'deleteService'
         */
        ueiList.add(EventConstants.SERVICE_DELETED_EVENT_UEI);
        // ueiList.add(EventConstants.DELETE_SERVICE_EVENT_UEI);

        // serviceManaged
        // serviceUnmanaged
        // interfaceManaged
        // interfaceUnmanaged
        /*
         * NOTE: These are all ignored because the responsibility is currently
         * on the class generating the event to restart the poller service. If
         * that implementation is ever changed, this message selector should
         * listen for these and act on them.
         */
        // ueiList.add(EventConstants.SERVICE_MANAGED_EVENT_UEI);
        // ueiList.add(EventConstants.SERVICE_UNMANAGED_EVENT_UEI);
        // ueiList.add(EventConstants.INTERFACE_MANAGED_EVENT_UEI);
        // ueiList.add(EventConstants.INTERFACE_UNMANAGED_EVENT_UEI);
        // interfaceIndexChanged
        // NOTE: No longer interested in this event...if Capsd detects
        // that in interface's index has changed a
        // 'reinitializePrimarySnmpInterface' event is generated.
        // ueiList.add(EventConstants.INTERFACE_INDEX_CHANGED_EVENT_UEI);
        // interfaceReparented
        ueiList.add(EventConstants.INTERFACE_REPARENTED_EVENT_UEI);

        // reloadPollerConfig
        /*
         * NOTE: This is ignored because the reload is handled through an
         * autoaction.
         */
        // ueiList.add(EventConstants.RELOAD_POLLER_CONFIG_EVENT_UEI);
        // NODE OUTAGE RELATED EVENTS
        // 
        // nodeAdded
        /*
         * NOTE: This is ignored. The real trigger will be the first
         * nodeGainedService event, at which time the interface and node will be
         * created
         */
        // ueiList.add(EventConstants.NODE_ADDED_EVENT_UEI);
        // nodeDeleted
        ueiList.add(EventConstants.NODE_DELETED_EVENT_UEI);

        // duplicateNodeDeleted
        ueiList.add(EventConstants.DUP_NODE_DELETED_EVENT_UEI);

        // nodeGainedInterface
        /*
         * NOTE: This is ignored. The real trigger will be the first
         * nodeGainedService event, at which time the interface and node will be
         * created
         */
        // ueiList.add(EventConstants.NODE_GAINED_INTERFACE_EVENT_UEI);
        // interfaceDeleted
        ueiList.add(EventConstants.INTERFACE_DELETED_EVENT_UEI);

        // suspendPollingService
        ueiList.add(EventConstants.SUSPEND_POLLING_SERVICE_EVENT_UEI);

        // resumePollingService
        ueiList.add(EventConstants.RESUME_POLLING_SERVICE_EVENT_UEI);
	
	// scheduled outage configuration change
	ueiList.add(EventConstants.SCHEDOUTAGES_CHANGED_EVENT_UEI);

        // Subscribe to eventd
        getEventManager().addEventListener(this, ueiList);
    }

    /**
     * Process the event, construct a new PollableService object representing
     * the node/interface/service/pkg combination, and schedule the service for
     * polling.
     * 
     * If any errors occur scheduling the interface no error is returned.
     * 
     * @param event
     *            The event to process.
     * 
     */
    private void nodeGainedServiceHandler(Event event) {
        Category log = ThreadCategory.getInstance(getClass());

        // Is this the result of a resumePollingService event?
        String whichEvent = "Unexpected Event: " + event.getUei() + ": ";
        if (event.getUei().equals(EventConstants.NODE_GAINED_SERVICE_EVENT_UEI)) {
            whichEvent = "nodeGainedService: ";
        } else if (event.getUei().equals(EventConstants.RESUME_POLLING_SERVICE_EVENT_UEI)) {
            whichEvent = "resumePollingService: ";
        }
        

        // First make sure the service gained is in active state before trying
        // to
        // schedule

        String ipAddr = event.getInterface();
        int nodeId = (int) event.getNodeid();
        String svcName = event.getService();

        getPoller().scheduleService(nodeId, ipAddr, svcName);
        
    }

    /**
     * This method is responsible for processing 'interfacReparented' events. An
     * 'interfaceReparented' event will have old and new nodeId parms associated
     * with it. Node outage processing hierarchy will be updated to reflect the
     * new associations.
     * 
     * @param event
     *            The event to process.
     * 
     */
    private void interfaceReparentedHandler(Event event) {
        Category log = ThreadCategory.getInstance(getClass());
        if (log.isDebugEnabled())
            log.debug("interfaceReparentedHandler:  processing interfaceReparented event for " + event.getInterface());

        // Verify that the event has an interface associated with it
        if (event.getInterface() == null)
            return;
        
        String ipAddr = event.getInterface();

        // Extract the old and new nodeId's from the event parms
        String oldNodeIdStr = null;
        String newNodeIdStr = null;
        Parms parms = event.getParms();
        if (parms != null) {
            String parmName = null;
            Value parmValue = null;
            String parmContent = null;

            Enumeration parmEnum = parms.enumerateParm();
            while (parmEnum.hasMoreElements()) {
                Parm parm = (Parm) parmEnum.nextElement();
                parmName = parm.getParmName();
                parmValue = parm.getValue();
                if (parmValue == null)
                    continue;
                else
                    parmContent = parmValue.getContent();

                // old nodeid
                if (parmName.equals(EventConstants.PARM_OLD_NODEID)) {
                    oldNodeIdStr = parmContent;
                }

                // new nodeid
                else if (parmName.equals(EventConstants.PARM_NEW_NODEID)) {
                    newNodeIdStr = parmContent;
                }
            }
        }

        // Only proceed provided we have both an old and a new nodeId
        //
        if (oldNodeIdStr == null || newNodeIdStr == null) {
            log.error("interfaceReparentedHandler: old and new nodeId parms are required, unable to process.");
            return;
        }
        
        PollableNode oldNode;
        PollableNode newNode;
        try {
            oldNode = getNetwork().getNode(Integer.parseInt(oldNodeIdStr));
            if (oldNode == null) {
                log.error("interfaceReparentedHandler: Cannot locate old node "+oldNodeIdStr+" belonging to interface "+ipAddr);
                return;
            }
            newNode = getNetwork().getNode(Integer.parseInt(newNodeIdStr));
            if (newNode == null) {
                log.error("interfaceReparentedHandler: Cannot locate new node "+newNodeIdStr+" to move interface to.");
                return;
            }
            
            PollableInterface iface = oldNode.getInterface(InetAddress.getByName(ipAddr));
            if (iface == null) {
                log.error("interfaceReparentedHandler: Cannot locate interface with ipAddr "+ipAddr+" to reparent.");
                return;
            }
            
            iface.reparentTo(newNode);
            
            
        } catch (NumberFormatException nfe) {
            log.error("interfaceReparentedHandler: failed converting old/new nodeid parm to integer, unable to process.");
            return;
        } catch (UnknownHostException e) {
            log.error("interfaceReparentedHandler: failed converting ipAddr "+ipAddr+" to an inet address");
            return;
        } 
        
    }

	/**
	 * This method is invoked by the EventIpcManager when a new event is
	 * available for processing. Each message is examined for its Universal
	 * Event Identifier and the appropriate action is taking based on each UEI.
	 * 
	 * @param event
	 *            The event
	 */
	public void onEvent(Event event) {
	    if (event == null)
	        return;
	
	    Category log = ThreadCategory.getInstance(getClass());
	
	    // print out the uei
	    //
	    if (log.isDebugEnabled()) {
	        log.debug("PollerEventProcessor: received event, uei = " + event.getUei());
	    }
	
	if(event.getUei().equals(EventConstants.SCHEDOUTAGES_CHANGED_EVENT_UEI)) {
		log.info("Reloading poller config factory and polloutages config factory");
	    
		scheduledOutagesChangeHandler(log);
	} else if(!event.hasNodeid()) {
	    // For all other events, if the event doesn't have a nodeId it can't be processed.
	
	        log.info("PollerEventProcessor: no database node id found, discarding event");
	    } else if (event.getUei().equals(EventConstants.NODE_GAINED_SERVICE_EVENT_UEI)) {
	        // If there is no interface then it cannot be processed
	        //
	        if (event.getInterface() == null || event.getInterface().length() == 0) {
	            log.info("PollerEventProcessor: no interface found, discarding event");
	        } else {
	            nodeGainedServiceHandler(event);
	        }
	    } else if (event.getUei().equals(EventConstants.RESUME_POLLING_SERVICE_EVENT_UEI)) {
	        // If there is no interface then it cannot be processed
	        //
	        if (event.getInterface() == null || event.getInterface().length() == 0) {
	            log.info("PollerEventProcessor: no interface found, cannot resume polling service, discarding event");
	        } else {
	            nodeGainedServiceHandler(event);
	        }
	    } else if (event.getUei().equals(EventConstants.SUSPEND_POLLING_SERVICE_EVENT_UEI)) {
	        // If there is no interface then it cannot be processed
	        //
	        if (event.getInterface() == null || event.getInterface().length() == 0) {
	            log.info("PollerEventProcessor: no interface found, cannot suspend polling service, discarding event");
	        } else {
	            nodeRemovePollableServiceHandler(event);
	        }
	    } else if (event.getUei().equals(EventConstants.INTERFACE_REPARENTED_EVENT_UEI)) {
	        // If there is no interface then it cannot be processed
	        //
	        if (event.getInterface() == null || event.getInterface().length() == 0) {
	            log.info("PollerEventProcessor: no interface found, discarding event");
	        } else {
	            interfaceReparentedHandler(event);
	        }
	    } else if (event.getUei().equals(EventConstants.NODE_DELETED_EVENT_UEI) || event.getUei().equals(EventConstants.DUP_NODE_DELETED_EVENT_UEI)) {
	        if (event.getNodeid() < 0) {
	            log.info("PollerEventProcessor: no node or interface found, discarding event");
	        }
	        // NEW NODE OUTAGE EVENTS
	        nodeDeletedHandler(event);
	    } else if (event.getUei().equals(EventConstants.INTERFACE_DELETED_EVENT_UEI)) {
	        // If there is no interface then it cannot be processed
	        //
	        if (event.getNodeid() < 0 || event.getInterface() == null || event.getInterface().length() == 0) {
	            log.info("PollerEventProcessor: invalid nodeid or no interface found, discarding event");
	        } else {
	            interfaceDeletedHandler(event);
	        }
	    } else if (event.getUei().equals(EventConstants.SERVICE_DELETED_EVENT_UEI)) {
	        // If there is no interface then it cannot be processed
	        //
	        if ((event.getNodeid() < 0) || (event.getInterface() == null) || (event.getInterface().length() == 0) || (event.getService() == null)) {
	            log.info("PollerEventProcessor: invalid nodeid or no nodeinterface " + "or service found, discarding event");
	        } else {
	            serviceDeletedHandler(event);
	        }
	
	    } // end single event proces
	
	} // end onEvent()

    /**
     * This method is responsible for removing a node's pollable service from
     * the pollable services list
     */
    private void nodeRemovePollableServiceHandler(Event event) {
        Category log = ThreadCategory.getInstance(getClass());

        int nodeId = (int) event.getNodeid();
        String ipAddr = event.getInterface();
        String svcName = event.getService();
        
        InetAddress address;
        try {
            address = InetAddress.getByName(ipAddr);
        } catch (UnknownHostException e) {
            log.error("Unable to convert "+ipAddr+" to an inet address", e);
            return;
        }
        

        if (svcName == null) {
            log.error("nodeRemovePollableServiceHandler: service name is null, ignoring event");
            return;
        }
        
        
        PollableService svc = getNetwork().getService(nodeId, address, svcName);
        svc.delete();

    }

    /**
     * This method is responsible for removing the node specified in the
     * nodeDeleted event from the Poller's pollable node map.
     */
    private void nodeDeletedHandler(Event event) {
        Category log = ThreadCategory.getInstance(getClass());

        int nodeId = (int) event.getNodeid();
        final String sourceUei = event.getUei();

        // Extract node label and transaction No. from the event parms
        long txNo = -1L;
        Parms parms = event.getParms();
        if (parms != null) {
            String parmName = null;
            Value parmValue = null;
            String parmContent = null;

            Enumeration parmEnum = parms.enumerateParm();
            while (parmEnum.hasMoreElements()) {
                Parm parm = (Parm) parmEnum.nextElement();
                parmName = parm.getParmName();
                parmValue = parm.getValue();
                if (parmValue == null)
                    continue;
                else
                    parmContent = parmValue.getContent();

                // get the external transaction number
                if (parmName.equals(EventConstants.PARM_TRANSACTION_NO)) {
                    String temp = parmContent;
                    if (log.isDebugEnabled())
                        log.debug("nodeDeletedHandler:  parmName: " + parmName + " /parmContent: " + parmContent);
                    try {
                        txNo = Long.valueOf(temp).longValue();
                    } catch (NumberFormatException nfe) {
                        log.warn("nodeDeletedHandler: Parameter " + EventConstants.PARM_TRANSACTION_NO + " cannot be non-numberic", nfe);
                        txNo = -1;
                    }
                }
            }
        }
        
        Date closeDate;
        try {
            closeDate = EventConstants.parseToDate(event.getTime());
        } catch (ParseException e) {
            closeDate = new Date();
        }
        
        getPoller().closeOutagesForNode(closeDate, event.getDbid(), nodeId);

        
        PollableNode node = getNetwork().getNode(nodeId);
        if (node == null) {
          log.error("Nodeid " + nodeId + " does not exist in pollable node map, unable to delete node.");
          if (isXmlRPCEnabled()) {
              int status = EventConstants.XMLRPC_NOTIFY_FAILURE;
              XmlrpcUtil.createAndSendXmlrpcNotificationEvent(txNo, sourceUei, "Node does not exist in pollable node map.", status, "OpenNMS.Poller");
          }
          return;
        }
        node.delete();

    }

    /**
     * 
     */
    private void interfaceDeletedHandler(Event event) {
        Category log = ThreadCategory.getInstance(getClass());

        int nodeId = (int) event.getNodeid();
        String sourceUei = event.getUei();
        String ipAddr = event.getInterface();
        
        // Extract node label and transaction No. from the event parms
        long txNo = -1L;
        Parms parms = event.getParms();
        if (parms != null) {
            String parmName = null;
            Value parmValue = null;
            String parmContent = null;
            Enumeration parmEnum = parms.enumerateParm();
            while (parmEnum.hasMoreElements()) {
                Parm parm = (Parm) parmEnum.nextElement();
                parmName = parm.getParmName();
                parmValue = parm.getValue();
                if (parmValue == null)
                    continue;
                else
                    parmContent = parmValue.getContent();

                // get the external transaction number
                if (parmName.equals(EventConstants.PARM_TRANSACTION_NO)) {
                    String temp = parmContent;
                    if (log.isDebugEnabled())
                        log.debug("interfaceDeletedHandlerHandler:  parmName: " + parmName + " /parmContent: " + parmContent);
                    try {
                        txNo = Long.valueOf(temp).longValue();
                    } catch (NumberFormatException nfe) {
                        log.warn("interfaceDeletedHandlerHandler: Parameter " + EventConstants.PARM_TRANSACTION_NO + " cannot be non-numberic", nfe);
                        txNo = -1;
                    }
                }
            }
        }
        
        InetAddress addr;
        try {
            addr = InetAddress.getByName(ipAddr);
        } catch (UnknownHostException e) {
            log.error("interfaceDeletedHandler: Could not convert interface "+event.getInterface()+" to InetAddress", e);
            return;
            
        }
        
        Date closeDate;
        try {
            closeDate = EventConstants.parseToDate(event.getTime());
        } catch (ParseException e) {
            closeDate = new Date();
        }
        
        getPoller().closeOutagesForInterface(closeDate, event.getDbid(), nodeId, ipAddr);

        
        PollableInterface iface = getNetwork().getInterface(nodeId, addr);
        if (iface == null) {
          log.error("Interface " + nodeId + "/" + event.getInterface() + " does not exist in pollable node map, unable to delete node.");
          if (isXmlRPCEnabled()) {
              int status = EventConstants.XMLRPC_NOTIFY_FAILURE;
              XmlrpcUtil.createAndSendXmlrpcNotificationEvent(txNo, sourceUei, "Interface does not exist in pollable node map.", status, "OpenNMS.Poller");
          }
          return;
        }
        iface.delete();

    }

    /**
     * <p>
     * This method remove a deleted service from the pollable service list of
     * the specified interface, so that it will not be scheduled by the poller.
     * </p>
     */
    private void serviceDeletedHandler(Event event) {
        Category log = ThreadCategory.getInstance(getClass());

        int nodeId = (int) event.getNodeid();
        String ipAddr = event.getInterface();
        String service = event.getService();
        
        InetAddress addr;
        try {
            addr = InetAddress.getByName(ipAddr);
        } catch (UnknownHostException e) {
            log.error("serviceDeletedHandler: Could not convert "+ipAddr+" to an InetAddress", e);
            return;
        }
        
        Date closeDate;
        try {
            closeDate = EventConstants.parseToDate(event.getTime());
        } catch (ParseException e) {
            closeDate = new Date();
        }
        
        getPoller().closeOutagesForService(closeDate, event.getDbid(), nodeId, ipAddr, service);
        
        PollableService svc = getNetwork().getService(nodeId, addr, service);
        if (svc == null) {
          log.error("Interface " + nodeId + "/" + event.getInterface() + " does not exist in pollable node map, unable to delete node.");
          return;
        }
        
        svc.delete();

    }
    
    /**
     * Constructor
     * 
     * @param pollableServices
     *            List of all the PollableService objects scheduled for polling
     */
    PollerEventProcessor(Poller poller) {

        m_poller = poller;

        createMessageSelectorAndSubscribe();

        Category log = ThreadCategory.getInstance(getClass());
        if (log.isDebugEnabled())
            log.debug("Subscribed to eventd");

    }

    /**
     * Unsubscribe from eventd
     */
    public void close() {
        getEventManager().removeEventListener(this);
    }

    /**
     * @return
     */
    private EventIpcManager getEventManager() {
        return getPoller().getEventManager();
    }

    private void scheduledOutagesChangeHandler(Category log) {
        try {
            getPollerConfig().update();
            getPoller().getPollOutagesConfig().update();
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Failed to reload PollerConfigFactory because "+e.getMessage(), e);
		}
        getPoller().refreshServicePackages();
    }

    /**
     * Return an id for this event listener
     */
    public String getName() {
        return "Poller:PollerEventProcessor";
    }

    /**
     * @return
     */
    private Poller getPoller() {
        return m_poller;
    }

    /**
     * @return
     */
    private PollerConfig getPollerConfig() {
        return getPoller().getPollerConfig();
    }

    private PollableNetwork getNetwork() {
        return getPoller().getNetwork();
    }

    /**
     * @return Returns the xmlrpc.
     */
    private boolean isXmlRPCEnabled() {
        return getPollerConfig().getXmlrpc();
    }

} // end class
