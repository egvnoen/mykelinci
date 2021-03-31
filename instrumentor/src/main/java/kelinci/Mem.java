package kelinci;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Properties;

/**
 * Class to record branching, analogous to the shared memory in AFL.
 * 
 * Because we measure inside a particular target method, we need
 * a way to start/stop measuring. Therefore, the array can be cleared.
 */

public class Mem {
	
	public static final int SIZE = 65538;

	int flen = SIZE;
	int fsize = 0;
	String shareFileName;
	String sharePath;
	MappedByteBuffer mapBuf = null;
	FileChannel fc = null;
	FileLock fl = null;
	Properties p = null;
	RandomAccessFile RAFile = null;

	byte[] mem = new byte[SIZE];

	/**
	 * Create a shm
	 * 
	 * @param sp shm file path
	 * @param sf shm file name
	 */
	private Mem(String sp, String sf) {
		if (sp.length() != 0) {
			File folder = new File(sp);
			if (!folder.exists()) {
				folder.mkdirs();
			}
		}

		this.shareFileName = sf;
		this.sharePath = sp + File.separator;

		try {
			// Get a random access file object
			RAFile = new RandomAccessFile(this.sharePath + this.shareFileName + ".sm", "rw");
			// Get file channel
			fc = RAFile.getChannel();
			// Get file size
			fsize = (int) fc.size();
			if (fsize < flen) {
				byte bb[] = new byte[flen - fsize];
				// Create byte buffer
				ByteBuffer bf = ByteBuffer.wrap(bb);
				bf.clear();
				// Set the file location of the channel
				fc.position(fsize);
				// Write bytes from this buffer to this channel
				fc.write(bf);
				fc.force(false);
				fsize = flen;
			}
			// Map the file area of this channel to memory
			mapBuf = fc.map(FileChannel.MapMode.READ_WRITE, 0, fsize);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param ps    the start position of the lock area
	 * @param buff  data written
	 */
	public void write(int ps, byte[] buff) {

		// Define the mark for the lock area
		FileLock fl = null;
		try {
			// Get the lock on the given region of this channel file
			fl = fc.lock(ps, 1, false);
			if (fl != null) {
				mapBuf.position(ps);
				ByteBuffer bf1 = ByteBuffer.wrap(buff);
				mapBuf.put(bf1);
				// Release this lock
				fl.release();
			}
		} catch (Exception e) {
			if (fl != null) {
				try {
					fl.release();
				} catch (IOException e1) {
					System.out.println(e1.toString());
				}
			}
		}
	}

	/**
	 * Clear the shm
	 */
	public synchronized void write_all() {
        
        FileLock fl = null;
        try {
            
            fl = fc.lock(0, 65536, false);
            if (fl != null) {
 
                mapBuf.position(0);
                ByteBuffer bf1 = ByteBuffer.wrap(mem);
                mapBuf.put(bf1);
                
                fl.release();
            }
        } catch (Exception e) {
            if (fl != null) {
                try {
                    fl.release();
                } catch (IOException e1) {
                    System.out.println(e1.toString());
                }
            }
        }
    }

	/**
	 * Write ID at the end of the shm
	 */
	public void write_ID(int id) {
		
		// Define the mark for the lock area
		FileLock fl = null;
		try {
			// Get the lock on the given region of this channel file
			fl = fc.lock(65536, 2, false);
			if (fl != null) {

				// Change (int)ID into byte[]
				byte[] buff = new byte[2];
				buff[1] = (byte) (id & 0xff);
        		buff[0] = (byte) (id >> 8 & 0xff);

				mapBuf.position(65536);
				ByteBuffer bf1 = ByteBuffer.wrap(buff);
				mapBuf.put(bf1);
				// Release this lock
				fl.release();
			}
		} catch (Exception e) {
			if (fl != null) {
				try {
					fl.release();
				} catch (IOException e1) {
					System.out.println(e1.toString());
				}
			}
		}
	}

	/**
	 * @param buff data read
	 */
	public void read(int ps, byte[] buff) {

		FileLock fl = null;
		try{
			fl = fc.lock(ps, 1, false);
			if(fl != null){
				mapBuf.position(ps);
				int len = 1;
				if(mapBuf.remaining() < len){
					len = mapBuf.remaining();
				}

				if(len > 0){
					mapBuf.get(buff, 0, len);
				}
				fl.release();
			}
		} catch (Exception e) {
			if (fl != null) {
				try {
					fl.release();
				} catch (IOException e1) {
					System.out.println(e1.toString());
				}
			}
		}
	}

	/**
	 * Read ID at the end of the shm
	 */
	public int read_ID() {
		
		FileLock fl = null;
		try{
			fl = fc.lock(65536, 2, false);
			if(fl != null){
				mapBuf.position(65536);

				byte[] buff = new byte[2];
				mapBuf.get(buff, 0, 2);
				fl.release();

				int id = 0;
				id += (buff[0] & 0xff) << 8;
				id += (buff[1] & 0xff);

				return id;
			}
		} catch (Exception e) {
			if (fl != null) {
				try {
					fl.release();
				} catch (IOException e1) {
					System.out.println(e1.toString());
				}
			}
			return 0;
		}
		return 0;
	}

	public byte[] read_all() {
		
		byte[] zero = new byte[65536];

		FileLock fl = null;
		try{
			fl = fc.lock(0, 65536, false);
			if(fl != null){
				mapBuf.position(0);

				byte[] buff = new byte[65536];
				mapBuf.get(buff, 0, 65536);
				fl.release();

				return buff;
			}
		} catch (Exception e) {
			if (fl != null) {
				try {
					fl.release();
				} catch (IOException e1) {
					System.out.println(e1.toString());
				}
			}
			return zero;
		}
		return zero;
	}

	/**
	 * close the shm
	 */
	public void closeSMFile(){
		if (fc != null) {
            try {
                fc.close();
            } catch (IOException e) {
                System.out.println(e.toString());
            }
            fc = null;
        }

        if (RAFile != null) {
            try {
                RAFile.close();
            } catch (IOException e) {
                System.out.println(e.toString());
            }
            RAFile = null;
        }
        mapBuf = null;
	}

	public static synchronized void coverage(int id){
		sm = Mem.getInstance();
		int last_id = sm.read_ID();
		int write_area = id ^ last_id;
		sm.write_ID(id >> 1);
		byte[] num = new byte[1];
		// sm.read(write_area, num);
		num[0] = 1;
		sm.write(write_area, num);
		//sm.closeSMFile();
	}

	//private static Mem sm = new Mem("/opt/oss/envs/Product-MCVPNDesignService/1.4.384/webapps/ROOT/WEB-INF/classes/shm", "bit_map");
	//private static Mem sm = new Mem("/home/user/kelinci_2/examples/simple/shm", "bit_map");
	private static Mem sm = new Mem("/home/user/kelinci_2/shm", "bit_map");

	public static Mem getInstance() {
		return sm;
	}
}
