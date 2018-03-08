/**
 * Class to implement the first block of the disk. This is the OS-managed
 * block.
 */
public class Superblock {

    private static final int DEFAULT_TOTAL_INODE_BLOCKS = 64;

    public int totalBlocks;     /* the number of disk blocks */
    public int totalINodes;     /* the number of inodes */
    public int freeList;        /* the block number of the free list's head */
    public int freeListHead;
    public int freeListTail;

    /**
     * Initializes a Superblock object with the provided disk size.
     *
     * @param diskSize size of disk that we want to initialize
     */
    public Superblock(int diskSize) {
        this.init(diskSize);
    }

    /**
     * Initializes a Superblock object with the provided disk size. If the information read from
     * disk is not valid then a default configuration will be used.
     *
     * @param diskSize size of disk that we want to initialize
     */
    private void init(int diskSize) {
        this.readSuperblock();

        // if the configuration that was read from disk is not valid then go
        // ahead and use a default configuration
        if (configNotValid(diskSize)) {
            this.totalBlocks = diskSize;
            this.freeListTail = diskSize - 1;
            this.format(DEFAULT_TOTAL_INODE_BLOCKS);
        }
    }

    /**
     * Validates the configuration that we have read from disk. If the configuration is
     * not valid this indicates a default config should be used.
     *
     * @param diskSize should be equal to totalBlocks
     * @return if the configuration read from disk is valid
     */
    private boolean configNotValid(int diskSize) {
        if (this.totalBlocks == diskSize && this.totalINodes > 0 &&
                this.freeListTail <= this.totalBlocks && this.freeListHead >= 2) {
            return true;
        }
        return false;
    }

    /**
     *
     *
     * @param totalINodes maximum number of files to be created
     */
    public void format(int totalINodes) {

        // TODO: check for valid totalINodes size

        this.totalINodes = totalINodes;

        // calculate the pointer for freeList.
        this.freeList = (FileSystemHelper.SHORT_BYTE_SIZE) * (this.totalINodes * FileSystemHelper.INODE_BYTE_SIZE) / Disk.blockSize;

        Inode inode;

        // create new unused Inode and write it to disk
        for (short index = 0; index < totalINodes; index++) {
            inode = new Inode();
            inode.flag = Flag.UNUSED.getValue();
            inode.toDisk(index);
        }

        // connect each of the free blocks together
        byte data[];
        for (int blockIndex = this.freeList; blockIndex < this.totalBlocks; blockIndex++) {
            data = new byte[Disk.blockSize];

            // convert all of the data to bytes
            SysLib.int2bytes(blockIndex + 1, data, 0);

            // write the data to the block specified
            SysLib.rawwrite(blockIndex, data);
        }
        this.writeSuperblock();
    }

    private void writeSuperblock() {
        byte blockInfo[] = new byte[Disk.blockSize];

        SysLib.int2bytes(this.totalBlocks, blockInfo, 0);
        SysLib.int2bytes(this.totalINodes, blockInfo, 4);
        SysLib.int2bytes(this.freeList, blockInfo, 8);

        SysLib.rawwrite(0, blockInfo);
    }

    private void readSuperblock() {
        // Disk determines our block size
        byte blockInfo[] = new byte[Disk.blockSize];

        // read in all of the block info into blockInfo buffer
        SysLib.rawread(0, blockInfo);

        // read the number of disk blocks from blockInfo
        this.totalBlocks = SysLib.bytes2int(blockInfo, 0);
        this.totalINodes = SysLib.bytes2int(blockInfo, 4);
        this.freeListTail = SysLib.bytes2int(blockInfo, 8);
        this.freeListTail = SysLib.bytes2int(blockInfo, 12);
    }
}

