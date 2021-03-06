/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.floodlightcontroller.interceptor;

import eu.netide.lib.netip.ErrorMessage;
import eu.netide.lib.netip.FenceMessage;
import eu.netide.lib.netip.NetIDEProtocolVersion;
import eu.netide.lib.netip.OpenFlowMessage;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;

import java.util.List;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class Relay {

    protected static Logger logger = LoggerFactory.getLogger(NetIdeModule.class);;
    
    protected static int lastXID = 0;
    
    public static int getNetIpID(){
    	lastXID +=1;
    	return lastXID;
    }
    
    public static void sendToCore(ZeroMQBaseConnector coreConnector, OFMessage msg, long datapathId, int moduleId) {
    	int netIpID = -1;
    	
    	// if OF XID is less than last seen NetIPID
    	// set the NetIpID = to last seen NetIpID + 1
    	// otherwise netIpID = to OF XID
    	if (msg.getXid() < lastXID){
    		netIpID = lastXID + 1;
    	}else{
    		netIpID = (int) msg.getXid();
    		lastXID = netIpID;
    	}
    	
        OpenFlowMessage ofMessage = new OpenFlowMessage();
        ofMessage.getHeader().setDatapathId(datapathId);
        ofMessage.getHeader().setTransactionId(netIpID);
        ofMessage.getHeader().setNetIDEProtocolVersion(NetIDEProtocolVersion.VERSION_1_4);
        ofMessage.getHeader().setModuleId(moduleId);
        ChannelBuffer dcb = ChannelBuffers.dynamicBuffer();
        msg.writeTo(dcb);
        byte[] payload = new byte[dcb.readableBytes()];
        ofMessage.getHeader().setPayloadLength((short) payload.length);
        ofMessage.setOfMessage(msg);
        coreConnector.SendData(ofMessage.toByteRepresentation());
    }

    public static void sendToController(ChannelFuture future, OFMessage message) {
        ChannelBuffer dcb = ChannelBuffers.dynamicBuffer();
        try {
            message.writeTo(dcb);
            future.getChannel().write(dcb);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
    
    public static void sendToController(NetIDEProtocolVersion netIpVersion, ZeroMQBaseConnector coreConnector, 
    		IFloodlightProviderService floodlightProvider, IOFSwitch sw, OFMessage message, String moduleName, 
    		int moduleId){
        // SEND PACKETS DIRECTLY TO THE PIPELINE
        FloodlightContext context = new FloodlightContext();
        if(moduleName == null || moduleName.isEmpty()){
            // send to all modules
            // context=null;
            floodlightProvider.handleMessage(sw,message,context);
        }else{
            // NetIDE composition, send to a specific module
            List<IOFMessageListener> listeners = null;
            Map<OFType,List<IOFMessageListener>> messageListeners = floodlightProvider.getListeners();
            if (messageListeners.containsKey(message.getType())) {
                listeners = messageListeners.get(message.getType());
            }
            for (IOFMessageListener listener : listeners) {
                if (moduleName.equals(listener.getName())) {
                    listener.receive(sw, message, context);
                    FenceMessage fence = new FenceMessage();
                    fence.getHeader().setNetIDEProtocolVersion(netIpVersion);
                    fence.getHeader().setModuleId(moduleId);
                    fence.getHeader().setPayloadLength((short) 0);
                    fence.getHeader().setDatapathId(-1);
                    fence.getHeader().setTransactionId(Relay.getNetIpID());
                    coreConnector.SendData(fence.toByteRepresentation());
                } else {
                    ErrorMessage error = new ErrorMessage();
                    error.getHeader().setNetIDEProtocolVersion(netIpVersion);
                    error.getHeader().setModuleId(moduleId);
                    error.getHeader().setPayloadLength((short) 0);
                    error.getHeader().setDatapathId(-1);
                    error.getHeader().setTransactionId(Relay.getNetIpID());
                    coreConnector.SendData(error.toByteRepresentation());
                }
            }
        }
    }

}
