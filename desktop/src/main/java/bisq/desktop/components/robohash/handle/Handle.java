package bisq.desktop.components.robohash.handle;


public final class Handle {
    private final long value;

    Handle(long v) {
        this.value = v;
    }

    public long pack() {
        return value;
    }

    public int getAt(byte index) {
        return HandleFactory.getNibbleAt(this.value, index);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public String toString() {
        return String.format("%016X", value);
    }

    @Override
    public int hashCode() {
        return (int) value;
    }

    public byte[] bucketValues() {
        return HandleFactory.bucketValues(this.value);
    }
}
