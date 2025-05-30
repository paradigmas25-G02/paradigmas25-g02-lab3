package namedEntity.categories.Person;
import namedEntity.categories.Category;

public class Person extends Category {
    static int c = 0;
    private LastName lastName;
    private Name name;
    private Title title;
    int id;

    public Person(LastName lastName, Name name, Title title) {
        super("Person");
        c++;
        this.id = c;
        this.lastName = lastName;
        this.name = name;
        this.title = title;
    }

    public String getCategoryName() {
        return "Person";
    }

    public boolean isOther () {
        return false;
    }
}
