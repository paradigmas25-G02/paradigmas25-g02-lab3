package namedEntity.categories.Place;

public class Country {

    String canonical;
    int poblacion;

    public Country(String canonical){
        this.canonical = canonical;
        this.poblacion = 100000 * canonical.length(); // xd
    }
}