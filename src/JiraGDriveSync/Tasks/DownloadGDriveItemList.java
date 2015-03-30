package JiraGDriveSync.Tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.api.services.drive.model.File;

import JiraGDriveSync.JiraGDriveSync;
import JiraGDriveSync.Domain.GDriveItem;
import JiraGDriveSync.Utils.Logger;

/**
 * This class is for creating a list of all files of a given path. As the
 * RTT of the GDrive is _really_ bad, we do thread the requests (and cache as
 * much as possible). It turns out the performance keeps improving every time
 * the thread-count is increased. Currently it is configured to 32 threads.
 * Which is a _lot_, decrease if you experience problems.
 * Also keep in mind, that it may be, only after a certain recursion you can
 * have 32 threads running. for the first round you only have one thread, which
 * then adds more thread for each folder it finds.
 * @author Tim Luginbühl
 *
 */
public class DownloadGDriveItemList extends Task {

	/**
	 * The logger, for the messages
	 */
	private Logger logger = new Logger(this.getClass().getSimpleName());

	/**
	 * The items which were downloaded (the final list of files)
	 */
	private List<GDriveItem> items = new ArrayList<GDriveItem>();

	/**
	 * The executor for the threading
	 */
	private ExecutorService executor = Executors.newFixedThreadPool(32);

	/**
	 * Returns the fetched files in a list.
	 * @return the list of fetched files
	 */
	public List<GDriveItem> getItems(){
		return this.items;
	}

	/**
	 * This method starts with the root folder and waits for the executor to
	 * finish. It is blocking, so this method will only return, after finishing
	 * all the requests.
	 */
	@Override
	public int doTask() {
		this.logger.info("Recursively going trough folders in gdrive.");
		this.executor.submit(new GDriveFetch(JiraGDriveSync.config.getProperty("google_drive_location")));
		try {
			while(((ThreadPoolExecutor)this.executor).getActiveCount() > 0)
				Thread.sleep(20);
			this.executor.shutdown();
			this.executor.awaitTermination(5, TimeUnit.MINUTES);
		} catch (InterruptedException e) { }
		this.logger.info("Finished fetching g-drive list.");
		return 0;
	}

	/**
	 * This class is a runnable for fetching a single item from the GDrive list.
	 * It does however automatically (recursively) start other threads to fetch
	 * all the children of the provided item.
	 * @author Tim Luginbühl
	 *
	 */
	private class GDriveFetch implements Runnable {

		/**
		 * The path to be fetch within this thread
		 */
		private String path = null;

		/**
		 * The constructor just stores the path
		 * @param path the path to fetch
		 */
		public GDriveFetch(String path){
			logger.debug("Init fetch thread for path: " + path);
			this.path = path;
		}

		/**
		 * This method downloads the list of the children of the given path.
		 * if any folders are found within the list of children, it will add
		 * a new runnable to the executor. if the found child is a file, it
		 * will add this file to the final (returnable) list.
		 */
		@Override
		public void run() {
			logger.debug("Fetching path: " + path);
			List<File> files = JiraGDriveSync.gdriveClient.getFilesByPath(path);
			for(File f : files){
				if(f.getMimeType().equals("application/vnd.google-apps.folder")){
					executor.submit(new GDriveFetch(path + f.getTitle() + "/"));
				}else{
					items.add(GDriveItem.populate(f, path));
				}
			}
		}
	}
}
