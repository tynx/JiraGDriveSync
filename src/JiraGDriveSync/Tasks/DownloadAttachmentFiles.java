package JiraGDriveSync.Tasks;

import java.util.List;

import JiraGDriveSync.JiraGDriveSync;
import JiraGDriveSync.Domain.UploadFile;
import JiraGDriveSync.Utils.Logger;

/**
 * This class downloads all needed attachments and storing them locally in a
 * temporary file. It does so threaded, to improve performance as RTT is a real
 * issue with the Jira-API
 * @author Tim Luginbühl
 *
 */
public class DownloadAttachmentFiles extends Task {

	/**
	 * The logger, for the messages
	 */
	private Logger logger = new Logger(this.getClass().getSimpleName());

	/**
	 * The list of files to download
	 */
	private List<UploadFile> downloads;

	/**
	 * The constructor just takes the list of files which should be downloaded
	 * @param downloads the attachments/files to download
	 */
	public DownloadAttachmentFiles(List<UploadFile> downloads){
		this.downloads = downloads;
	}

	/**
	 * Via the JiraRESTClient-class we download the files in a threaded manner.
	 * @return 0 if successful or <0 if something went wrong
	 */
	public int doTask(){
		this.logger.info("Downloading " + this.downloads.size() + " attachments.");
		for(UploadFile download : this.downloads)
			JiraGDriveSync.jiraClient.registerRequest(
					download.getDownloadURL(),
					download.getLocalFile()
				);
		this.logger.info("Downloading of attachments completed.");
		if(JiraGDriveSync.jiraClient.sendRegisteredRequests() != 0)
			return -1;
		return 0;
	}
}
