/**
 * After the {@link Superblock} are the Inode blocks. Each Inode describes one file.
 * This Inode is a simplified version of a Unix Inode. It includes 12 pointers of the
 * index block. The first of these 11 pointers point to direct blocks. The last pointer
 * points to an indirect block. 16 Inodes can be stored in one block.
 *
 * Each Inode includes:
 *      - the length of the corresponding file
 *      - the number of file (structure) table entries that point to this Inode
 *      - the flag to indicate if it is used, unused, or in some other status
 */
public class Inode {
    private final static int iNodeSize = 32;        /* fix to 32 bytes */
    private final static int directSize = 11;       /* # direct pointers */
    private static final int INDOES_PER_BLOCK = 16;
    private static final int INODE_BYTE_SIZE = 32;

    public int length;                             /* file size in bytes */
    public short count;                            /* # file-table entries pointing to this */
    public short flag;                             /* 0 = unused, 1 = used, ... */
    public short direct[] = new short[directSize]; /* direct pointers */
    public short indirect;                         /* a indirect pointer */

    /**
     * Enum for the flag data member.
     */
    public enum Status {
        UNUSED(0),
        USED(1),
        READ(2),
        WRITE(3),
        DELETE(4);

        private final int status;

        public short getValue() {
            return (short) this.status;
        }

        private Status(int status) {
            this.status = status;
        }
    }

    Inode( ) {                                     // a default constructor
        this.length = 0;
        this.count = 0;
        this.flag = Status.USED.getValue();

        for (int index = 0; index < directSize; index++) {
            direct[index] = -1;
        }
        indirect = -1;
    }

    /**
     * Reads the disk block corresponding to iNumber, locates the corresponding
     * Inode information in that block, and initializes a new Inode with that
     * information.
     *
     * @param iNumber disk block number to read
     */
    Inode(short iNumber) {
        int blockNumber = calculateBlockNumber(iNumber);
        int iNodeId = calculateInodeId(iNumber);
        int offset = calculateOffset(iNodeId);

        byte[] buffer = new byte[Disk.blockSize];
        SysLib.rawread(blockNumber, buffer);
    }

    /**
     * Saves this to disk as the i-th Inode.
     *
     * @param iNumber disk block number to save to
     */
    int toDisk( short iNumber ) {
        // design it by yourself.
    }

    public int getBlockID() {

    }

    private static int calculateBlockNumber(int iNumber) {
        return iNumber / INDOES_PER_BLOCK + 1;
    }

    private static int calculateInodeId(int iNumber) {
        return iNumber % INDOES_PER_BLOCK;
    }

    private static int calculateOffset(int iNodeId) {
        return iNodeId * INODE_BYTE_SIZE;
    }
}
