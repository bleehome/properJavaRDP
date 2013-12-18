package net.propero.rdp.rdp5.rdpdr;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.RdpPacket_Localised;
import net.propero.rdp.crypto.CryptoException;
import net.propero.rdp.rdp5.VChannel;
import net.propero.rdp.rdp5.VChannels;
import net.propero.rdp.tools.HexDump;

public class RdpdrChannel extends VChannel {
    
    static final String CLIENT_NAME = "CLOUDSOFT";

    private final static int DEVICE_TYPE_SERIAL = 0x01;
    private final static int DEVICE_TYPE_PARALLEL = 0x02;
    private final static int DEVICE_TYPE_PRINTER = 0x04;
    private final static int DEVICE_TYPE_DISK = 0x08;
    private final static int DEVICE_TYPE_SCARD = 0x20;

    /* NT status codes for RDPDR */
    private final static int RD_STATUS_SUCCESS = 0x00000000;
    private final static int RD_STATUS_NOT_IMPLEMENTED = 0x00000001;
    private final static int RD_STATUS_PENDING = 0x00000103;

    private final static int RD_STATUS_NO_MORE_FILES = 0x80000006;
    private final static int RD_STATUS_DEVICE_PAPER_EMPTY = 0x8000000e;
    private final static int RD_STATUS_DEVICE_POWERED_OFF = 0x8000000f;
    private final static int RD_STATUS_DEVICE_OFF_LINE = 0x80000010;
    private final static int RD_STATUS_DEVICE_BUSY = 0x80000011;

    private final static int RD_STATUS_INVALID_HANDLE = 0xc0000008;
    private final static int RD_STATUS_INVALID_PARAMETER = 0xc000000d;
    private final static int RD_STATUS_NO_SUCH_FILE = 0xc000000f;
    private final static int RD_STATUS_INVALID_DEVICE_REQUEST = 0xc0000010;
    private final static int RD_STATUS_ACCESS_DENIED = 0xc0000022;
    private final static int RD_STATUS_OBJECT_NAME_COLLISION = 0xc0000035;
    private final static int RD_STATUS_DISK_FULL = 0xc000007f;
    private final static int RD_STATUS_FILE_IS_A_DIRECTORY = 0xc00000ba;
    private final static int RD_STATUS_NOT_SUPPORTED = 0xc00000bb;
    private final static int RD_STATUS_TIMEOUT = 0xc0000102;
    private final static int RD_STATUS_NOTIFY_ENUM_DIR = 0xc000010c;
    private final static int RD_STATUS_CANCELLED = 0xc0000120;

    private final static int IRP_MJ_CREATE = 0x00;
    private final static int IRP_MJ_CLOSE = 0x02;
    private final static int IRP_MJ_READ = 0x03;
    private final static int IRP_MJ_WRITE = 0x04;
    private final static int IRP_MJ_QUERY_INFORMATION = 0x05;
    private final static int IRP_MJ_SET_INFORMATION = 0x06;
    private final static int IRP_MJ_QUERY_VOLUME_INFORMATION = 0x0a;
    private final static int IRP_MJ_DIRECTORY_CONTROL = 0x0c;
    private final static int IRP_MJ_DEVICE_CONTROL = 0x0e;
    private final static int IRP_MJ_LOCK_CONTROL = 0x11;

    /* RDPDR constants */
    private final static int RDPDR_COMPONENT_TYPE_CORE = 0x4472; // "sD" "Ds"
                                                                 // ???
    private final static int RDPDR_COMPONENT_TYPE_PRINTING = 0x5052; // "RP"
                                                                     // "PR"
                                                                     // (PR)inting

    private final static int PAKID_CORE_SERVER_ANNOUNCE = 0x496E; // "nI" "nI"
                                                                  // ???
    private final static int PAKID_CORE_CLIENTID_CONFIRM = 0x4343; // "CC" "CC"
                                                                   // (C)lientID
                                                                   // (C)onfirm
    private final static int PAKID_CORE_CLIENT_NAME = 0x434E; // "NC" "CN"
                                                              // (C)lient (N)ame
    private final static int PAKID_CORE_DEVICELIST_ANNOUNCE = 0x4441; // "AD"
                                                                      // "DA"
                                                                      // (D)evice
                                                                      // (A)nnounce
    private final static int PAKID_CORE_DEVICE_REPLY = 0x6472; // "rd" "dr"
                                                               // (d)evice
                                                               // (r)eply
    private final static int PAKID_CORE_DEVICE_IOREQUEST = 0x4952; // "RI" "IR"
                                                                   // (I)O
                                                                   // (R)equest
    private final static int PAKID_CORE_DEVICE_IOCOMPLETION = 0x4943; // "CI"
                                                                      // "IC"
                                                                      // (I)O
                                                                      // (C)ompletion
    private final static int PAKID_CORE_SERVER_CAPABILITY = 0x5350; // "PS" "SP"
                                                                    // (S)erver
                                                                    // (C)apability
    private final static int PAKID_CORE_CLIENT_CAPABILITY = 0x4350; // "PC" "CP"
                                                                    // (C)lient
                                                                    // (C)apability
    private final static int PAKID_CORE_DEVICELIST_REMOVE = 0x444D; // "MD" "DM"
                                                                    // (D)evice
                                                                    // list
                                                                    // (R)emove
    private final static int PAKID_PRN_CACHE_DATA = 0x5043; // "CP" "PC"
                                                            // (P)rinter (C)ache
                                                            // data
    private final static int PAKID_CORE_USER_LOGGEDON = 0x554C; // "LU" "UL"
                                                                // (U)ser
                                                                // (L)ogged on
    private final static int PAKID_PRN_USING_XPS = 0x5543; // "CU" "UC" (U)sing
                                                           // (?)XPS

    /* CAPABILITY_HEADER.CapabilityType */
    private final static int CAP_GENERAL_TYPE = 0x0001;
    private final static int CAP_PRINTER_TYPE = 0x0002;
    private final static int CAP_PORT_TYPE = 0x0003;
    private final static int CAP_DRIVE_TYPE = 0x0004;
    private final static int CAP_SMARTCARD_TYPE = 0x0005;

    /* CAPABILITY_HEADER.Version */
    private final static int GENERAL_CAPABILITY_VERSION_01 = 0x00000001;
    private final static int GENERAL_CAPABILITY_VERSION_02 = 0x00000002;
    private final static int PRINT_CAPABILITY_VERSION_01 = 0x00000001;
    private final static int PORT_CAPABILITY_VERSION_01 = 0x00000001;
    private final static int DRIVE_CAPABILITY_VERSION_01 = 0x00000001;
    private final static int DRIVE_CAPABILITY_VERSION_02 = 0x00000002;
    private final static int SMARTCARD_CAPABILITY_VERSION_01 = 0x00000001;

    private final static int DR_MINOR_RDP_VERSION_5_0 = 0x0002;
    private final static int DR_MINOR_RDP_VERSION_5_1 = 0x0005;
    private final static int DR_MINOR_RDP_VERSION_5_2 = 0x000A;
    private final static int DR_MINOR_RDP_VERSION_6_X = 0x000C;

    /* GENERAL_CAPS_SET.extendedPDU */
    private final static int RDPDR_DEVICE_REMOVE_PDUS = 0x00000001;
    private final static int RDPDR_CLIENT_DISPLAY_NAME_PDU = 0x00000002;
    private final static int RDPDR_USER_LOGGEDON_PDU = 0x00000004;

    /* GENERAL_CAPS_SET.extraFlags1 */
    private final static int ENABLE_ASYNCIO = 0x00000001;

    /* DEVICE_ANNOUNCE.DeviceType */
    public final static int RDPDR_DTYP_SERIAL = 0x00000001;
    public final static int RDPDR_DTYP_PARALLEL = 0x00000002;
    public final static int RDPDR_DTYP_PRINT = 0x00000004;
    public final static int RDPDR_DTYP_FILESYSTEM = 0x00000008;
    public final static int RDPDR_DTYP_SMARTCARD = 0x00000020;

    public final static int RDPDR_PRINTER_ANNOUNCE_FLAG_ASCII = 0x00000001;
    public final static int RDPDR_PRINTER_ANNOUNCE_FLAG_DEFAULTPRINTER = 0x00000002;
    public final static int RDPDR_PRINTER_ANNOUNCE_FLAG_NETWORKPRINTER = 0x00000004;
    public final static int RDPDR_PRINTER_ANNOUNCE_FLAG_TSPRINTER = 0x00000008;
    public final static int RDPDR_PRINTER_ANNOUNCE_FLAG_XPSFORMAT = 0x00000010;
    
    public final static int IRP_MN_QUERY_DIRECTORY         =  0x01;
    public final static int IRP_MN_NOTIFY_CHANGE_DIRECTORY =  0x02;

    public int rdpdr_version_minor = DR_MINOR_RDP_VERSION_6_X;
    public int rdpdr_clientid = 0;
    public String rdpdr_clientname = null;

    public ArrayList<RdpdrDevice> devices = new ArrayList<RdpdrDevice>();
    
    public RdpdrChannel() {
    }

    @Override
    public int flags() {
        return VChannels.CHANNEL_OPTION_INITIALIZED
                | VChannels.CHANNEL_OPTION_COMPRESS_RDP;
    }

    @Override
    public String name() {
        return "rdpdr";
    }
    
    private int receive_packet_index = 0;
    private int send_packet_index = 0;

    @Override
    public void process(RdpPacket data) throws RdesktopException, IOException,
            CryptoException {
        int size = data.size();
        int position = data.getPosition();
        byte[] dump = new byte[size];
        data.copyToByteArray(dump, 0, position, size-position);
        System.out.print("\n"+(receive_packet_index++)+"------------------->>>>>>>>>>>>>>> data recieved.");
        System.out.println(HexDump.dumpHexString(dump));
        
        int component = data.getLittleEndian16();
        int packetID = data.getLittleEndian16();
        

        if (component == RDPDR_COMPONENT_TYPE_CORE) {

            switch (packetID) {
            case PAKID_CORE_SERVER_ANNOUNCE:
                rdpdr_process_server_announce_request(data);
                rdpdr_send_client_announce_reply();
                rdpdr_send_client_name_request();
                break;
            case PAKID_CORE_SERVER_CAPABILITY:
                /* server capabilities */
                rdpdr_process_capabilities(data);
                rdpdr_send_capabilities();
                break;

            case PAKID_CORE_CLIENTID_CONFIRM:
                rdpdr_process_server_clientid_confirm(data);
//                rdpdr_send_capabilities();
//                rdpdr_send_available();

                /*
                 * versionMinor 0x0005 doesn't send PAKID_CORE_USER_LOGGEDON, so
                 * we have to send it here
                 */
                if (rdpdr_version_minor == 0x0005)
                    rdpdr_send_device_list_announce_request();
                break;

            case PAKID_CORE_USER_LOGGEDON:
                rdpdr_send_device_list_announce_request();
                break;

            case PAKID_CORE_DEVICE_REPLY:
                System.out.println(data.getLittleEndian32() + " status = " + data.getLittleEndian32());
                /* connect to a specific resource */
//                int deviceID = data.getLittleEndian32();
//                int status = data.getLittleEndian32();
                break;

            case PAKID_CORE_DEVICE_IOREQUEST:
                rdpdr_process_irp(data);
                break;

            default:
                // ui_unimpl(NULL, "RDPDR core component, packetID: 0x%02X\n",
                // packetID);
                break;

            }
        } else if (component == RDPDR_COMPONENT_TYPE_PRINTING) {
            switch (packetID) {
            case PAKID_PRN_CACHE_DATA:
                // printercache_process(s);
                break;

            default:
                // ui_unimpl(NULL,
                // "RDPDR printer component, packetID: 0x%02X\n", packetID);
                break;
            }
        } else
            System.out.printf("RDPDR component: 0x%02X packetID: 0x%02X\n",
                    component, packetID);
    }

    private void rdpdr_process_server_announce_request(RdpPacket data) {
        int versionMajor = data.getLittleEndian16();// versionMajor, must be 1
        int versionMinor = data.getLittleEndian16(); // versionMinor
        rdpdr_clientid = data.getLittleEndian32(); // clientID

        if (versionMinor < rdpdr_version_minor)
            rdpdr_version_minor = versionMinor;
    }

    private void rdpdr_process_server_clientid_confirm(RdpPacket data) {
        int _versionMajor = data.getLittleEndian16();
        int _versionMinor = data.getLittleEndian16();
        int _clientId = data.getLittleEndian32();

        if (rdpdr_clientid != _clientId)
            rdpdr_clientid = _clientId;

        if (_versionMinor != rdpdr_version_minor)
            rdpdr_version_minor = _versionMinor;
    }

    private void rdpdr_send_client_announce_reply() {
        RdpPacket_Localised s;

        s = new RdpPacket_Localised(12);
        s.setLittleEndian16(RDPDR_COMPONENT_TYPE_CORE);
        s.setLittleEndian16(PAKID_CORE_CLIENTID_CONFIRM);
        s.setLittleEndian16(1);// versionMajor, must be set to 1
        s.setLittleEndian16(rdpdr_version_minor);// versionMinor
        if (rdpdr_clientid > 0) {
            s.setLittleEndian32(rdpdr_clientid); // clientID, given by the
                                                 // server in a Server Announce
                                                 // Request
        } else {
            s.setLittleEndian32(0x815ed39d);// /* IP address (use 127.0.0.1)
                                            // 0x815ed39d */
        }
        s.markEnd();

        try {
            this.send_packet(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rdpdr_send_client_name_request() {
        int clientNameLen = CLIENT_NAME.length() * 2;
        RdpPacket_Localised s =new RdpPacket_Localised(16 + clientNameLen + 2);

        s.setLittleEndian16(RDPDR_COMPONENT_TYPE_CORE);
        s.setLittleEndian16(PAKID_CORE_CLIENT_NAME);
        s.setLittleEndian32(0x00000001);/* unicodeFlag, 0 for ASCII and 1 for Unicode */
        s.setLittleEndian32(0);/* codePage, must be set to zero */
        s.setLittleEndian32(clientNameLen + 2); //ComputerNameLen,including null terminator.
        if (clientNameLen > 0) {
            try {
                s.copyFromByteArray(CLIENT_NAME.getBytes("UTF-16LE"), 0,
                        s.getPosition(), clientNameLen);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            s.incrementPosition(clientNameLen);
        }
        s.setLittleEndian16(0);//the null terminator of client name
        s.markEnd();

        try {
            this.send_packet(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rdpdr_process_capabilities(RdpPacket data) {
        int numCapabilities = data.getLittleEndian16();
        data.incrementPosition(2);//2 bytes padding
        
        for(int i = 0; i < numCapabilities; i++) {
            int capabilityType = data.getLittleEndian16();
            int capabilityLength;
            switch(capabilityType) {
            case CAP_GENERAL_TYPE:
            case CAP_PRINTER_TYPE:
            case CAP_PORT_TYPE:
            case CAP_DRIVE_TYPE:
            case CAP_SMARTCARD_TYPE:
                capabilityLength = data.getLittleEndian16();
                data.incrementPosition(capabilityLength - 4);
                break;
            }
        }
    }

    /* Process device direction general capability set */
    private void rdpdr_process_general_capset(RdpPacket data) {
        int capabilityLength = data.getLittleEndian16();/* capabilityLength */
        int version = data.getLittleEndian32();/* version */
        data.incrementPosition(4);/* osType, ignored on receipt (4 bytes) */
        data.incrementPosition(4);/*
                                   * osVersion, unused and must be set to zero
                                   * (4 bytes)
                                   */
        data.incrementPosition(2);/*
                                   * protocolMajorVersion, must be set to 1 (2
                                   * bytes)
                                   */
        int protocolMinorVersion = data.getLittleEndian16();
        int ioCode1 = data.getLittleEndian32();
        data.incrementPosition(4);/*
                                   * ioCode2, must be set to zero, reserved for
                                   * future use (4 bytes)
                                   */
        int extendedPDU = data.getLittleEndian32();
        int extraFlags1 = data.getLittleEndian32();
        data.incrementPosition(4);/*
                                   * extraFlags2, must be set to zero, reserved
                                   * for future use (4 bytes)
                                   */

        /*
         * SpecialTypeDeviceCap (4 bytes): present when
         * GENERAL_CAPABILITY_VERSION_02 is used
         */

        if (version == GENERAL_CAPABILITY_VERSION_02) {
            int specialTypeDeviceCap = data.getLittleEndian32();
        }

        return;
    }

    /* Process printer direction capability set */
    private void rdpdr_process_printer_capset(RdpPacket data) {
        int capabilityLength = data.getLittleEndian16();
        int version = data.getLittleEndian32();
    }

    /* Process port redirection capability set */
    private void rdpdr_process_port_capset(RdpPacket data) {
        int capabilityLength = data.getLittleEndian16();
        int version = data.getLittleEndian32();
    }

    private void rdpdr_process_drive_capset(RdpPacket data) {
        int capabilityLength = data.getLittleEndian16();
        int version = data.getLittleEndian32();
    }

    private void rdpdr_process_smartcard_capset(RdpPacket data) {
        int capabilityLength = data.getLittleEndian16();
        int version = data.getLittleEndian32();
    }
    
    private void rdpdr_send_capabilities() {
        RdpPacket_Localised s;

        s = new RdpPacket_Localised(0x54);
        s.setLittleEndian16(RDPDR_COMPONENT_TYPE_CORE);
        s.setLittleEndian16(PAKID_CORE_CLIENT_CAPABILITY);
        s.setLittleEndian16(5);// numCapabilities
        s.setLittleEndian16(0);// pad

        s.setLittleEndian16(CAP_GENERAL_TYPE);
        s.setLittleEndian16(44);
        s.setLittleEndian32(GENERAL_CAPABILITY_VERSION_02);
        s.setLittleEndian32(0);// osType, ignored on receipt
        s.setLittleEndian32(0);// osVersion, unused and must be set to zero
        s.setLittleEndian16(1); // protocolMajorVersion, must be set to 1
        s.setLittleEndian16(rdpdr_version_minor);// protocolMinorVersion
        s.setLittleEndian32(0x0000FFFF); // ioCode1
        s.setLittleEndian32(0); // ioCode2, must be set to zero, reserved for
                                // future use
        s.setLittleEndian32(RDPDR_DEVICE_REMOVE_PDUS
                | RDPDR_CLIENT_DISPLAY_NAME_PDU | RDPDR_USER_LOGGEDON_PDU); // extendedPDU
        s.setLittleEndian32(ENABLE_ASYNCIO); // extraFlags1
        s.setLittleEndian32(0); // extraFlags2, must be set to zero, reserved
                                // for future use5f7pre
        s.setLittleEndian32(0); /*
                                 * SpecialTypeDeviceCap, number of special
                                 * devices to be redirected before logon
                                 */

        s.setLittleEndian16(CAP_PRINTER_TYPE);
        s.setLittleEndian16(8);
        s.setLittleEndian32(PRINT_CAPABILITY_VERSION_01);

        s.setLittleEndian16(CAP_PORT_TYPE); /* third */
        s.setLittleEndian16(8); /* length */
        s.setLittleEndian32(PORT_CAPABILITY_VERSION_01);

        s.setLittleEndian16(CAP_DRIVE_TYPE); /* fourth */
        s.setLittleEndian16(8); /* length */
        s.setLittleEndian32(DRIVE_CAPABILITY_VERSION_01);

        s.setLittleEndian16(CAP_SMARTCARD_TYPE); /* fifth */
        s.setLittleEndian16(8); /* length */
        s.setLittleEndian32(SMARTCARD_CAPABILITY_VERSION_01);

        s.markEnd();

        try {
            this.send_packet(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int announcedata_size() {
        int size;
        size = 8; /* static announce size */
        size += devices.size() * 0x14;

        for (RdpdrDevice dev : devices) {
            if (dev.type == RDPDR_DTYP_PRINT) {
                size += dev.deviceData.size();
            }
        }

        return size;
    }

    private int rdpdr_send_device_list_announce_request() {
        RdpPacket_Localised s;

        s = new RdpPacket_Localised(announcedata_size());
        s.setLittleEndian16(RDPDR_COMPONENT_TYPE_CORE);
        s.setLittleEndian16(PAKID_CORE_DEVICELIST_ANNOUNCE);
        s.setLittleEndian32(devices.size()); /* deviceCount */

        for (RdpdrDevice dev : devices) {
            s.setLittleEndian32(dev.type); /* deviceType */
            s.setLittleEndian32(devices.indexOf(dev)); /* deviceID */
            /* preferredDosName, Max 8 characters, may not be null terminated */
            String name = dev.name.replace(" ", "_").substring(0,
                    dev.name.length() > 8 ? 8 : dev.name.length());
            s.copyFromByteArray(name.getBytes(), 0, s.getPosition(),
                    name.length());
            s.incrementPosition(8);

            s.setLittleEndian32(dev.deviceData.size());
            if (dev.deviceData.size() > 0) {
                s.copyFromPacket(dev.deviceData, 0, s.getPosition(),
                        dev.deviceData.size());
                s.incrementPosition(dev.deviceData.size());
            }
        }
        s.markEnd();
//        byte[] outputbyte = new byte[s.size()];
//        s.copyToByteArray(outputbyte, 0, 0, s.size());

        try {
            this.send_packet(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // devices list
    public void deviceRegister(RdpdrDevice newDevice) {
        int port = 1;
        if (newDevice.type == RDPDR_DTYP_PRINT) {
            for (RdpdrDevice dev : devices) {
                if (dev.type == RDPDR_DTYP_PRINT) {
                    port++;
                }
            }
        }
        newDevice.register("", port);
        devices.add(newDevice);
    }

    private void rdpdr_process_irp(RdpPacket data) {

        byte[] buffer = new byte[1];
        int buffer_len = 0;

        int deviceid = data.getLittleEndian32();
        int fileId = data.getLittleEndian32();
        int completionId = data.getLittleEndian32();
        int major = data.getLittleEndian32();
        int minor = data.getLittleEndian32();
        
        int status = RD_STATUS_SUCCESS;
        
        int info_level;
        
        int result = 0;

        RdpdrDevice device = devices.get(deviceid);
System.out.println("执行:" + major + ", fileId=" + fileId);
        switch (major) {
        case IRP_MJ_CREATE:
            
            buffer = new byte[1];
            
//            status = fns->create(device, desired_access, share_mode, disposition,
//                         flags_and_attributes, filename, &result);
            ResultHolder holder = new ResultHolder();
            
            try {
                status = device.create(data, holder);
            } catch (IOException e1) {
                status = RD_STATUS_INVALID_PARAMETER;
            }
            if(status == RD_STATUS_SUCCESS) {
                result = (Integer) holder.get("fileId");
            }
            
            buffer_len = 1;
            
            break;

        case IRP_MJ_CLOSE:
            status = device.close(fileId);
            buffer = new byte[1];
            buffer_len = 1;
            break;

        case IRP_MJ_READ:
            if(device instanceof DiskRdpdrDevice) {
                DiskRdpdrDevice ddevice = (DiskRdpdrDevice) device;
                
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bout);
                
                try {
                    status = ddevice.read(data, fileId, out);
                    
                    out.flush();
                    bout.flush();
                    
                    buffer = bout.toByteArray();
                } catch (IOException e) {
                    status = RD_STATUS_INVALID_PARAMETER;
                    e.printStackTrace();
                }
                
                result = buffer_len = buffer.length;
            } else {
                status = RD_STATUS_NOT_SUPPORTED;
            }
            break;
        case IRP_MJ_WRITE:
            if(device instanceof DiskRdpdrDevice) {
                DiskRdpdrDevice ddevice = (DiskRdpdrDevice) device;
                
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bout);
                
                try {
                    status = ddevice.write(data, fileId, out);
                    
                    out.flush();
                    bout.flush();
                    
                    buffer = bout.toByteArray();
                    result = buffer[0] + (buffer[1] << 8) + (buffer[2] << 16) + (buffer[3] << 24);
                    buffer_len = 1;
                    buffer = new byte[1];
                } catch (IOException e) {
                    status = RD_STATUS_INVALID_PARAMETER;
                    result = 0;
                    buffer_len = 1;
                    buffer = new byte[1];
                    e.printStackTrace();
                }
            } else {
                status = RD_STATUS_NOT_SUPPORTED;
            }
            break;

        case IRP_MJ_QUERY_INFORMATION:
            if(device instanceof DiskRdpdrDevice) {
                DiskRdpdrDevice ddevice = (DiskRdpdrDevice) device;
                
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bout);
                
                try {
                    status = ddevice.disk_query_information(data, fileId, out);
                    out.flush();
                    bout.flush();
                    buffer = bout.toByteArray();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                buffer_len = buffer.length;
                result = buffer_len;
            }
            break;

        case IRP_MJ_SET_INFORMATION:
            if(device instanceof DiskRdpdrDevice) {
                DiskRdpdrDevice ddevice = (DiskRdpdrDevice) device;
                
                status = ddevice.disk_set_information(data, fileId);
                buffer = new byte[1];
                result = buffer_len = 1;
            }
            break;

        case IRP_MJ_QUERY_VOLUME_INFORMATION:
            if(device instanceof DiskRdpdrDevice) {
                DiskRdpdrDevice ddevice = (DiskRdpdrDevice) device;
                
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bout);
                
                try {
                    status = ddevice.disk_query_volume_information(data, fileId, out);
                    out.flush();
                    bout.flush();
                    buffer = bout.toByteArray();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                result = buffer_len = buffer.length;
                
            }
            break;

        case IRP_MJ_DIRECTORY_CONTROL:
            if(device instanceof DiskRdpdrDevice) {
                DiskRdpdrDevice ddevice = (DiskRdpdrDevice) device;
                switch (minor) {
                case IRP_MN_QUERY_DIRECTORY:
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(bout);
                    try {
                        status = ddevice.queryDirectory(data, fileId, out);
                        out.flush();
                        bout.flush();
                        buffer = bout.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    buffer_len = buffer.length;
                    if(buffer_len == 0) {
                        buffer_len++;
                        buffer = new byte[1];
                    }
                    result = buffer_len;
                    
                    break;

                case IRP_MN_NOTIFY_CHANGE_DIRECTORY:
                    status = ddevice.disk_create_notify(data, fileId);
                    result = 0;
                    
                    break;
                default:
                    status = RD_STATUS_INVALID_PARAMETER;
                    break;
                }
                
            }
            break;

        case IRP_MJ_DEVICE_CONTROL:
            status = RD_STATUS_NOT_SUPPORTED;
            break;

        case IRP_MJ_LOCK_CONTROL:
            break;

        default:
            break;
        }
        if (status != RD_STATUS_PENDING) {
            rdpdr_send_completion(deviceid, completionId, status, result, buffer, buffer_len);
        }
    }

    void rdpdr_send_completion(int deviceId, int completionId, int status, int result,
            byte[] buffer, int length) {
        RdpPacket_Localised s;

        s = new RdpPacket_Localised(20 + length);
        s.setLittleEndian16(RDPDR_COMPONENT_TYPE_CORE);// PAKID_CORE_DEVICE_REPLY?
        s.setLittleEndian16(PAKID_CORE_DEVICE_IOCOMPLETION);
        s.setLittleEndian32(deviceId);
        s.setLittleEndian32(completionId);
        s.setLittleEndian32(status);
        s.setLittleEndian32(result);
        if (length > 0) {
            s.copyFromByteArray(buffer, 0, s.getPosition(), length);
        }
        s.markEnd();

        try {
            this.send_packet(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void send_packet(RdpPacket_Localised s) throws RdesktopException, IOException, CryptoException {
        super.send_packet(s);

        int size = s.capacity();
        byte[] dump = new byte[size];
        s.copyToByteArray(dump, 0, 0, s.size());
        System.out.print("\n"+(send_packet_index++)+"=======================>>>>>>>> data sent");
        System.out.println(HexDump.dumpHexString(dump));
    }

}
