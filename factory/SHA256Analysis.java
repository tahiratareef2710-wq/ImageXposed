package displayPackage.factory;

import displayPackage.forensics.ForensicsEngine;

public class SHA256Analysis implements Analysis {
    private String filePath;

    public SHA256Analysis(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public Object execute() {
        return ForensicsEngine.computeSHA256(filePath);
    }
}