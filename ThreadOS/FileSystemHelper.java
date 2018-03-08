public class FileSystemHelper {

    public static final int INDOES_PER_BLOCK = 16;
    public static final int INODE_BYTE_SIZE = 32;
    public static final int SHORT_BYTE_SIZE = 2;

    public static int calculateBlockNumber(int iNumber) {
        return iNumber / INDOES_PER_BLOCK + 1;
    }

    public static int calculateInodeId(int iNumber) {
        return iNumber % INDOES_PER_BLOCK;
    }

    public static int calculateOffset(int iNodeId) {
        return iNodeId * INODE_BYTE_SIZE;
    }
}
