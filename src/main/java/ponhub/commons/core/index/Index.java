package ponhub.commons.core.index;

import ponhub.commons.util.ToStringBuilder;

/**
 * Represents an index whose zero-based and one-based values both fit in an {@code int}.
 * Zero-based values range from 0 to {@code Integer.MAX_VALUE - 1}; one-based values range from
 * 1 to {@code Integer.MAX_VALUE}.
 *
 * {@code Index} should be used right from the start (when parsing in a new user input), so that if the current
 * component wants to communicate with another component, it can send an {@code Index} to avoid having to know what
 * base the other component is using for its index. However, after receiving the {@code Index}, that component can
 * convert it back to an int if the index will not be passed to a different component again.
 */
public class Index {
    private final int zeroBasedIndex;

    /**
     * Index can only be created by calling {@link Index#fromZeroBased(int)} or
     * {@link Index#fromOneBased(int)}.
     */
    private Index(int zeroBasedIndex) {
        this.zeroBasedIndex = zeroBasedIndex;
    }

    public int getZeroBased() {
        return zeroBasedIndex;
    }

    public int getOneBased() {
        return zeroBasedIndex + 1;
    }

    /**
     * Creates a new {@code Index} using a zero-based index.
     *
     * @throws IndexOutOfBoundsException if {@code zeroBasedIndex} is negative or its one-based
     *     value cannot be represented as an {@code int}.
     */
    public static Index fromZeroBased(int zeroBasedIndex) {
        if (zeroBasedIndex < 0 || zeroBasedIndex == Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException();
        }

        return new Index(zeroBasedIndex);
    }

    /**
     * Creates a new {@code Index} using a one-based index.
     *
     * @throws IndexOutOfBoundsException if {@code oneBasedIndex} is not positive.
     */
    public static Index fromOneBased(int oneBasedIndex) {
        if (oneBasedIndex <= 0) {
            throw new IndexOutOfBoundsException();
        }

        return new Index(oneBasedIndex - 1);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Index otherIndex)) {
            return false;
        }

        return zeroBasedIndex == otherIndex.zeroBasedIndex;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(zeroBasedIndex);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("zeroBasedIndex", zeroBasedIndex).toString();
    }
}
