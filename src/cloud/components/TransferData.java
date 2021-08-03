package cloud.components;

/**Only used in read DAX file, store a task's all edges in a workflow*/
public class TransferData { 
	
	private String name; //Data file name
	private long size; //Transfer data size
	private String link; //Input or output link
		
	public TransferData(String name, long size,  String link) {
		this.name = name;
		this.size = size;
		this.link = link;
	}
	
	//-------------------------------------getters&setters--------------------------------
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}
}
