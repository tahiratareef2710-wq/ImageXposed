package displayPackage.factory;

import displayPackage.forensics.ForensicsEngine;

public class MD5Analysis implements Analysis {
    private String filePath;

    public MD5Analysis(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public Object execute() {
        return ForensicsEngine.computeMD5(filePath);
    }
}