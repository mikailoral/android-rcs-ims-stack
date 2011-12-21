package com.orangelabs.rcs.ri.capabilities;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;
import com.orangelabs.rcs.service.api.client.ClientApiException;
import com.orangelabs.rcs.service.api.client.ClientApiListener;
import com.orangelabs.rcs.service.api.client.capability.CapabilityApi;

/**
 * Refresh capabilities
 * 
 * @author jexa7410
 */
public class RefreshCapabilities extends Activity implements ClientApiListener {
    /**
     * UI handler
     */
    private Handler handler = new Handler();    
    
	/**
	 * Capability API
	 */
    private CapabilityApi capabilityApi;
	
    /**
     * Progress dialog
     */
    private Dialog progressDialog = null;    
   
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.capabilities_refresh);
        
        // Set title
        setTitle(R.string.menu_refresh_capabilities);
        
		// Set buttons callback
        Button btn = (Button)findViewById(R.id.refresh_btn);
        btn.setOnClickListener(btnSyncListener);        
        
        // Instanciate contacts API
        capabilityApi = new CapabilityApi(getApplicationContext());
    	capabilityApi.addApiEventListener(this);
    	capabilityApi.connectApi();
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
		// Disconnect contacts API
    	capabilityApi.disconnectApi();    	
    }
    
    /**
     * API disabled
     */
	public void handleApiDisabled() {
		handler.post(new Runnable() { 
			public void run() {
				Utils.showMessageAndExit(RefreshCapabilities.this, getString(R.string.label_api_disabled));
			}
		});
	}

    /**
     * API connected
     */
	public void handleApiConnected() {
	}

    /**
     * API disconnected
     */
	public void handleApiDisconnected() {
		handler.post(new Runnable() { 
			public void run() {
				Utils.showMessageAndExit(RefreshCapabilities.this, getString(R.string.label_api_failed));
			}
		});
	}
	
    /**
     * Publish button listener
     */
    private OnClickListener btnSyncListener = new OnClickListener() {
        public void onClick(View v) {
        	// Execute in background
        	final SyncTask tsk = new SyncTask(capabilityApi);
        	tsk.execute();

        	// Display a progress dialog
            progressDialog = Utils.showProgressDialog(RefreshCapabilities.this, getString(R.string.label_refresh_in_progress));
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            	public void onCancel(DialogInterface dialog) {
            		try {
            			tsk.cancel(true);
            		} catch(Exception e) {
            		}
            	}
            });
        }
    };
    
    /**
     * Background task
     */
    private class SyncTask extends AsyncTask<Void, Void, Void> {
    	private CapabilityApi api; 
    	
    	public SyncTask(CapabilityApi api) {
    		this.api = api;
    	}
    	
        protected Void doInBackground(Void... unused) {        	
        	try {
    			// Refresh all
        		api.refreshAllCapabilities();
        	} catch (ClientApiException e) {
        		// Display error
        		Utils.showMessage(RefreshCapabilities.this, getString(R.string.label_refresh_failed));
        	}
        	return null;
        }

        protected void onPostExecute(Void unused) {
			// Hide progress dialog
    		if (progressDialog != null && progressDialog.isShowing()) {
    			progressDialog.dismiss();
    			progressDialog = null;
    		}
    		
    		// Display message
			Utils.displayLongToast(RefreshCapabilities.this, getString(R.string.label_refresh_success));
        }
    }    
}
