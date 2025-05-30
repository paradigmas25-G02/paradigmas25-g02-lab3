package namedEntity.categories.Place;

import namedEntity.categories.Category;

public class OtherPlace extends Category {

    String canonical;

    public OtherPlace(String canonical){
        super(canonical);
        this.canonical = canonical;
    }

    public String getCategoryName() {
        return "OtherPlace";
    }

    public boolean isOther () {
        return false;
    }
}