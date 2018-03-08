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

    public int length;                             /* file size in bytes */
    public short count;                            /* # file-table entries pointing to this */
    public short flag;                             /* 0 = unused, 1 = used, ... */
    public short direct[] = new short[directSize]; /* direct pointers */
    public short indirect;                         /* indirect pointer */

    Inode( ) {                                     // a default constructor
        this.length = 0;
        this.count = 0;
        this.flag = Flag.USED.getValue();

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
        int blockNumber = FileSystemHelper.calculateBlockNumber(iNumber);
        int iNodeId = FileSystemHelper.calculateInodeId(iNumber);
        int offset = FileSystemHelper.calculateOffset(iNodeId);

        byte[] buffer = new byte[Disk.blockSize];
        SysLib.rawread(blockNumber, buffer);
    }

    /**
     * Saves this to disk as the i-th Inode.
     *
     * @param iNumber disk block number to save to
     */
    public int toDisk(short iNumber) {
        // design it by yourself.
    }

    /**
     * Gets the block Id of the Inode.
     */
    public int getBlockID() {
        return FileSystemHelper.calculateBlockNumber(this.iNu)
    }

}
