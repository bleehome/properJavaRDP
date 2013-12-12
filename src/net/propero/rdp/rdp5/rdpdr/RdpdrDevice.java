package net.propero.rdp.rdp5.rdpdr;

import java.io.DataOutputStream;
import java.io.IOException;

import net.propero.rdp.RdpPacket;
import net.propero.rdp.RdpPacket_Localised;

public abstract class RdpdrDevice {
    public String name;
    public int type;
    RdpPacket_Localised deviceData;
    
    public RdpdrDevice(int type) {
            super();
            this.type = type;
    }
    
    abstract public void register(String optarg, int port);
    abstract public int create(RdpPacket data, ResultHolder holder) throws IOException;
    abstract public int write(RdpPacket data, int fileId, DataOutputStream out) throws IOException;
    abstract public int close(int fileId);
}
