public class FileSystem {

    private Superblock superblock;
    private Directory directory;
    private FileTable filetable;

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    /* for clarity of return values */
    private final static int SUCCESS = 0;
    private final static int ERROR = -1;

    /**
     * Constructor.
     */
    public FileSystem(int diskBlocks) {
        // initialize the superblock
        superblock = new Superblock(diskBlocks);

        // initialize the directory with the correct number of files
        directory = new Directory(superblock.totalINodes);

        // initialize the file table with the given directory
        filetable = new FileTable(directory);

        // assemble our directory
        FileTableEntry entry = open("/", Mode.READ_ONLY);
        int directorySize = this.getFileSize(entry);

        if (directorySize > 0) {
            byte[] directoryData = new byte[directorySize];
            this.read(entry, directoryData);
            this.directory.bytes2directory(directoryData);
        }

        // close the file entry since we are done with it
        close(entry);
    }

    /**
     * Formats the disk (Disk.java's data contents). The parameter
     * files specifies the maximum number of files to be created (the
     * number of inodes to be allocated) in your file system. The
     * return value is 0 on success, otherwise -1.
     *
     * @param numFiles to format for
     * @return 0 indicating success, -1 indicating error
     */
    public int format(int numFiles) {
        // check for a system of at least 1 file
        if (numFiles > 0 && this.filetable.fempty()) {
            superblock.format(numFiles);
            return SUCCESS;
        }

        return ERROR;
    }

    /**
     * Opens the file specified by the fileName string in the given
     * mode.  The call allocates a new file descriptor, fd to this file.
     * The file is created if it does not exist in the mode "w", "w+" or "a".
     *
     * @param fileName of the file we want to open
     * @param mode that we want to open the file in
     * @return null if the mode was invalid, new {@link FileTableEntry} with the
     *         given fileName and mode otherwise.
     */
    public FileTableEntry open(String fileName, String mode) {
        // create a new entry
        FileTableEntry entry = filetable.falloc(fileName, mode);

        // we were able to make a new entry
        if (entry != null) {

            // what mode was the entry created in?
            switch (entry.mode) {

                // append functions always point to the end of the file
                case Mode.APPEND:
                    entry.seekPtr = this.getFileSize(entry);
                    break;

                // write only we always start at the beginning of the file and need
                // to deallocate all blocks so we can overwrite them
                case Mode.WRITE_ONLY:
                    entry.seekPtr = FileSystemHelper.BEGINNING_OF_FILE;
                    if (!this.deallocateBlocksForEntry(entry)) {
                        //return null;
                    }
                    break;

                // read only and read write always start at the beginning of the file
                case Mode.READ_ONLY:
                    // fall through
                case Mode.READ_WRITE:
                    entry.seekPtr = FileSystemHelper.BEGINNING_OF_FILE;
                    break;

                default:
                    break;
            }
        }

        return entry;
    }

    //Reads up to buffer.length bytes from the file indicated by fd,
    //starting at the position currently pointed to by the seek pointer.
    public int read(FileTableEntry entry, byte buffer[]) {
        if (buffer == null || buffer.length == 0)
            return FileSystemHelper.INVALID;

        int offset = 0;
        int bytesRemaining = buffer.length;
        int fileSize = getFileSize(entry);
        int buffSize = buffer.length;
        int bRead = 0;

        if (fileSize < buffSize) {
            bytesRemaining = fileSize;
        }

        while (entry.seekPtr < getFileSize(entry) && (bytesRemaining > 0)) {

            short bID = entry.inode.findBlockForOffset(entry.seekPtr);
            byte[] data = new byte[Disk.blockSize];

            if (bID < 0 || bID >= FileSystemHelper.directSize) {
                break;
            }

            SysLib.rawread(bID, data);

            // if there are more bytes remaining than our block size then we want to read in
            // an entire block size
            if (bytesRemaining > Disk.blockSize) {
                System.arraycopy(data, entry.seekPtr % Disk.blockSize, buffer, offset, Disk.blockSize);
                bytesRemaining = bytesRemaining - Disk.blockSize;
                offset = offset + Disk.blockSize;
                entry.seekPtr = entry.seekPtr + Disk.blockSize;
                bRead = bRead + Disk.blockSize;

                // otherwise just read in the bytes remaining
            } else {
                System.arraycopy(data, entry.seekPtr % Disk.blockSize, buffer, offset, bytesRemaining);
                bRead = bRead + bytesRemaining;

                // just for sanity, should never be used
                bytesRemaining = 0;
                break;
            }

        }

        // return the number of bytes that have been read
        entry.seekPtr = FileSystemHelper.BEGINNING_OF_FILE;
        return bRead;

    }

    public int write(FileTableEntry entry, byte buffer[]) {
        if (entry == null || entry.mode.equals(Mode.READ_ONLY)) {
            return FileSystemHelper.INVALID;
        }

        int bytesInBuffer = buffer.length;
        int writtenBytes = 0;
        byte data[] = new byte[Disk.blockSize];

        // while we still have data to write
        while(bytesInBuffer > 0) {

            short blockNumber = entry.inode.findBlockForOffset(entry.seekPtr);

            // no target block was found
            if (blockNumber == FileSystemHelper.FREE) {

                // get a new block
                blockNumber = superblock.getFreeBlock();

                // if there was a free direct pointer for this block
                if (entry.inode.getFreeDirectPoinerForBlock(blockNumber) > FileSystemHelper.FREE) {

                // try to get indirect pointer if there was not a direct pointer
                } else if (entry.inode.setInumberBlock(blockNumber)) {
                    blockNumber = superblock.getFreeBlock();
                    entry.inode.setIndirectPointer(blockNumber);

                // otherwise there were no pointers available
                } else {
                    break;
                }
            } else {
                SysLib.rawread(blockNumber, data); // read from currBlock
            }

            int pointer = entry.seekPtr % Disk.blockSize;
            int bytesInBlock = Disk.blockSize - pointer;

            // if there is any data left go ahead and write it
            if(bytesInBlock > bytesInBuffer) {
                System.arraycopy(buffer, writtenBytes, data, pointer, bytesInBuffer);
                SysLib.rawwrite(blockNumber, data);
                writtenBytes = writtenBytes + bytesInBuffer;
                bytesInBuffer = bytesInBuffer - bytesInBuffer;
                entry.seekPtr = entry.seekPtr + bytesInBuffer;
            } else { // write to the remainder in blocks
                System.arraycopy(buffer, writtenBytes, data, pointer, bytesInBlock);
                SysLib.rawwrite(blockNumber, data);
                writtenBytes = writtenBytes + bytesInBlock;
                bytesInBuffer = bytesInBuffer - bytesInBlock;
                entry.seekPtr = entry.seekPtr + bytesInBlock;
            }
        }

        switch(entry.mode) {
            case Mode.READ_WRITE:
                if ((getFileSize(entry) - writtenBytes) < 0) {
                    entry.inode.length = entry.inode.length + Math.abs(getFileSize(entry) - writtenBytes);
                } else {
                    entry.inode.length = entry.inode.length + writtenBytes;
                }
                break;

            case Mode.APPEND:
                entry.inode.length = getFileSize(entry) + writtenBytes;
                break;

            default:
                entry.inode.length = entry.inode.length + writtenBytes;
                break;
        }
        entry.inode.toDisk(entry.iNumber);  // write back to disk
        return writtenBytes; // return number of bytes that have been written
    }

    /**
     * Updates the seek pointer corresponding to fd
     * The offset can be positive or negative.
     * The offset location of the seek pointer in the file is
     * returned from the call to seek.
     *
     * @param entry whose seek pointer needs updating
     * @param offset amount to offset entry's seek pointer
     * @param whence point to offset from
     * @return -1 on error, value of updated seek pointer otherwise
     */
    public int seek(FileTableEntry entry, int offset, int whence){
        // can't set the seek pointer for a null entry, bail
        if(entry == null) {
            return ERROR;
        }

        switch (whence) {
            case SEEK_SET:
                if (!setSeekPtr(entry, offset)) {
                    entry.seekPtr = offset;
                }
                break;

            case SEEK_CUR:
                if (!setSeekPtr(entry, (entry.seekPtr + offset))) {
                    entry.seekPtr = (entry.seekPtr + offset);
                }
                break;

            case SEEK_END:
                if (!(setSeekPtr(entry, (getFileSize(entry) + offset)))) {
                    entry.seekPtr = (getFileSize(entry) + offset);
                }
                break;

            default:
                return ERROR;
        }
        return entry.seekPtr;

    }

    private boolean setSeekPtr(FileTableEntry ftEnt, int currVal) {
        if (currVal < 0) { // if negative, clamp it to 0
            ftEnt.seekPtr = 0;
            return true;
        } else if (currVal > getFileSize(ftEnt)) {   // greater than file length
            ftEnt.seekPtr = getFileSize(ftEnt);   // set it to EOF
            return true;
        }
        return false;
    }

    /**
     * Closes the file corresponding to fd, commits all file
     * transactions on this file, and unregisters fd from the user
     * file descriptor table of the calling thread's TCB. The return
     * value is 0 in success, otherwise -1.
     *
     * @param entry file table entry to close
     * @return 0 on success, -1 otherwise
     */
    public int close(FileTableEntry entry) {
        if (entry == null)
            return ERROR;

        entry.inode.count--;
        entry.count--;
        entry.inode.flag = 0;
        if (filetable.ffree(entry)) {
            return SUCCESS;
        }

        return ERROR;
    }

    /**
     * Deletes the file specified by fileName.
     * All blocks used by file are freed. If the file is currently
     * open, it is not deleted and the operation returns a -1. If
     * successfully deleted a 0 is returned.
     *
     * @param fileName of the file to delete
     * @return 0 indicating success, -1 if error
     */
    public synchronized int delete(String fileName) {

        short iNumber = directory.getInumberByFileName(fileName);

        // if the file is in the directory
        if (iNumber != FileSystemHelper.FREE) {
            FileTableEntry entry = filetable.table.get(iNumber);
            deallocateBlocksForEntry(entry); // release all blocks

            directory.removeFromDirectory(iNumber);
            return SUCCESS;
        }

        return ERROR;

    }

    /**
     * Deallocates all of the
     */
    private boolean deallocateBlocksForEntry(FileTableEntry entry) {
        // bail if we are null
        if (entry == null || entry.inode.count == 0) {
            return false;
        }

        // we do need to deallocate the indirect block
        if (entry.inode.indirect != FileSystemHelper.FREE) {
            byte[] data = new byte[Disk.blockSize];
            SysLib.rawread(entry.inode.indirect, data);
            entry.inode.indirect = FileSystemHelper.FREE;

            int blockNumber = SysLib.bytes2short(data, 0);
            if (blockNumber != FileSystemHelper.FREE) {
                this.superblock.freeBlock(blockNumber);
            }
        }

        // deallocate all of our direct blocks
        for (int index = 0; index < FileSystemHelper.directSize; index++) {
            int blockNumber = entry.inode.direct[index];

            if (blockNumber != FileSystemHelper.FREE) {
                superblock.freeBlock(blockNumber);
                entry.inode.direct[index] = FileSystemHelper.FREE;
            }
        }

        entry.inode.toDisk(entry.iNumber);
        return true;
    }

    /**
     * Returns the size in bytes of the entry.
     *
     * @param file to calculate the size for
     * @return size of entry in bytes
     */
    public int getFileSize(FileTableEntry file) {
        if (file == null) {
            return FileSystemHelper.INVALID;
        }

        return file.inode.length;
    }
}