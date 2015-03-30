package JiraGDriveSync.Domain;

import java.io.File;
import java.net.URL;
import java.util.Date;

import JiraGDriveSync.JiraGDriveSync;;

public class UploadFile {

	/**
	 * The name of the file (filename)
	 */
	private String name = null;

	/**
	 * The path (remote) of the file
	 */
	private String path = null;

	/**
	 * The mimeType of the file
	 */
	private String mimeType = null;

	/**
	 * The description of the file
	 */
	private String description = null;

	/**
	 * The URL to download the file from
	 */
	private URL downloadURL = null;

	/**
	 * The downloaded file of this file
	 */
	private File localFile = null;

	/**
	 * The date which this file was modified in _JIRA_
	 */
	private Date modifiedDate = null;

	/**
	 * The constructor just populates all the necessary attributes with the
	 * information provided in the JiraItem
	 * @param item the item to fetch the information from
	 */
	public UploadFile(JiraItem item){
		this.name = item.getName();
		this.path = "/" + item.getProjectKey() + "/" + item.getIssueKey() + "/";
		this.mimeType = item.getMimeType();
		this.description = item.getDescription();
		this.downloadURL = item.getDownloadURL();
		this.localFile = item.getTempFile();
		this.modifiedDate = item.getCreated();
	}

	/**
	 * This retuns the name of the file (filename)
	 * @return the name
	 */
	public String getName(){
		return this.name;
	}

	/**
	 * Returns the path in GDrive
	 * @return the path
	 */
	public String getPath(){
		return JiraGDriveSync.config.getProperty("google_drive_location") + this.path;
	}

	/**
	 * Returns the mimeType of the file
	 * @return the mimeType
	 */
	public String getMimeType(){
		return this.mimeType;
	}

	/**
	 * Returns the description of the file
	 * @return the description
	 */
	public String getDescription(){
		return this.description;
	}

	/**
	 * Returns the date modified in _JIRA_
	 * @return the modifiedDate
	 */
	public Date getModifiedDate(){
		return this.modifiedDate;
	}

	/**
	 * Returns the local temporary file
	 * @return the localFile
	 */
	public File getLocalFile(){
		return this.localFile;
	}

	/**
	 * The Download URL for downloading the temporary/local file
	 * @return the download URL
	 */
	public URL getDownloadURL() {
		return this.downloadURL;
	}
}
