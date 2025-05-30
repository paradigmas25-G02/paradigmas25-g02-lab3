package namedEntity.categories.Person;

public class Title {

    String canonical;
    String origen;
    boolean profesional;

    //aca va un mapa que dado un titulo canonc

    public Title(String origen, String canonical){
        this.canonical = canonical;
        this.origen = origen;
        this.profesional = true;
    }

}