package JiraGDriveSync.Tasks;

/**
 * This Class is for all the tasks related for the JiraGDriveSync. Its only
 * purpose is to make it possible to threat in the same manner
 * (eg getReturnValue()).
 * @author Tim Luginbühl
 *
 */
public abstract class Task extends Thread{

	/**
	 * The returnValue of the doTask method
	 */
	private int returnValue = -1;

	/**
	 * This method should handle the main task which is run while being
	 * threaded.
	 * @return
	 */
	public abstract int doTask();

	/**
	 * Returns the return value of the doTask method
	 * @return
	 */
	public final int getReturnValue(){
		return this.returnValue;
	}

	/**
	 * This is basically a alias for doTask()
	 */
	@Override
	public final void run(){
		this.returnValue = this.doTask();
	}
}
