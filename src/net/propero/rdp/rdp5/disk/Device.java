package net.propero.rdp.rdp5.disk;

import java.io.IOException;

import net.propero.rdp.RdpPacket;
import net.propero.rdp.rdp5.VChannel;

public interface Device {

    public int getType();
    public String getName();
    public void setChannel(VChannel channel);
    
    public int process(RdpPacket data, IRP irp) throws IOException;
    
}
