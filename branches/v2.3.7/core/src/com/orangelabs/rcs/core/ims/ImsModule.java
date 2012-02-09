/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright © 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.core.ims;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.CoreListener;
import com.orangelabs.rcs.core.ims.network.ImsConnectionManager;
import com.orangelabs.rcs.core.ims.network.ImsNetworkInterface;
import com.orangelabs.rcs.core.ims.network.gsm.CallManager;
import com.orangelabs.rcs.core.ims.network.sip.SipManager;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpConnection;
import com.orangelabs.rcs.core.ims.protocol.rtp.core.RtpSource;
import com.orangelabs.rcs.core.ims.protocol.sip.SipEventListener;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceDispatcher;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.capability.CapabilityService;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.presence.PresenceService;
import com.orangelabs.rcs.core.ims.service.richcall.RichcallService;
import com.orangelabs.rcs.core.ims.service.sip.SipService;
import com.orangelabs.rcs.core.ims.service.terms.TermsConditionsService;
import com.orangelabs.rcs.core.ims.userprofile.UserProfile;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.logger.Logger;

import java.util.Enumeration;

/**
 * IMS module
 *  
 * @author JM. Auffret
 */ 
public class ImsModule implements SipEventListener {
    /**
     * Core
     */
    private Core core;

    /**
	 * IMS user profile
	 */
    public static UserProfile IMS_USER_PROFILE = null;
   
    /**
     * IMS connection manager
     */
    private ImsConnectionManager connectionManager;

    /**
     * IMS services
     */
    private ImsService services[];

    /**
     * Service dispatcher
     */
    private ImsServiceDispatcher serviceDispatcher;    
    
    /**
	 * Call manager
	 */
	private CallManager callManager;    
    
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param core Core
     * @throws CoreException 
     */
    public ImsModule(Core core) throws CoreException {
    	this.core = core;
    	
    	if (logger.isActivated()) {
    		logger.info("IMS module initialization");
    	}
   	
		// Create the IMS connection manager
        try {
			connectionManager = new ImsConnectionManager(this);
        } catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("IMS connection manager initialization has failed", e);
        	}
            throw new CoreException("Can't instanciate the IMS connection manager");
        }

        // Set general parameters
		SipManager.TIMEOUT = RcsSettings.getInstance().getSipTransactionTimeout();
		RtpSource.CNAME = ImsModule.IMS_USER_PROFILE.getPublicUri();
		MsrpConnection.MSRP_TRACE_ENABLED = RcsSettings.getInstance().isMediaTraceActivated();

		// Instanciates the IMS services
        services = new ImsService[6];
        
        // Create terms & conditions service
        services[ImsService.TERMS_SERVICE] = new TermsConditionsService(this);

        // Create capability discovery service
        services[ImsService.CAPABILITY_SERVICE] = new CapabilityService(this);
        
        // Create IM service (mandatory)
        services[ImsService.IM_SERVICE] = new InstantMessagingService(this);
        
        // Create richcall service (optional)
        services[ImsService.RICHCALL_SERVICE] = new RichcallService(this);

        // Create presence service (optional)
        services[ImsService.PRESENCE_SERVICE] = new PresenceService(this);

        // Create generic SIP service
        services[ImsService.SIP_SERVICE] = new SipService(this);

        // Create the service dispatcher
        serviceDispatcher = new ImsServiceDispatcher(this);

        // Create the call manager
    	callManager = new CallManager(this);
        
    	if (logger.isActivated()) {
    		logger.info("IMS module has been created");
    	}
    }
    
    /**
     * Returns the SIP manager
     * 
     * @return SIP manager
     */
    public SipManager getSipManager() {
    	return getCurrentNetworkInterface().getSipManager();
    }
         
	/**
     * Returns the current network interface
     * 
     * @return Network interface
     */
	public ImsNetworkInterface getCurrentNetworkInterface() {
		return connectionManager.getCurrentNetworkInterface();
	}
	
	/**
	 * Returns the ImsConnectionManager
	 * 
	 * @return ImsConnectionManager
	 */
	public ImsConnectionManager getImsConnectionManager(){
		return connectionManager;
	}

	/**
     * Start the IMS module
     */
    public void start() {
    	if (logger.isActivated()) {
    		logger.info("Start the IMS module");
    	}
    	
    	// Start the service dispatcher
    	serviceDispatcher.start();

		// Start call monitoring
    	callManager.startCallMonitoring();
    	
    	if (logger.isActivated()) {
    		logger.info("IMS module is started");
    	}
    }
    	
    /**
     * Stop the IMS module
     */
    public void stop() {
    	if (logger.isActivated()) {
    		logger.info("Stop the IMS module");
    	}
         
		// Stop call monitoring
    	callManager.stopCallMonitoring();

    	// Terminate the connection manager
    	connectionManager.terminate();

    	// Terminate the service dispatcher
    	serviceDispatcher.terminate();

    	if (logger.isActivated()) {
    		logger.info("IMS module has been stopped");
    	}
    }

    /**
     * Start IMS services
     */
    public void startImsServices() {
    	// Start each services
		for(int i=0; i < services.length; i++) {
			if (services[i].isActivated()) {
				if (logger.isActivated()) {
					logger.info("Start IMS service: " + services[i].getClass().getName());
				}
				services[i].start();
			}
		}
    }
    
    /**
     * Stop IMS services
     */
    public void stopImsServices() {
    	// Abort all pending sessions
    	abortAllSessions();
    	
    	// Stop each services
    	for(int i=0; i < services.length; i++) {
    		if (services[i].isActivated()) {
				if (logger.isActivated()) {
					logger.info("Stop IMS service: " + services[i].getClass().getName());
				}
    			services[i].stop();
    		}
    	}
    }

    /**
     * Check IMS services
     */
    public void checkImsServices() {
    	for(int i=0; i < services.length; i++) {
    		if (services[i].isActivated()) {
				if (logger.isActivated()) {
					logger.info("Check IMS service: " + services[i].getClass().getName());
				}
    			services[i].check();
    		}
    	}
    }

	/**
	 * Returns the call manager
	 * 
	 * @return Call manager
	 */
	public CallManager getCallManager() {
		return callManager;
	}
	
	/**
     * Returns the IMS service
     * 
     * @param id Id of the IMS service
     * @return IMS service
     */
    public ImsService getImsService(int id) {
    	return services[id]; 
    }

    /**
     * Returns the IMS services
     * 
     * @return Table of IMS service
     */
    public ImsService[] getImsServices() {
    	return services; 
    }   

    /**
     * Returns the terms & conditions service
     * 
     * @return Terms & conditions service
     */
    public TermsConditionsService getTermsConditionsService() {
    	return (TermsConditionsService)services[ImsService.TERMS_SERVICE];
    }

    /**
     * Returns the capability service
     * 
     * @return Capability service
     */
    public CapabilityService getCapabilityService() {
    	return (CapabilityService)services[ImsService.CAPABILITY_SERVICE];
    }

    /**
     * Returns the rich call service
     * 
     * @return Richcall service
     */
    public RichcallService getRichcallService() {
    	return (RichcallService)services[ImsService.RICHCALL_SERVICE];
    }

    /**
     * Returns the presence service
     * 
     * @return Presence service
     */
    public PresenceService getPresenceService() {
    	return (PresenceService)services[ImsService.PRESENCE_SERVICE];
    }
    
    /**
     * Returns the Instant Messaging service
     * 
     * @return Instant Messaging service
     */
    public InstantMessagingService getInstantMessagingService() {
    	return (InstantMessagingService)services[ImsService.IM_SERVICE];
    }

    /**
     * Returns the SIP service
     * 
     * @return SIP service
     */
    public SipService getSipService() {
    	return (SipService)services[ImsService.SIP_SERVICE];
    }

    /**
     * Return the core instance
     * 
     * @return Core instance
     */
    public Core getCore() {
    	return core;
    }
    	
	/**
     * Return the core listener
     * 
     * @return Core listener
     */
    public CoreListener getCoreListener() {
    	return core.getListener();
    }
	
	/**
	 * Receive SIP request
	 * 
	 * @param request SIP request
	 */
	public void receiveSipRequest(SipRequest request) {
        // Post the incoming request to the service dispatcher
    	serviceDispatcher.postSipRequest(request);
	}
	
	/**
	 * Abort all sessions
	 */
	public void abortAllSessions() {
		if (logger.isActivated()) {
			logger.debug("Abort all pending sessions");
		}
		ImsService[] services = getImsServices();
		for(int i=0; i < services.length; i++) {
			ImsService service = services[i];
			for (Enumeration<ImsServiceSession> e = service.getSessions(); e.hasMoreElements() ;) {
				ImsServiceSession session = (ImsServiceSession)e.nextElement();
				if (logger.isActivated()) {
					logger.debug("Abort session " + session.getSessionID());
				}
				session.abortSession();
			}
		}
	}		
}
