/**
 * Created by Almaz on 22.04.17.
 */
public class Pair<First, Second> {

    private First first;
    private Second second;

    Pair(First first, Second second) {
        this.first = first;
        this.second = second;
    }

    public First getFirst() {
        return first;
    }

    public void setFirst(First first) {
        this.first = first;
    }

    public Second getSecond() {
        return second;
    }

    public void setSecond(Second second) {
        this.second = second;
    }
}