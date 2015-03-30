package JiraGDriveSync;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import GDriveClient.GDriveClient;
import JiraClient.JiraRESTClient;
import JiraGDriveSync.Domain.GDriveItem;
import JiraGDriveSync.Domain.JiraItem;
import JiraGDriveSync.Domain.SyncItem;
import JiraGDriveSync.Domain.UploadFile;
import JiraGDriveSync.Tasks.CreateFolderStructure;
import JiraGDriveSync.Tasks.DeleteOldItems;
import JiraGDriveSync.Tasks.DownloadAttachmentFiles;
import JiraGDriveSync.Tasks.DownloadGDriveItemList;
import JiraGDriveSync.Tasks.DownloadJiraItemList;
import JiraGDriveSync.Tasks.Task;
import JiraGDriveSync.Tasks.UploadAttachmentFiles;
import JiraGDriveSync.Utils.Logger;

/**
 * This class is the main wrapper for the whole sync process.
 * @author tim
 *
 */
public class JiraGDriveSync {

	/**
	 * public instance of the config, so the whole application as a simple
	 * access
	 */
	public static Properties config = null;

	/**
	 * We don't want multiple instances of the clients and singleton didn't seem
	 * the right thing to do: so globaly accessible.
	 */
	public static JiraRESTClient jiraClient = null;

	/**
	 * We don't want multiple instances of the clients and singleton didn't seem
	 * the right thing to do: so globaly accessible.
	 */
	public static GDriveClient gdriveClient = null;

	/**
	 * The logger for the messages
	 */
	private Logger logger = new Logger(this.getClass().getSimpleName());

	/**
	 * This method does the whole syncing. Firstly it downloads both list (from
	 * jira and gdrive) in parallel. Then it checks which folders need to be
	 * created in gdrive. It checks which folders and files to delete. And it
	 * downloads all needed files from Jira. these 3 tasks (delete, creation,
	 * download) are processed in parallel. Lastly it uploads all the downloaded
	 * attachments to GDrive.
	 */
	public void sync(){
		long start = (new Date()).getTime();
		this.logger.info("Starting syncing.");
		DownloadJiraItemList jList = new DownloadJiraItemList();
		DownloadGDriveItemList gList = new DownloadGDriveItemList();
		jList.start();
		gList.start();
		this.waitForTasks(new Task[]{jList, gList});
		
		if(jList.getReturnValue()!= 0){
			this.logger.err("Fetching Jira list failed. Exiting.");
			System.exit(-1);
		}
		if(gList.getReturnValue() != 0){
			this.logger.err("Fetching GDrive list failed. Exiting.");
			System.exit(-1);
		}

		List<UploadFile> toUpload = new ArrayList<UploadFile>();
		List<SyncItem> toDelete = new ArrayList<SyncItem>();
		List<SyncItem> syncItems = new ArrayList<SyncItem>();

		// Check which files are already present in GDrive
		// => to be deleted and uploaded can be determined
		for(GDriveItem gItem : gList.getItems()){
			SyncItem item = new SyncItem();
			item.setGItem(gItem);
			for(JiraItem jItem : jList.getItems()){
				String path = JiraGDriveSync.config.getProperty("google_drive_location") + jItem.getProjectKey() + "/" + jItem.getIssueKey() + "/";
				if(gItem.getName().equals(jItem.getName()) &&
						gItem.getPath().equals(path)
					){
					item.setJItem(jItem);
					jItem.setIsFound(true);
				}
			}
			if(item.getJItem() == null)
				toDelete.add(item);
			else{
				if(item.getJItem().getCreated().after(item.getGItem().getModified()))
					toUpload.add(new UploadFile(item.getJItem()));
			}
			syncItems.add(item);
		}

		// Checks which folders need to be created in GDrive
		List<String> pathsToCreate = new ArrayList<String>();
		for(JiraItem jItem : jList.getItems()){
			if(!jItem.isFound()){
				UploadFile uf = new UploadFile(jItem);
				if(!pathsToCreate.contains(uf.getPath()))
					pathsToCreate.add(uf.getPath());
				toUpload.add(uf);
			}
		}

		Task delete = new DeleteOldItems(toDelete);
		Task create = new CreateFolderStructure(pathsToCreate);
		Task download = new DownloadAttachmentFiles(toUpload);

		download.start();
		create.start();
		delete.start();
		this.waitForTasks(new Task[]{download, create, delete});
		if(download.getReturnValue()!= 0){
			this.logger.err("Download of Attachments faild. Exiting.");
			System.exit(-1);
		}
		if(create.getReturnValue()!= 0){
			this.logger.err("Creating of folder structure failed. Exiting.");
			System.exit(-1);
		}
		if(delete.getReturnValue()!= 0){
			this.logger.err("Deleting failed. Exiting.");
			System.exit(-1);
		}
		this.logger.info("Create folder, deletion and download complete.");

		// upload the new attachments.
		Task upload = new UploadAttachmentFiles(toUpload);
		upload.start();

		this.waitForTasks(new Task[]{upload});
		this.logger.info("Upload complete.");
		long diffSeconds = ((new Date()).getTime()-start) / 1000;
		this.logger.info("Syncing finished in " + diffSeconds + " seconds. Exiting.");
	}

	/**
	 * This methods awaits the termination of all provided tasks.
	 * @param tasks
	 */
	private void waitForTasks(Task[] tasks){
		for(Task task : tasks){
			while(task.isAlive()){
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) { }
			}
		}
	}
}
