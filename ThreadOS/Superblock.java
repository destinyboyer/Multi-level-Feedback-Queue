/**
 * Class to implement the first block of the disk. This is the OS-managed
 * block.
 */
public class Superblock {

    /* for if we are not given a number, or the number is not valid */
    private static final int DEFAULT_TOTAL_INODE_BLOCKS = 64;

    /* the number of disk blocks */
    public int totalBlocks;

    /* the number of inodes */
    public int totalINodes;

    /* the block number of the free list's head */
    public int freeList;

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
        if (configValid(diskSize) == false) {
            this.totalBlocks = diskSize;
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
    private boolean configValid(int diskSize) {
        return (this.totalBlocks == diskSize && this.totalINodes > 0 && this.freeList >= 2);
    }

    /**
     * Formats the Superblock.
     *
     * @param totalINodes maximum number of files to be created
     */
    public void format(int totalINodes) {
        this.totalINodes = totalINodes;

        // calculate the pointer for freeList.
        this.freeList = (FileSystemHelper.SHORT_BYTE_SIZE) * (this.totalINodes * FileSystemHelper.INODE_BYTE_SIZE) / Disk.blockSize;

        // this takes care of situations where we are given an odd OR even
        // number of total Inodes
        if (this.totalINodes % 16 != 0) {
            this.freeList = (this.totalINodes / 16) + 2;
        } else {
            this.freeList = this.totalINodes / 16 + 1;
        }

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

        // write the super block to disk
        this.writeSuperblock();
    }

    /**
     * Writes the block info to disk.
     */
    private void writeSuperblock() {
        byte blockInfo[] = new byte[Disk.blockSize];

        SysLib.int2bytes(-1, blockInfo, 0);
        SysLib.rawwrite(this.totalBlocks - 1, blockInfo);

        SysLib.int2bytes(this.totalBlocks, blockInfo, 0);
        SysLib.int2bytes(this.totalINodes, blockInfo, 4);
        SysLib.int2bytes(this.freeList, blockInfo, 8);

        SysLib.rawwrite(0, blockInfo);
    }

    /**
     * Reads all of the data for a super block and sets the totalBlocks,
     * totalINodes, and freeList.
     */
    private void readSuperblock() {
        // Disk determines our block size
        byte blockInfo[] = new byte[Disk.blockSize];

        // read in all of the block info into blockInfo buffer
        SysLib.rawread(0, blockInfo);

        // read the number of disk blocks from blockInfo
        this.totalBlocks = SysLib.bytes2int(blockInfo, 0);
        this.totalINodes = SysLib.bytes2int(blockInfo, 4);
        this.freeList = SysLib.bytes2int(blockInfo, 8);
    }

    /**
     * Flushes all of the data from the given block number and sets the
     * free list to the block.
     *
     * @param blockNumber of the block to free
     */
    public void freeBlock(int blockNumber) {
        byte data[] = new byte[Disk.blockSize];

        // clear all of the data
        for(int index = 0; index < Disk.blockSize; index++) {
            data[index] = 0;
        }

        // write to disk
        SysLib.int2bytes(this.freeList, data, 0);
        SysLib.rawwrite(blockNumber, data);
        this.freeList = blockNumber;
    }

    /**
     * Gets the free block and reads that blocks data.
     *
     * @return the iNumber of the free block.
     */
    public short getFreeBlock() {
        // there are no free blocks
        if (this.freeList == FileSystemHelper.INVALID) {
            return FileSystemHelper.INVALID;
        }

        int freeBlock = this.freeList;

        byte data[] = new byte[Disk.blockSize];
        SysLib.rawread(freeBlock, data);
        this.freeList = SysLib.bytes2int(data, 0);

        return (short) freeBlock;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public void setTotalBlocks(int totalBlocks) {
        this.totalBlocks = totalBlocks;
    }

    public int getTotalINodes() {
        return totalINodes;
    }

    public void setTotalINodes(int totalINodes) {
        this.totalINodes = totalINodes;
    }

    public int getFreeList() {
        return freeList;
    }

    public void setFreeList(int freeList) {
        this.freeList = freeList;
    }
}

