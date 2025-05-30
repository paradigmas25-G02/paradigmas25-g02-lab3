package namedEntity.topics;

public class Topic {
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
