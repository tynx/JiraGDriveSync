package JiraGDriveSync.Domain;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JiraGDriveSync.JiraGDriveSync;
import JiraGDriveSync.Utils.Logger;

/**
 * This class stores all the needed attributes of an jira-issue-attachment.
 * It represents a single attachment, so if different version of a file
 * is uploaded, it is not handled in here.
 * @author Tim Luginbühl
 *
 */
public class JiraItem {

	/**
	 * This is the Attachment ID in jira.
	 */
	private String id = null;

	/**
	 * The key/name of the issue to which this attachment belongs to
	 */
	private String issueKey = null;

	/**
	 * The project key/name to the current issue (which this attachment belongs
	 * to)
	 */
	private String projectKey = null;

	/**
	 * The mimeType of the file (eg. image/jpeg)
	 */
	private String mimeType = null;

	/**
	 * This is the filename of the attachment
	 */
	private String name = null;

	/**
	 * The description, which is created in the constructor
	 */
	private String description = null;

	/**
	 * This is the actual file which is represented by this attachment
	 */
	private File tempFile = null;

	/**
	 * This is the date, the file was uploaded
	 */
	private Date created = null;

	/**
	 * This is the url, where the actual file on the Jira-API can be found
	 */
	private URL downloadURL = null;

	/**
	 * This is a flag which is irrelevant for the JiraItem itself
	 */
	private boolean found = false;

	/**
	 * The constructor populates all the attributes.
	 * @param id the id of the attachment
	 * @param issueKey the issue key
	 * @param projectKey the project key
	 * @param name the name of the file
	 * @param mimeType the mimeType of the file
	 * @param uploader the upload-user in jira
	 * @param created the date whih the attachment was created/uploaded
	 * @param downloadURL the url pointing to the actual file
	 */
	public JiraItem(
			String id,
			String issueKey,
			String projectKey,
			String name,
			String mimeType,
			String uploader,
			Date created,
			URL downloadURL
		){
		this.id = id;
		this.issueKey = issueKey;
		this.projectKey = projectKey;
		this.name = name;
		this.mimeType = mimeType;
		this.created = created;
		this.downloadURL = downloadURL;
		this.tempFile = new File(JiraGDriveSync.config.getProperty("temp_file_location") + this.id);
		this.description = JiraGDriveSync.config.getProperty("file_description");
		this.description = this.description.replace("{{date}}", this.created.toString());
		this.description = this.description.replace("{{uploader}}", uploader);
	}

	/**
	 * Returns the ID of the attachment
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the name(filename) of the attachment
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the key of the issue
	 * @return the issue key
	 */
	public String getIssueKey() {
		return issueKey;
	}

	/**
	 * Returns the key of the project
	 * @return the project key
	 */
	public String getProjectKey() {
		return projectKey;
	}

	/**
	 * The mimeType of the attachment
	 * @return the mimeType
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Returns the temporary local file of the attachment
	 * @return the tempfile
	 */
	public File getTempFile() {
		return tempFile;
	}

	/**
	 * Returns the date, when the attachment was created/uploaded
	 * @return the creation date
	 */
	public Date getCreated() {
		return created;
	}

	/**
	 * Returns the description of the attachment (based on config)
	 * @return the description
	 */
	public String getDescription(){
		return this.description;
	}

	/**
	 * Return the url of the file
	 * @return the download url
	 */
	public URL getDownloadURL() {
		return downloadURL;
	}

	/**
	 * Returns the flag "isFound"
	 * @return if isFound
	 */
	public boolean isFound(){
		return this.found;
	}

	/**
	 * Allows to mark an attachment as found. useful in duplicate detection.
	 * @param found if found or not
	 */
	public void setIsFound(boolean found){
		this.found = found;
	}

	/**
	 * This method takes an JSONObject and tries to parse it. If successful
	 * the method will return a list with all attachments of a given issue.
	 * @param issue the issue to scan attachments in it
	 * @return the list of all successful parsed attachments
	 */
	public static List<JiraItem> populate(JSONObject issue){
		Logger logger = new Logger(JiraItem.class.getSimpleName());
		List<JiraItem> items = new ArrayList<JiraItem>();

		Object obIssueKey = issue.get("key");
		Object obFields = issue.get("fields");

		if(!(obIssueKey instanceof String)||
			!(obFields instanceof JSONObject)
		){
			logger.warn("Couldn't parse issue. Expected Issue-key to be string, or fileds to be object.");
			return items;
		}

		Object obProject = ((JSONObject)obFields).get("project");
		if(!(obProject instanceof JSONObject)){
			logger.warn("Couldn't parse project. Expected Object.");
			return items;
		}
		Object obProjectKey = ((JSONObject)obProject).get("key");
		if(!(obProjectKey instanceof String)){
			logger.warn("Couldn't parse project-id! Expected String.");
			return items;
		}
		Object obAttachments = ((JSONObject)obFields).get("attachment");
		if(!(obAttachments instanceof JSONArray)){
			logger.warn("Couldn't parse attachments-id! Expected Array.");
			return items;
		}
		JSONArray attachments = (JSONArray)obAttachments;
		for(Object obAttachment : attachments){
			if(!(obAttachment instanceof JSONObject)){
				logger.warn("Couldn't parse attachment! Expected Object. Ignoring.");
				continue;
			}
			JSONObject attachment = (JSONObject)obAttachment;
			Object obId = attachment.get("id");
			Object obName = attachment.get("filename");
			Object obMimeType = attachment.get("mimeType");
			Object obCreated = attachment.get("created");
			Object obURL = attachment.get("content");
			Object obAuthor = attachment.get("author");
			
			if(!(obId instanceof String) ||
				!(obName instanceof String) ||
				!(obMimeType instanceof String) ||
				!(obCreated instanceof String) ||
				!(obURL instanceof String) ||
				!(obAuthor instanceof JSONObject)
			){
				logger.warn("Couldn't parse attachment! Required fields not provided. Ignoring.");
				continue;
			}
			Object obUploader = ((JSONObject)obAuthor).get("displayName");
			if(!(obUploader instanceof String)){
				logger.warn("Couldn't parse author! Expected String. Ignoring.");
				continue;
			}
			URL downloadURL;
			Date created;
			try {
				String pattern = "yyyy-MM-dd'T'HH:mm:ss";
				SimpleDateFormat sdf = new SimpleDateFormat(pattern);
				created = sdf.parse((String)obCreated);
				downloadURL = new URL((String)obURL);
			} catch (MalformedURLException e) {
				logger.warn("Couldn't parse URL! MalformedURLException was thrown. Ignoring.");
				continue;
			} catch (ParseException e) {
				logger.warn("Couldn't parse Date! Expected in Format: yyyy-MM-dd'T'HH:mm:ss. Ignoring.");
				continue;
			}
			items.add(new JiraItem(
					(String)obId,
					(String)obIssueKey,
					(String)obProjectKey,
					(String)obName,
					(String)obMimeType,
					(String)obUploader,
					created,
					downloadURL
				));
		}
		logger = null;
		return items;
	}


}
