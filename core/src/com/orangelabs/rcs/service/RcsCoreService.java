/*******************************************************************************
 * Software Name : RCS IMS Stack
 * Version : 2.0
 * 
 * Copyright � 2010 France Telecom S.A.
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
package com.orangelabs.rcs.service;

import java.io.File;
import java.util.Vector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Vibrator;
import android.widget.Toast;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.CoreListener;
import com.orangelabs.rcs.core.ims.ImsError;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.service.capability.Capabilities;
import com.orangelabs.rcs.core.ims.service.im.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.TerminatingAdhocGroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.TerminatingOne2OneChatSession;
import com.orangelabs.rcs.core.ims.service.presence.FavoriteLink;
import com.orangelabs.rcs.core.ims.service.presence.Geoloc;
import com.orangelabs.rcs.core.ims.service.presence.PhotoIcon;
import com.orangelabs.rcs.core.ims.service.presence.PresenceError;
import com.orangelabs.rcs.core.ims.service.presence.PresenceInfo;
import com.orangelabs.rcs.core.ims.service.presence.pidf.OverridingWillingness;
import com.orangelabs.rcs.core.ims.service.presence.pidf.Person;
import com.orangelabs.rcs.core.ims.service.presence.pidf.PidfDocument;
import com.orangelabs.rcs.core.ims.service.presence.pidf.Tuple;
import com.orangelabs.rcs.core.ims.service.sharing.streaming.ContentSharingStreamingSession;
import com.orangelabs.rcs.core.ims.service.sharing.transfer.ContentSharingTransferSession;
import com.orangelabs.rcs.core.ims.service.toip.TerminatingToIpSession;
import com.orangelabs.rcs.core.ims.service.voip.TerminatingVoIpSession;
import com.orangelabs.rcs.core.ims.userprofile.UserProfileNotProvisionnedException;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.platform.logger.AndroidAppender;
import com.orangelabs.rcs.provider.eab.RichAddressBook;
import com.orangelabs.rcs.provider.messaging.RichMessaging;
import com.orangelabs.rcs.provider.messaging.RichMessagingData;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.client.management.IManagementApi;
import com.orangelabs.rcs.service.api.client.management.ManagementApiIntents;
import com.orangelabs.rcs.service.api.client.messaging.IChatSession;
import com.orangelabs.rcs.service.api.client.messaging.IMessagingApi;
import com.orangelabs.rcs.service.api.client.messaging.MessagingApiIntents;
import com.orangelabs.rcs.service.api.client.presence.IPresenceApi;
import com.orangelabs.rcs.service.api.client.presence.PresenceApiIntents;
import com.orangelabs.rcs.service.api.client.richcall.IRichCallApi;
import com.orangelabs.rcs.service.api.client.richcall.RichCallApiIntents;
import com.orangelabs.rcs.service.api.client.toip.IToIpApi;
import com.orangelabs.rcs.service.api.client.toip.ToIpApiIntents;
import com.orangelabs.rcs.service.api.client.voip.IVoIpApi;
import com.orangelabs.rcs.service.api.client.voip.VoIpApiIntents;
import com.orangelabs.rcs.service.api.server.ServerApiException;
import com.orangelabs.rcs.service.api.server.management.ManagementApiService;
import com.orangelabs.rcs.service.api.server.messaging.ChatSession;
import com.orangelabs.rcs.service.api.server.messaging.MessagingApiService;
import com.orangelabs.rcs.service.api.server.presence.PresenceApiService;
import com.orangelabs.rcs.service.api.server.richcall.RichCallApiService;
import com.orangelabs.rcs.service.api.server.toip.ToIpApiService;
import com.orangelabs.rcs.service.api.server.voip.VoIpApiService;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Appender;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * RCS core service. This service offers a flat API to any other process (activities)
 * to access to RCS features. This service is started automatically at device boot.
 * 
 * @author jexa7410
 */
public class RcsCoreService extends Service implements CoreListener {   
	/**
	 * Notification ID
	 */
	private final static int RCS_SERVICE_NOTIFICATION = 1000;
	
	/**
	 * Application context
	 */
    public static Context CONTEXT = null;

    /**
	 * UI message handler
	 */
    private final Handler handler = new Handler();

	/**
	 * CPU manager
	 */
	private CpuManager cpuManager = new CpuManager();

	/**
	 * Presence API
	 */
    private PresenceApiService presenceApi = new PresenceApiService(); 

	/**
	 * Messaging API
	 */
	private MessagingApiService messagingApi = new MessagingApiService(); 

	/**
	 * Rich call API
	 */
	private RichCallApiService richcallApi = new RichCallApiService(); 

	/**
	 * VoIP API
	 */
	private VoIpApiService voipApi = new VoIpApiService(); 

	/**
	 * ToIP API
	 */
	private ToIpApiService toipApi = new ToIpApiService(); 

	/**
	 * Management API
	 */
	private ManagementApiService mgtApi = new ManagementApiService(); 
	
	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	@Override
    public void onCreate() {
		// Set application context
		CONTEXT = getApplicationContext();

		// Set logger appenders
		Appender[] appenders = new Appender[] { 
//				new SipAppender(),
//				new XdmAppender(),
				new AndroidAppender()
			};
		Logger.setAppenders(appenders);
		Logger.activationFlag = Logger.TRACE_ON;

		// Start the core
		startCore();
	}

    @Override
    public void onDestroy() {
    	// Close APIs
		presenceApi.close();
		richcallApi.close();
		messagingApi.close();
		voipApi.close();
		toipApi.close();
		
        // Stop the core
        stopCore();
    }

    /**
     * Start core
     */
    public synchronized void startCore() {
		if (Core.getInstance() != null) {
			// Already started
			return;
		}

        try {
			// Instanciate platform factory
			AndroidFactory.loadFactory(getApplicationContext());

			// Instanciate the settings manager
            RcsSettings.createInstance(getApplicationContext());

            // Instanciate the rich address book
            RichAddressBook.createInstance(getApplicationContext());

            // Instanciate the rich messaging 
            RichMessaging.createInstance(getApplicationContext());

            // Create the core
			Core core = Core.createCore(this);
			
			// Restore the last presence info from the address book database
			PresenceInfo lastPresenceInfo = RichAddressBook.getInstance().getMyPresenceInfo();
			// TODO: read capabilities from the database
			Capabilities capabilities = core.getCapabilityService().getDefaultCapabilities();
			lastPresenceInfo.setCapabilities(capabilities);
			core.getPresenceService().setPresenceInfo(lastPresenceInfo);
			
			// Start the core
			Core.getInstance().startCore();		

			// Create multimedia directory on sdcard
			createDirectory(FileFactory.getFactory().getPhotoRootDirectory());
			createDirectory(FileFactory.getFactory().getVideoRootDirectory());
			createDirectory(FileFactory.getFactory().getFileRootDirectory());
			
			// Init CPU manager
			cpuManager.init();
			
			// Send startup intent 
	    	Intent intent = new Intent(ManagementApiIntents.SERVICE_STARTED);
			getApplicationContext().sendBroadcast(intent);

	        // Show a first notification
	    	addRcsServiceNotification(false, getString(R.string.rcs_core_label_rcs_loaded));

	    	if (logger.isActivated()) {
				logger.info("RCS core service started with success");
			}
        } catch(UserProfileNotProvisionnedException e) {
			// User profile not well provisionned
			if (logger.isActivated()) {
				logger.error("User profile not well provisionned: " + e.getMessage());
			}
	    	Intent intent = new Intent("com.orangelabs.rcs.PROVISIONING");
	    	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    	startActivity(intent);
	    	stopSelf();
		} catch(Exception e) {
			// Unexpected error
			if (logger.isActivated()) {
				logger.error("Can't instanciate the RCS core service", e);
			}
			
			// Show error in notification bar
	    	addRcsServiceNotification(false, getString(R.string.rcs_core_label_rcs_failed));
		}
    }
    
    /**
     * Stop core
     */
    public synchronized void stopCore() {
		if (Core.getInstance() == null) {
			// Already stopped
			return;
		}
		
		// Force the poke down, then a last publish will be sent at core shutdown
		PresenceInfo presenceInfo = Core.getInstance().getPresenceService().getPresenceInfo();
		presenceInfo.setHyperavailabilityStatus(false);
		try {
			RichAddressBook.getInstance().setMyPresenceInfo(presenceInfo);
		} catch(Exception e) {}

    	// Terminate the core
		Core.terminateCore();

		// Close CPU manager
		cpuManager.close();

		// Send stopped intent 
    	Intent intent = new Intent(ManagementApiIntents.SERVICE_STOPPED);
		getApplicationContext().sendBroadcast(intent);

		if (logger.isActivated()) {
			logger.info("RCS core service stopped with success");
		}
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (IPresenceApi.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("Presence API binding");
    		}
            return presenceApi;
        }
        if (IMessagingApi.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("Messaging API binding");
    		}
            return messagingApi;
        }
        if (IRichCallApi.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("Rich call API binding");
    		}
            return richcallApi;
        }
        if (IManagementApi.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("Settings API binding");
    		}
            return mgtApi;
        }
        if (IVoIpApi.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("VoIP API binding");
    		}
            return voipApi;
        }
        if (IToIpApi.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("ToIP API binding");
    		}
            return toipApi;
        }
        return null;
    }

	/**
	 * Create directory
	 * 
	 * @param path Direcory path
	 */
	private void createDirectory(String path) {
		File dir = new File(path); 
		if (!dir.exists()) {
			dir.mkdirs(); 
		}
	}
	
	/**
	 * Display a popup
	 * 
	 * @param txt Text
	 */
	private void displayPopup(final String txt) {
        handler.post(new Runnable() { 
			public void run() {
				Toast.makeText(RcsCoreService.this, txt, Toast.LENGTH_LONG).show();
        	}
    	});
    }

    /**
     * Add RCS service notification
     * 
     * @param state Service state (ON|OFF)
     * @param label Label
     */
    public static void addRcsServiceNotification(boolean state, String label) {
    	// Create notification
		PendingIntent contentIntent = PendingIntent.getActivity(RcsCoreService.CONTEXT, 0,
                new Intent("com.orangelabs.rcs.SETTINGS"), 0);
		int iconId; 
		if (state) {
			iconId  = R.drawable.rcs_core_notif_on_icon;
		} else {
			iconId  = R.drawable.rcs_core_notif_off_icon; 
		}
        Notification notif = new Notification(iconId, "", System.currentTimeMillis());
        notif.flags = Notification.FLAG_NO_CLEAR;
        notif.setLatestEventInfo(RcsCoreService.CONTEXT, RcsCoreService.CONTEXT.getString(R.string.rcs_core_rcs_notification_title),
        		label, contentIntent);
        
        // Send notification
		NotificationManager notificationManager = (NotificationManager)RcsCoreService.CONTEXT.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(RCS_SERVICE_NOTIFICATION, notif);
    }
    
    /*---------------------------- CORE EVENTS ---------------------------*/
    
    /**
     * Core layer has been started
     */
    public void handleCoreLayerStarted() {
		if (logger.isActivated()) {
			logger.debug("Handle event core started");
		}

		// Display a notification
		addRcsServiceNotification(false, getString(R.string.rcs_core_label_rcs_started));
    }

    /**
     * Core layer has been terminated
     */
    public void handleCoreLayerStopped() {
		if (logger.isActivated()) {
			logger.debug("Handle event core terminated");
		}

		// Display a notification
		addRcsServiceNotification(false, getString(R.string.rcs_core_label_rcs_stopped));
    }
    
	/**
	 * Handle "registration successful" event
	 * 
	 * @param registered Registration flag
	 */
	public void handleRegistrationSuccessful() {
		if (logger.isActivated()) {
			logger.debug("Handle event registration ok");
		}

		// Display a notification
		addRcsServiceNotification(true, getString(R.string.rcs_core_label_connected_to_rcs));
	}

	/**
	 * Handle "registration failed" event
	 * 
     * @param error IMS error
   	 */
	public void handleRegistrationFailed(ImsError error) {
		if (logger.isActivated()) {
			logger.debug("Handle event registration failed");
		}

		// Display a notification
		addRcsServiceNotification(false, getString(R.string.rcs_core_label_rcs_connection_failed));
	}

	/**
	 * Handle "registration terminated" event
	 */
	public void handleRegistrationTerminated() {
		if (logger.isActivated()) {
			logger.debug("Handle event registration terminated");
		}

		// Display a notification
		addRcsServiceNotification(false, getString(R.string.rcs_core_label_disconnected_from_rcs));
	}
	
    /**
     * Handle "publish successful" event
     */
    public void handlePublishPresenceSuccessful() {
		if (logger.isActivated()) {
			logger.debug("Handle event publish ok");
		}
		
		// Nothing to do
    }

    /**
     * Handle "publish presence failed" event
     * 
     * @param error Presence error 
     */
    public void handlePublishPresenceFailed(PresenceError error) {
		if (logger.isActivated()) {
			logger.debug("Handle event publish failed (error " + error.getErrorCode() + ")");
		}
		
		// Nothing to do
    }

    /**
     * Handle "publish presence terminated" event
     */
    public void handlePublishPresenceTerminated() {
		if (logger.isActivated()) {
			logger.debug("Handle event publish terminated");
		}
		
		// Nothing to do
    }

    /**
     * A new presence sharing notification has been received
     * 
     * @param contact Contact
     * @param status Status
     * @param reason Reason
     */
    public void handlePresenceSharingNotification(String contact, String status, String reason) {
		if (logger.isActivated()) {
			logger.debug("Handle event presence sharing notification for " + contact + " (" + status + ":" + reason + ")");
		}

		try {
			// Check if its a notification for a contact or for the end user
			String username = PhoneUtils.extractNumberFromUri(contact);
			if (RcsSettings.getInstance().getUserProfileUserName().equalsIgnoreCase(username)) {
				// End user notification
				if (logger.isActivated()) {
					logger.debug("Presence sharing notification for me: by-pass it");
				}
	    	} else { 
		    	// Update EAB provider
				RichAddressBook.getInstance().setContactSharingStatus(username, status, reason);
	
				// Broadcast intent
				Intent intent = new Intent(PresenceApiIntents.PRESENCE_SHARING_CHANGED);
		    	intent.putExtra("contact", contact);
		    	intent.putExtra("status", status);
		    	intent.putExtra("reason", reason);
				AndroidFactory.getApplicationContext().sendBroadcast(intent);
	    	}
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Internal exception", e);
    		}
    	}
    }

    /**
     * A new presence info notification has been received
     * 
     * @param contact Contact
     * @param presense Presence info document
     */
    public void handlePresenceInfoNotification(String contact, PidfDocument presence) {
    	if (logger.isActivated()) {
			logger.debug("Handle event presence info notification for " + contact);
		}

		try {
			// Test if person item is not null
			Person person = presence.getPerson();
			if (person == null) {
				if (logger.isActivated()) {
					logger.debug("Presence info is empty (i.e. no item person found) for contact " + contact);
				}
				return;
			}

			// Check if its a notification for a contact or for me
			String username = PhoneUtils.extractNumberFromUri(contact);
			if (RcsSettings.getInstance().getUserProfileUserName().equalsIgnoreCase(username)) {
				// Notification for me
				presenceInfoNotificationForMe(presence);
			} else {
				// Check that the contact exist in database
				String rcsStatus = RichAddressBook.getInstance().getContactSharingStatus(contact);
				if (rcsStatus == null) {
					if (logger.isActivated()) {
						logger.debug("Contact " + contact + " is not a RCS contact, by-pass the notification");
					}
					return;
				}

				// Notification for a contact
				presenceInfoNotificationForContact(contact, presence);
			}
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Internal exception", e);
    		}
		}
	}

    /**
     * A new presence info notification has been received for me
     * 
     * @param contact Contact
     * @param presense Presence info document
     */
    public void presenceInfoNotificationForMe(PidfDocument presence) {
    	if (logger.isActivated()) {
			logger.debug("Presence info notification for me");
		}

    	try {
			// Get the current presence info for me
    		PresenceInfo currentPresenceInfo = RichAddressBook.getInstance().getMyPresenceInfo();
    		if (currentPresenceInfo == null) {
    			currentPresenceInfo = new PresenceInfo();
    		}

			// Update the presence info
			Person person = presence.getPerson();
			if (person.getTimestamp() != -1) {
				currentPresenceInfo.setTimestamp(person.getTimestamp());
			}
			if (person.getNote() != null) {
				currentPresenceInfo.setFreetext(person.getNote().getValue());
			}
			if (person.getHomePage() != null) {
				currentPresenceInfo.setFavoriteLink(new FavoriteLink(person.getHomePage()));
			}
			
    		// Get photo Etag values
			String lastEtag = null;
			String newEtag = null; 
			if (person.getStatusIcon() != null) {
				newEtag = person.getStatusIcon().getEtag();
			}
			if (currentPresenceInfo.getPhotoIcon() != null) {
				lastEtag = currentPresenceInfo.getPhotoIcon().getEtag();
			}
    		
    		// Test if the photo has been removed
			if ((lastEtag != null) && (person.getStatusIcon() == null)) {
	    		if (logger.isActivated()) {
	    			logger.debug("Photo has been removed for me");
	    		}
	    		
    			// Update the presence info
				currentPresenceInfo.setPhotoIcon(null);

				// Update EAB provider
	    		RichAddressBook.getInstance().removeMyPhotoIcon();
			} else		
	    	// Test if the photo has been changed
	    	if ((person.getStatusIcon() != null) &&	(newEtag != null)) {
	    		if ((lastEtag == null) || (!lastEtag.equals(newEtag))) {
		    		if (logger.isActivated()) {
		    			logger.debug("Photo has changed for me, download it in background");
		    		}
		
		    		// Download the photo in background
		    		downloadPhotoForMe(presence.getPerson().getStatusIcon().getUrl(), newEtag);
	    		}
	    	}
	    	   		    		
	    	// Update EAB provider
    		RichAddressBook.getInstance().setMyPresenceInfo(currentPresenceInfo);

    		// Broadcast intent
	    	Intent intent = new Intent(PresenceApiIntents.MY_PRESENCE_INFO_CHANGED);
	    	getApplicationContext().sendBroadcast(intent);
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Internal exception", e);
    		}
		}
    }

    /**
     * A new presence info notification has been received for a given contact
     * 
     * @param contact Contact
     * @param presense Presence info document
     */
    public void presenceInfoNotificationForContact(String contact, PidfDocument presence) {
    	if (logger.isActivated()) {
			logger.debug("Presence info notification for contact " + contact);
		}

    	try {
			// Get the current presence info for the given contact
    		PresenceInfo currentPresenceInfo = RichAddressBook.getInstance().getContactPresenceInfo(contact);
    		if (currentPresenceInfo == null) {
    			currentPresenceInfo = new PresenceInfo();
    		}
			boolean lastHyperavailabilityStatus = currentPresenceInfo.isHyperavailable();

			// Update the current capabilities
			Capabilities capabilities =  new Capabilities(); 
			Vector<Tuple> tuples = presence.getTuplesList();
			for(int i=0; i < tuples.size(); i++) {
				Tuple tuple = (Tuple)tuples.elementAt(i);
				
				boolean state = false; 
				if (tuple.getStatus().getBasic().getValue().equals("open")) {
					state = true;
				}
					
				String id = tuple.getService().getId();
				if (id.equalsIgnoreCase(Capabilities.VIDEO_SHARING_CAPABILITY)) {
					capabilities.setVideoSharingSupport(state);
				} else
				if (id.equalsIgnoreCase(Capabilities.IMAGE_SHARING_CAPABILITY)) {
					capabilities.setImageSharingSupport(state);
				} else
				if (id.equalsIgnoreCase(Capabilities.FILE_SHARING_CAPABILITY)) {
					capabilities.setFileTransferSupport(state);
				} else
				if (id.equalsIgnoreCase(Capabilities.CS_VIDEO_CAPABILITY)) {
					capabilities.setCsVideoSupport(state);
				} else
				if (id.equalsIgnoreCase(Capabilities.IM_SESSION_CAPABILITY)) {
					capabilities.setImSessionSupport(state);
				}
			}
			currentPresenceInfo.setCapabilities(capabilities);

			// Update presence status
			boolean poke = false;
			String presenceStatus = PresenceInfo.UNKNOWN;
			Person person = presence.getPerson();
			OverridingWillingness willingness = person.getOverridingWillingness();
			if (willingness != null) {
				if ((willingness.getBasic() != null) && (willingness.getBasic().getValue() != null)) {
					// Set presence status
					presenceStatus = willingness.getBasic().getValue();
				}
				// TODO: test also the timestamp
				if (presenceStatus.equals(PresenceInfo.ONLINE) && (willingness.getUntilTimestamp() != -1)) {
					// Set poke status
					poke = true;
				}
			}				
			currentPresenceInfo.setPresenceStatus(presenceStatus);
			currentPresenceInfo.setHyperavailabilityStatus(poke);

			// Check poke status
	    	if (currentPresenceInfo.isHyperavailable()){
	    		// New poke period?
	    		if (!lastHyperavailabilityStatus) {    		
					// Display a toast when the e user that a contact is now hyper-available
					String contactName = RichAddressBook.getInstance().getContactDisplayName(contact);
					if (contactName == null){
						contactName = contact;
					}
					displayPopup(contactName + " " + getString(R.string.rcs_core_label_contact_hyperavailable));
					
					// Play a tone
					if (RcsSettings.getInstance().isPhoneBeepForHyperAvailability()) {
						ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_RING,100);
						toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2);
					}
					
					// Play a vibartion 
					if (RcsSettings.getInstance().isPhoneVibrateForHyperAvailability()) {
						Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
						vibrator.vibrate(1000);
					}
	    		}
	    	}

			// Update the presence info
			if (person.getTimestamp() != -1) {
				currentPresenceInfo.setTimestamp(person.getTimestamp());
			}
			if (person.getNote() != null) {
				currentPresenceInfo.setFreetext(person.getNote().getValue());
			}
			if (person.getHomePage() != null) {
				currentPresenceInfo.setFavoriteLink(new FavoriteLink(person.getHomePage()));
			}
			
			// Update geoloc info
			if (presence.getGeopriv() != null) {
				Geoloc geoloc = new Geoloc(presence.getGeopriv().getLatitude(),
						presence.getGeopriv().getLongitude(),
						presence.getGeopriv().getAltitude());
				currentPresenceInfo.setGeoloc(geoloc);
			}
			
	    	// Update EAB provider
    		RichAddressBook.getInstance().setContactPresenceInfo(contact, currentPresenceInfo);

    		// Get photo Etag values
			String lastEtag = RichAddressBook.getInstance().getContactPhotoEtag(contact);
			String newEtag = null; 
			if (person.getStatusIcon() != null) {
				newEtag = person.getStatusIcon().getEtag();
			}
    		
    		// Test if the photo has been removed
			if ((lastEtag != null) && (person.getStatusIcon() == null)) {
	    		if (logger.isActivated()) {
	    			logger.debug("Photo has been removed for " + contact);
	    		}

	    		// Update EAB provider
				RichAddressBook.getInstance().setContactPhotoIcon(contact, null, null);
				
				// Broadcast intent
				Intent intent = new Intent(PresenceApiIntents.CONTACT_PHOTO_CHANGED);
		    	intent.putExtra("contact", contact);
				AndroidFactory.getApplicationContext().sendBroadcast(intent);
			} else		
	    	// Test if the photo has been changed
	    	if ((person.getStatusIcon() != null) &&	(newEtag != null)) {
	    		if ((lastEtag == null) || (!lastEtag.equals(newEtag))) {
		    		if (logger.isActivated()) {
		    			logger.debug("Photo has changed for " + contact + ", download it in background");
		    		}
		
		    		// Download the photo in background
		    		downloadPhotoForContact(contact, presence.getPerson().getStatusIcon().getUrl(), newEtag);
	    		}
	    	}    	
	    	   		    		
    		// Broadcast intent
	    	Intent intent = new Intent(PresenceApiIntents.CONTACT_INFO_CHANGED);
	    	intent.putExtra("contact", contact);
	    	getApplicationContext().sendBroadcast(intent);
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Internal exception", e);
    		}
		}
    }
    
    /**
     * A new anonymous-fetch notification has been received
     * 
     * @param contact Contact
     * @param presense Presence info document
     */
    public void handleAnonymousFetchNotification(String contact, PidfDocument presence) {
    	if (logger.isActivated()) {
			logger.debug("Handle event anonymous fetch notification for " + contact);
		}

		try {
			// Get the current presence info for the given contact
    		PresenceInfo presenceInfo = RichAddressBook.getInstance().getContactPresenceInfo(contact);
    		if (presenceInfo == null) {
    			presenceInfo = new PresenceInfo();
    		}

			// Create intent
	    	Intent intent = new Intent(PresenceApiIntents.CONTACT_CAPABILITIES);
	    	intent.putExtra("contact", contact);

	    	// Read the capabilities
			if (presence != null) {
				Capabilities capabilities =  new Capabilities(); 
		    	Vector<Tuple> tuples = presence.getTuplesList();
				for(int i=0; i < tuples.size(); i++) {
					Tuple tuple = (Tuple)tuples.elementAt(i);
					boolean state = false; 
					if (tuple.getStatus().getBasic().getValue().equals("open")) {
						state = true;
					}
					String id = tuple.getService().getId();
					if (id.equalsIgnoreCase(Capabilities.VIDEO_SHARING_CAPABILITY)) {
						capabilities.setVideoSharingSupport(state);
					} else
					if (id.equalsIgnoreCase(Capabilities.IMAGE_SHARING_CAPABILITY)) {
						capabilities.setImageSharingSupport(state);
					} else
					if (id.equalsIgnoreCase(Capabilities.FILE_SHARING_CAPABILITY)) {
						capabilities.setFileTransferSupport(state);
					} else
					if (id.equalsIgnoreCase(Capabilities.CS_VIDEO_CAPABILITY)) {
						capabilities.setCsVideoSupport(state);
					} else
					if (id.equalsIgnoreCase(Capabilities.IM_SESSION_CAPABILITY)) {
						capabilities.setImSessionSupport(state);
					}

					// Update intent parameter
			    	intent.putExtra(id, state);
				}
				presenceInfo.setCapabilities(capabilities);
			} else {
		    	if (logger.isActivated()) {
					logger.debug("No presence document received in the anonymous fetch notification for contact " + contact);
				}
			}
			
	    	// Update EAB provider
			RichAddressBook.getInstance().setContactPresenceInfo(contact, presenceInfo);

			// Broadcast intent
	    	getApplicationContext().sendBroadcast(intent);
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Internal exception", e);
    		}
		}
    }
    
    /**
     * Download photo for me
     * 
     * @param url Photo URL
     * @param etag New Etag associated to the photo
     */
    private void downloadPhotoForMe(final String url, final String etag) {
		Thread t = new Thread() {
			public void run() {
		    	try {
		    		// Download from XDMS
		    		byte[] data = Core.getInstance().getPresenceService().getXdmManager().downloadContactPhoto(url);    		
		    		if (data != null) {
		    			// Update the presence info
		    			// TODO: remove -1 values
		    			PhotoIcon photoIcon = new PhotoIcon(data, -1, -1, etag);
		    			Core.getInstance().getPresenceService().getPresenceInfo().setPhotoIcon(photoIcon);
		    			
						// Update EAB provider
		    			RichAddressBook.getInstance().setMyPhotoIcon(photoIcon);
						
			    		// Broadcast intent
		    			// TODO : use a specific intent for the end user photo
				    	Intent intent = new Intent(PresenceApiIntents.MY_PRESENCE_INFO_CHANGED);
				    	getApplicationContext().sendBroadcast(intent);
			    	}
		    	} catch(Exception e) {
		    		if (logger.isActivated()) {
		    			logger.error("Internal exception", e);
		    		}
	    		}
			}
		};
		t.start();
    }
    
    /**
     * Download photo for a given contact
     * 
     * @param contact Contact
     * @param url Photo URL 
     * @param etag New Etag associated to the photo
     */
    private void downloadPhotoForContact(final String contact, final String url, final String etag) {
		Thread t = new Thread() {
			public void run() {
		    	try {
		    		// Download from XDMS
		    		byte[] data = Core.getInstance().getPresenceService().getXdmManager().downloadContactPhoto(url);    		
		    		if (data != null) {
						// Update EAB provider
		    			RichAddressBook.getInstance().setContactPhotoIcon(contact, data, etag);
						
			    		// Broadcast intent
				    	Intent intent = new Intent(PresenceApiIntents.CONTACT_PHOTO_CHANGED);
				    	intent.putExtra("contact", contact);
				    	getApplicationContext().sendBroadcast(intent);
			    	}
		    	} catch(Exception e) {
		    		if (logger.isActivated()) {
		    			logger.error("Internal exception", e);
		    		}
	    		}
			}
		};
		t.start();
    }

    /**
     * Poke period is started
     * 
     * @param expiration Expiration time
     */
    public void handlePokePeriodStarted(long expiration) {
		if (logger.isActivated()) {
			logger.debug("Handle event poke is started");
		}
		
		try {
			// Update EAB content provider
			RichAddressBook.getInstance().setMyPokeInfo(true);

			// Broadcast intent
			Intent intent = new Intent(PresenceApiIntents.MY_PRESENCE_STATUS_CHANGED);
			intent.putExtra("status", true);
			intent.putExtra("expireAt", expiration);
			AndroidFactory.getApplicationContext().sendBroadcast(intent);
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Internal exception", e);
    		}
		}
    }    
    
    /**
     * Poke period has been terminated
     */
    public void handlePokePeriodTerminated() {
		if (logger.isActivated()) {
			logger.debug("Handle event poke has been terminated");
		}

		try {
			// Update EAB content provider
			RichAddressBook.getInstance().setMyPokeInfo(false);
			
			// Broadcast intent
			Intent intent = new Intent(PresenceApiIntents.MY_PRESENCE_STATUS_CHANGED);
			intent.putExtra("status", false);
			AndroidFactory.getApplicationContext().sendBroadcast(intent);
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Internal exception", e);
    		}
		}
    }  

    /**
     * Content sharing capabilities indication 
     * 
     * @param contact Remote contact
     * @param image Image sharing supported
     * @param video Video sharing supported
     * @parem others Other supported services
     */
    public void handleContentSharingCapabilitiesIndication(String contact, boolean image, boolean video, Vector<String> others) {
		if (logger.isActivated()) {
			logger.debug("Handle event rich call capabilities indication for " + contact);
		}

		// Convert vector as string array
		String[] othersSupportedCapabilities = null;
		if (others != null) {
			othersSupportedCapabilities = new String[others.size()];
			others.toArray(othersSupportedCapabilities);
		}
		
		// Notify event listeners
    	Intent intent = new Intent(RichCallApiIntents.SHARING_CAPABILITIES);
    	intent.putExtra("contact", contact);
    	intent.putExtra("image", image);
    	intent.putExtra("video", video);
    	intent.putExtra("others", othersSupportedCapabilities);
    	getApplicationContext().sendBroadcast(intent);		
    }
    
    /**
     * A new presence sharing invitation has been received
     * 
     * @param contact Contact
     */
    public void handlePresenceSharingInvitation(String contact) {
		if (logger.isActivated()) {
			logger.debug("Handle event presence sharing invitation");
		}
		
        // Notify event listeners
    	Intent intent = new Intent(PresenceApiIntents.PRESENCE_INVITATION);
    	intent.putExtra("contact", contact);
    	getApplicationContext().sendBroadcast(intent);
    }
    
    /**
     * New content sharing transfer invitation
     * 
     * @param session Content sharing transfer invitation
     */
    public void handleContentSharingTransferInvitation(ContentSharingTransferSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event content sharing transfer invitation");
		}

		// Extract number from contact 
		String contact = SipUtils.extractUsernameFromAddress(session.getRemoteContact());

        // Notify event listeners
    	Intent intent = new Intent(RichCallApiIntents.IMAGE_SHARING_INVITATION);
    	intent.putExtra("contact", contact);
    	intent.putExtra("sessionId", session.getSessionID());
    	intent.putExtra("size", session.getContent().getSize());
    	getApplicationContext().sendBroadcast(intent);		
    }
    	
    /**
     * New content sharing streaming invitation
     * 
     * @param session CSh session
     */
    public void handleContentSharingStreamingInvitation(ContentSharingStreamingSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event content sharing streaming invitation");
		}

		// Extract number from contact 
		String contact = SipUtils.extractUsernameFromAddress(session.getRemoteContact());

        // Notify event listeners
    	Intent intent = new Intent(RichCallApiIntents.VIDEO_SHARING_INVITATION);
    	intent.putExtra("contact", contact);
    	intent.putExtra("sessionId", session.getSessionID());
    	getApplicationContext().sendBroadcast(intent);		
    }

	/**
	 * A new file transfer invitation has been received
	 * 
	 * @param session File transfer session
	 */
	public void handleFileTransferInvitation(ContentSharingTransferSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event file transfer invitation");
		}
		
		// Extract number from contact 
		String contact = SipUtils.extractUsernameFromAddress(session.getRemoteContact());

		// Load FT session Id and IM session Id if exists
		String ftSessionId = session.getSessionID();
		String imSessionId = null;
		
		try {
			// Check if there is a chat session in progress
			IChatSession chatSession = messagingApi.getChatSessionWithContact(contact);
			imSessionId = (chatSession!=null)?chatSession.getSessionID():ftSessionId;
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("GetChatSession",e);
			}
			imSessionId = ftSessionId;
		}
		
		// Update the messaging content provider
    	RichMessaging.getInstance().addMessage(RichMessagingData.FILETRANSFER, imSessionId, ftSessionId, contact, null, RichMessagingData.INCOMING, session.getContent().getEncoding(), session.getContent().getName(), session.getContent().getSize(), null, RichMessagingData.INVITED);
    	
        // Notify event listeners
    	Intent intent = new Intent(MessagingApiIntents.FILE_TRANSFER_INVITATION);
    	intent.putExtra("contact", contact);
    	intent.putExtra("sessionId", session.getSessionID());
    	intent.putExtra("size", session.getContent().getSize());
    	getApplicationContext().sendBroadcast(intent);
	}
    
	/**
	 * Handle "receive instant message" event
	 *
	 * @param message Received message
	 */
	public void handleReceiveInstantMessage(InstantMessage message) {
		if (logger.isActivated()) {
			logger.debug("Handle event receive IM");
		}

		// Extract number from contact 
		String contact = SipUtils.extractUsernameFromAddress(message.getRemote());
		
        // Notify event listeners
    	Intent intent = new Intent(MessagingApiIntents.INSTANT_MESSAGE);
    	intent.putExtra("contact", contact);
    	intent.putExtra("message", message.getTextMessage());
    	intent.putExtra("receivedAt", message.getDate());
    	getApplicationContext().sendBroadcast(intent);
    }

	/**
     * New one-to-one chat session invitation
     * 
     * @param session Chat session
     */
	public void handleOne2OneChatSessionInvitation(TerminatingOne2OneChatSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event receive 1-1 chat session invitation");
		}

		// Extract number from contact 
		String contact = SipUtils.extractUsernameFromAddress(session.getRemoteContact());
		
		// Update the messaging content provider
		RichMessaging.getInstance().addMessage(RichMessagingData.CHAT, session.getSessionID(), null, contact, session.getSubject(), RichMessagingData.INCOMING, "text/plain", null, session.getSubject().length(), null, RichMessagingData.INVITED);

    	// Notify event listeners
    	Intent intent = new Intent(MessagingApiIntents.CHAT_INVITATION);
    	intent.putExtra("contact", contact);
    	intent.putExtra("subject", session.getSubject());
    	intent.putExtra("sessionId", session.getSessionID());
    	getApplicationContext().sendBroadcast(intent);
    }

    /**
     * New ad-hoc group chat session invitation
     * 
     * @param session Chat session
     */
	public void handleAdhocGroupChatSessionInvitation(TerminatingAdhocGroupChatSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event receive ad-hoc group chat session invitation");
		}

		// Extract number from contact 
		String contact = SipUtils.extractUsernameFromAddress(session.getRemoteContact());
		
		// Update the messaging content provider
		RichMessaging.getInstance().addMessage(RichMessagingData.CHAT, session.getSessionID(), null, contact, session.getSubject(), RichMessagingData.INCOMING, "text/plain", null, session.getSubject().length(), null, RichMessagingData.INVITED);

    	// Notify event listeners
    	Intent intent = new Intent(MessagingApiIntents.CHAT_INVITATION);
    	intent.putExtra("contact", contact);
    	intent.putExtra("subject", session.getSubject());
    	intent.putExtra("sessionId", session.getSessionID());
    	getApplicationContext().sendBroadcast(intent);
	}

    /**
     * New VoIP call invitation
     * 
     * @param session VoIP session
     */
    public void handleVoIpCallInvitation(TerminatingVoIpSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event receive VoIP session invitation");
		}
		
		// Extract number from contact 
		String contact = SipUtils.extractUsernameFromAddress(session.getRemoteContact());

    	// Notify event listeners
    	Intent intent = new Intent(VoIpApiIntents.VOIP_CALL_INVITATION);
    	intent.putExtra("contact", contact);
    	intent.putExtra("sessionId", session.getSessionID());
    	getApplicationContext().sendBroadcast(intent);
    }
    
    /**
     * New ToIP call invitation
     * 
     * @param session VoIP session
     */
    public void handleToIpCallInvitation(TerminatingToIpSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event receive ToIP session invitation");
		}
		
		// Extract number from contact 
		String contact = SipUtils.extractUsernameFromAddress(session.getRemoteContact());

    	// Notify event listeners
    	Intent intent = new Intent(ToIpApiIntents.TOIP_CALL_INVITATION);
    	intent.putExtra("contact", contact);
    	intent.putExtra("sessionId", session.getSessionID());
    	getApplicationContext().sendBroadcast(intent);
    }
}
