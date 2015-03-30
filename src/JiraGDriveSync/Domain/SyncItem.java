package JiraGDriveSync.Domain;

/**
 * This class is for binding a JiraItem with a GDriveItem. That's all.
 * @author Tim Luginbühl
 *
 */
public class SyncItem {

	/**
	 * The GDriveItem we want to bind
	 */
	private GDriveItem gItem = null;

	/**
	 * The JiraItem we want to bind
	 */
	private JiraItem jItem = null;

	/**
	 * Returns the JiraItem which was previousely set
	 * @return the JiraItem
	 */
	public JiraItem getJItem(){
		return this.jItem;
	}

	/**
	 * Returns the GDriveItem which was previousely set
	 * @return the GDriveItem
	 */
	public GDriveItem getGItem(){
		return this.gItem;
	}

	/**
	 * Set a JiraItem
	 * @param jItem the JiraItem to set
	 */
	public void setJItem(JiraItem jItem){
		this.jItem = jItem;
	}

	/**
	 * Set a GDriveItem
	 * @param gItem the GDriveItem to set
	 */
	public void setGItem(GDriveItem gItem){
		this.gItem = gItem;
	}
}
