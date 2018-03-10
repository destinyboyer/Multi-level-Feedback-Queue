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

    /* file size in bytes */
    public int length;

    /* # file-table entries pointing to this */
    public short count;

    /* 0 = unused, 1 = used, ... */
    public short flag;

    /* direct pointers */
    public short direct[] = new short[FileSystemHelper.directSize];

    /* indirect pointer */
    public short indirect;

    /**
     * Default constructor. Initializes all data members and pointers to 0.
     */
    Inode() {

        // initialize data members
        this.length = 0;
        this.count = 0;
        this.flag = Flag.USED.getValue();

        // set all our direct pointer to be free since they are all unused
        for (int index = 0; index < FileSystemHelper.directSize; index++) {
            this.direct[index] = FileSystemHelper.FREE;
        }

        // set indirect pointer to be free
        this.indirect = FileSystemHelper.FREE;
    }

    /**
     * Constructor. Reads the disk block corresponding to iNumber, locates the corresponding
     * Inode information in that block, and initializes a new Inode with that
     * information.
     *
     * @param iNumber disk block number to read
     */
    Inode(short iNumber) {

        // calculate necessary variables
        int blockNumber = FileSystemHelper.calculateBlockNumber(iNumber);
        int iNodeId = FileSystemHelper.calculateInodeId(iNumber);
        int offset = FileSystemHelper.calculateOffset(iNodeId);

        // create read buffer and read data from disk
        byte[] data = new byte[Disk.blockSize];
        SysLib.rawread(blockNumber, data);

        // populate our data members
        this.length = SysLib.bytes2int(data, offset);
        offset = offset + FileSystemHelper.INT_BYT_SIZE;

        this.count = SysLib.bytes2short(data, offset);
        offset = offset + FileSystemHelper.SHORT_BYTE_SIZE;

        this.flag = SysLib.bytes2short(data, offset);
        offset = offset + FileSystemHelper.SHORT_BYTE_SIZE;

        // populate our direct pointers
        for (int index = 0; index < FileSystemHelper.directSize; index++) {
            this.direct[index] = SysLib.bytes2short(data, offset);
            offset = offset + FileSystemHelper.SHORT_BYTE_SIZE;
        }

        // populate our indirect pointer
        this.indirect = SysLib.bytes2short(data, offset);
    }

    /**
     * Saves this to disk as the i-th (iNumber) Inode.
     *
     * @param iNumber disk block number to save to
     */
    public void toDisk(short iNumber) {
        // sanity check, should never happen
        if (iNumber < 0 || iNumber >= FileSystemHelper.directSize) {
            return;
        }

        byte data[] = new byte[Disk.blockSize];

        // calculate the block number for this Inode
        int blockNumber = FileSystemHelper.calculateBlockNumber(iNumber);

        // read in the data that is currently in the this block
        SysLib.rawread(blockNumber, data);

        int offset = FileSystemHelper.calculateOffset(iNumber);

        // write all of our data members to disk
        SysLib.int2bytes(this.length, data, offset);
        offset = offset + FileSystemHelper.INT_BYT_SIZE;
        SysLib.short2bytes(this.count, data, offset);
        offset = offset + FileSystemHelper.SHORT_BYTE_SIZE;
        SysLib.short2bytes(this.flag, data, offset);
        offset = offset + FileSystemHelper.SHORT_BYTE_SIZE;

        // write the data at each of the direct pointers out
        for (int index = 0; index < FileSystemHelper.directSize; index++) {
            SysLib.short2bytes(this.direct[index], data, offset);
            offset = offset + FileSystemHelper.SHORT_BYTE_SIZE;
        }

        // write out indirect pointer
        SysLib.short2bytes(this.indirect, data, offset);
        SysLib.rawwrite(blockNumber, data);
    }

    /**
     * Returns the first free direct pointer in the block.
     *
     * @param blockNumber to find the free direct pointer in
     * @return the index of the first free direct pointer, -1 if no direct pointers are free
     */
    public int getFreeDirectPoinerForBlock(short blockNumber) {
        // there are no free blocks, bail now
        if (blockNumber == FileSystemHelper.INVALID) {
            return FileSystemHelper.INVALID;
        }

        // get the first free direct pointer
        for (int index = 0; index < FileSystemHelper.directSize; index++) {

            // if this direct pointer is unused
            if (this.direct[index] == FileSystemHelper.FREE) {

                // set it to our block number
                this.direct[index] = blockNumber;

                // return the index of the direct pointer
                return index;
            }
        }

        // we found no free direct pointers
        return FileSystemHelper.INVALID;
    }

    /**
     * Find the block that the offset is currently pointing to.
     *
     * @param offset from the start of the file
     * @return the block that the offset is pointing to
     */
    public short findBlockForOffset(int offset) {
        int blockNumber = offset / Disk.blockSize;

        // it is a direct pointer, go ahead and return that
        if (blockNumber < FileSystemHelper.directSize) {
            return this.direct[blockNumber];
        }

        // invalid since it is not in the direct pointers or the indirect pointer
        if (this.indirect == FileSystemHelper.FREE || this.indirect >= FileSystemHelper.directSize) {
            return FileSystemHelper.INVALID;
        }

        // the indirect pointer has the target block, read info in and return
        byte blockData[] = new byte[Disk.blockSize];
        SysLib.rawread(this.indirect, blockData);

        return SysLib.bytes2short(blockData, ((blockNumber - FileSystemHelper.directSize) * 2));
    }

    /**
     * Reads in the data for the indirect pointer for a given blockNumber.
     *
     * @param blockNumber to read data for the indirect pointer
     */
    public void setIndirectPointer(short blockNumber) {

        // sanity check
        if (this.indirect < 0 || this.indirect >= FileSystemHelper.directSize) {
            return;
        }

        // read indirect data into buffer
        byte blockData[] = new byte[Disk.blockSize];
        SysLib.rawread(this.indirect, blockData);

        int offset = 0;

        // iterate over all the indirect pointers
        for (int index = 0; index < FileSystemHelper.TOTAL_POINTERS; index++) {

            // we have encountered a free block
            if (SysLib.bytes2short(blockData, offset) ==  FileSystemHelper.FREE) {
                SysLib.short2bytes(blockNumber, blockData, offset);
                SysLib.rawwrite(this.indirect, blockData);
                return;
            }

            // increase offset by pointer amount
            offset = offset + FileSystemHelper.SHORT_BYTE_SIZE;
        }
    }

    public boolean setInumberBlock(short blockIndex) {

        // check if all direct pointers are used
        for(int index = 0; index < FileSystemHelper.directSize; index++) {

            // we found an unused direct pointer
            if (this.direct[index] == -FileSystemHelper.FREE) {
                return false;
            }
        }

        // we know that all of the direct pointers are in use if we have gotten here,
        // check if our indirect pointer is free
        if (this.indirect != FileSystemHelper.FREE) {
            return true;
        }

        // we know that our indirect pointer is free if we have gotten here
        this.indirect = blockIndex;
        byte block[] = new byte[Disk.blockSize];

        // format the block to 512/2 = 256 pointers, set it to short -1 (2 bytes each)
        int offset = 0;
        for (int index = 0; index < FileSystemHelper.TOTAL_POINTERS; index++) {
            SysLib.short2bytes((short) -1, block, offset);
            offset = offset + FileSystemHelper.SHORT_BYTE_SIZE;
        }

        // write information
        SysLib.rawwrite(blockIndex, block);
        return true;
    }
}
