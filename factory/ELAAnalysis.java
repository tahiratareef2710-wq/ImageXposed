package displayPackage.factory;

import displayPackage.forensics.ForensicsEngine;
import displayPackage.forensics.ELAResult;

public class ELAAnalysis implements Analysis {
    private String filePath;

    public ELAAnalysis(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public Object execute() {
        return ForensicsEngine.performELA(filePath);
    }
}