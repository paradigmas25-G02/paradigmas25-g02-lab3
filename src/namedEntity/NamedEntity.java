package namedEntity;

import java.io.Serializable;
import namedEntity.categories.Category;

import namedEntity.topics.Topic;

/*Esta clase modela la nocion de entidad nombrada*/

public class NamedEntity implements Serializable {
    private static final long serialVersionUID = 1L;

	private String name;
	private int frequency;
	
	private Category category;
	private Topic topic;
	
	public NamedEntity(String name, int frequency, Category category,Topic topic) {
		this.name = name;
		this.frequency = frequency;
		this.category = category;
		this.topic = topic;
	}

	private String getTopicDescription () {
		return this.topic.getDescription();
	}

	public Topic getTopic() {
		return this.topic;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Category getCategory() {
		return this.category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public int getFrequency() {
		return this.frequency;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	public void incFrequency() {
		this.frequency++;
	}

	@Override
	public String toString() {
		return "ObjectNamedEntity [name=" + name + ", frequency=" + frequency + "]";
	}

	public void prettyPrint() {
		System.out.println(this.getName() + " is a " + this.category.getCategoryName() + " that appears " + this.getFrequency() + " times and is related to " + this.topic.getTopicName() + " specifically " + this.getTopicDescription());
	}
}
