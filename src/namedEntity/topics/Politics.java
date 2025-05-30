package namedEntity.topics;

public class Politics extends Topic {
    String description;

    public Politics(String description) {
        super("Politics","Politics: " +description);
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }
}
