package namedEntity.categories.Place;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class City {

    String canonical;
    String pais;
    String capital;
    int poblacion;

    public City(String canonical) {
        this.canonical = canonical;
        this.poblacion = 100000 * canonical.length(); // xd
        this.pais = getCountryAndCapital(canonical).get(0);
        this.capital = getCountryAndCapital(canonical).get(1);
    }

    // Static map: city name -> [country, capital]
    private static final Map<String, List<String>> cityTable = new HashMap<>();

    static {
        cityTable.put("London", Arrays.asList("United Kingdom", "London"));
        cityTable.put("Paris", Arrays.asList("France", "Paris"));
        cityTable.put("Berlin", Arrays.asList("Germany", "Berlin"));
        cityTable.put("Tokyo", Arrays.asList("Japan", "Tokyo"));
        cityTable.put("Beijing", Arrays.asList("China", "Beijing"));
        cityTable.put("Delhi", Arrays.asList("India", "New Delhi"));
        cityTable.put("Moscow", Arrays.asList("Russia", "Moscow"));
        cityTable.put("Toronto", Arrays.asList("Canada", "Ottawa"));

        // All US cities share the same country and capital
        List<String> usa = Arrays.asList("USA", "Washington, D.C.");
        cityTable.put("NYC", usa);
        cityTable.put("SF", usa);
        cityTable.put("LA", usa);
        cityTable.put("Chicago", usa);
        cityTable.put("Boston", usa);
        cityTable.put("Austin", usa);
        cityTable.put("Seattle", usa);
    }

    public static List<String> getCountryAndCapital(String city) {
        List<String> res = cityTable.get(city);
        if (res == null){
            return Arrays.asList("","");
        }
        return res;
    }

}