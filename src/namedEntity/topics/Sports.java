package namedEntity.topics;

public class Sports extends Topic{
    String description;
    
    public Sports(String description){
        super("Sports","Sports: "+description);
        this.description = description;
    }

    public String getDescription(){
        return this.description;
    }
}