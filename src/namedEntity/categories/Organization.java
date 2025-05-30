package namedEntity.categories;
import namedEntity.NamedEntity;
import namedEntity.topics.Topic;

public class Organization extends Category {
    String canonical;
    int members;
    String type;

    // aca va un mapa de organizaciones con sus datos

    public Organization(String canonical) {
        super(canonical);
        this.canonical = canonical;

        // enrealidad aca hay que mapear para setear la cantidad de miembros de las organizaciones conocidas
        this.members = 0;
        this.type = "Generic";
    }

    public String getCategoryName() {
        return "Organization";
    }

    public boolean isOther () {
        return false;
    }
}