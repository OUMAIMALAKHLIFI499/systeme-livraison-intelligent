



package agents;

import java.util.LinkedHashMap;
import java.util.Map;

public class Catalog {

    public static final Map<String, Double> PRODUCTS = new LinkedHashMap<>();
    public static final Map<String, Double> QUARTIERS = new LinkedHashMap<>();

    static {
        PRODUCTS.put("Pizza Margherita", 60.0);
        PRODUCTS.put("Burger Menu", 45.0);
        PRODUCTS.put("Sushi Box", 85.0);
        PRODUCTS.put("Salade Cesar", 35.0);

        QUARTIERS.put("Founty", 10.0);
        QUARTIERS.put("Talborjt", 12.0);
        QUARTIERS.put("Charaf", 15.0);
        QUARTIERS.put("Bensergao", 15.0);
        QUARTIERS.put("Hay Mohammadi", 18.0);
        QUARTIERS.put("Dakhla", 20.0);
    }

    public static double productPrice(String p) { return PRODUCTS.getOrDefault(p, 0.0); }
    public static double deliveryPrice(String q) { return QUARTIERS.getOrDefault(q, 0.0); }
}