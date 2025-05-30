package namedEntity.categories.Place;
import namedEntity.categories.Category;

public class Place extends Category {

    String canonical;
    Address address;
    City city;
    Country country;

    public Place(String canonical, Address address, City city, Country country) {
        super(canonical);
        this.canonical = canonical;
        this.address = address;
        this.city = city;
        this.country = country;
    }

    public String getCategoryName() {
        return "Place";
    }

    public boolean isOther () {
        return false;
    }
}
