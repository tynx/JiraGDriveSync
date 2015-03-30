package JiraGDriveSync.Domain;

import java.util.Date;

import com.google.api.services.drive.model.File;

/**
 * This is a simplified version with only the necessary attributes of the File
 * provided by the GoogleDrive-API. Also an important thing is the "path", which
 * allows to interact with it, as with a file.
 * @author Tim Luginbühl
 *
 */
public class GDriveItem {

	/**
	 * The GDrive ID
	 */
	private String id = null;

	/**
	 * The name of the Item
	 */
	private String name = null;

	/**
	 * The path of the object. in case of a folder combine path + name!
	 */
	private String path = null;

	/**
	 * The modified date in the GDrive
	 */
	private Date modified = null;

	/**
	 * The constructor just sets the basic attributes
	 * @param id
	 * @param name
	 * @param path
	 * @param modified
	 */
	public GDriveItem(String id, String name, String path, Date modified){
		this.id = id;
		this.name = name;
		this.path = path;
		this.modified = modified;
	}

	/**
	 * Returns the id of the google-drive-file
	 * @return the id
	 */
	public String getId(){
		return this.id;
	}

	/**
	 * Returns the filename of the gdrive-file
	 * @return the name
	 */
	public String getName(){
		return this.name;
	}

	/**
	 * Returns the path of this gdrive-file
	 * @return the path
	 */
	public String getPath(){
		return this.path;
	}

	/**
	 * Returns the modified date of the gdrive-file
	 * @return the modified-date
	 */
	public Date getModified(){
		return this.modified;
	}

	/**
	 * This allows an easy way to populate a GDriveItem when having an actual
	 * File (of the Google-API).
	 * @param file the object of the Google-API
	 * @param path the path in which this file is stored
	 * @return the populated GDriveItem
	 */
	public static GDriveItem populate(File file, String path){
		GDriveItem item = new GDriveItem(
				file.getId(),
				file.getTitle(),
				path,
				new Date(file.getModifiedDate().getValue())
			);
		return item;
	}
}
