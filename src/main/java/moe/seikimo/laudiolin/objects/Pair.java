package moe.seikimo.laudiolin.objects;

public record Pair<A, B>(A a, B b) {
    /**
     * Creates a new pair.
     *
     * @param a The first value.
     * @param b The second value.
     * @return A new pair.
     */
    public static <A, B> Pair<A, B> of(A a, B b) {
        return new Pair<>(a, b);
    }

    /**
     * @return The first value.
     */
    public A first() {
        return this.a;
    }

    /**
     * @return The second value.
     */
    public B second() {
        return this.b;
    }
}
