package dev.mgu.standalone;

public final class UpdatePolicyTest {
    private static void require(boolean condition) { if (!condition) throw new AssertionError("Update policy regression"); }
    public static void main(String[] args) {
        String base="https://github.com/"+UpdatePolicy.REPOSITORY+"/releases/download/v0.10.0/";
        require(UpdatePolicy.assetUrl(base+"app.apk"));
        for(String bad:new String[]{"http://github.com/"+UpdatePolicy.REPOSITORY+"/releases/download/v0.10.0/app.apk",
                "https://github.com/statist1c/MGU-Store-Installer/releases/download/v0.10.0/app.apk",
                "https://example.com/app.apk",base+"../app.apk",base+"app.apk?token=x",base+"app.apk#x", "not a url"}) require(!UpdatePolicy.assetUrl(bad));
        require(UpdatePolicy.eligibleVersion("v0.10.0","0.9.0",false,false,false));
        require(!UpdatePolicy.eligibleVersion("v0.8.0","0.9.0",false,false,true));
        require(!UpdatePolicy.eligibleVersion("v0.9.0","0.9.0",false,false,true));
        require(!UpdatePolicy.eligibleVersion("v0.10.0","0.9.0",true,false,true));
        require(!UpdatePolicy.eligibleVersion("v0.10.0","0.9.0",false,true,false));
        require(UpdatePolicy.eligibleVersion("v0.10.0","0.9.0",false,true,true));
        require(!UpdatePolicy.eligibleVersion("v0.10.0-rc.1","0.9.0",false,false,false));
        require(UpdatePolicy.eligibleVersion("v0.10.0-rc.1","0.9.0",false,true,true));
        require(!UpdatePolicy.eligibleVersion("main","0.9.0",false,false,true));
        System.out.println("PASS: 17 repository-URL, release-channel and version policy cases");
    }
}
