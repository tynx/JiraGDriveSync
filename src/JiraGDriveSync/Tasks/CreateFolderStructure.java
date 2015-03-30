package JiraGDriveSync.Tasks;

import java.util.List;

import JiraGDriveSync.Utils.Logger;

/**
 * This class creates the basic folder structure in the GDrive so later on
 * the files can be uploaded with the according folder-structure as parents.
 * @author Tim Luginbühl
 *
 */
public class CreateFolderStructure extends Task{

	/**
	 * Logger for the log-messages
	 */
	private Logger logger = new Logger(this.getClass().getSimpleName());

	/**
	 * The paths which need to be created. These may contain full path (not
	 * incremental).
	 */
	private List<String> paths = null;

	/**
	 * The constructor just takes the paths and stores them internally.
	 * @param paths
	 */
	public CreateFolderStructure(List<String> paths){
		this.paths = paths;
	}

	/**
	 * This method creates the folder structure. It doesn't do this in parallel
	 * as this would require a lot of synchronization and the cache in GDrive
	 * would need additional code to provide a way to do that.
	 * And as all the folders don't change that often, it is only slow when
	 * the initial sync is called. after that, it is pretty fast.
	 * @return 0 if successful or <0 if something went wrong
	 */
	@Override
	public int doTask() {
		this.logger.info("Creating basic folder structure");
		int returnValue = 0;
		for(String path : this.paths)
			if(!JiraGDriveSync.JiraGDriveSync.gdriveClient.createFolderRecursive(path)){
				this.logger.warn("Couldn't create folder at path: " + path);
				returnValue = -1;
			}
		this.logger.info("Finished creating basic folder structure.");
		return returnValue;
	}
}
