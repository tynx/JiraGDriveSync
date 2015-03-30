package JiraGDriveSync.Tasks;

import java.util.ArrayList;
import java.util.List;

import com.google.api.services.drive.model.File;

import JiraGDriveSync.JiraGDriveSync;
import JiraGDriveSync.Domain.SyncItem;
import JiraGDriveSync.Utils.Logger;

/**
 * This class is responsible for cleaning up old/unused items in the GDrive
 * folder. First it deletes all attachments, which no longer exist in jira,
 * then it finds all empty issue-folders and deletes them. after that
 * it does check all projects and checks if there is a project without an issue
 * and if so deletes that folder as well.
 * @author Tim Luginbühl
 *
 */
public class DeleteOldItems extends Task {

	/**
	 * The logger for the messages
	 */
	private Logger logger = new Logger(this.getClass().getSimpleName());

	/**
	 * This is the list of items/attachments which should be deleted
	 */
	private List<SyncItem> items = null;

	/**
	 * Teh constructor just takes the list of items, which should be deleted.
	 * @param items the items do be deleted
	 */
	public DeleteOldItems(List<SyncItem> items){
		this.items = items;
	}

	/**
	 * Deletes all the items and additionally all empty folders.
	 * @return 0 if successful and <0 if something went wrong
	 */
	public int doTask(){
		this.logger.info("Starting to delete all the old items.");
		List<String> pathsToCheck = new ArrayList<String>();
		for(SyncItem i : this.items){
			if(i.getJItem() == null){
				JiraGDriveSync.gdriveClient.deleteFile(i.getGItem().getId());
				pathsToCheck.add(i.getGItem().getPath());
			}else{
				if(pathsToCheck.contains(i.getGItem().getPath())){
					pathsToCheck.remove(i.getGItem().getPath());
				}
			}
		}
		boolean checkProjectFolders = false;
		// We wanna check if any empty issue-folders where created by previous
		// deletion of attachments/files.
		JiraGDriveSync.gdriveClient.clearCache();
		String base = JiraGDriveSync.config.getProperty("google_drive_location");
		for(String path : pathsToCheck){
			if(JiraGDriveSync.gdriveClient.getFilesByPath(base + path).size() == 0){
				File folder = JiraGDriveSync.gdriveClient.getFolderByPath(base + path);
				JiraGDriveSync.gdriveClient.deleteFile(folder.getId());
				checkProjectFolders = true;
			}
		}
		// Check if there are any projects without any issue in it
		// only do it, if we actually deleted an issue-folder
		if(checkProjectFolders){
			List<File> projects = JiraGDriveSync.gdriveClient.getFilesByPath(base);
			for(File project : projects){
				if(JiraGDriveSync.gdriveClient.getFilesByPath(base + project.getTitle()).size() == 0){
					JiraGDriveSync.gdriveClient.deleteFile(project.getId());
				}
			}
		}
		this.logger.info("Deleted all the old items.");
		return 0;
	}
}
