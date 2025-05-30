package namedEntity.categories;

public class Other extends Category {

    String comment;

    public Other(String comment){
        super(comment);
        this.comment = comment;
    }

    public String getCategoryName() {
        return "Other";
    }

    public boolean isOther () {
        return true;
    }
}