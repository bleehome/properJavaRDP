package net.propero.rdp.rdp5.disk;

import java.io.DataOutputStream;

public class IRP {

    public int fileId;
    
    public int majorFunction;
    
    public int minorFunction; 

    public DataOutputStream out;
    
    public IRP(int fileId, int majorFunction, int minorFunction,
            DataOutputStream out) {
        super();
        this.fileId = fileId;
        this.majorFunction = majorFunction;
        this.minorFunction = minorFunction;
        this.out = out;
    }
    
}
