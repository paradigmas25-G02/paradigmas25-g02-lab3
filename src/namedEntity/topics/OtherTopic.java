package namedEntity.topics;

public class OtherTopic extends Topic{
    String description;
    
    public OtherTopic(String description){
        super("OtherTopic","OtherTopic" + description);
        this.description = description;
    }

    public String getDescription(){
        return this.description;
    }
}