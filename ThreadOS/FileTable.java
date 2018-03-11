/**
 * Holds a table of up to 32 file entries representing the open files in
 * a directory for a given user.
 *
 * Each user thread maintains a user file descriptor table in its own {@link TCB}.
 * Every time it opens a file, it allocates a new entry table including a
 * reference to the corresponding file (structure) table entry. Whenever a
 * thread spawns a new child thread, it passes a copy of its TCB to this
 * child which thus has a copy of its parent's user file descriptor table.
 * This in turn means the both the parent and the child refer to the same
 * file (structure) table entries and eventually share the same files.
 */
import java.util.Vector;

public class FileTable {

    /* the actual entity of this file table */
    public Vector<FileTableEntry> table;

    /* the root directory */
    private Directory directory;

    /**
     * Constructor.
     */
    public FileTable( Directory directory ) {
        table = new Vector();
        this.directory = directory;
    }

    /**
     * Allocate a new {@link FileTableEntry} with the given filename and mode and add
     * it to the table.
     *
     * @param filename for the new file
     * @param mode the file can be accessed in
     * @return the new FileTableEntry for the file, null if error or no space left in table
     */
    public synchronized FileTableEntry falloc(String filename, String mode) {

        Inode iNode = null;

        //iNumber has to be a short because of Inode
        short iNumber;

        //check for "/" or file name
        if (filename.equals(FileSystemHelper.DELIMITER)) {
            iNumber = 0;
        } else {
            iNumber = directory.getInumberByFileName(filename);
        }

        while (true) {

            //check for "/" or file name
            if (filename.equals(FileSystemHelper.DELIMITER)) {
                iNumber = 0;
            } else {
                iNumber = directory.getInumberByFileName(filename);
            }

            //Check for file
            if (iNumber >= 0) {

                iNode = new Inode(iNumber);

                if (mode.equals(Mode.READ_ONLY)) {

                    if (iNode.flag == FileSystemHelper.FLAG_WRITE) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            // do nothing
                        }
                        break;
                    } else if (iNode.flag == FileSystemHelper.FLAG_DELETE) {
                        return null;
                    } else {
                        iNode.flag = FileSystemHelper.FLAG_READ;
                        break;
                    }
                } else {
                    //if the iNode hasn't been edited at all, it's write(3)
                    if (iNode.flag == FileSystemHelper.FLAG_UNUSED || iNode.flag == FileSystemHelper.FLAG_USED) {
                        iNode.flag = FileSystemHelper.FLAG_WRITE;
                    }

                        //if the iNode is busy, wait
                    else if (iNode.flag == FileSystemHelper.FLAG_READ || iNode.flag == FileSystemHelper.FLAG_WRITE) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            // do nothing
                        }
                        break;
                    } else if (iNode.flag == FileSystemHelper.FLAG_DELETE) {
                        return null;
                    }
                }
            } else {
                iNumber = directory.ialloc(filename);
                iNode = new Inode(iNumber);
                break;
            }
        }
        // allocate/retrieve and register the corresponding inode using directory
        // increment this inode's count
        iNode.count++;
        // immediately write back this inode to the disk
        iNode.toDisk(iNumber);
        // return a reference to this file (structure) table entry
        FileTableEntry retVal = new FileTableEntry(iNode, iNumber, mode);
        table.addElement(retVal);
        return retVal;
    }

    /**
     * Write a file to disk and remove it from the table.
     *
     * @param entry to write to disk
     * @return success of removal
     */
    public synchronized boolean ffree(FileTableEntry entry) {
        if (entry.count != 0) {
            notifyAll();
        }

        entry.inode.toDisk(entry.iNumber);
        return this.table.remove(entry);
    }

    /**
     * Returns whether the file table data mamber is empty.
     */
    public synchronized boolean fempty( ) {
        return table.isEmpty( );  // return if table is empty
    }                            // should be called before starting a format
}