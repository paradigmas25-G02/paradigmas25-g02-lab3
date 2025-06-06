package namedEntity.categories;

import java.io.Serializable;

public abstract class Category implements Serializable {
    private static final long serialVersionUID = 1L;

    private String atom;

    public Category(String embryo){
        this.atom = embryo;
    }

    public abstract String getCategoryName();

    public abstract boolean isOther();
}