package org.nqm.command;

import java.util.Properties;
import picocli.CommandLine.IVersionProvider;

public class GisVersion implements IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        var p = new Properties();
        var gisVersion = "";
        var commitHash = "";
        try (var gisConfigs = this.getClass().getClassLoader().getResourceAsStream(".properties")) {
            p.load(gisConfigs);
            gisVersion = p.getProperty("gis.version");
            commitHash = p.getProperty("git.commit.hash");
        }
        return new String[] {"gis " + gisVersion, "commit " + commitHash};
    }
}
