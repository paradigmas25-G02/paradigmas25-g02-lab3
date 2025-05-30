package namedEntity.categories;

public class DateEntity extends Category {
    String origin;
    String canonical;

    public DateEntity(String origin, String canonical){
        super(origin);
        this.origin = origin;
        this.canonical = "";
    }

    public String getCategoryName() {
        return "Date";
    }
    public boolean isOther () {
        return false;
    }
}