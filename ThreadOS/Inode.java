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

    public int length;                             /* file size in bytes */
    public short count;                            /* # file-table entries pointing to this */
    public short flag;                             /* 0 = unused, 1 = used, ... */
    public short direct[] = new short[FileSystemHelper.directSize]; /* direct pointers */
    public short indirect;                         /* indirect pointer */

    Inode( ) {                                     // a default constructor
        this.length = 0;
        this.count = 0;
        this.flag = Flag.USED.getValue();

        for (int index = 0; index < FileSystemHelper.directSize; index++) {
            direct[index] = FileSystemHelper.FREE;
        }
        indirect = FileSystemHelper.FREE;
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

        byte[] data = new byte[Disk.blockSize];
        SysLib.rawread(blockNumber, data);

        this.length = SysLib.bytes2int(data, offset);
        offset = offset + FileSystemHelper.INT_BYT_SIZE;

        this.count = SysLib.bytes2short(data, offset);
        offset = offset + FileSystemHelper.SHORT_BYTE_SIZE;

        this.flag = SysLib.bytes2short(data, offset);
        offset = offset + FileSystemHelper.SHORT_BYTE_SIZE;

        for (int index = 0; index < FileSystemHelper.directSize; index++) {
            this.direct[index] = SysLib.bytes2short(data, offset);
            offset = offset + FileSystemHelper.SHORT_BYTE_SIZE;
        }

        this.indirect = SysLib.bytes2short(data, offset);
    }

    /**
     * Saves this to disk as the i-th (iNumber) Inode.
     *
     * @param iNumber disk block number to save to
     */
    public void toDisk(short iNumber) {
        byte data[] = new byte[Disk.blockSize];
        int blockNumber = FileSystemHelper.calculateBlockNumber(iNumber);

        // read in the data that is currently in the block number
        SysLib.rawread(blockNumber, data);

        int offset = FileSystemHelper.calculateOffset(iNumber);

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

        // write out indirect info
        SysLib.short2bytes(this.indirect, data, offset);
        SysLib.rawwrite(blockNumber, data);
    }


    public int getFreeDirectPoinerForBlock(short blockNumber) {
        // there are no free blocks
        if (blockNumber == FileSystemHelper.INVALID) {
            return FileSystemHelper.INVALID;
        }

        // get the first free direct pointer
        for (int index = 0; index < FileSystemHelper.directSize; index++) {
            // this direct pointer is unused
            if (this.direct[index] == FileSystemHelper.FREE) {
                // set it to our block number
                this.direct[index] = blockNumber;
                return index;
            }
        }
        return FileSystemHelper.INVALID;
    }



    public short findTargetBlock(int offset) {
        int blockNumber = offset / Disk.blockSize;

        if (blockNumber >= FileSystemHelper.directSize) {
            if(this.indirect == FileSystemHelper.FREE) {
                return this.indirect;
            }
            byte blockData[] = new byte[Disk.blockSize];
            SysLib.rawread(this.indirect, blockData);
            return SysLib.bytes2short(blockData, ((blockNumber - FileSystemHelper.directSize) * 2));
        } else {
            return this.direct[blockNumber];
        }
    }

    public void setIndirectPointer(short blockNumber) {
        byte blockData[] = new byte[Disk.blockSize];
        int offset = 0;
        SysLib.rawread(this.indirect, blockData);

        for (int index = 0; index < FileSystemHelper.TOTAL_POINTERS; index++) {

            if (SysLib.bytes2short(blockData, offset) ==  FileSystemHelper.FREE) {
                SysLib.short2bytes(blockNumber, blockData, offset);
                SysLib.rawwrite(this.indirect, blockData);
                return;
            }
            offset = offset + FileSystemHelper.SHORT_BYTE_SIZE;
        }
    }

    public void setIndexBlock(short indexBlockNumber) {
        this.indirect = indexBlockNumber;
        byte blockData[] = new byte[Disk.blockSize];
        int offset = 0;
        short indexPtr = FileSystemHelper.FREE;

        for (int index = 0; index < FileSystemHelper.TOTAL_POINTERS; index++) {
            SysLib.short2bytes(indexPtr, blockData, offset);
            offset = offset + FileSystemHelper.SHORT_BYTE_SIZE;
        }

        SysLib.rawwrite(indexBlockNumber, blockData);
    }
}
