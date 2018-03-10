/**
 *
 */

import java.util.Vector;

public class FileTable {

    /* the actual entity of this file table */
    public Vector<FileTableEntry> table;

    /* the root directory */
    private Directory dir;

    /**
     * Constructor.
     */
    public FileTable( Directory directory ) {
        table = new Vector();
        dir = directory;
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
        Inode node = null;
        //nodeNum has to be a short because of Inode
        short nodeNum = -1;

        //check for "/" or file name
        if (filename.equals("/"))
            nodeNum = 0;
        else
            nodeNum = dir.getInumberByFileName(filename);

        while (true) {
            //check for "/" or file name
            if (filename.equals("/"))
                nodeNum = 0;
            else
                nodeNum = dir.getInumberByFileName(filename);

            //Check for file
            if (nodeNum >= 0) {
                node = new Inode(nodeNum);
                if (mode.equals(Mode.READ_ONLY)) {
                    if (node.flag == 3) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                        }
                        break;
                    } else if (node.flag == 4) {
                        nodeNum = -1;
                        return null;
                    } else {
                        node.flag = 2;
                        break;
                    }
                } else {
                    //if the node hasn't been edited at all, it's write(3)
                    if (node.flag == 0 || node.flag == 1)
                        node.flag = 3;

                        //if the node is busy, wait
                    else if (node.flag == 2 || node.flag == 3) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                        }
                        break;
                    } else if (node.flag == 4) {
                        nodeNum = -1;
                        return null;
                    }
                }
            } else {
                nodeNum = dir.ialloc(filename);

                // did we have enough space in the directory?
                if (nodeNum == FileSystemHelper.INVALID) {
                    return null;
                }
                node = new Inode(nodeNum);
                break;
            }
        }
        // allocate/retrieve and register the corresponding inode using dir
        // increment this inode's count
        node.count++;
        // immediately write back this inode to the disk
        node.toDisk(nodeNum);
        // return a reference to this file (structure) table entry
        FileTableEntry retVal = new FileTableEntry(node, nodeNum, mode);
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