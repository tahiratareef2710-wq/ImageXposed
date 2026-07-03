package displayPackage.factory;

public class AnalysisFactory {

    public static Analysis createAnalysis(String type, String filePath) {
        switch (type.toLowerCase()) {
            case "md5":
                return new MD5Analysis(filePath);
            case "sha256":
                return new SHA256Analysis(filePath);
            case "ela":
                return new ELAAnalysis(filePath);
            default:
                throw new IllegalArgumentException("Unknown analysis type: " + type);
        }
    }
}