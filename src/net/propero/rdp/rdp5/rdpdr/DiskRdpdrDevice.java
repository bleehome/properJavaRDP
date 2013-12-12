package net.propero.rdp.rdp5.rdpdr;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.propero.rdp.RdpPacket;
import net.propero.rdp.RdpPacket_Localised;

public class DiskRdpdrDevice extends RdpdrDevice {

    private final static int OPEN_EXISTING = 1;
    private final static int CREATE_NEW = 2;
    private final static int OPEN_ALWAYS = 3;
    private final static int TRUNCATE_EXISTING = 4;
    private final static int CREATE_ALWAYS = 5;
    
    private final static int O_ACCMODE    =   0003;
    private final static int O_RDONLY     =     00;
    private final static int O_WRONLY     =    01;
    private final static int O_RDWR       =      02;
    private final static int O_CREAT      =      0100; /* not fcntl */
    private final static int O_EXCL       =       0200; /* not fcntl */
    private final static int O_NOCTTY     =     0400; /* not fcntl */
    private final static int O_TRUNC      =      01000; /* not fcntl */
    private final static int O_APPEND     =     02000;
    private final static int O_NONBLOCK   =  04000;
    private final static int O_NDELAY     =      O_NONBLOCK;
    private final static int O_SYNC       =       010000;
    private final static int O_FSYNC      =       O_SYNC;
    private final static int O_ASYNC      =      020000;
    
    private final static int FILE_DIRECTORY_FILE              =       0x00000001;
    private final static int FILE_WRITE_THROUGH               =       0x00000002;
    private final static int FILE_SEQUENTIAL_ONLY             =       0x00000004; 
    private final static int FILE_NO_INTERMEDIATE_BUFFERING   =       0x00000008; 
    private final static int FILE_COMPLETE_IF_OPLOCKED        =       0x00000100;
    private final static int FILE_DELETE_ON_CLOSE             =       0x00001000;
    
    private final static int FILE_SYNCHRONOUS_IO_ALERT        =       0x00000010; 
    private final static int FILE_SYNCHRONOUS_IO_NONALERT     =       0x00000020; 
    private final static int FILE_NON_DIRECTORY_FILE          =       0x00000040; 
    private final static int FILE_CREATE_TREE_CONNECTION      =       0x00000080; 
    
    private final static int FILE_ATTRIBUTE_READONLY     =    0x00000001;
    private final static int FILE_ATTRIBUTE_HIDDEN       =    0x00000002;
    private final static int FILE_ATTRIBUTE_SYSTEM       =    0x00000004;
    private final static int FILE_ATTRIBUTE_NORMAL       =    0x00000080;
    private final static int FILE_ATTRIBUTE_DIRECTORY    =    0x00000010;
    
    private final static int RD_STATUS_SUCCESS              =    0x00000000;
    private final static int RD_STATUS_NO_MORE_FILES        =    0x80000006;
    private final static int RD_STATUS_DEVICE_PAPER_EMPTY   =    0x8000000e;
    private final static int RD_STATUS_DEVICE_POWERED_OFF   =    0x8000000f;
    private final static int RD_STATUS_DEVICE_OFF_LINE      =    0x80000010;
    private final static int RD_STATUS_DEVICE_BUSY          =    0x80000011;

    private final static int RD_STATUS_INVALID_HANDLE       =    0xc0000008;
    private final static int RD_STATUS_INVALID_PARAMETER    =    0xc000000d;
    private final static int RD_STATUS_NO_SUCH_FILE          =   0xc000000f;
    private final static int RD_STATUS_INVALID_DEVICE_REQUEST=   0xc0000010;
    private final static int RD_STATUS_ACCESS_DENIED         =   0xc0000022;
    private final static int RD_STATUS_OBJECT_NAME_COLLISION =   0xc0000035;
    private final static int RD_STATUS_DISK_FULL             =   0xc000007f;
    private final static int RD_STATUS_FILE_IS_A_DIRECTORY   =   0xc00000ba;
    private final static int RD_STATUS_NOT_SUPPORTED         =   0xc00000bb;
    private final static int RD_STATUS_TIMEOUT               =   0xc0000102;
    private final static int RD_STATUS_NOTIFY_ENUM_DIR       =   0xc000010c;
    private final static int RD_STATUS_CANCELLED             =   0xc0000120;
    private final static int RD_STATUS_DIRECTORY_NOT_EMPTY   =   0xc0000101;
    
    private final static int RD_STATUS_NOT_IMPLEMENTED       =   0x00000001;
    private final static int RD_STATUS_PENDING               =   0x00000103;
    
    private final static int FS_CASE_SENSITIVE       =    0x00000001;
    private final static int FS_CASE_IS_PRESERVED    =        0x00000002;
    
    private static final int FILE_SUPERSEDE    = 0x00000000;
    private static final int FILE_OPEN         = 0x00000001;
    private static final int FILE_CREATE       = 0x00000002;
    private static final int FILE_OPEN_IF      = 0x00000003;
    private static final int FILE_OVERWRITE    = 0x00000004;
    private static final int FILE_OVERWRITE_IF = 0x00000005;
    
    
    private final static int FileFsVolumeInformation               =   1;
    private final static int FileFsSizeInformation               =   3;
    private final static int FileFsFullSizeInformation               =   7;
    private final static int FileFsAttributeInformation               =   5;
    
    
    private final static int FileBasicInformation = 0x00000004;
    private static final int FileStandardInformation = 0x00000005;
    private static final int FileAttributeTagInformation = 0x00000023;
    private final static int FileEndOfFileInformation = 0x00000014;
    private final static int FileDispositionInformation = 0x0000000D;
    private final static int FileRenameInformation = 0x0000000A;
    private final static int FileAllocationInformation = 0x00000013;

    private final static int FileDirectoryInformation = 0x00000001;
    private final static int FileFullDirectoryInformation = 0x00000002;
    private final static int FileBothDirectoryInformation = 0x00000003;
    private final static int FileNamesInformation = 0x0000000C;
    

    private final static int DEVICE_TYPE_DISK = 0x08;
    private String path;
    
    private int openFileIndexGener = 1;
    private Map<Integer, OpenedFile> openedFiles = new HashMap<Integer, DiskRdpdrDevice.OpenedFile>();

    public DiskRdpdrDevice(String name, String path) {
        super(DEVICE_TYPE_DISK);
        this.name = name;
        this.path = path;
        this.deviceData = new RdpPacket_Localised(0);
    }

    @Override
    public void register(String optarg, int port) {
        // TODO Auto-generated method stub
        System.out.println(DiskRdpdrDevice.class.getName() + " register");
    }

    @Override
    public int create(RdpPacket data, ResultHolder holder) throws IOException {
        // TODO Auto-generated method stub
        System.out.println(DiskRdpdrDevice.class.getName() + " create");
        
        int handle; 

        int flags, mode;
        
        flags = 0;
//        mode = S_IRWXU | S_IRGRP | S_IXGRP | S_IROTH | S_IXOTH;

        int desiredAccess = data.getBigEndian32();
        long allocationSize = data.getLittleEndian32() + (data.getLittleEndian32() << 32);
        int fileAttributes = data.getLittleEndian32();
        int sharedAccess = data.getLittleEndian32();
        int createDisposition = data.getLittleEndian32();
        int createOptions = data.getLittleEndian32();
        int pathLength = data.getLittleEndian32();

        String fileName = ""; 
        if(pathLength > 0 && (pathLength / 2) < 256) {
            byte[] pathByte = new byte[pathLength];
            data.copyToByteArray(pathByte, 0, data.getPosition(), pathLength);
            fileName = parsePath(pathByte);
            fileName = fileName.replaceAll("\\\\", "/");
        }
        
        System.out.println("creating path=" + path + fileName);
        
        if(fileName.indexOf("/..") != -1) {
            return RD_STATUS_ACCESS_DENIED;
        }
        
        boolean find = false;
        OpenedFile of = null;
        Iterator<Integer> keys = openedFiles.keySet().iterator();
        while(keys.hasNext()) {
            int key = keys.next();
            of = openedFiles.get(key);
            if(fileName.equals(of.name)) {
                find = true;
                break;
            }
        }
        
        if(!find) {
            int newId = openFileIndexGener++;
            of = new OpenedFile(newId, fileName);
            of.accessmask = desiredAccess;
        }
        
        int result = RD_STATUS_SUCCESS;
        
        switch(createDisposition) {
        //TODO
        case FILE_SUPERSEDE:
            break;
        case FILE_OPEN:
            if(of.file.exists()) {
                result = RD_STATUS_SUCCESS;
            } else {
                result = RD_STATUS_NO_SUCH_FILE;
            }
            break;
        case FILE_CREATE:
            if(of.file.exists()) {
                result = RD_STATUS_ACCESS_DENIED;
            } else {
                if((createOptions & FILE_DIRECTORY_FILE) != 0) {
                    of.file.mkdir();
                } else {
                    of.file.createNewFile();
                }
            }
            break;
        case FILE_OPEN_IF:
            if(of.file.exists()) {
                result = RD_STATUS_SUCCESS;
            } else {
                if((createOptions & FILE_DIRECTORY_FILE) != 0) {
                    of.file.mkdir();
                } else {
                    of.file.createNewFile();
                }
            }
            break;
        case FILE_OVERWRITE:
            if(of.file.exists()) {
                if(of.file.isDirectory()) {
                    result = RD_STATUS_FILE_IS_A_DIRECTORY;
                } else {
                    of.file.createNewFile();
                }
            } else {
                result = RD_STATUS_ACCESS_DENIED;
            }
            break;
        case FILE_OVERWRITE_IF:
            if(of.file.exists()) {
                if(of.file.isDirectory()) {
                    result = RD_STATUS_FILE_IS_A_DIRECTORY;
                } else {
                    of.file.delete();
                    of.file.createNewFile();
                }
            } else {
                if((createOptions & FILE_DIRECTORY_FILE) != 0) {
                    of.file.mkdir();
                } else {
                    of.file.createNewFile();
                }
            }
            break;
        }
        
//        if(of.file.exists() && of.file.isDirectory()) {
//            if((fileAttributes & FILE_NON_DIRECTORY_FILE) != 0) {
//                return RD_STATUS_FILE_IS_A_DIRECTORY;
//            } else {
//                createOptions |= FILE_DIRECTORY_FILE;
//            }
//        }
//        
//        if((createOptions |= FILE_DIRECTORY_FILE) != 0) {
//            
//        } else {
//            //TODO
//        }
        
        
        if(result == RD_STATUS_SUCCESS) {
            holder.put("fileId", of.fileId);
            if(!find) {
                openedFiles.put(of.fileId, of);
            }
        } else {
            holder.put("fileId", 0);
        }
        
        System.out.println("create new file, fileId=" + of.fileId);
        
        return result;
    }

    @Override
    public int write(RdpPacket data, int fileId, DataOutputStream out) throws IOException {
        int length = data.getLittleEndian32();
        long offset = data.getLittleEndian32() + (data.getLittleEndian32() << 32);
        data.incrementPosition(20);
        
        OpenedFile of = openedFiles.get(fileId);
        if(of == null) {
            return RD_STATUS_INVALID_HANDLE;
        }
        
        RandomAccessFile raf = null;
        try {
            raf = of.getRaf();
        } catch (FileNotFoundException e) {
            return RD_STATUS_ACCESS_DENIED;
        }
        raf.seek(offset);
        
        for(int l = 0; l < length; l++) {
            
            raf.write(data.get8());
        }
        writeIntLe(out, length);
        
        return RD_STATUS_SUCCESS;
    }

    @Override
    public int close(int fileId) {
        OpenedFile of = openedFiles.get(fileId);
        if(!of.file.exists()) {
            return RD_STATUS_NO_SUCH_FILE;
        }
        if(of.delete_on_close) {
            if(of.file.delete()) {
                return RD_STATUS_SUCCESS;
            } else {
                return RD_STATUS_ACCESS_DENIED;
            }
        }
        try {
            if(of.fc != null) {
                of.fc.close();
            }
            if(of.raf != null) {
                of.closeRaf();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RD_STATUS_SUCCESS;
    }

    public byte[] disk_query_information(RdpPacket data, int fileId) throws IOException {
        int fsInformationClass = data.getLittleEndian32();
        int length = data.getLittleEndian32();
        OpenedFile of = openedFiles.get(fileId);
        if(of == null) {
            return null;
        }
        
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bout);
        
        System.out.println("disk_query_information:" + of.filePath);
        
        String filePath = of.filePath;
        File javaFile = new File(filePath);
        javaxt.io.File f = new javaxt.io.File(javaFile);
        
        /* Set file attributes */
        int file_attributes = 0;
        if (javaFile.isDirectory()) {
            file_attributes |= FILE_ATTRIBUTE_DIRECTORY;
        }
        if (f.isHidden()) {
            file_attributes |= FILE_ATTRIBUTE_HIDDEN;
        }
        if (file_attributes == 0) {
            file_attributes |= FILE_ATTRIBUTE_NORMAL;
        }
        if (f.isReadOnly()) {
            file_attributes |= FILE_ATTRIBUTE_READONLY;
        }
        
        switch(fsInformationClass) {
        case FileBasicInformation:
            long createTime = getWindowsTime(f.getCreationTime());
            long lastAccessTime = getWindowsTime(f.getLastAccessTime());
            long lastWriteTime = getWindowsTime(f.getLastModifiedTime());
            
            writeLongLe(out, createTime);
            writeLongLe(out, lastAccessTime);
            writeLongLe(out, lastWriteTime);
            writeLongLe(out, lastWriteTime);
            
            writeIntLe(out, file_attributes);
            
            break;
        case FileStandardInformation:
            writeLongLe(out, f.getSize());/* Allocation size */
            writeLongLe(out, f.getSize());/* End of file */
            writeIntLe(out, 0);  /* Number of links */
            out.write(0);  /* Delete pending */
            out.write(javaFile.isDirectory() ? 1 : 0);  /* Directory */
            
            break;
        case FileAttributeTagInformation:
            writeIntLe(out, file_attributes);    /* File Attributes */
            writeIntLe(out, 0);  /* Reparse Tag */
            
            break;
        }
        out.flush();
        bout.flush();
        
        byte[] buffer = bout.toByteArray();
        return buffer;
    }
    
    public int disk_set_information(RdpPacket data, int fileId, DataOutputStream out) {
        int fsInformationClass = data.getLittleEndian32();
        int length = data.getLittleEndian32();
        data.incrementPosition(24);
        OpenedFile of = openedFiles.get(fileId);
        if(of == null || !of.file.exists()) {
            return RD_STATUS_NO_SUCH_FILE;
        }
        switch(fsInformationClass) {
        case FileBasicInformation:
            //do nothing
            javaxt.io.File f = new javaxt.io.File(of.file);
//            of.file.set
//            f.set
            long createTime = data.getLittleEndian32() + (data.getLittleEndian32() << 32);
            long accessTime = parseWindowsTime(data.getLittleEndian32() + (data.getLittleEndian32() << 32));
            long writeTime = parseWindowsTime(data.getLittleEndian32() + (data.getLittleEndian32() << 32));
            long changeTime = parseWindowsTime(data.getLittleEndian32() + (data.getLittleEndian32() << 32));
            int fileAttributes = data.getLittleEndian32();
            
            of.file.setLastModified(changeTime);
            
//            if((fileAttributes & FILE_ATTRIBUTE_READONLY) != 0) {
//                of.file.setReadOnly();
//            }
            
            
            
            break;
        case FileEndOfFileInformation:
            // we can do nothing
            break;
        case FileDispositionInformation://This information class is used to mark a file for deletion
            if ((of.accessmask &
                    (FILE_DELETE_ON_CLOSE | FILE_COMPLETE_IF_OPLOCKED)) != 0) {
                if(of.file.isDirectory()) {
                    String[] fs = of.file.list();
                    if(fs != null && fs.length > 0) {
                        return RD_STATUS_DIRECTORY_NOT_EMPTY;
                    }
                }
                of.delete_on_close = true;
            }
            break;
        case FileRenameInformation:
            int replaceIfExists = data.get8();
            int rootDirectory = data.get8();//RootDirectory
            int pathLength = data.getLittleEndian32();
            String fileName = ""; 
            if(pathLength > 0 && (pathLength / 2) < 256) {
                byte[] pathByte = new byte[pathLength];
                data.copyToByteArray(pathByte, 0, data.getPosition(), pathLength);
                fileName = parsePath(pathByte);
                fileName = fileName.replaceAll("\\\\", "/");
            } else {
                return RD_STATUS_INVALID_PARAMETER;
            }
            if(!of.file.renameTo(new File(path, fileName))) {
                return RD_STATUS_ACCESS_DENIED;
            }
            break;
        case FileAllocationInformation:
            break;
        default :
            return RD_STATUS_INVALID_PARAMETER;
        }
        
        return RD_STATUS_SUCCESS;
    }
    
    public int queryDirectory(RdpPacket data, int fileId, DataOutputStream out) throws IOException {
        int fsInformationClass = data.getLittleEndian32();
        int initialQuery = data.get8();
        int pathLength = data.getLittleEndian32();
        
        int file_attributes = 0;
        
        //23 bytes padding
        int i = 0;
        while(i < 23) {
            i++;
            data.get8();
        }
        OpenedFile of = openedFiles.get(fileId);
        
        String pattern = "";
        if (pathLength > 0 && pathLength < 2 * 255) {
            byte[] pathByte = new byte[pathLength];
            data.copyToByteArray(pathByte, 0, data.getPosition(), pathLength);
            pattern = parsePath(pathByte);
            pattern = pattern.replaceAll("\\\\", "/");
//            return RD_STATUS_NO_MORE_FILES;
        }
        
        String subFile = null;
        File subJavaFile = null;
        javaxt.io.File subf = null;
        
        switch (fsInformationClass)  {
            case FileBothDirectoryInformation:
            case FileDirectoryInformation:
            case FileFullDirectoryInformation:
            case FileNamesInformation:
                if(pattern.length() != 0) {
                    int index = pattern.lastIndexOf("/");
                    if(index != -1) {
                        of.pattern = pattern.substring(index);
                    } else {
                        of.pattern = pattern;
                    }
                    String[] files = null;
                    if(of.file.isDirectory()) {
                        files = of.file.list();
                    }
                    of.subfiles = Arrays.asList(files == null ? new String[]{} : files).iterator();
                }
                while(of.subfiles.hasNext()) {
                    subFile = of.subfiles.next();
                   /* if(符合正则表达式) {
                        
                    }*/
                    break;
                }
                if(subFile == null) {
                    return RD_STATUS_NO_MORE_FILES;// STATUS_NO_MORE_FILES;
                }
                System.out.println("pattern=" + pattern);
                System.out.println("find subfile:" + of.filePath + subFile);
                
                subJavaFile = new File(of.filePath, subFile);
                subf = new javaxt.io.File(subJavaFile);
                
                if (subJavaFile.isDirectory()) {
                    file_attributes |= FILE_ATTRIBUTE_DIRECTORY;
                }
                if (subf.isHidden()) {
                    file_attributes |= FILE_ATTRIBUTE_HIDDEN;
                }
                if (file_attributes == 0) {
                    file_attributes |= FILE_ATTRIBUTE_NORMAL;
                }
                if (subf.isReadOnly()) {
                    file_attributes |= FILE_ATTRIBUTE_READONLY;
                }
                
                writeIntLe(out, 0);
                writeIntLe(out, 0);
                
                break;
            default:
                return RD_STATUS_INVALID_PARAMETER;
        }
        long createTime = getWindowsTime(subf.getCreationTime());
        long lastAccessTime = getWindowsTime(subf.getLastAccessTime());
        long lastWriteTime = getWindowsTime(subf.getLastModifiedTime());
        
        switch (fsInformationClass) {
        case FileBothDirectoryInformation:
            writeLongLe(out, createTime);
            writeLongLe(out, lastAccessTime);
            writeLongLe(out, lastWriteTime);
            writeLongLe(out, lastWriteTime);
            
            writeLongLe(out, subf.getSize());/* Allocation size */
            writeLongLe(out, subf.getSize());/* End of file */
            
            writeIntLe(out, file_attributes);
            
            writeIntLe(out, 2 * subFile.length() + 2);
            writeIntLe(out, 0);//EaSize
            out.write(0);//ShortNameLength
//            out.write(0);//padding
            byte[] shortName = new byte[24];
            out.write(shortName);
            
            writePath(out, subFile);
            
            break;
        case FileDirectoryInformation:
            writeLongLe(out, createTime);
            writeLongLe(out, lastAccessTime);
            writeLongLe(out, lastWriteTime);
            writeLongLe(out, lastWriteTime);
            
            writeLongLe(out, subf.getSize());/* Allocation size */
            writeLongLe(out, subf.getSize());/* End of file */
            
            writeIntLe(out, file_attributes);
            
            writeIntLe(out, 2 * subFile.length() + 2);
            writePath(out, subFile);
            
            break;
        case FileFullDirectoryInformation:
            writeLongLe(out, createTime);
            writeLongLe(out, lastAccessTime);
            writeLongLe(out, lastWriteTime);
            writeLongLe(out, lastWriteTime);
            
            writeLongLe(out, subf.getSize());/* End of file */
            writeLongLe(out, subf.getSize());/* Allocation size */
            
            writeIntLe(out, file_attributes);
            
            writeIntLe(out, 2 * subFile.length() + 2);
            writeIntLe(out, 0);//EaSize
            writePath(out, subFile);
            break;
        case FileNamesInformation:
            writeIntLe(out, 0);//EaSize
            writePath(out, subFile);
            break;
        default:
            return RD_STATUS_INVALID_PARAMETER;
        }
        
        return RD_STATUS_SUCCESS;
    }
    
    public int disk_create_notify(RdpPacket data, int fileId) {
        int fsInformationClass = data.getLittleEndian32();
        
        int result = RD_STATUS_PENDING;
        
        OpenedFile of = openedFiles.get(fileId);
        of.infoClass = fsInformationClass;
        
        result = notifyInfo(of);
        
        if ((fsInformationClass & 0x1000) != 0) { 
            if (result == RD_STATUS_PENDING) {
                return RD_STATUS_SUCCESS;
            }
        }
        
        return result;
    }
    
    private long getWindowsTime(Date date) {
        if(date == null) {
            date = new Date();
        }
        return (date.getTime() + 11644473600000L) * 10000;
    }
    
    private long parseWindowsTime(long t) {
        return t / 10000 - 11644473600000L;
    }
    
    private int notifyInfo(OpenedFile of) {
        File f = of.file;
        if(!f.exists() || !f.isDirectory()) {
            return RD_STATUS_ACCESS_DENIED;
        }
        
        
        
        
        return RD_STATUS_PENDING;
    }
    
    public int disk_query_volume_information(RdpPacket data, int fileId, DataOutputStream out) throws IOException {
        int fsInformationClass = data.getLittleEndian32();
        
        OpenedFile of = openedFiles.get(fileId);
        
        if(!of.file.exists()) {
            return RD_STATUS_ACCESS_DENIED;
        }
        
        int serial = 0;
        String label = "CLOUDSOFT";
        String type ="RDPFS";
        
        switch(fsInformationClass) {
        case FileFsVolumeInformation:
            out.writeInt(0);
            out.writeInt(0);
            writeIntLe(out, serial);
            
            writeIntLe(out, 2 * label.length());
            
            out.write(0);
            
            for(int i = 0; i < label.length(); i++) {
                char c = label.charAt(i);
                out.write((byte) c);
                out.write((byte) (c >> 8));
            }
            
            break;
        case FileFsSizeInformation:
            writeLongLe(out, 10L * 1024 * 1024);
            writeLongLe(out, 5L * 1024 * 1024);//可用
            writeIntLe(out, 4 * 1024 / 0x200);//8 sectors/unit
            writeIntLe(out, 0x200);//512 bytes/sector
            
            break;
        case FileFsFullSizeInformation:
            writeLongLe(out, 10L * 1024 * 1024);
            writeLongLe(out, 5L * 1024 * 1024);//可用
            writeLongLe(out, 6L * 1024 * 1024);//free
            writeIntLe(out, 4 * 1024 / 0x200);
            writeIntLe(out, 0x200);
            
            break;
        case FileFsAttributeInformation:
            writeIntLe(out, FS_CASE_SENSITIVE | FS_CASE_IS_PRESERVED);
            writeIntLe(out, 0xFF);
            
            writeIntLe(out, 2 * type.length());
            for(int i = 0; i < type.length(); i++) {
                char c = type.charAt(i);
                out.write((byte) c);
                out.write((byte) (c >> 8));
            }
            
            break;
            
        default:
            
            return RD_STATUS_INVALID_PARAMETER;
            
        }
        return RD_STATUS_SUCCESS;
    }
    
    public int read(RdpPacket data, int fileId, DataOutputStream out) throws IOException {
        int length = data.getLittleEndian32();
        long offset = data.getLittleEndian32() + (data.getLittleEndian32() << 32);
        
        OpenedFile of = openedFiles.get(fileId);
        if(of == null) {
            return RD_STATUS_CANCELLED;
        }
        
        long fileLength = of.file.length();
        if((offset + length) > fileLength) {
            length = (int) (fileLength - offset);
        }
        
        FileChannel fc = of.getFc();
        MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, offset, length);
        int n = 0;
        while((n++) < length) {
            out.write(mbb.get());
        }
        
        return RD_STATUS_SUCCESS;
    }
    
    private void writePath(DataOutputStream out, String path) throws IOException {
        for(int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            out.write((byte) c);
            out.write((byte) (c >> 8));
        }
        out.write(0);//终结符
        out.write(0);
    }
    
    private String parsePath(byte[] unicodeBytes) {
        StringBuilder sb = new StringBuilder("");
        int i = 0;
        while(i < unicodeBytes.length) {
            char c = (char) ((0xFF&unicodeBytes[i]) | ((0xFF&unicodeBytes[i+1]) << 8));
            i += 2;
            if(c != 0) {
                sb.append(c);
            }
        }
        
        return sb.toString();
    }
    
    private void writeLongLe(DataOutputStream out, long v) throws IOException {
        out.write((byte)(v >>>  0));
        out.write((byte)(v >>>  8));
        out.write((byte)(v >>> 16));
        out.write((byte)(v >>> 24));
        out.write((byte)(v >>> 32));
        out.write((byte)(v >>> 40));
        out.write((byte)(v >>> 48));
        out.write((byte)(v >>> 56));
//        out.write(0);
//        out.write(0);
//        out.write(0);
//        out.write(0);
//        out.writeLong(v);
    }
    
    private void writeIntLe(DataOutputStream out, int v) throws IOException {
//        out.writeInt(v);
        out.write((byte)(v >>>  0));
        out.write((byte)(v >>>  8));
        out.write((byte)(v >>> 16));
        out.write((byte)(v >>> 24));
    }
    
    private void unlink(File file) {
        if(file.exists()) {
            file.delete();
        }
    }
    
    private class OpenedFile {
        int fileId;
        String name;
        String filePath;
        File file;
        String pattern;
        int infoClass = 0;
        int accessmask;
        boolean delete_on_close = false;
        RandomAccessFile raf = null;
        FileChannel fc = null;
        private boolean rafClosed = true;
        
        Iterator<String> subfiles = null;
        
        public OpenedFile(int fileId, String name) {
            super();
            this.fileId = fileId;
            this.name = name;
            this.filePath = path + this.name;
            file = new File(filePath);
        }
        
        RandomAccessFile getRaf() throws FileNotFoundException {
            if(raf == null || rafClosed) {
                raf = new RandomAccessFile(file, "rw");
            }
            return raf;
        }
        
        void closeRaf() throws IOException {
            rafClosed = true;
            if(raf != null) {
                raf.close();
            }
        }
        
        FileChannel getFc() throws FileNotFoundException {
            if(fc == null || !fc.isOpen()) {
                fc = getRaf().getChannel();
            }
            return fc;
        }
        
    }

}
