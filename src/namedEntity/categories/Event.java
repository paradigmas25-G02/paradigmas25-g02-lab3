package namedEntity.categories;
import java.util.Random;

public class Event extends Category {
    String canonical;
    String date;
    boolean recurrente;
    
    // aca va un mapa con la fecha de eventos importantes

    private static Random random = new Random();

    public Event(String origin){
        super(origin);
        
        this.date = "";
        this.canonical = "";
        this.recurrente = random.nextBoolean();
    }

    public String getCategoryName() {
        return "Event";
    }

    public boolean isOther () {
        return false;
    }
}