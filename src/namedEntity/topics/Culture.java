package namedEntity.topics;

public class Culture extends Topic{
    String description;
    public static String name = "Culture";

    public Culture(String description){
        super("Culture","Culture: " + description);
        this.description = description;
    }

    public String getDescription(){
        return this.description;
    }
}