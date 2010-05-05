package com.orangelabs.rcs.core.ims.service.presence;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Geoloc info
 * 
 * @author jexa7410
 */
public class Geoloc implements Parcelable {	
	/**
	 * Latitude
	 */
	private double latitude;
	
	/**
	 * Longitude
	 */
	private double longitude;
	
	/**
	 * Altitude
	 */
	private double altitude;
	
	/**
	 * Constructor
	 * 
	 * @param latitude Latitude
	 * @param longitude Longitude
	 * @param altitude Altitude
	 */
	public Geoloc(double latitude, double longitude, double altitude) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
	}
	
	/**
	 * Constructor
	 * 
	 * @param source Parcelable source
	 */
	public Geoloc(Parcel source) {
		this.latitude = source.readDouble();
		this.longitude = source.readDouble();
		this.altitude = source.readDouble();
    }

	/**
	 * Describe the kinds of special objects contained in this Parcelable's
	 * marshalled representation
	 * 
	 * @return Integer
	 */
	public int describeContents() {
        return 0;
    }

	/**
	 * Write parcelable object
	 * 
	 * @param dest The Parcel in which the object should be written
	 * @param flags Additional flags about how the object should be written
	 */
    public void writeToParcel(Parcel dest, int flags) {
    	dest.writeDouble(latitude);    	
    	dest.writeDouble(longitude);    	
    	dest.writeDouble(altitude);    	
    }

    /**
     * Parcelable creator
     */
    public static final Parcelable.Creator<Geoloc> CREATOR
            = new Parcelable.Creator<Geoloc>() {
        public Geoloc createFromParcel(Parcel source) {
            return new Geoloc(source);
        }

        public Geoloc[] newArray(int size) {
            return new Geoloc[size];
        }
    };	
    
    /**
	 * Returns the latitude
	 * 
	 * @return Latitude
	 */
	public double getLatitude() {
		return latitude;
	}
	
	/**
	 * Set the latitude
	 * 
	 * @param latitude Latitude
	 */
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	
	/**
	 * Returns the longitude
	 * 
	 * @return Longitude
	 */
	public double getLongitude() {
		return longitude;
	}
	
	/**
	 * Set the longitude
	 * 
	 * @param longitude Longitude
	 */
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	/**
	 * Returns the altitude
	 * 
	 * @return Altitude
	 */
	public double getAltitude() {
		return altitude;
	}
	
	/**
	 * Set the altitude
	 * 
	 * @param altitude Altitude
	 */
	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}

	/**
	 * Returns a string representation of the object
	 * 
	 * @return String
	 */
	public String toString() {
		return "latitude=" + latitude + ", longitude=" + longitude + ", altitude=" + altitude;
	}
}
