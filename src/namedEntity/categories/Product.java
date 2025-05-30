package namedEntity.categories;

public class Product extends Category {
    String prod;
    Boolean comer;
    public Product(String productito){
    
        super(productito);
        this.prod = productito;
        this.comer = true;

    }

    public String getCategoryName() {
        return "Product";
    }

    public boolean isOther () {
        return false;
    }
}