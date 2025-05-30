package namedEntity.categories;

public abstract class Category {

    private String atom;

    public Category(String embryo){
        this.atom = embryo;
    }

    public abstract String getCategoryName();

    public abstract boolean isOther();
}