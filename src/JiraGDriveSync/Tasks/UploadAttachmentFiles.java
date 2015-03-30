package JiraGDriveSync.Tasks;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import JiraGDriveSync.JiraGDriveSync;
import JiraGDriveSync.Domain.UploadFile;
import JiraGDriveSync.Utils.Logger;

/**
 * This class does upload all the files/attachments (which need to exist locally)
 * to GDrive. It does that threaded. Based on different runnings, 4 seems the
 * most appropriate, as you get (very soon) exception from the GDrive-backend
 * about "usage-limit exceeded". This does not mean, that this is an actual error
 * and the file can't be uploaded. It just means: right now, we don't allow to
 * upload the file. Try again in 1 sec. if 2 times fail: 2 sec. so 2^(n-1)
 * If we have 6 failures it should be handled as an actual error.
 * @author Tim Luginbühl
 *
 */
public class UploadAttachmentFiles extends Task{

	/**
	 * The logger for the messages
	 */
	private Logger logger = new Logger(this.getClass().getSimpleName());

	/**
	 * The amount of retries before marking an upload as failed.
	 */
	private static final int MAX_RETRIES = 6;

	/**
	 * The list of files which should be uploaded
	 */
	private List<UploadFile> uploads;

	/**
	 * The constructor just takes a list of uploads which should be done
	 * @param uploads
	 */
	public UploadAttachmentFiles(List<UploadFile> uploads){
		this.uploads = uploads;
	}

	/**
	 * This uploads all the upload-files.
	 */
	@Override
	public int doTask(){
		this.logger.info("Starting upload of attachments.");
		ExecutorService executor = Executors.newFixedThreadPool(4);
		for(UploadFile uf : this.uploads)
			executor.execute(new UploadFileThread(uf));
		executor.shutdown();
		try {
			// high timeout, as we can't be sure there will be only small files
			executor.awaitTermination(60, TimeUnit.MINUTES);
		} catch (InterruptedException e) { }
		this.logger.info("Finished upload of attachments.");
		return 0;
	}

	/**
	 * This class is for the threading of the files to upload. Each file gets
	 * an seperate file.
	 * @author Tim Luginbühl
	 *
	 */
	private class UploadFileThread implements Runnable {

		/**
		 * The file to upload
		 */
		private UploadFile file = null;

		/**
		 * The constructor just takes the file to upload as argument
		 * @param file the file to upload
		 */
		public UploadFileThread(UploadFile file){
			logger.debug("Init upload thread for file: " + file.getPath() + file.getName());
			this.file = file;
		}

		/**
		 * This method uploads the file and retries couple of times if something
		 * went wrong. Also it does wait 2^(n-1) before retrying
		 */
		@Override
		public void run() {
			int retried = 0;
			boolean success = JiraGDriveSync.gdriveClient.uploadFile(file);
			while(!success){
				retried ++;
				if(retried == MAX_RETRIES){
					logger.warn("Failed to upload file: " + file.getPath() + file.getName());
					break;
				}
				int wait = (int)Math.pow(2, retried-1);
				logger.debug("Upload failed. Tries so far: " + retried + ". (retrying in: " + wait + "s)");
				try {
					Thread.sleep(wait*1000);
				} catch (InterruptedException e) {}
				success = JiraGDriveSync.gdriveClient.uploadFile(file);
			}
			if(success)
				logger.debug("Uploaded file: " + file.getPath() + file.getName());
			this.file.getLocalFile().delete();
		}
	}
}
