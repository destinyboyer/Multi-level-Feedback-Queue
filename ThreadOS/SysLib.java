import java.util.*;

public class SysLib {

    /*------------------------------------------------------------------
     *                    Added for Final Project
     -----------------------------------------------------------------*/

    /**
     * Formats the disk (Disk.java's data contents).
     *
     * @param files maximum number of files {@link Inode}'s to be allocated in the file system
     * @return 0 on success, -1 otherwise
     */
    public static int format(int files) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.FORMAT, files, null);
    }

    /**
     * Opens the file specified by the fileName. Allocates a new file descriptor to the file.
     *
     * @param fileName file which to open
     * @param mode to open the file in, read ("r"), write ("w"), or append ("a")
     * @return number between 3 and 31 if successful, -1 if there was an error
     */
    public static int open(String fileName, String mode) {
        String params[] = {fileName, mode};
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.OPEN, 0, params);
    }

    /**
     * Reads up to buffer.length bytes from the file indicated by fd, starting at the position
     * currently pointed to by the seek pointer. If bytes remain between the current seek pointer
     * and the end of file are less that buffer.length then as many bytes as possible are read
     * and put into the beginning of the buffer. The seek pointer is incremented equal to the number
     * of bytes that have been read.
     *
     * @param fd file to read from
     * @param buffer bytes to read
     * @return number of bytes that have been read, -1 if error
     */
    public static int read(int fd, byte buffer[]) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.READ, fd, buffer);
    }

    /**
     * Writes the contents of buffer to the file indicated by fd, starting at the position
     * indicated by the seek pointer. The operation may overwrite existing data in the file
     * and/or append data to the end of the file. Increments the seek pointer by the number
     * of bytes written.
     *
     * @param fd file to write to
     * @param buffer bytes to write
     * @return number of bytes written, -1 if error
     */
    public static int write(int fd, byte buffer[]) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.WRITE, fd, buffer);
    }

    /**
     * Updates the seek pointer corresponding to fd as follows:
     *      - if whence is SEEK_SET (=0), the file's seek pointer is set to offset bytes from
     *        the beginning of the file
     *      - if whence is SEEK_CUR (=1), the file's seek pointer is set to its current value
     *        plus the offset; offset can be positive or negative
     *      - if whence if SEEK_END (=2), the file's seek pointer is set to the size of the file
     *        plus the offset; offset can be positive or negative
     *
     * NOTE: if you attempt to set the seek pointer to a negative number it will be
     *       set to zero, if you attempt to set it beyond the file size it will be
     *       set to the end of the file.
     *
     * @param fd file whose seek pointer needs to be incremented/decremented
     * @param offset amount to increment/decrement the seek pointer
     * @param whence at what point in the file to add the offset
     * @return the offset location of the seek pointer in the file
     */
    public static int seek(int fd, int offset, int whence) {
        int param[] = {fd, offset, whence};
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.SEEK, 0, param);
    }

    /**
     * Closes the file corresponding to fd, commits all file transactions on this file,
     * and unregisters fd from the user file descriptor table of the calling thread's TCB/
     *
     * @param fd file to close
     * @return 0 upon success, -1 if error
     */
    public static int close(int fd) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.CLOSE, fd, null);
    }

    /**
     * Deletes the file specified by fileName. All blocks used by file are freed. If the
     * file is currently open it is deleted.
     *
     * @param fileName file to be deleted
     * @return 0 upon success, -1 if error
     */
    public static int delete(String fileName) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.DELETE, 0, fileName);
    }

    //------------------------------------------------------------------

    public static int exec( String args[] ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.EXEC, 0, args );
    }

    public static int join( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.WAIT, 0, null );
    }

    public static int boot( ) {
	return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.BOOT, 0, null );
    }

    public static int exit( ) {
	return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.EXIT, 0, null );
    }

    public static int sleep( int milliseconds ) {
	return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.SLEEP, milliseconds, null );
    }

    public static int disk( ) {
	return Kernel.interrupt( Kernel.INTERRUPT_DISK,
				 0, 0, null );
    }

    public static int cin( StringBuffer s ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.READ, 0, s );
    }

    public static int cout( String s ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.WRITE, 1, s );
    }

    public static int cerr( String s ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.WRITE, 2, s );
    }

    public static int rawread( int blkNumber, byte[] b ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.RAWREAD, blkNumber, b );
    }

    public static int rawwrite( int blkNumber, byte[] b ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.RAWWRITE, blkNumber, b );
    }

    public static int sync( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.SYNC, 0, null );
    }

    public static int cread( int blkNumber, byte[] b ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.CREAD, blkNumber, b );
    }

    public static int cwrite( int blkNumber, byte[] b ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.CWRITE, blkNumber, b );
    }

    public static int flush( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.CFLUSH, 0, null );
    }

    public static int csync( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.CSYNC, 0, null );
    }

    public static String[] stringToArgs( String s ) {
	StringTokenizer token = new StringTokenizer( s," " );
	String[] progArgs = new String[ token.countTokens( ) ];
	for ( int i = 0; token.hasMoreTokens( ); i++ ) {
	    progArgs[i] = token.nextToken( );
	}
	return progArgs;
    }

    public static void short2bytes( short s, byte[] b, int offset ) {
	b[offset] = (byte)( s >> 8 );
	b[offset + 1] = (byte)s;
    }

    public static short bytes2short( byte[] b, int offset ) {
	short s = 0;
        s += b[offset] & 0xff;
	s <<= 8;
        s += b[offset + 1] & 0xff;
	return s;
    }

    public static void int2bytes( int i, byte[] b, int offset ) {
	b[offset] = (byte)( i >> 24 );
	b[offset + 1] = (byte)( i >> 16 );
	b[offset + 2] = (byte)( i >> 8 );
	b[offset + 3] = (byte)i;
    }

    public static int bytes2int( byte[] b, int offset ) {
	int n = ((b[offset] & 0xff) << 24) + ((b[offset+1] & 0xff) << 16) +
	        ((b[offset+2] & 0xff) << 8) + (b[offset+3] & 0xff);
	return n;
    }
}
