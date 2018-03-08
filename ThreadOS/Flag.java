public enum Flag {
    UNUSED(0),
    USED(1),
    READ(2),
    WRITE(3),
    DELETE(4);

    private final int status;

    public short getValue() {
        return (short) this.status;
    }

    private Flag(int status) {
        this.status = status;
    }
}
