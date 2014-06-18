package net.propero.rdp.rdp5.snd;

import java.io.IOException;

import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.crypto.CryptoException;
import net.propero.rdp.rdp5.VChannel;
import net.propero.rdp.rdp5.VChannels;

public class SoundChannel extends VChannel {

    @Override
    public String name() {
        return "rdpsnd";
    }

    @Override
    public int flags() {
        return VChannels.CHANNEL_OPTION_INITIALIZED | VChannels.CHANNEL_OPTION_ENCRYPT_RDP;
    }

    @Override
    public void process(RdpPacket data) throws RdesktopException, IOException,
            CryptoException {
        // do nothing

    }

}
