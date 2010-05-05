package com.orangelabs.rcs.core.ims.userprofile;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.provider.settings.RcsSettings;

/**
 * User profile read from RCS settings database
 * 
 * @author JM. Auffret
 */
public class SettingsUserProfileInterface extends UserProfileInterface {
	/**
	 * Constructor
	 */
	public SettingsUserProfileInterface() {
		super();
	}
	
	/**
	 * Read the user profile
	 * 
	 * @return User profile
	 * @throws CoreException
	 */
	public UserProfile read() throws CoreException {
		String username = RcsSettings.getInstance().getUserProfileUserName(); 
		String displayName = RcsSettings.getInstance().getUserProfileDisplayName();
		String privateID = RcsSettings.getInstance().getUserProfilePrivateId();
		String password = RcsSettings.getInstance().getUserProfilePassword();
		
		String homeDomain = RcsSettings.getInstance().getUserProfileDomain();
		String proxyAddr = RcsSettings.getInstance().getUserProfileProxy();
		
		String xdmServer = RcsSettings.getInstance().getUserProfileXdmServer();
		String xdmLogin = RcsSettings.getInstance().getUserProfileXdmLogin();
		String xdmPassword = RcsSettings.getInstance().getUserProfileXdmPassword();
		
		return new UserProfile(username, displayName, privateID, password,
				homeDomain, proxyAddr,
				xdmServer, xdmLogin, xdmPassword);
	}
}
