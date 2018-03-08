/**
 * Represents the root directory of the file system. The root directory maintains each
 * file in a different directory entry that contains its file name (maximum 30
 * characters, 60 bytes in Java) and the corresponding Inode number. The directory
 * receives a maximum number of Inodes to be created (aka the maximum number of files
 * to be created) and keeps track of which Inode numbers are in use. Since the directory
 * itself is considered as a file, its contents are maintained by an Inode, specifically
 * Inode 0. This can be located in the first 32 bytes of the disk block 1.
 */
public class Directory {

    private static final int INT_BYTE_SIZE = 4;     /* integer is 4 bytes */
    private static final int MAX_CHAR_SIZE = 60;    /* char is two bytes */
    private static int maxChars = 30;               /* max characters of each file name */

    private int offset = 0;

    /* Directory entries */
    private int fileSizes[];       /* each element stores a different file size. */

    /**
     * Each element stores a different file name. The file name is stored as a
     * character array which is why fileNames is a 2D array.
     */
    private char fileNames[][];

    /**
     * Initializes a Directory object with the maximum number of files (Inodes)
     * to be created.
     *
     * @param maxInumber maximum number of Inodes in the Directory
     */
    public Directory(int maxInumber) {
        fileSizes = new int[maxInumber];

        // initialize the size of all the files to be 0
        for (int i = 0; i < maxInumber; i++) {
            fileSizes[i] = 0;
        }

        fileNames = new char[maxInumber][maxChars];

        // initializes the name and size of the root directory
        String root = "/";
        fileSizes[0] = root.length();
        root.getChars(0, fileSizes[0], fileNames[0], 0);
    }

    /**
     * Initializes the Directory instance with a byte array read from the disk.
     *
     * @param data directory information to be read from disk
     */
    public void bytes2directory(byte data[]) {

        // Reads in the file sizes from data. Sets the size of the
        // file at index equal to the file size read from data.
        // Also increments our offset.
        for (int index = 0; index < this.fileSizes.length; index++) {

            this.incrementOffset(INT_BYTE_SIZE);
            this.fileSizes[index] = SysLib.bytes2int(data, offset);
        }


        // Reads in the file names from data. Sets the name of the file at
        // index equal to the file name read from data. Also increments
        // our offset.
        for (int index = 0; index < this.fileNames.length; index++) {

            this.incrementOffset(MAX_CHAR_SIZE);

            // Creating the fileName populated with data, the offset where it starts
            // and the maximum number of chars
            String fileName = new String(data, offset, MAX_CHAR_SIZE);

            // Copies the characters in fileName from index 0 to index fileSizes[index]
            // into the fileNames[index] array (since fileNames is a 2D array).
            fileName.getChars(0, this.fileSizes[index], this.fileNames[index], 0);
        }

        this.resetOffset();
    }

    /**
     * Converts a Directory instance to a byte array to be written to disk.
     */
    public byte[] directory2bytes( ) {
        // buffer size is equal to the sum of the number of bytes of fileNames and fileSizes
        int bufferSize = (this.fileSizes.length * INT_BYTE_SIZE) + (this.fileNames.length * MAX_CHAR_SIZE);

        // initialize the buffer
        byte[] buffer = new byte[bufferSize];

        // writing the file size to disk
        for (int fileSize : this.fileSizes) {
            this.incrementOffset(INT_BYTE_SIZE);
            SysLib.int2bytes(fileSize, buffer, offset);
        }

        for (char[] fileName : this.fileNames) {
            this.incrementOffset(MAX_CHAR_SIZE);
            byte fileNameBuffer[] = (new String(fileName)).getBytes();
            System.arraycopy(fileNameBuffer, 0, buffer, offset, fileNameBuffer.length);
        }

        this.resetOffset();
        return buffer;
    }

    /**
     * Allocates a new Inode number for the file with the name filename.
     *
     * @param fileName name of the file to be created
     */
    public short ialloc(String fileName) {
        for (short iNodeBlock = 0; iNodeBlock < this.fileSizes.length; iNodeBlock++) {
            int fileSize = this.fileSizes[iNodeBlock];

            // checks if the node is free, if so sets the size and name of the node.
            if (fileSize == 0) {    // if this node is free
                fileSize = fileName.length();
                fileName.getChars(0, fileSize, this.fileNames[iNodeBlock], 0);

                // return the number of the inode block that we just allocated
                return iNodeBlock;
            }
        }

        // there was an error, we have no inodes that are free to allocate
        return -1;
    }

    /**
     * Deallocates the iNumber and deletes the file corresponding to the
     * iNumber.
     *
     * @param iNumber file to delete
     */
    public boolean ifree(short iNumber) {
        if (this.isValidINumber(iNumber)) {
            this.fileSizes[iNumber] = 0;    // set the size to 0 since we are freeing it
            return true;    // success
        }

        return false;
    }

    /**
     * Returns the iNumber corresponding the the fileName
     *
     * @param fileName whose iNumber we want to fetch
     */
    public short namei(String fileName) {
        for (short index = 0; index < this.fileSizes.length; index++) {

            // if they do not have a matching length then we will for sure know that they
            // are not going to have the same name, so we can continue on to the next
            // iteration
            if (this.fileSizes[index] != fileName.length()) {
                continue;
            }

            if (fileName.equals((new String(this.fileNames[index])))) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Returns whether iNumber falls within a valid range and corresponds to an inode
     * that is in use.
     *
     * @param iNumber block to check for validity
     */
    private boolean isValidINumber(int iNumber) {
        // out of range
        if (iNumber >= this.fileSizes.length) {
            return false;
        }

        // out of range
        if (iNumber < 0) {
            return false;
        }

        // we can't free a block that isn't yet allocated
        if (this.fileSizes[iNumber] == 0) {
            return false;
        }

        return true;
    }

    /**
     * Resets offset to zero.
     */
    private void resetOffset() {
        this.offset = 0;
    }

    /**
     * Increments offset by amount.
     *
     * @param amount to add to offset
     */
    private void incrementOffset(int amount) {
        this.offset = this.offset + amount;
    }
}
