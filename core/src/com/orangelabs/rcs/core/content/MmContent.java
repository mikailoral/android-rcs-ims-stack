package com.orangelabs.rcs.core.content;


/**
 * Multimedia content
 * 
 * @author jexa7410
 */
public abstract class MmContent {
	/**
	 * Content URL
	 */
	private String url;
	
	/**
	 * Content size in bytes
	 */
	private long size;
	
	/**
	 * Encoding
	 */
	private String encoding;

	/**
	 * Data
	 */
	private byte[] data = null;

	/**
	 * Content name
	 */
	private String name = null;

	/**
	 * Constructor
	 * 
	 * @param url URL
	 * @param encoding Encoding
	 */
	public MmContent(String url, String encoding) {
		this.url = url;
		this.encoding = encoding;
		this.size = -1;
	}
	
	/**
	 * Constructor
	 * 
	 * @param url URL
	 * @param encoding Encoding
	 * @param size Content size
	 */
	public MmContent(String url, String encoding, long size) {
		this.url = url;
		this.encoding = encoding;
		this.size = size;
	}

	/**
	 * Returns the URL
	 * 
	 * @return String
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Returns the content size in bytes
	 * 
	 * @return Size in bytes
	 */
	public long getSize() {
		return size;
	}

	/**
	 * Returns the content size in Kbytes
	 * 
	 * @return Size in Kbytes
	 */
	public long getKbSize() {
		return size/1024;
	}
	
	/**
	 * Returns the content size in Mbytes
	 * 
	 * @return Size in Mbytes
	 */
	public long getMbSize(){
		return size/(1024*1024);
	}

	/**
	 * Returns the encoding type
	 * 
	 * @return Encoding type
	 */
	public String getEncoding() {
		return encoding;
	}
	
	/**
	 * Returns the codec from the encoding type
	 *  
	 * @return Codec name
	 */
	public String getCodec() {
		int index = encoding.indexOf("/");
		if (index != -1) {
			return encoding.substring(index+1);
		} else {
			return encoding;
		}
	}	
	
	/**
	 * Get the name
	 * 
	 * @return Name
	 */
	public String getName() {
		if (name == null) {
			// Extract filename from URL
			int index = url.lastIndexOf('/');
			if (index != -1) {
				return url.substring(index+1);
			} else {
				return url;
			}
		} else {
			// Return the name associated to the content
			return name;
		}
	}		

	/**
	 * Returns the string representtaion of a content
	 * 
	 * @return String
	 */
	public String toString() {
		return url + " (" + size + " bytes)";
	}
	
	/**
	 * Returns the content data
	 * 
	 * @return Data
	 */
	public byte[] getData() {
		return data;
	}
	
	/**
	 * Sets the content data
	 * 
	 * @param Data
	 */
	public void setData(byte[] data) {
		this.data = data;
	}
}
