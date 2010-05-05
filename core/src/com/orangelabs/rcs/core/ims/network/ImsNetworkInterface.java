package com.orangelabs.rcs.core.ims.network;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.access.NetworkAccess;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.registration.RegistrationManager;
import com.orangelabs.rcs.core.ims.network.sip.SipManager;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Abstract IMS network interface
 *  
 * @author JM. Auffret
 */
public abstract class ImsNetworkInterface {
	/**
	 * IMS module
	 */
	private ImsModule imsModule;
	
    /**
	 * Network access
	 */
	private NetworkAccess access;
	
    /**
     * SIP manager
     */
    private SipManager sip;

    /**
     * Registration manager
     */
    private RegistrationManager registration;
	
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
	 * Constructor
	 * 
	 * @param imsModule IMS module
	 * @param access Network access
	 * @throws CoreException
	 */
	public ImsNetworkInterface(ImsModule imsModule, NetworkAccess access) throws CoreException {
		this.imsModule = imsModule;
		this.access = access;
		
        // Instanciates the SIP manager
        sip = new SipManager(this,
        		ImsModule.IMS_USER_PROFILE.getOutboundProxyAddr(),
        		imsModule.getCore().getConfig().getInteger("SipListeningPort"));
         
        // Instanciates the registration manager
        registration = new RegistrationManager(this,
        		imsModule.getCore().getConfig().getString("RegistrationProcedure"),
        		imsModule.getCore().getConfig().getInteger("RegisterExpirePeriod"));
	}
	
	/**
	 * Returns the IMS module
	 * 
	 * @return IMS module
	 */
	public ImsModule getImsModule() {
		return imsModule;
	}

	/**
     * Returns the network access
     * 
     * @return Network access
     */
    public NetworkAccess getNetworkAccess() {
    	return access;
    }

	/**
     * Returns the SIP manager
     * 
     * @return SIP manager
     */
    public SipManager getSipManager() {
    	return sip;
    }

    /**
     * Is registered
     * 
     * @return Return True if the terminal is registered, else return False
     */
    public boolean isRegistered() {
        return registration.isRegistered();
    }    

    /**
     * Register to the IMS
     * 
     * @return Registration result
     */
    public boolean register() {
		if (logger.isActivated()) {
			logger.debug("Register to IMS");
		}

		// Initialize the SIP stack
		try {
	    	sip.initStack(access.getIpAddress());
	    	sip.getSipStack().addSipListener(imsModule);
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't instanciate the SIP stack", e);
			}
			return false;
		}
		
    	// Register to IMS
		boolean registered = registration.registration();
		if (!registered) {
			if (logger.isActivated()) {
				logger.debug("IMS registration has failed");
			}
		} else {
			if (logger.isActivated()) {
				logger.debug("IMS registration successful");
			}
		}
    	
    	return registered;
    }
    
	/**
     * Unregister from the IMS
     */
    public void unregister() {
		if (logger.isActivated()) {
			logger.debug("Unregister from IMS");
		}

		// Unregister from IMS
		registration.unRegistration();
    	
    	// Close the SIP stack
    	sip.closeStack();
    }
    
	/**
     * Registration terminated
     */
    public void registrationTerminated() {
		if (logger.isActivated()) {
			logger.debug("Registration has been terminated");
		}

		// Stop registration
		registration.stopRegistration();

		// Close the SIP stack
    	sip.closeStack();
    }
    
    /**
     * Returns the network access info
     * 
     * @return String
     * @throws CoreException
     */
    public String getAccessInfo() throws CoreException {
    	return getNetworkAccess().getType();
    }
}