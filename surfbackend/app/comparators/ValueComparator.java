package comparators;

import java.util.Comparator;
import java.util.Map;

public class ValueComparator implements Comparator<String> {

    Map<String, Integer> base;

    public ValueComparator(Map<String, Integer> base) {
        this.base = base;
    }

    @Override
    public int compare(String a, String b) {
    	Integer x = base.get(a);
        Integer y = base.get(b);
        if (x.equals(y)) {
            return a.compareTo(b);
        }
        return x.compareTo(y);
    }
}



