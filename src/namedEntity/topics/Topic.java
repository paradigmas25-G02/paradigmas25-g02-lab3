package namedEntity.topics;

import java.io.Serializable;

public class Topic implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String topic;
    private String description;

    public Topic(String topic, String description) {
        this.topic = topic;
        this.description = description;
    }

    public String getTopicName(){
        return this.topic;
    }
    public void setTopic(String topic) {
        this.topic = topic;
    }


    public String getDescription() {
        return this.description;
    }
    // public Topic(Sports t) {
    //     topic = t;
    // } ....
}
