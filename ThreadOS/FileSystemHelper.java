public class FileSystemHelper {

    public static final int INODES_PER_BLOCK = 16;
    public static final int INODE_BYTE_SIZE = 32;
    public static final int SHORT_BYTE_SIZE = 2;
    public static final int INT_BYT_SIZE = 4;
    public final static int iNodeSize = 32;        /* fix to 32 bytes */
    public final static int directSize = 11;       /* # direct pointers */
    public final static int FREE = -1;
    public final static int INVALID = -1;
    public final static int TOTAL_POINTERS = 256;
    public final static int NOT_ALLOCATED = 0;
    public final static int BEGINNING_OF_FILE = 0;

    public static int calculateBlockNumber(int iNumber) {
        return (iNumber / INODES_PER_BLOCK) + 1;
    }

    public static int calculateInodeId(int iNumber) {
        return iNumber % INODES_PER_BLOCK;
    }

    public static int calculateOffset(int iNodeId) {
        return (iNodeId % INODES_PER_BLOCK) * INODE_BYTE_SIZE;
    }
}
